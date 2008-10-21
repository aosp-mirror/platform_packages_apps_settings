/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.view.Menu;
import android.view.MenuItem;

public class ApnSettings extends PreferenceActivity {

    public static final String EXTRA_POSITION = "position";
    
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;

    private static final int MENU_NEW = Menu.FIRST;
    
    private Cursor mCursor;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        addPreferencesFromResource(R.xml.apn_settings);    
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        fillList();
    }
    
    private void fillList() {
        mCursor = managedQuery(Telephony.Carriers.CONTENT_URI, new String[] {
                "_id", "name", "apn"}, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        PreferenceCategory apnList = (PreferenceCategory) findPreference("apn_list");
        apnList.removeAll();
        
        mCursor.moveToFirst();
        while (!mCursor.isAfterLast()) {
            String name = mCursor.getString(NAME_INDEX);
            String apn = mCursor.getString(APN_INDEX);
            
            Preference pref = new Preference((Context) this);
            pref.setKey(mCursor.getString(ID_INDEX));
            pref.setTitle(name);
            pref.setSummary(apn);
            pref.setPersistent(false);
            apnList.addPreference(pref);
            mCursor.moveToNext();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_NEW, 0, 
                getResources().getString(R.string.menu_new))
                .setIcon(android.R.drawable.ic_menu_add);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void addNewApn() {
        startActivity(new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI));
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        startActivity(new Intent(Intent.ACTION_EDIT, url));
        return true;
    }
}
