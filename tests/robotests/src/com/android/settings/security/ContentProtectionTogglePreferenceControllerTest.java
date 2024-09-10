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

import static android.view.contentprotection.flags.Flags.FLAG_MANAGE_DEVICE_POLICY_ENABLED;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.security.ContentProtectionTogglePreferenceController.KEY_CONTENT_PROTECTION_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class})
public class ContentProtectionTogglePreferenceControllerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock private PreferenceScreen mMockPreferenceScreen;

    @Mock private SettingsMainSwitchPreference mMockSwitchPreference;

    @Nullable private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    @DevicePolicyManager.ContentProtectionPolicy
    private int mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;

    private TestContentProtectionTogglePreferenceController mController;

    private int mSettingBackupValue;

    @Before
    public void setUp() {
        mController = new TestContentProtectionTogglePreferenceController();
        SettingsMainSwitchPreference switchPreference = new SettingsMainSwitchPreference(mContext);
        when(mMockPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(switchPreference);
        mSettingBackupValue = getContentProtectionGlobalSetting();
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 0);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                KEY_CONTENT_PROTECTION_PREFERENCE,
                mSettingBackupValue);
        ShadowUtils.reset();
    }

    @Test
    public void constructor_flagDisabled_doesNotFetchData() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.mCounterGetManagedProfile).isEqualTo(0);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
        assertThat(mController.mCounterGetContentProtectionPolicy).isEqualTo(0);
    }

    @Test
    public void constructor_flagEnabled_fetchesData() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        assertThat(mController.mCounterGetContentProtectionPolicy).isEqualTo(1);
    }

    @Test
    public void getAvailabilityStatus_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isChecked_noEnforcedAdmin_readsSettingsTrue() {
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_noEnforcedAdmin_readsSettingsFalse() {
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_noEnforcedAdmin_readsSettingsDefaultTrue() {
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcedAdmin_flagDisabled_false() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);
        setupForUpdateState();
        mController.updateState(mMockSwitchPreference);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_enforcedAdmin_flagEnabled_policyDisabled_false() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_enforcedAdmin_flagEnabled_policyEnabled_true() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcedAdmin_flagEnabled_policyNotControlled_readsSettingsTrue() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcedAdmin_flagEnabled_policyNotControlled_readsSettingsFalse() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_enforcedAdmin_flagEnabled_policyNotControlled_readsSettingsDefaultTrue() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void displayPreference() {
        setupForDisplayPreference();

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference).addOnSwitchChangeListener(mController);
    }

    @Test
    public void updateState_flagDisabled_noEnforcedAdmin() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(any());
    }

    @Test
    public void updateState_flagDisabled_enforcedAdmin() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
    }

    @Test
    public void updateState_flagEnabled_noEnforcedAdmin_policyDisabled() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(any());
    }

    @Test
    public void updateState_flagEnabled_noEnforcedAdmin_policyEnabled() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(any());
    }

    @Test
    public void updateState_flagEnabled_noEnforcedAdmin_policyNotControlled() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(any());
    }

    @Test
    public void updateState_flagEnabled_enforcedAdmin_policyDisabled() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
    }

    @Test
    public void updateState_flagEnabled_enforcedAdmin_policyEnabled() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
    }

    @Test
    public void updateState_flagEnabled_enforcedAdmin_policyNotControlled() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(any());
    }

    @Test
    public void onSwitchChanged_switchChecked_manuallyEnabled() {
        mController.setChecked(false);

        mController.onCheckedChanged(/* switchView= */ null, /* isChecked= */ true);

        assertThat(getContentProtectionGlobalSetting()).isEqualTo(1);
    }

    @Test
    public void onSwitchChanged_switchUnchecked_manuallyDisabled() {
        mController.onCheckedChanged(/* switchView= */ null, /* isChecked= */ false);

        assertThat(getContentProtectionGlobalSetting()).isEqualTo(-1);
    }

    private int getContentProtectionGlobalSetting() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 0);
    }

    private void setupForDisplayPreference() {
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mMockSwitchPreference);
        when(mMockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());
        mController = new TestContentProtectionTogglePreferenceController();
    }

    private void setupForUpdateState() {
        setupForDisplayPreference();
        mController.displayPreference(mMockPreferenceScreen);
    }

    private class TestContentProtectionTogglePreferenceController
            extends ContentProtectionTogglePreferenceController {

        public int mCounterGetManagedProfile;

        public int mCounterGetEnforcedAdmin;

        public int mCounterGetContentProtectionPolicy;

        TestContentProtectionTogglePreferenceController() {
            super(ContentProtectionTogglePreferenceControllerTest.this.mContext, "key");
        }

        @Override
        @Nullable
        protected UserHandle getManagedProfile() {
            mCounterGetManagedProfile++;
            return null;
        }

        @Override
        @Nullable
        protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin() {
            mCounterGetEnforcedAdmin++;
            return mEnforcedAdmin;
        }

        @Override
        @DevicePolicyManager.ContentProtectionPolicy
        protected int getContentProtectionPolicy(@Nullable UserHandle userHandle) {
            mCounterGetContentProtectionPolicy++;
            return mContentProtectionPolicy;
        }
    }
}
