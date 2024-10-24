/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.hardware.devicestate.DeviceState.PROPERTY_EMULATED_ONLY;
import static android.hardware.devicestate.DeviceState.PROPERTY_FEATURE_REAR_DISPLAY;
import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_INNER_PRIMARY;
import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY;
import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_CLOSED;
import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_HALF_OPEN;
import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_OPEN;
import static android.provider.Settings.Secure.DEVICE_STATE_ROTATION_LOCK_LOCKED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.devicestate.DeviceState;
import android.hardware.devicestate.DeviceStateManager;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.devicestate.DeviceStateRotationLockSettingsManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class DeviceStateAutoRotateDetailsFragmentTest {
    private static final DeviceState DEVICE_STATE_FOLDED = new DeviceState(
            new DeviceState.Configuration.Builder(/* identifier= */ 0, "FOLDED")
                    .setSystemProperties(Set.of(
                            PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY))
                    .setPhysicalProperties(Set.of(
                            PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_CLOSED))
                    .build());
    private static final DeviceState DEVICE_STATE_HALF_FOLDED = new DeviceState(
            new DeviceState.Configuration.Builder(/* identifier= */ 1, "HALF_FOLDED")
                    .setSystemProperties(Set.of(
                            PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_INNER_PRIMARY))
                    .setPhysicalProperties(Set.of(
                            PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_HALF_OPEN))
                    .build());
    private static final DeviceState DEVICE_STATE_UNFOLDED = new DeviceState(
            new DeviceState.Configuration.Builder(/* identifier= */ 2, "UNFOLDED")
                    .setSystemProperties(Set.of(
                            PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_INNER_PRIMARY))
                    .setPhysicalProperties(Set.of(
                            PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_OPEN))
                    .build());
    private static final DeviceState DEVICE_STATE_REAR_DISPLAY = new DeviceState(
            new DeviceState.Configuration.Builder(/* identifier= */ 3, "REAR_DISPLAY")
                    .setSystemProperties(Set.of(
                            PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY,
                            PROPERTY_FEATURE_REAR_DISPLAY, PROPERTY_EMULATED_ONLY))
                    .setPhysicalProperties(Set.of(
                            PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_CLOSED))
                    .build());

    private final DeviceStateAutoRotateDetailsFragment mFragment =
            spy(new DeviceStateAutoRotateDetailsFragment());
    private final Context mContext = spy(RuntimeEnvironment.application);
    private final Resources mResources = spy(mContext.getResources());
    @Mock
    private DeviceStateManager mDeviceStateManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getResources()).thenReturn(mResources);
        doReturn(mDeviceStateManager).when(mContext).getSystemService(DeviceStateManager.class);
        setUpPostureMappings();
    }

    @Test
    public void getMetricsCategory_returnsAutoRotateSettings() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.DISPLAY_DEVICE_STATE_AUTO_ROTATE_SETTINGS);
    }

    @Test
    public void getPreferenceScreenResId_returnsDeviceStateAutoRotationSettings() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.device_state_auto_rotate_settings);
    }

    @Test
    public void createPreferenceControllers_settableDeviceStates_returnsDeviceStateControllers() {
        enableDeviceStateSettableRotationStates(
                new String[]{DEVICE_STATE_FOLDED.getIdentifier() + ":"
                        + DEVICE_STATE_ROTATION_LOCK_LOCKED,
                        DEVICE_STATE_UNFOLDED.getIdentifier() + ":"
                                + DEVICE_STATE_ROTATION_LOCK_LOCKED},
                new String[]{"Folded", "Unfolded"});

        List<AbstractPreferenceController> preferenceControllers =
                mFragment.createPreferenceControllers(mContext);

        assertThat(preferenceControllers).hasSize(2);
        assertThat(preferenceControllers.get(0)).isInstanceOf(
                DeviceStateAutoRotateSettingController.class);
        assertThat(preferenceControllers.get(1)).isInstanceOf(
                DeviceStateAutoRotateSettingController.class);
    }

    @Test
    public void createPreferenceControllers_noSettableDeviceStates_returnsEmptyList() {
        enableDeviceStateSettableRotationStates(new String[]{}, new String[]{});

        List<AbstractPreferenceController> preferenceControllers =
                mFragment.createPreferenceControllers(mContext);

        assertThat(preferenceControllers).isEmpty();
    }

    private void enableDeviceStateSettableRotationStates(String[] settableStates,
            String[] settableStatesDescriptions) {
        when(mResources.getStringArray(
                com.android.internal.R.array.config_perDeviceStateRotationLockDefaults)).thenReturn(
                settableStates);
        when(mResources.getStringArray(
                R.array.config_settableAutoRotationDeviceStatesDescriptions)).thenReturn(
                settableStatesDescriptions);
        DeviceStateRotationLockSettingsManager.resetInstance();
        DeviceStateRotationLockSettingsManager.getInstance(mContext)
                .resetStateForTesting(mResources);
    }

    // Sets up posture mappings for PosturesHelper
    private void setUpPostureMappings() {
        when(mResources.getIntArray(
                com.android.internal.R.array.config_foldedDeviceStates)).thenReturn(
                    new int[]{DEVICE_STATE_FOLDED.getIdentifier()});
        when(mResources.getIntArray(
                com.android.internal.R.array.config_halfFoldedDeviceStates)).thenReturn(
                    new int[]{DEVICE_STATE_HALF_FOLDED.getIdentifier()});
        when(mResources.getIntArray(
                com.android.internal.R.array.config_openDeviceStates)).thenReturn(
                    new int[]{DEVICE_STATE_UNFOLDED.getIdentifier()});
        when(mResources.getIntArray(
                com.android.internal.R.array.config_rearDisplayDeviceStates)).thenReturn(
                    new int[]{DEVICE_STATE_REAR_DISPLAY.getIdentifier()});
        when(mDeviceStateManager.getSupportedDeviceStates()).thenReturn(
                List.of(DEVICE_STATE_FOLDED, DEVICE_STATE_HALF_FOLDED, DEVICE_STATE_UNFOLDED,
                        DEVICE_STATE_REAR_DISPLAY));
    }
}
