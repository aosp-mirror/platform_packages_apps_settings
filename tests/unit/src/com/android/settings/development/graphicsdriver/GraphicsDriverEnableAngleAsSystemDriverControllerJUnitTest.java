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

package com.android.settings.development.graphicsdriver;

import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.ANGLE_DRIVER_SUFFIX;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.Injector;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.PROPERTY_DEBUG_ANGLE_DEVELOPER_OPTION;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.PROPERTY_PERSISTENT_GRAPHICS_EGL;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.SystemProperties;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.development.DevelopmentSettingsDashboardFragment;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class GraphicsDriverEnableAngleAsSystemDriverControllerJUnitTest {
    private Context mContext;
    private SwitchPreference mPreference;

    private GraphicsDriverEnableAngleAsSystemDriverController mController;

    // Signal to wait for SystemProperty values changed
    private static class PropertyChangeSignal {
        private CountDownLatch mCountDownLatch;

        private Runnable mCountDownJob;

        PropertyChangeSignal() {
            mCountDownLatch = new CountDownLatch(1);
            mCountDownJob =
                    new Runnable() {
                        @Override
                        public void run() {
                            mCountDownLatch.countDown();
                        }
                    };
        }

        public Runnable getCountDownJob() {
            return mCountDownJob;
        }

        public void wait(int timeoutInMilliSeconds) {
            try {
                mCountDownLatch.await(timeoutInMilliSeconds, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Mock private DevelopmentSettingsDashboardFragment mFragment;

    @Mock private GraphicsDriverSystemPropertiesWrapper mSystemPropertiesMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mContext = ApplicationProvider.getApplicationContext();
        when(mSystemPropertiesMock.getBoolean(eq(PROPERTY_DEBUG_ANGLE_DEVELOPER_OPTION),
                                              anyBoolean())).thenReturn(true);

        // Construct a GraphicsDriverEnableAngleAsSystemDriverController with two Overrides:
        // 1) Override the mSystemProperties with mSystemPropertiesMock,
        // so we can force the SystemProperties with values we need to run tests.
        // 2) Override the showRebootDialog() to do nothing.
        // We do not need to pop up the reboot dialog in the test.
        // 3) Override the rebootDevice() to do nothing.
        mController = new GraphicsDriverEnableAngleAsSystemDriverController(
                mContext,
                mFragment,
                new Injector() {
                    @Override
                    public GraphicsDriverSystemPropertiesWrapper
                            createSystemPropertiesWrapper() {
                            return mSystemPropertiesMock;
                        }
                    }) {
                    @Override
                    void showRebootDialog() {
                        // do nothing
                    }

                    @Override
                    void rebootDevice(Context context) {
                        // do nothing
                    }
                };

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        screen.addPreference(mPreference);
        mController.displayPreference(screen);
    }

    @Test
    public void onPreferenceChange_switchOn_shouldEnableAngleAsSystemDriver() {
        // Step 1: toggle the switch "Enable ANGLE" on
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal.getCountDownJob());
        mController.onPreferenceChange(mPreference, true);
        propertyChangeSignal.wait(100);

        // Step 2: verify results
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo(ANGLE_DRIVER_SUFFIX);

        // Step 3: clean up
        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(propertyChangeSignal.getCountDownJob());
    }

    @Test
    public void onPreferenceChange_switchOff_shouldDisableAngleAsSystemDriver() {
        // Step 1: toggle the switch "Enable ANGLE" off
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal.getCountDownJob());
        mController.onPreferenceChange(mPreference, false);
        propertyChangeSignal.wait(100);

        // Step 2: verify results
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");

        // Step 3: clean up
        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(propertyChangeSignal.getCountDownJob());
    }

    @Test
    public void updateState_PreferenceShouldEnabled() {
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_angleIsSystemGLESDriver_PreferenceShouldBeChecked() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(ANGLE_DRIVER_SUFFIX);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_angleIsNotSystemGLESDriver_PreferenceShouldNotBeChecked() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any())).thenReturn("");
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onDeveloperOptionSwitchDisabled_angleShouldNotBeSystemGLESDriver() {
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal1 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal1.getCountDownJob());

        // Test that onDeveloperOptionSwitchDisabled,
        // persist.graphics.egl updates to ""
        mController.onDeveloperOptionsSwitchDisabled();
        propertyChangeSignal1.wait(100);
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");

        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(propertyChangeSignal1.getCountDownJob());
    }

    @Test
    public void onDeveloperOptionSwitchDisabled_PreferenceShouldNotBeChecked() {
        mController.onDeveloperOptionsSwitchDisabled();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onDeveloperOptionSwitchDisabled_PreferenceShouldDisabled() {
        mController.onDeveloperOptionsSwitchDisabled();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onRebootCancelled_ToggleSwitchFromOnToOff() {
        // Step 1: Toggle the "Enable ANGLE" switch on
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal1 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal1.getCountDownJob());
        mController.onPreferenceChange(mPreference, true);
        // Block the following code execution until the "persist.graphics.egl" property value is
        // changed.
        propertyChangeSignal1.wait(100);

        // Step 2: Cancel reboot
        PropertyChangeSignal propertyChangeSignal2 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal2.getCountDownJob());
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL));
        mController.onRebootCancelled();
        mController.onRebootDialogDismissed();
        // Block the following code execution until the "persist.graphics.egl" property valye is
        // changed.
        propertyChangeSignal2.wait(100);

        // Step 3: Verify results
        // 1) Test that persist.graphics.egl is changed back to "".
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");
        // 2) Test that the switch is set to unchecked.
        assertThat(mPreference.isChecked()).isFalse();

        // Step 4: Clean up
        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(propertyChangeSignal1.getCountDownJob());
        SystemProperties.removeChangeCallback(propertyChangeSignal2.getCountDownJob());
    }

    @Test
    public void onRebootCancelled_ToggleSwitchFromOffToOn() {
        // Step 1: Toggle off the switch "Enable ANGLE"
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal1 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal1.getCountDownJob());
        mController.onPreferenceChange(mPreference, false);
        // Block the following code execution until the "persist.graphics.egl" property value is
        // changed.
        propertyChangeSignal1.wait(100);

        // Step 2: Cancel reboot
        PropertyChangeSignal propertyChangeSignal2 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal2.getCountDownJob());
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL));
        mController.onRebootCancelled();
        mController.onRebootDialogDismissed();
        // Block the following code execution until the "persist.graphics.egl" property valye is
        // changed.
        propertyChangeSignal2.wait(100);

        // Step 3: Verify results
        // 1) Test that persist.graphics.egl is changed back to "ANGLE"
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo(ANGLE_DRIVER_SUFFIX);
        // 2) Test that the switch is set to checked
        assertThat(mPreference.isChecked()).isTrue();

        // Step 4: Clean up
        // Done with the test, remove the callback.
        SystemProperties.removeChangeCallback(propertyChangeSignal1.getCountDownJob());
        SystemProperties.removeChangeCallback(propertyChangeSignal2.getCountDownJob());
    }

    @Test
    public void onRebootDialogDismissed_ToggleSwitchFromOnToOff() {
        // Step 1: Toggle on the switch "Enable ANGLE"
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal1 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal1.getCountDownJob());
        mController.onPreferenceChange(mPreference, true);
        // Block the following code execution until the "persist.graphics.egl" property value is
        // changed.
        propertyChangeSignal1.wait(100);

        // Step 2: Dismiss the reboot dialog
        PropertyChangeSignal propertyChangeSignal2 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal2.getCountDownJob());
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL));
        mController.onRebootDialogDismissed();
        // Block the following code execution until the "persist.graphics.egl" property valye is
        // changed.
        propertyChangeSignal2.wait(100);

        // Step 3: Verify results
        // 1) Test that persist.graphics.egl is changed back to "".
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");
        // 2) Test that the switch is set to unchecked.
        assertThat(mPreference.isChecked()).isFalse();

        // Step 4: Clean up
        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(propertyChangeSignal1.getCountDownJob());
        SystemProperties.removeChangeCallback(propertyChangeSignal2.getCountDownJob());
    }

    @Test
    public void onRebootDialogDismissed_ToggleSwitchFromOffToOn() {
        // Step 1: Toggle on the switch "Enable ANGLE"
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal1 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal1.getCountDownJob());
        mController.onPreferenceChange(mPreference, false);
        // Block the following code execution until the "persist.graphics.egl" property value is
        // changed.
        propertyChangeSignal1.wait(100);

        // Step 2: Dismiss the reboot dialog
        PropertyChangeSignal propertyChangeSignal2 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal2.getCountDownJob());
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL));
        mController.onRebootDialogDismissed();
        // Block the following code execution until the "persist.graphics.egl" property valye is
        // changed.
        propertyChangeSignal2.wait(100);

        // Step 3: Verify results
        // 1) Test that persist.graphics.egl is changed back to "ANGLE"
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo(ANGLE_DRIVER_SUFFIX);
        // 2) Test that the switch is set to checked
        assertThat(mPreference.isChecked()).isTrue();

        // Step 4: Clean up
        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(propertyChangeSignal1.getCountDownJob());
        SystemProperties.removeChangeCallback(propertyChangeSignal2.getCountDownJob());
    }

    @Test
    public void onRebootDialogConfirmed_ToggleSwitchOnRemainsOn() {
        // Step 1: Toggle on the switch "Enable ANGLE"
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal1 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal1.getCountDownJob());
        mController.onPreferenceChange(mPreference, true);
        // Block the following code execution until the "persist.graphics.egl" property value is
        // changed.
        propertyChangeSignal1.wait(100);

        // Step 2: Confirm reboot
        PropertyChangeSignal propertyChangeSignal2 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal2.getCountDownJob());
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL));
        mController.onRebootConfirmed(mContext);
        mController.onRebootDialogDismissed();
        // Block the following code execution until the "persist.graphics.egl" property valye is
        // changed.
        propertyChangeSignal2.wait(100);

        // Step 3: Verify Results
        // Test that persist.graphics.egl remains to be "ANGLE"
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo(ANGLE_DRIVER_SUFFIX);

        // Step 4: Clean up
        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(propertyChangeSignal1.getCountDownJob());
        SystemProperties.removeChangeCallback(propertyChangeSignal2.getCountDownJob());
    }

    @Test
    public void onRebootDialogConfirmed_ToggleSwitchOffRemainsOff() {
        // Step 1: Toggle off the switch "Enable ANGLE"
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        PropertyChangeSignal propertyChangeSignal1 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal1.getCountDownJob());
        mController.onPreferenceChange(mPreference, false);
        // Block the following code execution until the "persist.graphics.egl" property value is
        // changed.
        propertyChangeSignal1.wait(100);

        // Step 2: Confirm reboot
        PropertyChangeSignal propertyChangeSignal2 = new PropertyChangeSignal();
        SystemProperties.addChangeCallback(propertyChangeSignal2.getCountDownJob());
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL));
        mController.onRebootConfirmed(mContext);
        mController.onRebootDialogDismissed();
        // Block the following code execution until the "persist.graphics.egl" property valye is
        // changed.
        propertyChangeSignal2.wait(100);

        // Step 3: Verify Results
        // Test that persist.graphics.egl remains to be ""
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");

        // Step 4: Clean up
        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(propertyChangeSignal1.getCountDownJob());
        SystemProperties.removeChangeCallback(propertyChangeSignal2.getCountDownJob());
    }

    @Test
    public void updateState_DeveloperOptionPropertyIsFalse() {
        // Test that when debug.graphics.angle.developeroption.enable is false:
        when(mSystemPropertiesMock.getBoolean(eq(PROPERTY_DEBUG_ANGLE_DEVELOPER_OPTION),
                                              anyBoolean())).thenReturn(false);

        // 1. "Enable ANGLE" switch is on, the switch should be enabled.
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(ANGLE_DRIVER_SUFFIX);
        mController.updateState(mPreference);
        assertTrue(mPreference.isChecked());
        assertTrue(mPreference.isEnabled());

        // 2. "Enable ANGLE" switch is off, the switch should be disabled.
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn("");
        mController.updateState(mPreference);
        assertFalse(mPreference.isChecked());
        assertFalse(mPreference.isEnabled());
    }
}
