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
package com.android.settings.location;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class LocationForPrivateProfilePreferenceControllerTest {

    @Mock
    private RestrictedSwitchPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private LocationEnabler mEnabler;
    @Mock
    private UserHandle mUserHandle;

    private Context mContext;
    private LocationForPrivateProfilePreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private LocationSettings mLocationSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        mockPrivateProfile();
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLocationSettings = spy(new LocationSettings());
        when(mLocationSettings.getSettingsLifecycle()).thenReturn(mLifecycle);
        mController = new LocationForPrivateProfilePreferenceController(mContext, "key");
        mController.init(mLocationSettings);
        ReflectionHelpers.setField(mController, "mLocationEnabler", mEnabler);
        when(mScreen.findPreference(any())).thenReturn(mPreference);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
        when(mPreference.isVisible()).thenReturn(true);
    }

    @Test
    public void handlePreferenceTreeClick_preferenceChecked_shouldSetRestrictionAndOnSummary() {
        mController.displayPreference(mScreen);
        when(mPreference.isChecked()).thenReturn(true);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mUserManager)
                .setUserRestriction(UserManager.DISALLOW_SHARE_LOCATION, false, mUserHandle);
        verify(mPreference).setSummary(R.string.switch_on_text);
    }

    @Test
    public void handlePreferenceTreeClick_preferenceUnchecked_shouldSetRestritionAndOffSummary() {
        mController.displayPreference(mScreen);
        when(mPreference.isChecked()).thenReturn(false);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mUserManager)
                .setUserRestriction(UserManager.DISALLOW_SHARE_LOCATION, true, mUserHandle);
        verify(mPreference).setSummary(R.string.switch_off_text);
    }

    @Test
    public void onLocationModeChanged_disabledByAdmin_shouldDisablePreference() {
        mController.displayPreference(mScreen);
        final EnforcedAdmin admin = mock(EnforcedAdmin.class);
        doReturn(admin).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setDisabledByAdmin(any());
    }

    @Test
    public void onLocationModeChanged_locationOff_shouldDisablePreference() {
        mController.displayPreference(mScreen);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_OFF, false);

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
        verify(mPreference).setSummary(R.string.location_app_permission_summary_location_off);
    }

    @Test
    public void onLocationModeChanged_locationOn_shouldEnablePreference() {
        mController.displayPreference(mScreen);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());
        doReturn(true).when(mEnabler).isEnabled(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference, times(2)).setEnabled(true);
        verify(mPreference).setSummary(R.string.switch_on_text);
    }

    @Test
    public void onLocationModeChanged_noRestriction_shouldCheckedPreference() {
        mController.displayPreference(mScreen);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());
        doReturn(true).when(mEnabler).isEnabled(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void onLocationModeChanged_hasRestriction_shouldCheckedPreference() {
        mController.displayPreference(mScreen);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(true).when(mEnabler).hasShareLocationRestriction(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setChecked(false);
    }

    private void mockPrivateProfile() {
        final List<UserHandle> userProfiles = new ArrayList<>();
        doReturn(9).when(mUserHandle).getIdentifier();
        userProfiles.add(mUserHandle);
        doReturn(userProfiles).when(mUserManager).getUserProfiles();
        doReturn(new UserInfo(
                9,
                "user 9",
                "",
                0,
                UserManager.USER_TYPE_PROFILE_PRIVATE)).when(mUserManager).getUserInfo(9);
    }
}
