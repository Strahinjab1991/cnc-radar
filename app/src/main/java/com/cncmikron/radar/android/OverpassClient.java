package com.cncmikron.radar.android;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Preuzima kamere sa nadzorom brzine iz OpenStreetMap-a (Overpass API) za zadane države.
 * Poziva se samo na zahtjev korisnika i samo kad postoji internet.
 */
public final class OverpassClient {
    private static final String URL = "https://overpass-api.de/api/interpreter";

    // BiH, Srbija, Hrvatska, Crna Gora, Slovenija
    static final String UPIT =
            "[out:json][timeout:90];"
          + "(area[\"ISO3166-1\"=\"BA\"];area[\"ISO3166-1\"=\"RS\"];area[\"ISO3166-1\"=\"HR\"];"
          + "area[\"ISO3166-1\"=\"ME\"];area[\"ISO3166-1\"=\"SI\"];)->.a;"
          + "node[\"highway\"=\"speed_camera\"](area.a);"
          + "out body;";

    private OverpassClient() {}

    public static String preuzmi() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(URL).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(120000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        // Overpass prima upit kao polje "data"
        byte[] tijelo = ("data=" + URLEncoder.encode(UPIT, "UTF-8")).getBytes("UTF-8");
        try (OutputStream os = c.getOutputStream()) { os.write(tijelo); }
        if (c.getResponseCode() != 200) throw new Exception("Overpass HTTP " + c.getResponseCode());
        try (InputStream is = c.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        }
    }
}
