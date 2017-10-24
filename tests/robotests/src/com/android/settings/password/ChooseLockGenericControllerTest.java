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
 * limitations under the License
 */

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.regex.Pattern;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class
        })
public class ChooseLockGenericControllerTest {

    private ChooseLockGenericController mController;

    @Mock
    private ManagedLockPasswordProvider mManagedLockPasswordProvider;

    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new ChooseLockGenericController(
                application,
                0 /* userId */,
                mDevicePolicyManager,
                mManagedLockPasswordProvider);
        SettingsShadowResources.overrideResource(R.bool.config_hide_none_security_option, false);
        SettingsShadowResources.overrideResource(R.bool.config_hide_swipe_security_option, false);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void isScreenLockVisible_shouldRespectResourceConfig() {
        for (ScreenLockType lock : ScreenLockType.values()) {
            // All locks except managed defaults to visible
            assertThat(mController.isScreenLockVisible(lock)).named(lock + " visible")
                    .isEqualTo(lock != ScreenLockType.MANAGED);
        }

        SettingsShadowResources.overrideResource(R.bool.config_hide_none_security_option, true);
        SettingsShadowResources.overrideResource(R.bool.config_hide_swipe_security_option, true);
        assertThat(mController.isScreenLockVisible(ScreenLockType.NONE)).named("NONE visible")
                .isFalse();
        assertThat(mController.isScreenLockVisible(ScreenLockType.SWIPE)).named("SWIPE visible")
                .isFalse();
    }

    @Test
    public void isScreenLockVisible_notCurrentUser_shouldHideSwipe() {
        mController = new ChooseLockGenericController(application, 1 /* userId */);
        assertThat(mController.isScreenLockVisible(ScreenLockType.SWIPE)).named("SWIPE visible")
                .isFalse();
    }

    @Test
    public void isScreenLockVisible_managedPasswordChoosable_shouldShowManaged() {
        doReturn(true).when(mManagedLockPasswordProvider).isManagedPasswordChoosable();

        assertThat(mController.isScreenLockVisible(ScreenLockType.MANAGED)).named("MANAGED visible")
                .isTrue();
    }

    @Test
    public void isScreenLockEnabled_lowerQuality_shouldReturnFalse() {
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(mController.isScreenLockEnabled(lock, lock.maxQuality + 1))
                    .named(lock + " enabled")
                    .isFalse();
        }
    }

    @Test
    public void isScreenLockEnabled_equalQuality_shouldReturnTrue() {
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(mController.isScreenLockEnabled(lock, lock.defaultQuality))
                    .named(lock + " enabled")
                    .isTrue();
        }
    }

    @Test
    public void isScreenLockEnabled_higherQuality_shouldReturnTrue() {
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(mController.isScreenLockEnabled(lock, lock.maxQuality - 1))
                    .named(lock + " enabled")
                    .isTrue();
        }
    }

    @Test
    public void isScreenLockDisabledByAdmin_lowerQuality_shouldReturnTrue() {
        doReturn(true).when(mManagedLockPasswordProvider).isManagedPasswordChoosable();
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(mController.isScreenLockDisabledByAdmin(lock, lock.maxQuality + 1))
                    .named(lock + " disabledByAdmin")
                    .isTrue();
        }
    }

    @Test
    public void isScreenLockDisabledByAdmin_equalQuality_shouldReturnFalse() {
        doReturn(true).when(mManagedLockPasswordProvider).isManagedPasswordChoosable();
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(mController.isScreenLockDisabledByAdmin(lock, lock.maxQuality))
                    .named(lock + " disabledByAdmin")
                    .isFalse();
        }
    }

    @Test
    public void isScreenLockDisabledByAdmin_higherQuality_shouldReturnFalse() {
        doReturn(true).when(mManagedLockPasswordProvider).isManagedPasswordChoosable();
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(mController.isScreenLockDisabledByAdmin(lock, lock.maxQuality - 1))
                    .named(lock + " disabledByAdmin")
                    .isFalse();
        }
    }

    @Test
    public void isScreenLockDisabledByAdmin_managedNotChoosable_shouldReturnTrue() {
        doReturn(false).when(mManagedLockPasswordProvider).isManagedPasswordChoosable();
        assertThat(mController.isScreenLockDisabledByAdmin(
                ScreenLockType.MANAGED, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED))
                .named("MANANGED disabledByAdmin")
                .isTrue();
    }

    @Test
    public void getTitle_shouldContainEnumName() {
        doReturn("MANAGED").when(mManagedLockPasswordProvider).getPickerOptionTitle(anyBoolean());
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(mController.getTitle(lock).toString())
                    .containsMatch(Pattern.compile(lock.toString(), Pattern.CASE_INSENSITIVE));
        }
    }

    @Test
    public void getVisibleScreenLockTypes_qualitySomething_shouldReturnPatterPinPassword() {
        assertThat(mController.getVisibleScreenLockTypes(
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, false))
                .isEqualTo(Arrays.asList(
                        ScreenLockType.PATTERN,
                        ScreenLockType.PIN,
                        ScreenLockType.PASSWORD));
    }

    @Test
    public void getVisibleScreenLockTypes_showDisabled_shouldReturnAllButManaged() {
        assertThat(mController.getVisibleScreenLockTypes(
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, true))
                .isEqualTo(Arrays.asList(
                        ScreenLockType.NONE,
                        ScreenLockType.SWIPE,
                        ScreenLockType.PATTERN,
                        ScreenLockType.PIN,
                        ScreenLockType.PASSWORD));
    }

    @Test
    public void upgradeQuality_noDpmRequirement_shouldReturnQuality() {
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED)
                .when(mDevicePolicyManager)
                .getPasswordQuality(nullable(ComponentName.class), anyInt());

        int upgradedQuality = mController.upgradeQuality(
                DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
        assertThat(upgradedQuality).named("upgradedQuality")
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
    }

    @Test
    public void upgradeQuality_dpmRequirement_shouldReturnRequiredQuality() {
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC)
                .when(mDevicePolicyManager)
                .getPasswordQuality(nullable(ComponentName.class), anyInt());

        int upgradedQuality = mController.upgradeQuality(
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        assertThat(upgradedQuality).named("upgradedQuality")
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
    }
}
