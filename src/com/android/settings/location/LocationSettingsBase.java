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
import android.content.Intent;
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
    /** Broadcast intent action when the location mode is about to change. */
    private static final String MODE_CHANGING_ACTION =
            "com.android.settings.location.MODE_CHANGING";
    private static final String CURRENT_MODE_KEY = "CURRENT_MODE";
    private static final String NEW_MODE_KEY = "NEW_MODE";

    private static final int LOADER_ID_LOCATION_MODE = 1;
    private int mCurrentMode;

    /**
     * Whether the fragment is actively running.
     */
    private boolean mActive = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getLoaderManager().initLoader(LOADER_ID_LOCATION_MODE, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mActive = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mActive = false;
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
            if (mActive) {
                onModeChanged(mode, true);
            }
            return;
        }
        Intent intent = new Intent(MODE_CHANGING_ACTION);
        intent.putExtra(CURRENT_MODE_KEY, mCurrentMode);
        intent.putExtra(NEW_MODE_KEY, mode);
        getActivity().sendBroadcast(intent, android.Manifest.permission.WRITE_SECURE_SETTINGS);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, mode);
        refreshLocationMode();
    }

    public void refreshLocationMode() {
        if (mActive) {
            int mode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            mCurrentMode = mode;
            onModeChanged(mode, isRestricted());
        }
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
