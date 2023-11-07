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

package com.android.settings.security;

import static com.android.settings.security.ContentProtectionTogglePreferenceController.KEY_CONTENT_PROTECTION_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowUtils.class,
        })
public class ContentProtectionTogglePreferenceControllerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private PreferenceScreen mScreen;

    private SettingsMainSwitchPreference mSwitchPreference;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentProtectionTogglePreferenceController mController;
    private int mSettingBackupValue;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new ContentProtectionTogglePreferenceController(mContext, "key");
        mSwitchPreference = new SettingsMainSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mSwitchPreference);
        mSettingBackupValue = getContentProtectionGlobalSetting();
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 0);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                KEY_CONTENT_PROTECTION_PREFERENCE,
                mSettingBackupValue);
    }

    @Test
    public void isAvailable_alwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isChecked_settingTurnOn() {
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_fullyManagedMode_settingTurnOff() {
        final ComponentName componentName =
                ComponentName.unflattenFromString("com.android.test/.DeviceAdminReceiver");
        ShadowUtils.setDeviceOwnerComponent(componentName);
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);

        ContentProtectionTogglePreferenceController controller =
                new ContentProtectionTogglePreferenceController(mContext, "key");

        assertThat(controller.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingTurnOff() {
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);

        assertThat(mController.isChecked()).isFalse();
        assertThat(getContentProtectionGlobalSetting()).isEqualTo(-1);
    }

    @Test
    public void isChecked_settingDefaultOn() {
        assertThat(mController.isChecked()).isTrue();
        assertThat(getContentProtectionGlobalSetting()).isEqualTo(0);
    }

    @Test
    public void onSwitchChanged_switchChecked_manuallyEnabled() {
        mController.displayPreference(mScreen);
        mController.setChecked(false);

        mController.onCheckedChanged(/* switchView= */ null, /* isChecked= */ true);

        assertThat(getContentProtectionGlobalSetting()).isEqualTo(1);
    }

    @Test
    public void onSwitchChanged_switchUnchecked_manuallyDisabled() {
        mController.displayPreference(mScreen);

        mController.onCheckedChanged(/* switchView= */ null, /* isChecked= */ false);

        assertThat(getContentProtectionGlobalSetting()).isEqualTo(-1);
    }

    private int getContentProtectionGlobalSetting() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 0);
    }
}
