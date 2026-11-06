package com.cncmikron.radar.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.cncmikron.radar.R;
import com.cncmikron.radar.core.AlertEngine;
import com.cncmikron.radar.core.Camera;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

/**
 * Radi u pozadini (foreground servis): prati lokaciju, računa upozorenja i javlja ih
 * obavijesti + glasom. Ne treba internet; kamere su u kešu ili ugrađene.
 */
public class AlertService extends Service {
    public static final String AKCIJA_START = "start";
    public static final String AKCIJA_STOP = "stop";
    private static final String KANAL = "upozorenja";
    private static final int ID_OBAVJESTI = 1;
    private static final int ID_UPOZORENJA = 2;

    private FusedLocationProviderClient lokacija;
    private LocationCallback povratni;
    private AlertEngine motor;
    private TextToSpeech glas;
    private boolean glasSpreman;

    public static boolean radi = false;

    @Override
    public void onCreate() {
        super.onCreate();
        lokacija = LocationServices.getFusedLocationProviderClient(this);
        napraviKanal();
        glas = new TextToSpeech(this, status -> {
            glasSpreman = status == TextToSpeech.SUCCESS;
            if (glasSpreman) {
                // BCS jezik nema uvijek glas na uređaju; hrvatski je najbliži
                int r = glas.setLanguage(new Locale("hr", "HR"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    glas.setLanguage(Locale.ENGLISH);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && AKCIJA_STOP.equals(intent.getAction())) {
            zaustavi();
            stopSelf();
            return START_NOT_STICKY;
        }
        List<Camera> kamere = CameraStore.ucitaj(this);
        motor = new AlertEngine(kamere);
        Notification n = napraviObavijest("Pratim " + kamere.size() + " kamera u regionu");
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID_OBAVJESTI, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(ID_OBAVJESTI, n);
        }
        pokreniLokaciju();
        radi = true;
        return START_STICKY;
    }

    private void pokreniLokaciju() {
        LocationRequest zahtjev = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();
        povratni = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult rezultat) {
                Location l = rezultat.getLastLocation();
                if (l == null || motor == null) return;
                List<AlertEngine.Alert> upozorenja = motor.onPosition(l.getLatitude(), l.getLongitude(), System.currentTimeMillis());
                for (AlertEngine.Alert u : upozorenja) prikaziUpozorenje(u);
            }
        };
        try {
            lokacija.requestLocationUpdates(zahtjev, povratni, Looper.getMainLooper());
        } catch (SecurityException e) {
            stopSelf();
        }
    }

    private void prikaziUpozorenje(AlertEngine.Alert u) {
        long metara = Math.round(u.distanceM / 50.0) * 50;
        String tekst = "Kamera za " + metara + " metara";
        if (u.camera.name != null && !u.camera.name.isEmpty()) tekst += ": " + u.camera.name;

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, KANAL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Upozorenje")
                .setContentText(tekst)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        nm.notify(ID_UPOZORENJA, b.build());

        if (glasSpreman) glas.speak(tekst, TextToSpeech.QUEUE_ADD, null, "k" + u.camera.id);
    }

    private Notification napraviObavijest(String tekst) {
        return new NotificationCompat.Builder(this, KANAL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("CNC Radar")
                .setContentText(tekst)
                .setOngoing(true)
                .build();
    }

    private void napraviKanal() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel k = new NotificationChannel(KANAL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(k);
        }
    }

    private void zaustavi() {
        if (lokacija != null && povratni != null) lokacija.removeLocationUpdates(povratni);
        radi = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override
    public void onDestroy() {
        zaustavi();
        if (glas != null) glas.shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
