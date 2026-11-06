package com.cncmikron.radar.core;

/** Jedna kamera ili radar sa koordinatama. */
public final class Camera {
    public final String id;
    public final double lat;
    public final double lon;
    public final String name;

    public Camera(String id, double lat, double lon, String name) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.name = name == null ? "" : name;
    }
}
