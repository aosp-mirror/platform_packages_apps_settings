/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.ListActivity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Locale;

public class LocalePicker extends ListActivity {
    private static final String TAG = "LocalePicker";

    Loc[] mLocales;

    private static class Loc {
        String label;
        Locale locale;

        public Loc(String label, Locale locale) {
            this.label = label;
            this.locale = locale;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    int getContentView() {
        return R.layout.locale_picker;
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(getContentView());

        String[] locales = getAssets().getLocales();
        final int N = locales.length;
        mLocales = new Loc[N];
        for (int i = 0; i < N; i++) {
            Locale locale = null;
            String s = locales[i];
            int len = s.length();
            if (len == 0) {
                locale = new Locale("en", "US");
            } else if (len == 2) {
                locale = new Locale(s);
            } else if (len == 5) {
                locale = new Locale(s.substring(0, 2), s.substring(3, 5));
            }
            String displayName = "";
            if (locale != null) {
                displayName = locale.getDisplayName();
            }
            if ("zz_ZZ".equals(s)) {
                displayName = "Pseudo...";
            }

            mLocales[i] = new Loc(displayName, locale);
        }

        int layoutId = R.layout.locale_picker_item;
        int fieldId = R.id.locale;
        ArrayAdapter<Loc> adapter = new ArrayAdapter<Loc>(this, layoutId, fieldId, mLocales);
        getListView().setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().requestFocus();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            Loc loc = mLocales[position];
            config.locale = loc.locale;
            final String language = loc.locale.getLanguage();
            final String region = loc.locale.getCountry();

            am.updateConfiguration(config);
            
            // Update the System properties
            SystemProperties.set("user.language", language);
            SystemProperties.set("user.region", region);
            // Write to file for persistence across reboots
            try {
                BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(
                        System.getenv("ANDROID_DATA") + "/locale"));
                bw.write(language + "_" + region);
                bw.close();
            } catch (java.io.IOException ioe) {
                Log.e(TAG, 
                        "Unable to persist locale. Error writing to locale file." 
                        + ioe);
            }    
        } catch (RemoteException e) {
            // Intentionally left blank
        }
        finish();
    }
}
