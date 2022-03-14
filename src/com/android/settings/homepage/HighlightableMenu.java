/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.homepage;

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_HIGHLIGHTABLE_MENU_KEY;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_KEY;

import android.annotation.XmlRes;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *  Class for mapping highlightable menu keys and preference keys
 */
public class HighlightableMenu {
    private static final String TAG = "HighlightableMenu";

    /**
     * Map from highlightable menu key to preference key.
     */
    private static final Map<String, String> MENU_TO_PREFERENCE_KEY_MAP;

    /**
     * Map from old menu key to current key string id.
     */
    private static final Map<String, Integer> MENU_KEY_COMPAT_MAP;

    private static boolean sXmlParsed;

    static {
        MENU_TO_PREFERENCE_KEY_MAP = new ArrayMap<>();
        MENU_KEY_COMPAT_MAP = new ArrayMap<>();

        // Manual mapping for platform compatibility, e.g.
        //  MENU_KEY_COMPAT_MAP.put("top_level_apps_and_notifs", R.string.menu_key_apps);
    }

    /** Parses the highlightable menu keys from xml */
    public static synchronized void fromXml(Context context, @XmlRes int xmlResId) {
        if (sXmlParsed) {
            return;
        }

        Log.d(TAG, "parsing highlightable menu from xml");
        final List<Bundle> preferenceMetadata;
        try {
            preferenceMetadata = PreferenceXmlParserUtils.extractMetadata(context, xmlResId,
                    MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_NEED_HIGHLIGHTABLE_MENU_KEY);
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Failed to parse preference xml for getting highlightable menu keys", e);
            return;
        }

        for (Bundle metadata : preferenceMetadata) {
            final String menuKey = metadata.getString(METADATA_HIGHLIGHTABLE_MENU_KEY);
            if (TextUtils.isEmpty(menuKey)) {
                continue;
            }
            final String prefKey = metadata.getString(METADATA_KEY);
            if (TextUtils.isEmpty(prefKey)) {
                Log.w(TAG, "Highlightable menu requires android:key but it's missing in xml: "
                        + menuKey);
                continue;
            }
            MENU_TO_PREFERENCE_KEY_MAP.put(menuKey, prefKey);
        }

        if (MENU_TO_PREFERENCE_KEY_MAP.isEmpty()) {
            return;
        }

        sXmlParsed = true;
        MENU_KEY_COMPAT_MAP.forEach((compatMenuKey, keyId) -> {
            final String prefKey = lookupPreferenceKey(context.getString(keyId));
            if (prefKey != null) {
                MENU_TO_PREFERENCE_KEY_MAP.put(compatMenuKey, prefKey);
            }
        });
    }

    /** Manually adds a preference as the menu key for Injection */
    public static synchronized void addMenuKey(String key) {
        Log.d(TAG, "add menu key: " + key);
        MENU_TO_PREFERENCE_KEY_MAP.put(key, key);
    }

    /** Looks up the preference key by a specified menu key */
    public static String lookupPreferenceKey(String menuKey) {
        return MENU_TO_PREFERENCE_KEY_MAP.get(menuKey);
    }
}
