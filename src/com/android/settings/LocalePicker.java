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
        Arrays.sort(locales);

        final int origSize = locales.length;
        Loc[] preprocess = new Loc[origSize];
        int finalSize = 0;
        for (int i = 0 ; i < origSize; i++ ) {
            String s = locales[i];
            int len = s.length();
            if (len == 2) {
                Locale l = new Locale(s);
                preprocess[finalSize++] = new Loc(toTitleCase(l.getDisplayLanguage()), l);
            } else if (len == 5) {
                String language = s.substring(0, 2);
                String country = s.substring(3, 5);
                Locale l = new Locale(language, country);

                if (finalSize == 0) {
                    preprocess[finalSize++] = new Loc(toTitleCase(l.getDisplayLanguage()), l);
                } else {
                    // check previous entry:
                    //  same lang and no country -> overwrite it with a lang-only name
                    //  same lang and a country -> upgrade to full name and 
                    //    insert ours with full name
                    //  diff lang -> insert ours with lang-only name
                    if (preprocess[finalSize-1].locale.getLanguage().equals(language)) {
                       String prevCountry = preprocess[finalSize-1].locale.getCountry();
                       if (prevCountry.length() == 0) {
                            preprocess[finalSize-1].locale = l;
                            preprocess[finalSize-1].label = toTitleCase(l.getDisplayLanguage());
                        } else {
                            preprocess[finalSize-1].label = toTitleCase(preprocess[finalSize-1].locale.getDisplayName());
                            preprocess[finalSize++] = new Loc(toTitleCase(l.getDisplayName()), l);
                        }
                    } else {
                        String displayName;
                        if (s.equals("zz_ZZ")) {
                            displayName = "Pseudo...";
                        } else {
                            displayName = toTitleCase(l.getDisplayLanguage());
                        }
                        preprocess[finalSize++] = new Loc(displayName, l);
                    }
                }
            }
        }
        mLocales = new Loc[finalSize];
        for (int i = 0; i < finalSize ; i++) {
            mLocales[i] = preprocess[i];
        }
        int layoutId = R.layout.locale_picker_item;
        int fieldId = R.id.locale;
        ArrayAdapter<Loc> adapter = new ArrayAdapter<Loc>(this, layoutId, fieldId, mLocales);
        getListView().setAdapter(adapter);
    }

    private static String toTitleCase(String s) {
        if (s.length() == 0) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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

            // indicate this isn't some passing default - the user wants this remembered
            config.userSetLocale = true;

            am.updateConfiguration(config);
        } catch (RemoteException e) {
            // Intentionally left blank
        }
        finish();
    }
}
