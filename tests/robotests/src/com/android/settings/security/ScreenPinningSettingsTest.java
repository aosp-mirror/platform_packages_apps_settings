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

import static org.robolectric.Shadows.shadowOf;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.MessageFormat;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.FooterPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowUserManager;

import java.util.List;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowLockPatternUtils.class)
public class ScreenPinningSettingsTest {
    private static final String KEY_FOOTER = "screen_pinning_settings_screen_footer";
    private Context mContext;
    private UserManager mUserManager;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mUserManager = mContext.getSystemService(UserManager.class);
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

    @Test
    public void onCreate_lockToAppEnabled_guestModeSupported_verifyFooterText() {
        setupLockToAppState(/* enabled= */ true);
        setupGuestModeState(/* supported= */ true);

        launchFragmentAndRunTest(fragment -> {
            FooterPreference footer = fragment.findPreference(KEY_FOOTER);

            assertThat(footer.getSummary())
                    .isEqualTo(getExpectedFooterText(/* guestModeSupported= */ true));
        });
    }

    @Test
    public void onCreate_lockToAppEnabled_guestModeNotSupported_verifyFooterText() {
        setupLockToAppState(/* enabled= */ true);
        setupGuestModeState(/* supported= */ false);

        launchFragmentAndRunTest(fragment -> {
            FooterPreference footer = fragment.findPreference(KEY_FOOTER);

            assertThat(footer.getSummary())
                    .isEqualTo(getExpectedFooterText(/* guestModeSupported= */ false));
        });
    }

    @Test
    public void onCreate_lockToAppDisabled_guestModeSupported_verifyFooterText() {
        setupLockToAppState(/* enabled= */ false);
        setupGuestModeState(/* supported= */ true);

        launchFragmentAndRunTest(fragment -> {
            FooterPreference footer = fragment.findPreference(KEY_FOOTER);

            assertThat(footer.getSummary())
                    .isEqualTo(getExpectedFooterText(/* guestModeSupported= */ true));
        });
    }

    @Test
    public void onCreate_lockToAppDisabled_guestModeNotSupported_verifyFooterText() {
        setupLockToAppState(/* enabled= */ false);
        setupGuestModeState(/* supported= */ false);

        launchFragmentAndRunTest(fragment -> {
            FooterPreference footer = fragment.findPreference(KEY_FOOTER);

            assertThat(footer.getSummary())
                    .isEqualTo(getExpectedFooterText(/* guestModeSupported= */ false));
        });
    }

    private CharSequence getExpectedFooterText(boolean guestModeSupported) {
        final int stringResource = guestModeSupported
                ? R.string.screen_pinning_guest_user_description
                : R.string.screen_pinning_description;
        return MessageFormat.format(mContext.getString(stringResource), 1, 2, 3);
    }

    private void setupLockToAppState(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED,
                enabled ? 1 : 0);
    }

    private void setupGuestModeState(boolean supported) {
        ShadowUserManager shadowUserManager = shadowOf(mUserManager);
        shadowUserManager.setSupportsMultipleUsers(supported);
        shadowUserManager.setUserRestriction(
                UserHandle.of(UserHandle.myUserId()), UserManager.DISALLOW_USER_SWITCH, !supported);
    }

    private void launchFragmentAndRunTest(Consumer<ScreenPinningSettings> test) {
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT,
                SecurityAdvancedSettings.class.getName());
        // ScreenPinningSettings is tightly coupled with the SettingsActivity
        // In order to successfully launch the ScreenPinningSettings, have to use an indirect route
        // to launch the SecurityAdvancedSetting first, then replace it with ScreenPinningSettings.
        try (ActivityController<SettingsActivity> controller =
                     ActivityController.of(new SettingsActivity(), intent)) {
            controller.create().start().resume();

            controller.get().getSupportFragmentManager().beginTransaction().replace(
                    R.id.main_content, ScreenPinningSettings.class, null).commitNow();
            Fragment fragment = controller.get().getSupportFragmentManager()
                    .findFragmentById(R.id.main_content);
            assertThat(fragment).isNotNull();
            assertThat(fragment).isInstanceOf(ScreenPinningSettings.class);

            test.accept((ScreenPinningSettings) fragment);
        }
    }
}
