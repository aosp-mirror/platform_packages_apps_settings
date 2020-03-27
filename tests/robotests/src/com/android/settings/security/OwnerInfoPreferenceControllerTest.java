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

import static com.android.settings.security.OwnerInfoPreferenceController.KEY_OWNER_INFO;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.users.OwnerInfoSettings;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class OwnerInfoPreferenceControllerTest {

    @Mock
    private ObservablePreferenceFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceManager mPreferenceManager;
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
        mContext = spy(RuntimeEnvironment.application);

        when(mFragment.isAdded()).thenReturn(true);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mPreference.getContext()).thenReturn(mContext);
        when(mFragment.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragment.getSettingsLifecycle()).thenReturn(mock(Lifecycle.class));
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);

        mController = spy(new OwnerInfoPreferenceController(mContext, mFragment));
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
    public void handlePreferenceTreeClick_shouldLaunchOwnerInfoSettings() {
        final RestrictedPreference preference = new RestrictedPreference(mContext);
        preference.setKey(KEY_OWNER_INFO);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(preference);
        doReturn(false).when(mController).isDeviceOwnerInfoEnabled();
        doReturn(false).when(mLockPatternUtils).isLockScreenDisabled(anyInt());
        mController.displayPreference(mScreen);
        mController.updateEnableState();

        mController.handlePreferenceTreeClick(preference);

        verify(mFragment.getFragmentManager().beginTransaction())
                .add(any(OwnerInfoSettings.class), anyString());
    }
}