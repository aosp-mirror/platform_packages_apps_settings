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

package com.android.settings.support.actionbar;

import static com.android.settings.support.actionbar.HelpResourceProvider.HELP_URI_RESOURCE_KEY;

import android.annotation.NonNull;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import androidx.fragment.app.Fragment;

import com.android.settingslib.HelpUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.ObservableFragment;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;

/**
 * A controller that adds help menu to any Settings page.
 */
public class HelpMenuController implements LifecycleObserver, OnCreateOptionsMenu {

    private final Fragment mHost;

    public static void init(@NonNull ObservablePreferenceFragment host) {
        host.getSettingsLifecycle().addObserver(new HelpMenuController(host));
    }

    public static void init(@NonNull ObservableFragment host) {
        host.getSettingsLifecycle().addObserver(new HelpMenuController(host));
    }

    private HelpMenuController(@NonNull Fragment host) {
        mHost = host;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final Bundle arguments = mHost.getArguments();
        int helpResourceId = 0;
        if (arguments != null && arguments.containsKey(HELP_URI_RESOURCE_KEY)) {
            helpResourceId = arguments.getInt(HELP_URI_RESOURCE_KEY);
        } else if (mHost instanceof HelpResourceProvider) {
            helpResourceId = ((HelpResourceProvider) mHost).getHelpResource();
        }

        String helpUri = null;
        if (helpResourceId != 0) {
            helpUri = mHost.getContext().getString(helpResourceId);
        }
        final Activity activity = mHost.getActivity();
        if (helpUri != null && activity != null) {
            HelpUtils.prepareHelpMenuItem(activity, menu, helpUri, mHost.getClass().getName());
        }
    }
}
