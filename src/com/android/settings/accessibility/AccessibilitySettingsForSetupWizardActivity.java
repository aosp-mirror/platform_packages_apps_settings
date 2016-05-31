/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.setupwizardlib.view.NavigationBar;

public class AccessibilitySettingsForSetupWizardActivity extends SettingsActivity {

    private static final String SAVE_KEY_TITLE = "activity_title";

    private boolean mSendExtraWindowStateChanged;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // Finish configuring the content view.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setIsDrawerPresent(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedState) {
        savedState.putCharSequence(SAVE_KEY_TITLE, getTitle());
        super.onSaveInstanceState(savedState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        setTitle(savedState.getCharSequence(SAVE_KEY_TITLE));
    }

    @Override
    public void onResume() {
        super.onResume();
        mSendExtraWindowStateChanged = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Return true, so we get notified when items in the menu are clicked.
        return true;
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();

        // Clear accessibility focus and let the screen reader announce the new title.
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        return true;
    }

    @Override
    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
            CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        // Set the title.
        if (!TextUtils.isEmpty(titleText)) {
            setTitle(titleText);
        } else if (titleRes > 0) {
            setTitle(getString(titleRes));
        }

        // Start the new Fragment.
        args.putInt(SettingsPreferenceFragment.HELP_URI_RESOURCE_KEY, 0);
        startPreferenceFragment(Fragment.instantiate(this, fragmentClass, args), true);
        mSendExtraWindowStateChanged = true;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (mSendExtraWindowStateChanged) {
            // Clear accessibility focus and let the screen reader announce the new title.
            getWindow().getDecorView()
                    .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }
}
