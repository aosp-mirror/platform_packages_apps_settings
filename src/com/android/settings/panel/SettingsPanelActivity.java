/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.panel;

import static com.android.settingslib.media.MediaOutputSliceConstants.EXTRA_PACKAGE_NAME;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.panel.PanelLoggingContract.PanelClosedKeys;

/**
 * Dialog Activity to host Settings Slices.
 */
public class SettingsPanelActivity extends FragmentActivity {

    private final String TAG = "panel_activity";

    @VisibleForTesting
    final Bundle mBundle = new Bundle();

    /**
     * Key specifying which Panel the app is requesting.
     */
    public static final String KEY_PANEL_TYPE_ARGUMENT = "PANEL_TYPE_ARGUMENT";

    /**
     * Key specifying the package which requested the Panel.
     */
    public static final String KEY_CALLING_PACKAGE_NAME = "PANEL_CALLING_PACKAGE_NAME";

    /**
     * Key specifying the package name for which the
     */
    public static final String KEY_MEDIA_PACKAGE_NAME = "PANEL_MEDIA_PACKAGE_NAME";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent callingIntent = getIntent();
        if (callingIntent == null) {
            Log.e(TAG, "Null intent, closing Panel Activity");
            finish();
            return;
        }

        // We will use it once media output switch panel support remote device.
        final String mediaPackageName = callingIntent.getStringExtra(EXTRA_PACKAGE_NAME);

        setContentView(R.layout.settings_panel);

        // Move the window to the bottom of screen, and make it take up the entire screen width.
        final Window window = getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

        mBundle.putString(KEY_PANEL_TYPE_ARGUMENT, callingIntent.getAction());
        mBundle.putString(KEY_CALLING_PACKAGE_NAME, getCallingPackage());
        mBundle.putString(KEY_MEDIA_PACKAGE_NAME, mediaPackageName);

        final PanelFragment panelFragment = new PanelFragment();
        panelFragment.setArguments(mBundle);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentById(R.id.main_content);
        if (fragment == null) {
            fragmentManager.beginTransaction().add(R.id.main_content, panelFragment).commit();
        }
    }
}
