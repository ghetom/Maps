package com.ghetom.maps;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NavigationManager {

    private NavType navType;
    private String baseUrl;
    private String finalAddress;
    private boolean isRedirected;
    private static final String LAT = "$latitude";
    private static final String LON = "$longitude";

    public NavigationManager(NavType navType) {
        this.navType = navType;
        this.isRedirected = false;
        setNavType(navType);
    }

    public void setNavType(NavType navType){
        switch (navType){
            case WAZE:
                baseUrl= "https://waze.com/ul?ll=" + LAT + "%2C" + LON + "&amp;navigate=yes";
                break;
            case MAGIC_EARTH:
                baseUrl= "magicearth://?drive_to&lat=" + LAT + "&lon=" + LON;
                break;
            default:
                baseUrl = "";
        }
    }

    public boolean isRedirected() {
        return isRedirected;
    }

    public String redirect(String request)
    {
        Log.d("NavigationManager", "Request: " + request);
        String currentUrl = baseUrl;

        String start = "link=";
        String end = "data";
        String decodedRequest = decode(request,start,end);

        start = "dir//";
        end = "/@";
        int startIndex = decodedRequest.indexOf(start) + start.length();
        int endIndex = decodedRequest.indexOf(end) ;

        LinkedList<String> addressData = new LinkedList<>();
        Collections.addAll(addressData, decodedRequest.substring(startIndex, endIndex).split(","));

        if(addressData.size() > 0 && addressData.size() <= 3) {
            if (addressData.size() == 3) {
                addressData.removeFirst();
            }

            String address = String.join("",addressData);
            address += " " + Locale.getDefault().getCountry();

            Map<String,Double> coordinates = getCoordinates(address);

            if(!coordinates.isEmpty()) {
                Double lon = coordinates.get(OpenStreetMapUtils.lon);
                currentUrl = currentUrl.replace(LON, Double.toString(lon));
                Double lat = coordinates.get(OpenStreetMapUtils.lat);
                currentUrl = currentUrl.replace(LAT, Double.toString(lat));
                this.isRedirected = true;
            }
        }
        return currentUrl;
    }

    public Map<String, Double> getCoordinates(String address) {
        Map<String, Double> coordinates = null;
        finalAddress = address;
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OpenStreetMapUtils osmUtils = OpenStreetMapUtils.getInstance();
                    Map<String, Double> coordinates = osmUtils.getCoordinates(finalAddress);
                    completableFuture.complete(coordinates);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        try {
            coordinates = (Map<String, Double>) completableFuture.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("NavigationManager", "Coordinates: " + coordinates.values());
        
        return coordinates;
    }

    private String decode (String url, String start, String end){
        int startIndexOfUrl = url.indexOf(start);
        int endIndexOfUrl = url.indexOf(end);
        url = url.substring(startIndexOfUrl, endIndexOfUrl).substring(start.length());
        String decodedRequest = "";
        try {
            decodedRequest = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        }catch(Exception e){
            e.printStackTrace();
        }
        Log.d("NavigationManager", "Decoded url: " + decodedRequest);
        return decodedRequest;
    }
}
