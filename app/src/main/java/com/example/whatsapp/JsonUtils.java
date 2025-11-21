package com.example.whatsapp;

import org.json.JSONObject;

public class JsonUtils {
    public static String extractImageUrl(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONObject data = obj.getJSONObject("data");
            return data.getString("url"); // key chính xác là "url"
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
