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

package com.android.settings.shortcut;

import static com.android.settings.search.actionbar.SearchMenuController.NEED_SEARCH_ICON_IN_ACTION_BAR;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * UI for create widget/shortcut screen.
 */
public class CreateShortcut extends DashboardFragment {

    private static final String TAG = "CreateShortcut";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
            setArguments(args);
        }
        args.putBoolean(NEED_SEARCH_ICON_IN_ACTION_BAR, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(CreateShortcutPreferenceController.class).setActivity(getActivity());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.create_shortcut;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_CREATE_SHORTCUT;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }
}
