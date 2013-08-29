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

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;

/**
 * A base class that listens to location settings change and modifies location
 * settings.
 */
public abstract class LocationSettingsBase extends SettingsPreferenceFragment
        implements LoaderCallbacks<Cursor> {
    private static final String TAG = "LocationSettingsBase";

    private static final int LOADER_ID_LOCATION_MODE = 1;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getLoaderManager().initLoader(LOADER_ID_LOCATION_MODE, null, this);
    }

    /** Called when location mode has changed. */
    public abstract void onModeChanged(int mode, boolean restricted);

    private boolean isRestricted() {
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        return um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
    }

    public void setLocationMode(int mode) {
        if (isRestricted()) {
            // Location toggling disabled by user restriction. Read the current location mode to
            // update the location master switch.
            if (Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "Restricted user, not setting location mode");
            }
            mode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            onModeChanged(mode, true);
            return;
        }
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, mode);
        refreshLocationMode();
    }

    public void refreshLocationMode() {
        int mode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF);
        onModeChanged(mode, isRestricted());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_LOCATION_MODE:
                return new CursorLoader(getActivity(), Settings.Secure.CONTENT_URI, null,
                        "(" + Settings.System.NAME + "=?)",
                        new String[] { Settings.Secure.LOCATION_MODE }, null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        refreshLocationMode();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Nothing to do here.
    }
}
