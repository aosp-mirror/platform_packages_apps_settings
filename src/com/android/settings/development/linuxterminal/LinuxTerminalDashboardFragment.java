/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.development.linuxterminal;

import static android.content.Intent.EXTRA_USER_ID;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Fragment shown for 'Linux terminal development' preference in developer option. */
@SearchIndexable
public class LinuxTerminalDashboardFragment extends DashboardFragment {
    private static final String TAG = "LinuxTerminalFrag";

    private Context mUserAwareContext;

    private int mUserId;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.LINUX_TERMINAL_DASHBOARD;
    }

    @NonNull
    @Override
    public String getLogTag() {
        return TAG;
    }

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.linux_terminal_settings;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        // Initialize mUserId and mUserAwareContext before super.onAttach(),
        // so createPreferenceControllers() can be called with proper values from super.onAttach().
        int currentUserId = UserHandle.myUserId();
        mUserId = getArguments().getInt(EXTRA_USER_ID, currentUserId);
        mUserAwareContext =
                (currentUserId == mUserId)
                        ? context
                        : context.createContextAsUser(UserHandle.of(mUserId), /* flags= */ 0);

        // Note: This calls createPreferenceControllers() inside.
        super.onAttach(context);
    }

    @Override
    @NonNull
    public List<AbstractPreferenceController> createPreferenceControllers(
            @NonNull Context context) {
        List<AbstractPreferenceController> list = new ArrayList<>();
        list.add(new EnableLinuxTerminalPreferenceController(context, mUserAwareContext, mUserId));
        return list;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.linux_terminal_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
                }
            };
}
