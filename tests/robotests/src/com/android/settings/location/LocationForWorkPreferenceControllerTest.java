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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class LocationForWorkPreferenceControllerTest {

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
    private LocationForWorkPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private LocationSettings mLocationSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLocationSettings = spy(new LocationSettings());
        when(mLocationSettings.getSettingsLifecycle()).thenReturn(mLifecycle);
        mController = spy(new LocationForWorkPreferenceController(mContext, "key"));
        mController.init(mLocationSettings);
        mockManagedProfile();
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
        doReturn(false).when(mEnabler).isManagedProfileRestrictedByBase();

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setDisabledByAdmin(any());
    }

    @Test
    public void onLocationModeChanged_locationOff_shouldDisablePreference() {
        mController.displayPreference(mScreen);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).isManagedProfileRestrictedByBase();

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_OFF, false);

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
        verify(mPreference).setSummary(R.string.location_app_permission_summary_location_off);
    }

    @Test
    public void onLocationModeChanged_locationOn_shouldEnablePreference() {
        mController.displayPreference(mScreen);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).isManagedProfileRestrictedByBase();
        doReturn(true).when(mEnabler).isEnabled(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setEnabled(true);
        verify(mPreference).setSummary(R.string.switch_on_text);
    }

    @Test
    public void onLocationModeChanged_noRestriction_shouldCheckedPreference() {
        mController.displayPreference(mScreen);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).isManagedProfileRestrictedByBase();
        doReturn(true).when(mEnabler).isEnabled(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void onLocationModeChanged_hasRestriction_shouldCheckedPreference() {
        mController.displayPreference(mScreen);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(true).when(mEnabler).isManagedProfileRestrictedByBase();

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setChecked(false);
    }

    private void mockManagedProfile() {
        final List<UserHandle> userProfiles = new ArrayList<>();
        when(mUserHandle.getIdentifier()).thenReturn(5);
        userProfiles.add(mUserHandle);
        when(mUserManager.getUserProfiles()).thenReturn(userProfiles);
        when(mUserManager.getUserHandle()).thenReturn(1);
        when(mUserManager.getUserInfo(5))
                .thenReturn(new UserInfo(5, "user 5", UserInfo.FLAG_MANAGED_PROFILE));
    }
}
