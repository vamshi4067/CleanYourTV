package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class CricketTitleStore {
    private final SharedPreferences prefs;

    CricketTitleStore(Context context, String name) {
        prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    List<CricketCatalog.Clip> items() {
        List<CricketCatalog.Clip> saved = new ArrayList<>();
        try {
            JSONArray list = new JSONArray(prefs.getString("titles", "[]"));
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                if (item == null) continue;
                String title = item.optString("title");
                String url = item.optString("url");
                String app = item.optString("app");
                if (title.isEmpty() || !(url.startsWith("https://") || url.startsWith("http://"))
                        || !app.matches("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+")) continue;
                saved.add(new CricketCatalog.Clip(item.optInt("photo"), item.optInt("color", 0xff83bcff),
                        item.optString("player"), title, url, app));
            }
        } catch (JSONException unreadable) {
            // Ignore malformed saved data so Home remains usable.
        }
        return saved;
    }

    boolean save(CricketCatalog.Clip clip) {
        List<CricketCatalog.Clip> saved = items();
        for (CricketCatalog.Clip item : saved) if (same(item, clip)) return false;
        saved.add(clip);
        write(saved);
        return true;
    }

    void remove(CricketCatalog.Clip clip) {
        List<CricketCatalog.Clip> saved = items();
        saved.removeIf(item -> same(item, clip));
        write(saved);
    }

    boolean importTitles(List<CricketCatalog.Clip> titles) {
        java.util.LinkedHashMap<String, CricketCatalog.Clip> merged = new java.util.LinkedHashMap<>();
        for (CricketCatalog.Clip clip : items()) merged.put(clip.app + "\n" + clip.url, clip);
        for (CricketCatalog.Clip clip : titles) merged.putIfAbsent(clip.app + "\n" + clip.url, clip);
        return write(new ArrayList<>(merged.values()), true);
    }

    private static boolean same(CricketCatalog.Clip left, CricketCatalog.Clip right) {
        return left.app.equals(right.app) && left.url.equals(right.url);
    }

    private void write(List<CricketCatalog.Clip> saved) {
        write(saved, false);
    }

    private boolean write(List<CricketCatalog.Clip> saved, boolean synchronous) {
        JSONArray list = new JSONArray();
        try {
            for (CricketCatalog.Clip clip : saved) {
                list.put(new JSONObject().put("photo", clip.photo).put("color", clip.color)
                        .put("player", clip.player).put("title", clip.title)
                        .put("url", clip.url).put("app", clip.app));
            }
        } catch (JSONException invalid) {
            throw new IllegalStateException("Could not save titles", invalid);
        }
        SharedPreferences.Editor editor = prefs.edit().putString("titles", list.toString());
        if (synchronous) return editor.commit();
        editor.apply();
        return true;
    }
}
