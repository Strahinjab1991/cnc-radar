package com.cncmikron.radar.android;

import android.content.Context;

import com.cncmikron.radar.core.Camera;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista kamera za rad bez interneta.
 * Prvo se čita keš (kamere.json u internoj memoriji), a ako ga nema, uzima se ugrađena lista iz assets-a.
 * Keš se piše pri osvježavanju iz OpenStreetMap-a (OverpassClient).
 */
public final class CameraStore {
    private static final String KESH = "kamere.json";
    private static final String ASSET = "kamere.json";

    private CameraStore() {}

    public static List<Camera> ucitaj(Context ctx) {
        try {
            File f = new File(ctx.getFilesDir(), KESH);
            if (f.exists()) return parsiraj(citaj(new FileInputStream(f)));
            return parsiraj(citaj(ctx.getAssets().open(ASSET)));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Snima sirovi Overpass odgovor u keš. */
    public static void sacuvaj(Context ctx, String sirovi) throws IOException {
        try (FileOutputStream out = new FileOutputStream(new File(ctx.getFilesDir(), KESH))) {
            out.write(sirovi.getBytes("UTF-8"));
        }
    }

    /** Čita Overpass odgovor ({"elements":[{lat,lon,id,tags}]}) ili prosti niz iz assets-a. */
    public static List<Camera> parsiraj(String json) throws Exception {
        List<Camera> out = new ArrayList<>();
        JSONArray niz;
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            niz = new JSONArray(trimmed);
        } else {
            niz = new JSONObject(trimmed).optJSONArray("elements");
            if (niz == null) return out;
        }
        for (int i = 0; i < niz.length(); i++) {
            JSONObject o = niz.getJSONObject(i);
            if (!o.has("lat") || !o.has("lon")) continue;
            String id = String.valueOf(o.optString("id", String.valueOf(i)));
            String ime = "";
            JSONObject tags = o.optJSONObject("tags");
            if (tags != null) ime = tags.optString("name", "");
            out.add(new Camera(id, o.getDouble("lat"), o.getDouble("lon"), ime));
        }
        return out;
    }

    private static String citaj(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        }
    }
}
