/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.localepicker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * A data manager that manages the {@link SharedPreferences} for the locale notification
 * information.
 */
public class LocaleNotificationDataManager {
    public static final String LOCALE_NOTIFICATION = "locale_notification";
    private Context mContext;

    /**
     * Constructor
     *
     * @param context The context
     */
    public LocaleNotificationDataManager(Context context) {
        this.mContext = context;
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(LOCALE_NOTIFICATION, Context.MODE_PRIVATE);
    }

    /**
     * Adds one entry with the corresponding locale and {@link NotificationInfo} to the
     * {@link SharedPreferences}.
     *
     * @param locale A locale which the application sets to
     * @param info   The notification metadata
     */
    public void putNotificationInfo(String locale, NotificationInfo info) {
        Gson gson = new Gson();
        String json = gson.toJson(info);
        SharedPreferences.Editor editor = getSharedPreferences(mContext).edit();
        editor.putString(locale, json);
        editor.apply();
    }

    /**
     * Gets the {@link NotificationInfo} with the associated locale from the
     * {@link SharedPreferences}.
     *
     * @param locale A locale which the application sets to
     * @return {@link NotificationInfo}
     */
    public NotificationInfo getNotificationInfo(String locale) {
        Gson gson = new Gson();
        String json = getSharedPreferences(mContext).getString(locale, "");
        return json.isEmpty() ? null : gson.fromJson(json, NotificationInfo.class);
    }

    /**
     * Gets the locale notification map.
     *
     * @return A map which maps the locale to the corresponding {@link NotificationInfo}
     */
    public Map<String, NotificationInfo> getLocaleNotificationInfoMap() {
        Gson gson = new Gson();
        Map<String, String> map = (Map<String, String>) getSharedPreferences(mContext).getAll();
        Map<String, NotificationInfo> result = new HashMap<>(map.size());
        map.forEach((key, value) -> {
            result.put(key, gson.fromJson(value, NotificationInfo.class));
        });
        return result;
    }

    /**
     * Clears the locale notification map.
     */
    @VisibleForTesting
    void clearLocaleNotificationMap() {
        getSharedPreferences(mContext).edit().clear().apply();
    }
}
