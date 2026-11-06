package com.cncmikron.radar.core;

/** Računanje udaljenosti na površini Zemlje (haversine). */
public final class Geo {
    private static final double R_METARA = 6371000.0;

    private Geo() {}

    public static double udaljenostM(double lat1, double lon1, double lat2, double lon2) {
        double f1 = Math.toRadians(lat1), f2 = Math.toRadians(lat2);
        double df = Math.toRadians(lat2 - lat1), dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(df / 2) * Math.sin(df / 2)
                + Math.cos(f1) * Math.cos(f2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * R_METARA * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
