package com.cncmikron.radar.android;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cncmikron.radar.R;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView status, info;
    private Button btnToggle, btnRefresh;
    private final Handler glavna = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        info = findViewById(R.id.info);
        btnToggle = findViewById(R.id.btnToggle);
        btnRefresh = findViewById(R.id.btnRefresh);

        btnToggle.setOnClickListener(v -> {
            if (AlertService.radi) {
                startService(new Intent(this, AlertService.class).setAction(AlertService.AKCIJA_STOP));
            } else {
                trazDozvole();
            }
            glavna.postDelayed(this::osvjezi, 400);
        });
        btnRefresh.setOnClickListener(v -> osvjeziKamere());
        osvjezi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        osvjezi();
    }

    private void osvjezi() {
        boolean radi = AlertService.radi;
        status.setText(radi ? R.string.service_running : R.string.service_stopped);
        btnToggle.setText(radi ? R.string.btn_stop : R.string.btn_start);
        int n = CameraStore.ucitaj(this).size();
        info.setText("Kamera u bazi: " + n + "\nRadi i bez interneta.");
    }

    private void trazDozvole() {
        String[] potrebne = Build.VERSION.SDK_INT >= 33
                ? new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS}
                : new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(this, potrebne, 10);
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] dozvole, int[] rezultati) {
        super.onRequestPermissionsResult(kod, dozvole, rezultati);
        if (kod != 10) return;
        boolean lokacija = rezultati.length > 0 && rezultati[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (lokacija) {
            Intent i = new Intent(this, AlertService.class).setAction(AlertService.AKCIJA_START);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            Toast.makeText(this, "Upozorenja su uključena", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Treba dozvola za lokaciju", Toast.LENGTH_LONG).show();
        }
        glavna.postDelayed(this::osvjezi, 400);
    }

    private void osvjeziKamere() {
        btnRefresh.setEnabled(false);
        info.setText("Preuzimam kamere sa interneta…");
        new Thread(() -> {
            String poruka;
            try {
                String odgovor = OverpassClient.preuzmi();
                List<com.cncmikron.radar.core.Camera> lista = CameraStore.parsiraj(odgovor);
                CameraStore.sacuvaj(this, odgovor);
                poruka = "Preuzeto kamera: " + lista.size();
            } catch (Exception e) {
                poruka = "Nije uspjelo (provjerite internet): " + e.getMessage();
            }
            String konacna = poruka;
            glavna.post(() -> {
                btnRefresh.setEnabled(true);
                Toast.makeText(this, konacna, Toast.LENGTH_LONG).show();
                osvjezi();
            });
        }).start();
    }
}
