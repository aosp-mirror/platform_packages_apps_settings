/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.applications;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;

import java.util.List;
import java.util.Objects;

/**
 * A preference for choosing the default emergency app
 */
public class DefaultEmergencyPreference extends ListPreference {

    private final ContentResolver mContentResolver;

    public DefaultEmergencyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        load();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(getEntries(), getEntryValues(), getSummary(), superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            setEntries(savedState.entries);
            setEntryValues(savedState.entryValues);
            setSummary(savedState.summary);
            super.onRestoreInstanceState(savedState.superState);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    protected boolean persistString(String value) {
        String previousValue = Settings.Secure.getString(mContentResolver,
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);

        if (!TextUtils.isEmpty(value) && !Objects.equals(value, previousValue)) {
            Settings.Secure.putString(mContentResolver,
                    Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION,
                    value);
        }
        setSummary(getEntry());
        return true;
    }

    private void load() {
        new AsyncTask<Void, Void, ArrayMap<String, CharSequence>>() {
            @Override
            protected ArrayMap<String, CharSequence> doInBackground(Void[] params) {
                return resolveAssistPackageAndQueryApps();
            }

            @Override
            protected void onPostExecute(ArrayMap<String, CharSequence> entries) {
                setEntries(entries.values().toArray(new CharSequence[entries.size()]));
                setEntryValues(entries.keySet().toArray(new String[entries.size()]));

                setValue(Settings.Secure.getString(mContentResolver,
                        Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION));
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private ArrayMap<String, CharSequence> resolveAssistPackageAndQueryApps() {
        ArrayMap<String, CharSequence> packages = new ArrayMap<>();

        Intent queryIntent = new Intent(TelephonyManager.ACTION_EMERGENCY_ASSISTANCE);
        PackageManager packageManager = getContext().getPackageManager();
        List<ResolveInfo> infos = packageManager.queryIntentActivities(queryIntent, 0);

        PackageInfo bestMatch = null;
        for (int i = 0; i < infos.size(); i++) {
            if (infos.get(i) == null || infos.get(i).activityInfo == null
                    || packages.containsKey(infos.get(i).activityInfo.packageName)) {
                continue;
            }

            String packageName = infos.get(i).activityInfo.packageName;
            CharSequence label = infos.get(i).activityInfo.applicationInfo
                    .loadLabel(packageManager);

            packages.put(packageName, label);

            PackageInfo packageInfo;
            try {
                packageInfo = packageManager.getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }

            // Get earliest installed app, but prioritize system apps.
            if (bestMatch == null
                    || !isSystemApp(bestMatch) && isSystemApp(packageInfo)
                    || isSystemApp(bestMatch) == isSystemApp(packageInfo)
                    && bestMatch.firstInstallTime > packageInfo.firstInstallTime) {
                bestMatch = packageInfo;
            }
        }

        String defaultPackage = Settings.Secure.getString(mContentResolver,
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);
        boolean defaultMissing = TextUtils.isEmpty(defaultPackage)
                || !packages.containsKey(defaultPackage);
        if (bestMatch != null && defaultMissing) {
            Settings.Secure.putString(mContentResolver,
                    Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION,
                    bestMatch.packageName);
        }

        return packages;
    }

    private static boolean isSystemApp(PackageInfo info) {
        return info.applicationInfo != null
                && (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private static class SavedState implements Parcelable {

        public final CharSequence[] entries;
        public final CharSequence[] entryValues;
        public final CharSequence summary;
        public final Parcelable superState;

        public SavedState(CharSequence[] entries, CharSequence[] entryValues,
                CharSequence summary, Parcelable superState) {
            this.entries = entries;
            this.entryValues = entryValues;
            this.summary = summary;
            this.superState = superState;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeCharSequenceArray(entries);
            dest.writeCharSequenceArray(entryValues);
            dest.writeCharSequence(summary);
            dest.writeParcelable(superState, flags);
        }

        public Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                CharSequence[] entries = source.readCharSequenceArray();
                CharSequence[] entryValues = source.readCharSequenceArray();
                CharSequence summary = source.readCharSequence();
                Parcelable superState = source.readParcelable(getClass().getClassLoader());
                return new SavedState(entries, entryValues, summary, superState);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
