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
package com.android.settings.location;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class LocationModePreferenceControllerTest {

    @Mock
    private LocationSettings mFragment;
    @Mock
    private SettingsActivity mActivity;
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;

    private Context mContext;
    private LocationModePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new LocationModePreferenceController(mContext, mFragment, new Lifecycle());
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void onLocationModeChanged_locationOff_shouldDisablePreference() {
        when(mUserManager.hasUserRestriction(any())).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_OFF, false);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onLocationModeChanged_restricted_shouldDisablePreference() {
        when(mUserManager.hasUserRestriction(any())).thenReturn(true);
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onLocationModeChanged_locationOnNotRestricted_shouldEnablePreference() {
        when(mUserManager.hasUserRestriction(any())).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setEnabled(true);
    }

    @Test
    public void onLocationModeChanged_shouldUpdateSummary() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setSummary(anyInt());
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartLocationModeFragment() {
        final Preference preference = new Preference(mContext);
        preference.setKey(mController.getPreferenceKey());

        mController.handlePreferenceTreeClick(preference);

        verify(mActivity).startPreferencePanel(any(), eq(LocationMode.class.getName()), any(),
                eq(R.string.location_mode_screen_title), any(), any(), anyInt());
    }

}
