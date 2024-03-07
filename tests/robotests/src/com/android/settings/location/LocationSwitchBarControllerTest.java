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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Switch;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class LocationSwitchBarControllerTest {

    @Mock
    private SettingsMainSwitchBar mSwitchBar;
    @Mock
    private Switch mSwitch;
    @Mock
    private LocationEnabler mEnabler;

    private Context mContext;
    private LocationSwitchBarController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ReflectionHelpers.setField(mSwitchBar, "mSwitch", mSwitch);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new LocationSwitchBarController(mContext, mSwitchBar, mLifecycle));
        ReflectionHelpers.setField(mController, "mLocationEnabler", mEnabler);
    }

    @Test
    public void onStart_shouldAddOnSwitchChangeListener() {
        mController.onStart();

        verify(mSwitchBar).addOnSwitchChangeListener(mController);
    }

    @Test
    public void onStop_shouldRemoveOnSwitchChangeListener() {
        mController.onStart();
        mController.onStop();

        verify(mSwitchBar).removeOnSwitchChangeListener(mController);
    }

    @Test
    public void onSwitchChanged_switchChecked_shouldSetLocationEnabled() {
        mController.onCheckedChanged(mSwitch, true);

        verify(mEnabler).setLocationEnabled(true);
    }

    @Test
    public void onSwitchChanged_switchUnchecked_shouldSetLocationDisabled() {
        mController.onCheckedChanged(mSwitch, false);

        verify(mEnabler).setLocationEnabled(false);
    }

    @Test
    public void onLocationModeChanged_hasEnforcedAdmin_shouldDisableSwitchByAdmin() {
        final RestrictedLockUtils.EnforcedAdmin admin =
                mock(RestrictedLockUtils.EnforcedAdmin.class);
        doReturn(admin).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mSwitchBar).setDisabledByAdmin(admin);
    }

    @Test
    public void onLocationModeChanged_Restricted_shouldDisableSwitchByAdmin() {
        final RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.EnforcedAdmin
                .createDefaultEnforcedAdminWithRestriction(UserManager.DISALLOW_SHARE_LOCATION);
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, true);

        verify(mSwitchBar).setDisabledByAdmin(admin);
    }

    @Test
    public void onLocationModeChanged_Restricted_shouldDisableSwitch() {
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(true).when(mEnabler).hasShareLocationRestriction(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mSwitchBar).setEnabled(true);
    }

    @Test
    public void onLocationModeChanged_notRestricted_shouldEnableSwitch() {
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mSwitchBar).setEnabled(true);
    }

    @Test
    public void onLocationModeChanged_locationOn_shouldCheckSwitch() {
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());
        when(mSwitchBar.isChecked()).thenReturn(false);
        doReturn(true).when(mEnabler).isEnabled(anyInt());

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mSwitchBar).setChecked(true);
    }

    @Test
    public void onLocationModeChanged_locationOff_shouldUncheckSwitch() {
        doReturn(null).when(mEnabler).getShareLocationEnforcedAdmin(anyInt());
        doReturn(false).when(mEnabler).hasShareLocationRestriction(anyInt());
        when(mSwitchBar.isChecked()).thenReturn(true);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_OFF, false);

        verify(mSwitchBar).setChecked(false);
    }
}
