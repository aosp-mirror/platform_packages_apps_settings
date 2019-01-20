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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;

/**
 * Dialog Activity to host Settings Slices.
 */
public class SettingsPanelActivity extends FragmentActivity {

    private final String TAG = "panel_activity";

    /**
     * Key specifying which Panel the app is requesting.
     */
    public static final String KEY_PANEL_TYPE_ARGUMENT = "PANEL_TYPE_ARGUMENT";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent callingIntent = getIntent();
        if (callingIntent == null) {
            Log.e(TAG, "Null intent, closing Panel Activity");
            finish();
            return;
        }

        setContentView(R.layout.settings_panel);

        // Move the window to the bottom of screen, and make it take up the entire screen width.
        final Window window = getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

        final Bundle bundle = new Bundle();
        bundle.putString(KEY_PANEL_TYPE_ARGUMENT, callingIntent.getAction());

        final PanelFragment panelFragment = new PanelFragment();
        panelFragment.setArguments(bundle);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentById(R.id.main_content);
        if (fragment == null) {
            fragmentManager.beginTransaction().add(R.id.main_content, panelFragment).commit();
        }
    }
}
