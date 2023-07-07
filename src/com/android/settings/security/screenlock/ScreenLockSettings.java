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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.OwnerInfoPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class ScreenLockSettings extends DashboardFragment
        implements OwnerInfoPreferenceController.OwnerInfoCallback {

    private static final String TAG = "ScreenLockSettings";

    private static final int MY_USER_ID = UserHandle.myUserId();

    static final int AUTO_PIN_SETTING_ENABLING_REQUEST_CODE = 111;
    static final int AUTO_PIN_SETTING_DISABLING_REQUEST_CODE = 112;

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
        return buildPreferenceControllers(context, this /* parent */, mLockPatternUtils);
    }

    @Override
    public void onOwnerInfoUpdated() {
        use(OwnerInfoPreferenceController.class).updateSummary();
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            DashboardFragment parent, LockPatternUtils lockPatternUtils) {

        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new PatternVisiblePreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new PinPrivacyPreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new PowerButtonInstantLockPreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new LockAfterTimeoutPreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new AutoPinConfirmPreferenceController(
                context, MY_USER_ID, lockPatternUtils, parent));
        controllers.add(new OwnerInfoPreferenceController(context, parent));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_lock_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* parent */,
                            new LockPatternUtils(context));
                }
            };

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTO_PIN_SETTING_ENABLING_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onAutoPinConfirmSettingChange(/* newState= */ true);
            }
        } else if (requestCode == AUTO_PIN_SETTING_DISABLING_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onAutoPinConfirmSettingChange(/* newState= */ false);
            }
        }
    }

    private void onAutoPinConfirmSettingChange(boolean newState) {
        // update the auto pin confirm setting.
        mLockPatternUtils.setAutoPinConfirm(newState, MY_USER_ID);
        // store the pin length info to disk; If it fails, reset the setting to prev state.
        if (!mLockPatternUtils.refreshStoredPinLength(MY_USER_ID)) {
            mLockPatternUtils.setAutoPinConfirm(!newState, MY_USER_ID);
        }
    }
}
