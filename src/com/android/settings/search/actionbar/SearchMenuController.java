/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.search.actionbar;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;

import com.google.android.setupcompat.util.WizardManagerHelper;

public class SearchMenuController implements LifecycleObserver, OnCreateOptionsMenu {

    public static final String NEED_SEARCH_ICON_IN_ACTION_BAR = "need_search_icon_in_action_bar";
    public static final int MENU_SEARCH = Menu.FIRST + 10;

    private final Fragment mHost;
    private final int mPageId;

    public static void init(@NonNull InstrumentedPreferenceFragment host) {
        host.getSettingsLifecycle().addObserver(
                new SearchMenuController(host, host.getMetricsCategory()));
    }

    public static void init(@NonNull InstrumentedFragment host) {
        host.getSettingsLifecycle().addObserver(
                new SearchMenuController(host, host.getMetricsCategory()));
    }

    private SearchMenuController(@NonNull Fragment host, int pageId) {
        mHost = host;
        mPageId = pageId;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final Activity activity = mHost.getActivity();
        final String SettingsIntelligencePkgName = activity.getString(
                R.string.config_settingsintelligence_package_name);
        if (!WizardManagerHelper.isDeviceProvisioned(activity)
                || WizardManagerHelper.isAnySetupWizard(activity.getIntent())) {
            return;
        }
        if (!Utils.isPackageEnabled(activity, SettingsIntelligencePkgName)) {
            return;
        }
        if (menu == null) {
            return;
        }
        final Bundle arguments = mHost.getArguments();
        if (arguments != null && !arguments.getBoolean(NEED_SEARCH_ICON_IN_ACTION_BAR, true)) {
            return;
        }
        // menu contains search item, skip it
        if (menu.findItem(MENU_SEARCH) != null) {
            return;
        }
        final MenuItem searchItem = menu.add(Menu.NONE, MENU_SEARCH, 0 /* order */,
                R.string.search_menu);
        searchItem.setIcon(R.drawable.ic_search_24dp);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        searchItem.setOnMenuItemClickListener(target -> {
            final Intent intent = FeatureFactory.getFactory(activity)
                    .getSearchFeatureProvider()
                    .buildSearchIntent(activity, mPageId);

            if (activity.getPackageManager().queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                return true;
            }

            FeatureFactory.getFactory(activity).getMetricsFeatureProvider()
                    .action(activity, SettingsEnums.ACTION_SEARCH_RESULTS);
            mHost.startActivityForResult(intent, SearchFeatureProvider.REQUEST_CODE);
            return true;
        });
    }
}
