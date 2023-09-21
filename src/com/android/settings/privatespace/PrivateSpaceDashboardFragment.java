/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Flags;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

/** Fragment representing the Private Space dashboard in Settings. */
@SearchIndexable
public class PrivateSpaceDashboardFragment extends DashboardFragment {
    private static final String TAG = "PrivateSpaceDashboardFragment";
    private static final String KEY_CREATE_PROFILE_PREFERENCE = "private_space_create";
    private static final String KEY_DELETE_PROFILE_PREFERENCE = "private_space_delete";
    private static final String KEY_ONE_LOCK_PREFERENCE = "private_space_use_one_lock";
    private static final String KEY_PS_HIDDEN_PREFERENCE = "private_space_hidden";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.private_space_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.private_space_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SafetyCenterManagerWrapper.get().isEnabled(context)
                            && Flags.allowPrivateProfile();
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_CREATE_PROFILE_PREFERENCE);
                    keys.add(KEY_DELETE_PROFILE_PREFERENCE);
                    keys.add(KEY_ONE_LOCK_PREFERENCE);
                    keys.add(KEY_PS_HIDDEN_PREFERENCE);
                    return keys;
                }
            };
}
