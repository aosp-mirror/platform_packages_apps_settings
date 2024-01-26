/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowLockPatternUtils.class)
public class ScreenPinningSettingsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() {
        ShadowLockPatternUtils.reset();
    }

    @Test
    public void getDynamicRawDataToIndex_numericPassword_shouldIndexUnlockPinTitle() {
        ShadowLockPatternUtils.setKeyguardStoredPasswordQuality(
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);

        final List<SearchIndexableRaw> indexRaws =
                ScreenPinningSettings.SEARCH_INDEX_DATA_PROVIDER.getDynamicRawDataToIndex(
                        mContext, /* enabled= */ true);

        assertThat(indexRaws.size()).isEqualTo(1);
        assertThat(indexRaws.get(0).title).isEqualTo(
                mContext.getString(R.string.screen_pinning_unlock_pin));
    }

    @Test
    public void getDynamicRawDataToIndex_alphabeticPassword_shouldIndexUnlockPasswordTitle() {
        ShadowLockPatternUtils.setKeyguardStoredPasswordQuality(
                DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);

        final List<SearchIndexableRaw> indexRaws =
                ScreenPinningSettings.SEARCH_INDEX_DATA_PROVIDER.getDynamicRawDataToIndex(
                        mContext, /* enabled= */ true);

        assertThat(indexRaws.size()).isEqualTo(1);
        assertThat(indexRaws.get(0).title).isEqualTo(
                mContext.getString(R.string.screen_pinning_unlock_password));
    }

    @Test
    public void getDynamicRawDataToIndex_patternPassword_shouldIndexUnlockPatternTitle() {
        ShadowLockPatternUtils.setKeyguardStoredPasswordQuality(
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        ShadowLockPatternUtils.setIsLockPatternEnabled(
                UserHandle.myUserId(), /* isLockPatternEnabled= */ true);

        final List<SearchIndexableRaw> indexRaws =
                ScreenPinningSettings.SEARCH_INDEX_DATA_PROVIDER.getDynamicRawDataToIndex(
                        mContext, /* enabled= */ true);

        assertThat(indexRaws.size()).isEqualTo(1);
        assertThat(indexRaws.get(0).title).isEqualTo(
                mContext.getString(R.string.screen_pinning_unlock_pattern));
    }

    @Test
    public void getDynamicRawDataToIndex_nonePassword_shouldIndexUnlockNoneTitle() {
        ShadowLockPatternUtils.setKeyguardStoredPasswordQuality(
                DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        final List<SearchIndexableRaw> indexRaws =
                ScreenPinningSettings.SEARCH_INDEX_DATA_PROVIDER.getDynamicRawDataToIndex(
                        mContext, /* enabled= */ true);

        assertThat(indexRaws.size()).isEqualTo(1);
        assertThat(indexRaws.get(0).title).isEqualTo(
                mContext.getString(R.string.screen_pinning_unlock_none));
    }
}
