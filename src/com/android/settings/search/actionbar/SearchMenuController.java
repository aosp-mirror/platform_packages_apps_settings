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
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.ObservableFragment;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;

public class SearchMenuController implements LifecycleObserver, OnCreateOptionsMenu {

    public static final String NEED_SEARCH_ICON_IN_ACTION_BAR = "need_search_icon_in_action_bar";

    private final Fragment mHost;

    public static void init(@NonNull ObservablePreferenceFragment host) {
        host.getLifecycle().addObserver(new SearchMenuController(host));
    }

    public static void init(@NonNull ObservableFragment host) {
        host.getLifecycle().addObserver(new SearchMenuController(host));
    }

    private SearchMenuController(@NonNull Fragment host) {
        mHost = host;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!Utils.isDeviceProvisioned(mHost.getContext())) {
            return;
        }
        if (menu == null) {
            return;
        }
        final Bundle arguments = mHost.getArguments();
        if (arguments != null && !arguments.getBoolean(NEED_SEARCH_ICON_IN_ACTION_BAR, true)) {
            return;
        }
        final MenuItem searchItem = menu.add(Menu.NONE, Menu.NONE, 0 /* order */,
                R.string.search_menu);
        searchItem.setIcon(R.drawable.ic_search_24dp);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        searchItem.setOnMenuItemClickListener(target -> {
            final Intent intent = SearchFeatureProvider.SEARCH_UI_INTENT;
            intent.setPackage(FeatureFactory.getFactory(mHost.getContext())
                    .getSearchFeatureProvider().getSettingsIntelligencePkgName());

            mHost.startActivityForResult(intent, 0 /* requestCode */);
            return true;
        });
    }
}
