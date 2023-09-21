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

package com.android.settings.display;

import static android.provider.Settings.System.FOLD_LOCK_BEHAVIOR;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.testutils.shadow.ShadowSystemSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    ShadowSystemSettings.class,
})
public class FoldLockBehaviorSettingsTest {

    private Context mContext;
    private FoldLockBehaviorSettings mSetting;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSetting = new FoldLockBehaviorSettings();
        mSetting.onAttach(mContext);
    }

    @Test
    public void getDefaultKey_returnFoldSetting() {
        setFoldSetting("stay_awake_on_fold_key");

        String key = mSetting.getDefaultKey();

        assertThat(key).isEqualTo("stay_awake_on_fold_key");
    }

    @Test
    public void setDefaultKey_returnFoldSetting() {
        mSetting.setDefaultKey("stay_awake_on_fold_key");

        String key = getFoldSettingValue();

        assertThat(key).isEqualTo("stay_awake_on_fold_key");
    }

    @Test
    public void setInvalidDefaultKey_returnDefaultFoldSetting() {
        setFoldSetting("invalid_fold_lock_behavior_key");

        String key = mSetting.getDefaultKey();

        assertThat(key).isEqualTo("selective_stay_awake_key");
    }

    private void setFoldSetting(String selectedSetting) {
        Settings.System.putStringForUser(mContext.getContentResolver(),
                FOLD_LOCK_BEHAVIOR, selectedSetting, UserHandle.USER_CURRENT);
    }

    private String getFoldSettingValue() {
        return Settings.System.getStringForUser(mContext.getContentResolver(),
                FOLD_LOCK_BEHAVIOR, UserHandle.USER_CURRENT);
    }
}
