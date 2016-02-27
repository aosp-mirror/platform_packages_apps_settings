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
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.setupwizardlib.view.NavigationBar;

public class AccessibilitySettingsForSetupWizardActivity extends SettingsActivity {

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setIsDrawerPresent(false);

        // Hide System Nav Bar
        SystemBarHelper.hideSystemBars(getWindow());

        // Show SUW Nav Bar
        setContentView(R.layout.accessibility_settings_for_suw);
        NavigationBar navigationBar = (NavigationBar) findViewById(R.id.suw_navigation_bar);
        navigationBar.getNextButton().setVisibility(View.GONE);
        navigationBar.setNavigationBarListener(new NavigationBar.NavigationBarListener() {
            @Override
            public void onNavigateBack() {
                onNavigateUp();
            }

            @Override
            public void onNavigateNext() {
                // Do nothing. We don't show this button.
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Return true, so we get notified when items in the menu are clicked.
        return true;
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
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
    }

    /**
     * Start a new fragment.
     *
     * @param fragment The fragment to start
     * @param push If true, the current fragment will be pushed onto the back stack.  If false,
     * the current fragment will be replaced.
     */
    @Override
    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.suw_main_content, fragment);
        if (push) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack(BACK_STACK_PREFS);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
        transaction.commitAllowingStateLoss();
    }
}
