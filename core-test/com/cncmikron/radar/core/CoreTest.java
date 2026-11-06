package com.cncmikron.radar.core;

import java.util.Arrays;
import java.util.List;

public class CoreTest {
    static int pad = 0, fail = 0;

    static void check(String ime, boolean uslov) {
        if (uslov) { System.out.println("✓ " + ime); pad++; }
        else { System.out.println("✗ " + ime); fail++; }
    }

    // pomak u metrima po geografskoj širini (1 stepen ≈ 111.195 km)
    static double lat(double baza, double metara) { return baza + metara / 111195.0; }

    public static void main(String[] args) {
        double baza = 44.8700; // Gradiška okolina
        double lonBaza = 17.2500;
        Camera k = new Camera("k1", baza, lonBaza, "Kamera 1");
        List<Camera> kamere = Arrays.asList(k);

        // 1) udaljenost
        double d = Geo.udaljenostM(0, 0, 1, 0);
        check("haversine: 1 stepen geo. širine ≈ 111.2 km", Math.abs(d - 111195) < 200);
        check("haversine: ista tačka = 0 m", Geo.udaljenostM(baza, lonBaza, baza, lonBaza) < 0.001);

        // 2) ulazak u zonu upozorava jednom
        AlertEngine e = new AlertEngine(kamere);
        long t = 0;
        List<AlertEngine.Alert> a1 = e.onPosition(lat(baza, 900), lonBaza, t);
        check("van zone (900 m): nema upozorenja", a1.isEmpty());
        List<AlertEngine.Alert> a2 = e.onPosition(lat(baza, 400), lonBaza, t += 3000);
        check("ulazak u 400 m: jedno upozorenje", a2.size() == 1);
        check("upozorenje ima tačnu udaljenost (~400 m)", a2.size() == 1 && Math.abs(a2.get(0).distanceM - 400) < 2);
        List<AlertEngine.Alert> a3 = e.onPosition(lat(baza, 200), lonBaza, t += 3000);
        check("približavanje u zoni: ne ponavlja upozorenje", a3.isEmpty());
        List<AlertEngine.Alert> a4 = e.onPosition(lat(baza, 50), lonBaza, t += 3000);
        check("prolazak kroz kameru: ne ponavlja upozorenje", a4.isEmpty());

        // 3) izlazak van zone i povratak PRIJE isteka cooldowna: bez upozorenja
        e.onPosition(lat(baza, 1000), lonBaza, t += 3000);
        List<AlertEngine.Alert> a5 = e.onPosition(lat(baza, 300), lonBaza, t += 60_000);
        check("povratak prije cooldowna (1 min): bez upozorenja", a5.isEmpty());

        // 4) izlazak van zone i povratak POSLIJE cooldowna: ponovo upozorava
        e = new AlertEngine(kamere);
        t = 0;
        e.onPosition(lat(baza, 300), lonBaza, t);
        e.onPosition(lat(baza, 1000), lonBaza, t += 60_000);
        List<AlertEngine.Alert> a6 = e.onPosition(lat(baza, 300), lonBaza, t += AlertEngine.COOLDOWN_MS);
        check("povratak poslije cooldowna: upozorava ponovo", a6.size() == 1);

        // 5) između 500 m i 800 m (histereza): ne re-arma se
        e = new AlertEngine(kamere);
        t = 0;
        e.onPosition(lat(baza, 300), lonBaza, t);
        e.onPosition(lat(baza, 650), lonBaza, t += AlertEngine.COOLDOWN_MS);
        List<AlertEngine.Alert> a7 = e.onPosition(lat(baza, 300), lonBaza, t += 1000);
        check("zona histereze (650 m): nije re-armovano", a7.isEmpty());

        // 6) više kamera: upozorava samo one u zoni
        Camera daleko = new Camera("k2", baza + 0.05, lonBaza, "Daleko");
        AlertEngine e2 = new AlertEngine(Arrays.asList(k, daleko));
        List<AlertEngine.Alert> a8 = e2.onPosition(lat(baza, 100), lonBaza, 0);
        check("dvije kamere, samo bliža upozorava", a8.size() == 1 && a8.get(0).camera.id.equals("k1"));

        // 7) prazna lista kamera
        check("bez kamera: nema upozorenja", new AlertEngine(new java.util.ArrayList<Camera>()).onPosition(baza, lonBaza, 0).isEmpty());

        System.out.println(fail == 0 ? "\nSvi testovi prošli (" + pad + ")" : "\n" + fail + " neuspjelo");
        if (fail > 0) System.exit(1);
    }
}
