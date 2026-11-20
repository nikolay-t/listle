package org.inline.listle;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PagesStore {
    public static final String[] PRIORITIES = {"!!", "!", "!?", "?", "+"};
    public static final int DEFAULT_PRIORITY_INDEX = 4; // "+"
    public static final String DEFAULT_PAGE_NAME = "default";
    private static final String KEY_ITEMS = "items";

    public static class Page {
        public String name;
        public List<ListleItem> items;

        public Page(String name) {
            this.name = name;
            this.items = new ArrayList<>();
        }
    }

    public static List<Page> load(SharedPreferences prefs) {
        List<Page> pages = new ArrayList<>();
        String itemsJson = prefs.getString(KEY_ITEMS, null);
        if (itemsJson != null && !itemsJson.isEmpty()) {
            try {
                JSONArray foldersArray = new JSONArray(itemsJson);
                for (int i = 0; i < foldersArray.length(); i++) {
                    JSONObject folderObj = foldersArray.getJSONObject(i);
                    Page page = new Page(folderObj.optString("name", DEFAULT_PAGE_NAME));
                    JSONArray itemsArray = folderObj.optJSONArray("items");
                    if (itemsArray != null) {
                        for (int j = 0; j < itemsArray.length(); j++) {
                            try {
                                JSONObject obj = itemsArray.getJSONObject(j);
                                String name = obj.getString("name");
                                page.items.add(new ListleItem(name, parsePriority(obj)));
                            } catch (Exception e) {
                                // Skip malformed item
                            }
                        }
                    }
                    pages.add(page);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (pages.isEmpty()) {
            pages.add(new Page(DEFAULT_PAGE_NAME));
        }
        return pages;
    }

    private static int parsePriority(JSONObject obj) {
        try {
            String priorityStr = obj.getString("priority");
            int index = getPriorityIndex(priorityStr);
            if (index >= 0) {
                return index;
            }
        } catch (Exception e) {
            // Not a string, try as integer below
        }
        try {
            int priorityInt = obj.getInt("priority");
            if (priorityInt >= 0 && priorityInt < PRIORITIES.length) {
                return priorityInt;
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    public static int getPriorityIndex(String priority) {
        for (int i = 0; i < PRIORITIES.length; i++) {
            if (PRIORITIES[i].equals(priority)) {
                return i;
            }
        }
        return -1;
    }

    public static void save(SharedPreferences prefs, List<Page> pages) {
        try {
            JSONArray foldersArray = new JSONArray();
            for (Page page : pages) {
                JSONObject folderObj = new JSONObject();
                folderObj.put("name", page.name);
                JSONArray itemsArray = new JSONArray();
                for (ListleItem item : page.items) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", item.getName());
                    obj.put("priority", PRIORITIES[item.getPriority()]);
                    itemsArray.put(obj);
                }
                folderObj.put("items", itemsArray);
                foldersArray.put(folderObj);
            }
            prefs.edit().putString(KEY_ITEMS, foldersArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
