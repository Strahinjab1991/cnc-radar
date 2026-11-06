package com.cncmikron.radar.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pravila upozorenja, bez Android zavisnosti.
 *
 * - Upozorenje se šalje kad vozač uđe u NEAR_RADIUS_M oko kamere.
 * - Ista kamera se ponovo ne najavljuje dok vozač ne izađe van REARM_RADIUS_M
 *   i dok ne prođe COOLDOWN_MS od zadnjeg upozorenja.
 */
public final class AlertEngine {

    public static final double NEAR_RADIUS_M = 500.0;
    public static final double REARM_RADIUS_M = 800.0;
    public static final long COOLDOWN_MS = 10 * 60 * 1000L;

    public static final class Alert {
        public final Camera camera;
        public final double distanceM;

        Alert(Camera camera, double distanceM) {
            this.camera = camera;
            this.distanceM = distanceM;
        }
    }

    private final List<Camera> cameras;
    private final Map<String, Boolean> inside = new HashMap<>();
    private final Map<String, Long> lastAlertAt = new HashMap<>();

    public AlertEngine(List<Camera> cameras) {
        this.cameras = new ArrayList<>(cameras);
    }

    /** Vraća upozorenja za trenutnu poziciju. Poziva se pri svakom očitavanju lokacije. */
    public List<Alert> onPosition(double lat, double lon, long nowMs) {
        List<Alert> out = new ArrayList<>();
        for (Camera c : cameras) {
            double d = Geo.udaljenostM(lat, lon, c.lat, c.lon);
            boolean wasInside = inside.getOrDefault(c.id, false);

            if (d > REARM_RADIUS_M) {
                inside.put(c.id, false);
                continue;
            }
            if (d <= NEAR_RADIUS_M && !wasInside) {
                Long last = lastAlertAt.get(c.id);
                if (last == null || nowMs - last >= COOLDOWN_MS) {
                    out.add(new Alert(c, d));
                    lastAlertAt.put(c.id, nowMs);
                }
                inside.put(c.id, true);
            }
        }
        return out;
    }
}
