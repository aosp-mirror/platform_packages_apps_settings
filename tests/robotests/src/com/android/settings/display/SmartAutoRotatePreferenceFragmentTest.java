/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static android.provider.Settings.Secure.DEVICE_STATE_ROTATION_LOCK_LOCKED;

import static com.android.settings.display.SmartAutoRotatePreferenceFragment.AUTO_ROTATE_MAIN_SWITCH_PREFERENCE_KEY;
import static com.android.settings.display.SmartAutoRotatePreferenceFragment.AUTO_ROTATE_SWITCH_PREFERENCE_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.view.View;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.ResolveInfoBuilder;
import com.android.settings.testutils.shadow.ShadowDeviceStateRotationLockSettingsManager;
import com.android.settings.testutils.shadow.ShadowRotationPolicy;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.devicestate.DeviceStateRotationLockSettingsManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowDeviceStateRotationLockSettingsManager.class,
        ShadowRotationPolicy.class
})
public class SmartAutoRotatePreferenceFragmentTest {

    private static final int STATE_FOLDED = 0;
    private static final int STATE_HALF_FOLDED = 1;
    private static final int STATE_UNFOLDED = 2;
    private static final int STATE_REAR_DISPLAY = 3;

    private static final String PACKAGE_NAME = "package_name";

    private SmartAutoRotatePreferenceFragment mFragment;

    @Mock
    private PackageManager mPackageManager;

    @Mock
    private View mView;

    @Mock
    private SettingsActivity mActivity;

    @Mock
    private Preference mRotateSwitchPreference;
    private Resources mResources;
    private Context mContext;

    @Mock
    private Preference mRotateMainSwitchPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        ContentResolver mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        doReturn(PACKAGE_NAME).when(mPackageManager).getRotationResolverPackageName();
        doReturn(PackageManager.PERMISSION_GRANTED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);

        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);

        final ResolveInfo resolveInfo = new ResolveInfoBuilder(PACKAGE_NAME).build();
        resolveInfo.serviceInfo = new ServiceInfo();
        when(mPackageManager.resolveService(any(), anyInt())).thenReturn(resolveInfo);

        mFragment = spy(new SmartAutoRotatePreferenceFragment());
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mActivity.getResources()).thenReturn(mResources);

        doReturn(mView).when(mFragment).getView();

        when(mFragment.findPreference(AUTO_ROTATE_SWITCH_PREFERENCE_KEY)).thenReturn(
                mRotateSwitchPreference);

        when(mFragment.findPreference(AUTO_ROTATE_MAIN_SWITCH_PREFERENCE_KEY))
                .thenReturn(mRotateMainSwitchPreference);

        when(mResources.getIntArray(com.android.internal.R.array.config_foldedDeviceStates))
                .thenReturn(new int[] {STATE_FOLDED});
        when(mResources.getIntArray(com.android.internal.R.array.config_halfFoldedDeviceStates))
                .thenReturn(new int[] {STATE_HALF_FOLDED});
        when(mResources.getIntArray(com.android.internal.R.array.config_openDeviceStates))
                .thenReturn(new int[] {STATE_UNFOLDED});
        when(mResources.getIntArray(com.android.internal.R.array.config_rearDisplayDeviceStates))
                .thenReturn(new int[] {STATE_REAR_DISPLAY});
    }

    @Test
    public void createHeader_faceDetectionSupported_switchBarIsEnabled() {
        ShadowDeviceStateRotationLockSettingsManager.setDeviceStateRotationLockEnabled(false);
        mFragment.createHeader(mActivity);

        verify(mRotateMainSwitchPreference, never()).setVisible(false);
        verify(mRotateSwitchPreference, times(1)).setVisible(false);
    }

    @Test
    public void createHeader_deviceStateRotationSupported_switchBarIsDisabled() {
        ShadowRotationPolicy.setRotationSupported(true);
        ShadowDeviceStateRotationLockSettingsManager.setDeviceStateRotationLockEnabled(true);

        mFragment.createHeader(mActivity);

        verify(mRotateMainSwitchPreference, times(1)).setVisible(false);
        verify(mRotateSwitchPreference, never()).setVisible(false);
    }

    @Test
    public void createHeader_faceDetectionUnSupported_switchBarIsDisabled() {
        doReturn(null).when(mPackageManager).getRotationResolverPackageName();

        mFragment.createHeader(mActivity);

        verify(mRotateMainSwitchPreference, times(1)).setVisible(false);
        verify(mRotateSwitchPreference, never()).setVisible(false);
    }

    @Test
    public void createHeader_faceDetectionNotEnabledByConfig_switchBarIsDisabled() {
        doReturn(false).when(mResources).getBoolean(
                R.bool.config_auto_rotate_face_detection_available);

        mFragment.createHeader(mActivity);

        verify(mRotateMainSwitchPreference, times(1)).setVisible(false);
        verify(mRotateSwitchPreference, never()).setVisible(false);
    }

    @Test
    public void createPreferenceControllers_noSettableDeviceStates_returnsEmptyList() {
        enableDeviceStateSettableRotationStates(new String[] {}, new String[] {});

        List<AbstractPreferenceController> preferenceControllers =
                mFragment.createPreferenceControllers(mContext);

        assertThat(preferenceControllers).isEmpty();
    }

    @Test
    public void createPreferenceControllers_settableDeviceStates_returnsDeviceStateControllers() {
        enableDeviceStateSettableRotationStates(
                new String[] {
                    STATE_FOLDED + ":" + DEVICE_STATE_ROTATION_LOCK_LOCKED,
                    STATE_UNFOLDED + ":" + DEVICE_STATE_ROTATION_LOCK_LOCKED
                },
                new String[] {"Folded", "Unfolded"});

        List<AbstractPreferenceController> preferenceControllers =
                mFragment.createPreferenceControllers(mContext);

        assertThat(preferenceControllers).hasSize(2);
        assertThat(preferenceControllers.get(0))
                .isInstanceOf(DeviceStateAutoRotateSettingController.class);
        assertThat(preferenceControllers.get(1))
                .isInstanceOf(DeviceStateAutoRotateSettingController.class);
    }

    @Test
    public void setupFooter_linkAddedWhenAppropriate() {
        doReturn("").when(mFragment).getText(anyInt());
        doReturn("").when(mFragment).getString(anyInt());
        mFragment.setupFooter();
        verify(mFragment, never()).addHelpLink();

        doReturn("testString").when(mFragment).getText(anyInt());
        doReturn("testString").when(mFragment).getString(anyInt());
        mFragment.setupFooter();
        verify(mFragment, times(1)).addHelpLink();
    }

    private void enableDeviceStateSettableRotationStates(
            String[] settableStates, String[] settableStatesDescriptions) {
        when(mResources.getStringArray(
                        com.android.internal.R.array.config_perDeviceStateRotationLockDefaults))
                .thenReturn(settableStates);
        when(mResources.getStringArray(R.array.config_settableAutoRotationDeviceStatesDescriptions))
                .thenReturn(settableStatesDescriptions);
        when(mResources.getBoolean(R.bool.config_auto_rotate_face_detection_available))
                .thenReturn(true);
        DeviceStateRotationLockSettingsManager.resetInstance();
        DeviceStateRotationLockSettingsManager.getInstance(mContext)
                .resetStateForTesting(mResources);
    }
}
