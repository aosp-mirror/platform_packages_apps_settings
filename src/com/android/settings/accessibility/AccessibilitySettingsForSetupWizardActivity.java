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
 * limitations under the License.
 */

package com.android.settings.accessibility;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

public class AccessibilitySettingsForSetupWizardActivity extends SettingsActivity {


    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setIsDrawerPresent(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Return true, so we get notified when items in the menu are clicked.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    @Override
    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
            CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        args.putInt(SettingsPreferenceFragment.HELP_URI_RESOURCE_KEY, 0);
        startPreferenceFragment(Fragment.instantiate(this, fragmentClass, args), true);
    }
}
