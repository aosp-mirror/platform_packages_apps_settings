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

package com.android.settings.security.screenlock;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;

import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.security.OwnerInfoPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class ScreenLockSettings extends DashboardFragment
        implements OwnerInfoPreferenceController.OwnerInfoCallback {

    private static final String TAG = "ScreenLockSettings";

    private static final int MY_USER_ID = UserHandle.myUserId();
    private LockPatternUtils mLockPatternUtils;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_LOCK_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_lock_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mLockPatternUtils = new LockPatternUtils(context);
        return buildPreferenceControllers(context, this /* parent */, getSettingsLifecycle(),
                mLockPatternUtils);
    }

    @Override
    public void onOwnerInfoUpdated() {
        use(OwnerInfoPreferenceController.class).updateSummary();
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Fragment parent, Lifecycle lifecycle, LockPatternUtils lockPatternUtils) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new PatternVisiblePreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new PowerButtonInstantLockPreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new LockAfterTimeoutPreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new OwnerInfoPreferenceController(context, parent, lifecycle));
        return controllers;
    }


    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.screen_lock_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* parent */,
                            null /* lifecycle */, new LockPatternUtils(context));
                }
            };
}
