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
package com.android.settings.development;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings.Global;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class VerifyAppsOverUsbPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private RestrictedSwitchPreference mPreference;

    @Mock
    private VerifyAppsOverUsbPreferenceController.RestrictedLockUtilsDelegate
            mRestrictedLockUtilsDelegate;

    private Context mContext;
    private VerifyAppsOverUsbPreferenceController mController;

    /** Convenience class for setting global int settings. */
    class GlobalSetter {
        public GlobalSetter set(String setting, int value) {
            Global.putInt(mContext.getContentResolver(), setting, value);
            return this;
        }
    }
    private final GlobalSetter mGlobals = new GlobalSetter();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ShadowApplication shadowContext = ShadowApplication.getInstance();
        mContext = spy(shadowContext.getApplicationContext());
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new VerifyAppsOverUsbPreferenceController(mContext);
        ReflectionHelpers.setField(
                mController, "mRestrictedLockUtils", mRestrictedLockUtilsDelegate);
    }

    private void setupVerifyBroadcastReceivers(boolean nonEmpty) {
        final List<ResolveInfo> resolveInfos = nonEmpty
                ? Collections.singletonList(mock(ResolveInfo.class))
                : Collections.<ResolveInfo>emptyList();
        when(mPackageManager.queryBroadcastReceivers((Intent) any(), anyInt()))
                .thenReturn(resolveInfos);
    }

    private void setupEnforcedAdmin(EnforcedAdmin result) {
        when(mRestrictedLockUtilsDelegate.checkIfRestrictionEnforced(
                (Context) any(), anyString(), anyInt())).thenReturn(result);
    }

    @Test
    public void updateState_preferenceCheckedWhenSettingIsOn() {
        setupVerifyBroadcastReceivers(true);
        setupEnforcedAdmin(null);
        mGlobals.set(Global.ADB_ENABLED, 1).set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1);
        mController.displayPreference(mScreen);
        mController.updatePreference();
        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_preferenceUncheckedWhenSettingIsOff() {
        setupVerifyBroadcastReceivers(true);
        setupEnforcedAdmin(null);
        mGlobals.set(Global.ADB_ENABLED, 1).set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 0);
        mController.displayPreference(mScreen);
        mController.updatePreference();
        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_preferenceUncheckedWhenNoAdb() {
        setupVerifyBroadcastReceivers(true);
        setupEnforcedAdmin(null);
        mGlobals.set(Global.ADB_ENABLED, 0).set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1);
        mController.displayPreference(mScreen);
        mController.updatePreference();
        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_preferenceUncheckedWhenVerifierIsOff() {
        setupVerifyBroadcastReceivers(true);
        setupEnforcedAdmin(null);
        mGlobals.set(Global.ADB_ENABLED, 1)
                .set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1)
                .set(Global.PACKAGE_VERIFIER_ENABLE, 0);
        mController.displayPreference(mScreen);
        mController.updatePreference();
        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_preferenceUncheckedWhenNoVerifyBroadcastReceivers() {
        setupVerifyBroadcastReceivers(false);
        setupEnforcedAdmin(null);
        mGlobals.set(Global.ADB_ENABLED, 1)
                .set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1);
        mController.displayPreference(mScreen);
        mController.updatePreference();
        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_preferenceDisabledWhenRestrictedByAdmin() {
        setupVerifyBroadcastReceivers(true);
        final EnforcedAdmin admin = new EnforcedAdmin();
        setupEnforcedAdmin(admin);
        mGlobals.set(Global.ADB_ENABLED, 1)
                .set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1);
        mController.displayPreference(mScreen);
        mController.updatePreference();
        verify(mPreference).setDisabledByAdmin(admin);
    }

    @Test
    public void updateState_preferenceRemovedWhenVerifierSettingsVisibleIsOff() {
        setupVerifyBroadcastReceivers(true);
        mGlobals.set(Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 0);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(anyInt())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        verify(mScreen).removePreference(mPreference);
    }
}