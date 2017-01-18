/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings;

import com.android.internal.logging.MetricsLogger;

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.Map;
import java.util.Set;

public class SharedPreferencesLogger implements SharedPreferences {

    private final String mTag;
    private final Context mContext;

    public SharedPreferencesLogger(Context context, String tag) {
        mContext = context;
        mTag = tag;
    }

    @Override
    public Map<String, ?> getAll() {
        return null;
    }

    @Override
    public String getString(String key, @Nullable String defValue) {
        return defValue;
    }

    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        return false;
    }

    @Override
    public Editor edit() {
        return new EditorLogger();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
    }

    private void logValue(String key, String value) {
        MetricsLogger.count(mContext, mTag + "/" + key + "|" + value, 1);
    }

    private void logPackageName(String key, String value) {
        MetricsLogger.count(mContext, mTag + "/" + key, 1);
        MetricsLogger.action(mContext, MetricsEvent.ACTION_GENERIC_PACKAGE,
                mTag + "/" + key + "|" + value);
    }

    private void safeLogValue(String key, String value) {
        new AsyncPackageCheck().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key, value);
    }

    private class AsyncPackageCheck extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String key = params[0];
            String value = params[1];
            PackageManager pm = mContext.getPackageManager();
            try {
                // Check if this might be a component.
                ComponentName name = ComponentName.unflattenFromString(value);
                if (value != null) {
                    value = name.getPackageName();
                }
            } catch (Exception e) {
            }
            try {
                pm.getPackageInfo(value, PackageManager.MATCH_UNINSTALLED_PACKAGES);
                logPackageName(key, value);
            } catch (PackageManager.NameNotFoundException e) {
                // Clearly not a package, lets log it.
                logValue(key, value);
            }
            return null;
        }
    }

    public class EditorLogger implements Editor {
        @Override
        public Editor putString(String key, @Nullable String value) {
            safeLogValue(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            safeLogValue(key, TextUtils.join(",", values));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            logValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            logValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            logValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            logValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor remove(String key) {
            return this;
        }

        @Override
        public Editor clear() {
            return this;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {
        }
    }
}
