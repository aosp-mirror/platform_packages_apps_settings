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
package com.android.settings.security;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.OwnerInfoSettings;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class OwnerInfoPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private RestrictedPreference mPreference;
    @Mock
    private LockPatternUtils mLockPatternUtils;

    private Context mContext;
    private OwnerInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        mContext = spy(shadowContext.getApplicationContext());

        when(mFragment.isAdded()).thenReturn(true);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);

        mController = spy(new OwnerInfoPreferenceController(mContext, mFragment, null));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        ReflectionHelpers.setField(mController, "mLockPatternUtils", mLockPatternUtils);
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onResume_shouldUpdateEnableState() {
        mController.onResume();

        verify(mController).updateEnableState();
    }

    @Test
    public void onResume_shouldUpdateSummary() {
        mController.onResume();

        verify(mController).updateSummary();
    }

    @Test
    public void updateSummary_deviceOwnerInfoEnabled_shouldSetDeviceOwnerInfoSummary() {
        final String deviceOwnerInfo = "Test Device Owner Info";
        doReturn(true).when(mController).isDeviceOwnerInfoEnabled();
        doReturn(deviceOwnerInfo).when(mController).getDeviceOwnerInfo();
        mController.displayPreference(mScreen);

        mController.updateSummary();

        verify(mPreference).setSummary(deviceOwnerInfo);
    }

    @Test
    public void updateSummary_ownerInfoEnabled_shouldSetOwnerInfoSummary() {
        final String ownerInfo = "Test Owner Info";
        doReturn(false).when(mController).isDeviceOwnerInfoEnabled();
        doReturn(true).when(mController).isOwnerInfoEnabled();
        doReturn(ownerInfo).when(mController).getOwnerInfo();
        mController.displayPreference(mScreen);

        mController.updateSummary();

        verify(mPreference).setSummary(ownerInfo);
    }

    @Test
    public void updateSummary_ownerInfoDisabled_shouldSetDefaultSummary() {
        doReturn(false).when(mController).isDeviceOwnerInfoEnabled();
        doReturn(false).when(mController).isOwnerInfoEnabled();
        mController.displayPreference(mScreen);

        mController.updateSummary();

        verify(mPreference).setSummary(mContext.getString(
            com.android.settings.R.string.owner_info_settings_summary));
    }

    @Test
    public void updateEnableState_deviceOwnerInfoEnabled_shouldSetDisabledByAdmin() {
        doReturn(true).when(mController).isDeviceOwnerInfoEnabled();
        doReturn(mock(EnforcedAdmin.class)).when(mController).getDeviceOwner();
        mController.displayPreference(mScreen);

        mController.updateEnableState();

        verify(mPreference).setDisabledByAdmin(any(EnforcedAdmin.class));
    }

    @Test
    public void updateEnableState_lockScreenDisabled_shouldDisablePreference() {
        doReturn(false).when(mController).isDeviceOwnerInfoEnabled();
        doReturn(true).when(mLockPatternUtils).isLockScreenDisabled(anyInt());
        mController.displayPreference(mScreen);

        mController.updateEnableState();

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void updateEnableState_lockScreenEnabled_shouldEnablePreference() {
        doReturn(false).when(mController).isDeviceOwnerInfoEnabled();
        doReturn(false).when(mLockPatternUtils).isLockScreenDisabled(anyInt());
        mController.displayPreference(mScreen);

        mController.updateEnableState();

        verify(mPreference).setEnabled(true);
    }

    @Test
    public void performClick_shouldLaunchOwnerInfoSettings() {
        final ShadowApplication application = ShadowApplication.getInstance();
        final RestrictedPreference preference =
            new RestrictedPreference(application.getApplicationContext());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(preference);
        doReturn(false).when(mController).isDeviceOwnerInfoEnabled();
        doReturn(false).when(mLockPatternUtils).isLockScreenDisabled(anyInt());
        mController.displayPreference(mScreen);
        mController.updateEnableState();

        preference.performClick();

        verify(mFragment).getFragmentManager();
        verify(mFragment.getFragmentManager().beginTransaction())
            .add(any(OwnerInfoSettings.class), anyString());
    }

}