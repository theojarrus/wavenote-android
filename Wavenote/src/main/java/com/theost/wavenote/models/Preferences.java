package com.theost.wavenote.models;

import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Preferences extends BucketObject {
    public static final String BUCKET_NAME = "preferences";
    public static final String PREFERENCES_OBJECT_KEY = "preferences-key";
    public static final int MAX_RECENT_SEARCHES = 5;
    private static final String RECENT_SEARCHES_KEY = "recent_searches";

    private Preferences(String key, JSONObject properties) {
        super(key, properties);
    }

    public ArrayList<String> getRecentSearches() {
        JSONArray recents = (JSONArray) getProperty(RECENT_SEARCHES_KEY);

        if (recents == null) {
            recents = new JSONArray();
        }

        ArrayList<String> recentsList = new ArrayList<>(recents.length());

        for (int i = 0; i < recents.length(); i++) {
            String recent = recents.optString(i);

            if (!recent.isEmpty()) {
                recentsList.add(recent);
            }
        }

        return recentsList;
    }

    public void setRecentSearches(List<String> recents) {
        if (recents == null) {
            recents = new ArrayList<>();
        }

        setProperty(RECENT_SEARCHES_KEY, new JSONArray(recents));
    }

    public static class Schema extends BucketSchema<Preferences> {

        public Schema() {
            autoIndex();
        }

        public String getRemoteName() {
            return BUCKET_NAME;
        }

        public Preferences build(String key, JSONObject properties) {
            return new Preferences(key, properties);
        }

        public void update(Preferences prefs, JSONObject properties) {
            prefs.setProperties(properties);
        }
    }
}