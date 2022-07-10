package com.ghetom.maps;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OpenStreetMapUtils {

    private static OpenStreetMapUtils instance;
    private String baseUrl;
    private String urlFormat;

    //API elements
    public static final String lon = "lon";
    public static final String lat = "lat";

    private OpenStreetMapUtils() {
        instance = null;
        baseUrl= "https://nominatim.openstreetmap.org/search?q=";
        urlFormat = "&format=json&addressdetails=1";
    }

    public static OpenStreetMapUtils getInstance() {
        if (instance == null) {
            instance = new OpenStreetMapUtils();
        }
        return instance;
    }

    private String getRequest(String url) {
        try {
            final URL obj = new URL(url);
            final HttpURLConnection con;

            con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");

            if (con.getResponseCode() != 200) {
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public Map<String, Double> getCoordinates(String address) {
        Map<String, Double> res;
        StringBuffer query;
        String[] split = address.split(" ");
        String queryResult = null;

        query = new StringBuffer();
        res = new HashMap<String, Double>();

        query.append(baseUrl);

        if (split.length == 0) {
            return null;
        }

        for (int i = 0; i < split.length; i++) {
            query.append(split[i]);
            if (i < (split.length - 1)) {
                query.append("+");
            }
        }
        query.append(urlFormat);

        Log.d("OpenStreetMapUtils", "API query: " + query);

        try {
            queryResult = getRequest(query.toString());
            Log.d("OpenStreetMapUtils", "API result: " + queryResult);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (queryResult == null) {
            return null;
        }

        JsonArray json = (JsonArray) JsonParser.parseString(queryResult);
        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            if (array.size() > 0) {
                try {
                    JsonObject el = (JsonObject) array.get(0);

                    String longitude = el.get(lon).toString();
                    longitude = longitude.substring(1,longitude.length()-2);
                    res.put(lon, Double.parseDouble(longitude));

                    String latitude = el.get(lat).toString();
                    latitude = latitude.substring(1,latitude.length()-2);
                    res.put(lat, Double.parseDouble(latitude));
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return res;
    }
}
