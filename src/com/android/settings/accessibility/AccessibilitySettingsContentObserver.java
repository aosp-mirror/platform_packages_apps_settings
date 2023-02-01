/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AccessibilitySettingsContentObserver extends ContentObserver {

    private static final String TAG = "AccessibilitySettingsContentObserver";

    public interface ContentObserverCallback {
        void onChange(String key);
    }

    // Key: Preference key's uri, Value: Preference key
    private final Map<Uri, String> mUriToKey = new HashMap<>(2);

    // Key: Collection of preference keys, Value: onChange callback for keys
    private final Map<List<String>, ContentObserverCallback> mUrisToCallback = new HashMap<>();

    AccessibilitySettingsContentObserver(Handler handler) {
        super(handler);

        // default key to be observed
        addDefaultKeysToMap();
    }

    public void register(ContentResolver contentResolver) {
        for (Uri uri : mUriToKey.keySet()) {
            contentResolver.registerContentObserver(uri, false, this);
        }
    }

    public void unregister(ContentResolver contentResolver) {
        contentResolver.unregisterContentObserver(this);
    }

    private void addDefaultKeysToMap() {
        addKeyToMap(Settings.Secure.ACCESSIBILITY_ENABLED);
        addKeyToMap(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    }

    private boolean isDefaultKey(String key) {
        return Settings.Secure.ACCESSIBILITY_ENABLED.equals(key)
                || Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES.equals(key);
    }

    private void addKeyToMap(String key) {
        mUriToKey.put(Settings.Secure.getUriFor(key), key);
    }

    /**
     * {@link ContentObserverCallback} is added to {@link ContentObserver} to handle the
     * onChange event triggered by the key collection of {@code keysToObserve} and the default
     * keys.
     *
     * Note: The following key are default to be observed.
     *      {@link Settings.Secure.ACCESSIBILITY_ENABLED}
     *      {@link Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES}
     *
     * @param keysToObserve A collection of keys which are going to be observed.
     * @param observerCallback A callback which is used to handle the onChange event triggered
     *                         by the key collection of {@code keysToObserve}.
     */
    public void registerKeysToObserverCallback(List<String> keysToObserve,
            ContentObserverCallback observerCallback) {

        for (String key: keysToObserve) {
            addKeyToMap(key);
        }

        mUrisToCallback.put(keysToObserve, observerCallback);
    }

    /**
     * {@link ContentObserverCallback} is added to {@link ContentObserver} to handle the
     * onChange event triggered by the default keys.
     *
     * Note: The following key are default to be observed.
     *      {@link Settings.Secure.ACCESSIBILITY_ENABLED}
     *      {@link Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES}
     *
     * @param observerCallback A callback which is used to handle the onChange event triggered
     *      *                         by the key collection of {@code keysToObserve}.
     */
    public void registerObserverCallback(ContentObserverCallback observerCallback) {
        mUrisToCallback.put(Collections.emptyList(), observerCallback);
    }

    @Override
    public final void onChange(boolean selfChange, Uri uri) {

        final String key = mUriToKey.get(uri);

        if (key == null) {
            Log.w(TAG, "AccessibilitySettingsContentObserver can not find the key for "
                    + "uri: " + uri);
            return;
        }

        for (List<String> keys : mUrisToCallback.keySet()) {
            final boolean isDefaultKey = isDefaultKey(key);
            if (isDefaultKey || keys.contains(key)) {
                mUrisToCallback.get(keys).onChange(key);
            }
        }
    }
}
