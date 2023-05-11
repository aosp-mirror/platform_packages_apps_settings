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
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.PROPERTY_PERSISTENT_GRAPHICS_EGL;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.PROPERTY_RO_GFX_ANGLE_SUPPORTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    @Mock
    private GraphicsDriverSystemPropertiesWrapper mSystemPropertiesMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mContext = ApplicationProvider.getApplicationContext();

        // Construct a GraphicsDriverEnableAngleAsSystemDriverController with two Overrides:
        // 1) Override the mSystemProperties with mSystemPropertiesMock,
        // so we can force the SystemProperties with values we need to run tests.
        // 2) Override the showRebootDialog() to do nothing.
        // We do not need to pop up the reboot dialog in the test.
        mController = new GraphicsDriverEnableAngleAsSystemDriverController(
            mContext, mFragment, new Injector(){
                @Override
                public GraphicsDriverSystemPropertiesWrapper createSystemPropertiesWrapper() {
                    return mSystemPropertiesMock;
                }
            }) {
                @Override
                void showRebootDialog() {
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
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Runnable countDown = new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        };
        SystemProperties.addChangeCallback(countDown);

        // Test onPreferenceChange(true) updates the persist.graphics.egl to "angle"
        mController.onPreferenceChange(mPreference, true);
        try {
            countDownLatch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo(ANGLE_DRIVER_SUFFIX);

        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(countDown);
    }

    @Test
    public void onPreferenceChange_switchOff_shouldDisableAngleAsSystemDriver() {
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Runnable countDown = new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        };
        SystemProperties.addChangeCallback(countDown);

        // Test onPreferenceChange(false) updates the persist.graphics.egl to ""
        mController.onPreferenceChange(mPreference, false);
        try {
            countDownLatch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");

        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(countDown);
    }

    @Test
    public void updateState_angleNotSupported_PreferenceShouldDisabled() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_RO_GFX_ANGLE_SUPPORTED), any())).thenReturn("");
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_angleNotSupported_PreferenceShouldNotBeChecked() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_RO_GFX_ANGLE_SUPPORTED), any()))
                .thenReturn("");
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_angleSupported_PreferenceShouldEnabled() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_RO_GFX_ANGLE_SUPPORTED), any()))
                .thenReturn("true");
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_angleSupported_angleIsSystemGLESDriver_PreferenceShouldBeChecked() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_RO_GFX_ANGLE_SUPPORTED), any()))
                .thenReturn("true");
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(ANGLE_DRIVER_SUFFIX);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void
            updateState_angleSupported_angleIsNotSystemGLESDriver_PreferenceShouldNotBeChecked() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_RO_GFX_ANGLE_SUPPORTED), any()))
                .thenReturn("true");
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn("");
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onDeveloperOptionSwitchEnabled_angleSupported_PreferenceShouldEnabled() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_RO_GFX_ANGLE_SUPPORTED), any()))
                .thenReturn("true");
        mController.onDeveloperOptionsSwitchEnabled();
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void onDeveloperOptionSwitchEnabled_angleNotSupported_PrefenceShouldDisabled() {
        when(mSystemPropertiesMock.get(eq(PROPERTY_RO_GFX_ANGLE_SUPPORTED), any()))
                .thenReturn("false");
        mController.onDeveloperOptionsSwitchEnabled();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onDeveloperOptionSwitchDisabled_angleIsNotSystemGLESDriver() {
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Runnable countDown = new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        };
        SystemProperties.addChangeCallback(countDown);

        // Test that onDeveloperOptionSwitchDisabled,
        // persist.graphics.egl updates to ""
        mController.onDeveloperOptionsSwitchDisabled();
        try {
            countDownLatch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");

        // Done with the test, remove the callback
        SystemProperties.removeChangeCallback(countDown);
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
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Runnable countDown = new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        };
        SystemProperties.addChangeCallback(countDown);

        // Test that if the current persist.graphics.egl is "angle",
        // when reboot is cancelled, persist.graphics.egl is changed back to "",
        // and switch is set to unchecked.
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn(ANGLE_DRIVER_SUFFIX);
        mController.onRebootCancelled();
        try {
            countDownLatch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");
        assertThat(mPreference.isChecked()).isFalse();

        // Done with the test, remove the callback.
        SystemProperties.removeChangeCallback(countDown);
    }

    @Test
    public void onRebootCancelled_ToggleSwitchFromOffToOn() {
        // Add a callback when SystemProperty changes.
        // This allows the thread to wait until
        // GpuService::toggleAngleAsSystemDriver() updates the persist.graphics.egl.
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Runnable countDown = new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        };
        SystemProperties.addChangeCallback(countDown);

        // Test that if the current persist.graphics.egl is "",
        // when reboot is cancelled, persist.graphics.egl is changed back to "angle",
        // and switch is set to checked.
        when(mSystemPropertiesMock.get(eq(PROPERTY_PERSISTENT_GRAPHICS_EGL), any()))
                .thenReturn("");
        mController.onRebootCancelled();
        try {
            countDownLatch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo(ANGLE_DRIVER_SUFFIX);
        assertThat(mPreference.isChecked()).isTrue();

        // Done with the test, remove the callback.
        SystemProperties.removeChangeCallback(countDown);
    }

}
