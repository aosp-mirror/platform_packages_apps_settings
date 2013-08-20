/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.location;

import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;

import java.util.Observable;
import java.util.Observer;

/**
 * A base class that listens to location settings change and modifies location
 * settings.
 */
public abstract class LocationSettingsBase extends SettingsPreferenceFragment {
    private ContentQueryMap mContentQueryMap;
    private Observer mSettingsObserver;

    @Override
    public void onStart() {
        super.onStart();
        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[] { Settings.Secure.LOCATION_PROVIDERS_ALLOWED },
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
        mSettingsObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                refreshLocationMode();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        mContentQueryMap.addObserver(mSettingsObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        mContentQueryMap.deleteObserver(mSettingsObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        mContentQueryMap.close();
    }

    /** Called when location mode has changed. */
    public abstract void onModeChanged(int mode);

    public void setLocationMode(int mode) {
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)) {
            return;
        }
        Settings.Secure.setLocationMode(getContentResolver(), mode);
        refreshLocationMode();
    }

    public void refreshLocationMode() {
        ContentResolver res = getContentResolver();
        int mode = Settings.Secure.getLocationMode(getContentResolver());
        onModeChanged(mode);
    }
}
