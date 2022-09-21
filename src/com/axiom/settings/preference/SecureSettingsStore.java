/*
 * Copyright (C) 2017 AICP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.axiom.settings.preference;

import android.content.ContentResolver;
import android.preference.PreferenceDataStore;
import android.provider.Settings;

public class SecureSettingsStore extends androidx.preference.PreferenceDataStore
        implements PreferenceDataStore {

    private ContentResolver mContentResolver;

    public SecureSettingsStore(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    public boolean getBoolean(String key, boolean defValue) {
        return getInt(key, defValue ? 1 : 0) != 0;
    }

    public float getFloat(String key, float defValue) {
        return Settings.Secure.getFloat(mContentResolver, key, defValue);
    }

    public int getInt(String key, int defValue) {
        return Settings.Secure.getInt(mContentResolver, key, defValue);
    }

    public long getLong(String key, long defValue) {
        return Settings.Secure.getLong(mContentResolver, key, defValue);
    }

    public String getString(String key, String defValue) {
        String result = Settings.Secure.getString(mContentResolver, key);
        return result == null ? defValue : result;
    }

    public void putBoolean(String key, boolean value) {
        putInt(key, value ? 1 : 0);
    }

    public void putFloat(String key, float value) {
        Settings.Secure.putFloat(mContentResolver, key, value);
    }

    public void putInt(String key, int value) {
        Settings.Secure.putInt(mContentResolver, key, value);
    }

    public void putLong(String key, long value) {
        Settings.Secure.putLong(mContentResolver, key, value);
    }

    public void putString(String key, String value) {
        Settings.Secure.putString(mContentResolver, key, value);
    }

}
