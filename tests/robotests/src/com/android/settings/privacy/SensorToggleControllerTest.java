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

package com.android.settings.privacy;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.utils.SensorPrivacyManagerHelper.SENSOR_CAMERA;
import static com.android.settings.utils.SensorPrivacyManagerHelper.SENSOR_MICROPHONE;
import static com.android.settings.utils.SensorPrivacyManagerHelper.TOGGLE_TYPE_SOFTWARE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.utils.SensorPrivacyManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
public class SensorToggleControllerTest {

    private MockitoSession mMockitoSession;

    @Mock
    private Context mContext;
    @Mock
    private SensorPrivacyManagerHelper mSensorPrivacyManagerHelper;

    @Before
    public void setUp() {
        mMockitoSession = Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.WARN)
                .startMocking();

        doReturn((Executor) r -> r.run()).when(mContext).getMainExecutor();
    }

    @After
    public void tearDown() {
        mMockitoSession.finishMocking();
    }

    /**
     * Test the availability status when mic toggle is not supported.
     */
    @Test
    public void getAvailabilityStatus_MicrophoneToggleNotSupported_returnUnsupported() {
        // Return not supported
        doReturn(false).when(mSensorPrivacyManagerHelper).supportsSensorToggle(SENSOR_MICROPHONE);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        // Verify not available
        assertEquals(UNSUPPORTED_ON_DEVICE, micToggleController.getAvailabilityStatus());
    }

    /**
     * Test the availability status when mic toggle is supported.
     */
    @Test
    public void getAvailabilityStatus_MicrophoneToggleSupported_returnAvailable() {
        // Return supported
        doReturn(true).when(mSensorPrivacyManagerHelper).supportsSensorToggle(SENSOR_MICROPHONE);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        // Verify available
        assertEquals(AVAILABLE, micToggleController.getAvailabilityStatus());
    }

    /**
     * Test the initial state shows mic unblocked when created.
     */
    @Test
    public void isChecked_disableMicrophoneSensorPrivacy_returnTrue() {
        // Starts off unblocked
        doReturn(false).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_MICROPHONE);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        // Verify the controller is checked
        assertTrue(micToggleController.isChecked());
    }

    /**
     * Test the initial state shows mic blocked when created.
     */
    @Test
    public void isChecked_enableMicrophoneSensorPrivacy_returnFalse() {
        // Starts off blocked
        doReturn(true).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_MICROPHONE);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        // Verify the controller is unchecked
        assertFalse(micToggleController.isChecked());
    }

    @Test
    public void startMicrophoneToggleController_invokeAddSensorBlockedListener() {
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        micToggleController.onStart();
        verify(mSensorPrivacyManagerHelper, times(1))
                .addSensorBlockedListener(eq(SENSOR_MICROPHONE), any(), any());
    }

    @Test
    public void stopMicrophoneToggleController_invokeRemoveSensorBlockedListener() {
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        micToggleController.onStart();

        ArgumentCaptor<SensorPrivacyManagerHelper.Callback> callbackCaptor =
                ArgumentCaptor.forClass(SensorPrivacyManagerHelper.Callback.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .addSensorBlockedListener(eq(SENSOR_MICROPHONE), any(), callbackCaptor.capture());

        micToggleController.onStop();

        verify(mSensorPrivacyManagerHelper, times(1))
                .removeSensorBlockedListener(callbackCaptor.getValue());
    }

    /**
     * Test the state of the mic controller switches from unblocked to blocked when state is changed
     * externally.
     */
    @Test
    public void isChecked_disableMicrophoneSensorPrivacyThenChanged_returnFalse() {
        // Starts off unblocked
        doReturn(false).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_MICROPHONE);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        // Preference is started
        micToggleController.displayPreference(mock(PreferenceScreen.class));
        micToggleController.onStart();

        ArgumentCaptor<SensorPrivacyManagerHelper.Callback> callbackCaptor =
                ArgumentCaptor.forClass(SensorPrivacyManagerHelper.Callback.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .addSensorBlockedListener(eq(SENSOR_MICROPHONE), any(), callbackCaptor.capture());

        // The state changed externally, update return value of isSensorBlocked and invoke callback
        doReturn(true).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_MICROPHONE);
        callbackCaptor.getValue()
                .onSensorPrivacyChanged(TOGGLE_TYPE_SOFTWARE, SENSOR_MICROPHONE, true);
        assertFalse(micToggleController.isChecked());
    }


    /**
     * Test the state of the mic controller switches from blocked to unblocked when state is changed
     * externally.
     */
    @Test
    public void isChecked_enableMicrophoneSensorPrivacyThenChanged_returnTrue() {
        // Starts off blocked
        doReturn(true).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_MICROPHONE);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        // Preference is started
        micToggleController.displayPreference(mock(PreferenceScreen.class));
        micToggleController.onStart();

        // The state changed externally, update return value of isSensorBlocked and invoke callback
        ArgumentCaptor<SensorPrivacyManagerHelper.Callback> callbackCaptor =
                ArgumentCaptor.forClass(SensorPrivacyManagerHelper.Callback.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .addSensorBlockedListener(eq(SENSOR_MICROPHONE), any(), callbackCaptor.capture());

        doReturn(false).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_MICROPHONE);
        callbackCaptor.getValue()
                .onSensorPrivacyChanged(TOGGLE_TYPE_SOFTWARE, SENSOR_MICROPHONE, false);
        assertTrue(micToggleController.isChecked());
    }


    /**
     * Test the mic controller requests to block the mic when unblocked on invocation of setChecked.
     */
    @Test
    public void blocked_uncheckMicToggle_returnTrue() {
        // Starts off unblocked
        doReturn(false).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_MICROPHONE);

        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        verify(mSensorPrivacyManagerHelper, never()).setSensorBlocked(anyInt(), anyBoolean());

        // User set blocked
        micToggleController.setChecked(false);

        ArgumentCaptor<Boolean> blockedResult = ArgumentCaptor.forClass(Boolean.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .setSensorBlocked(ArgumentMatchers.eq(SENSOR_MICROPHONE), blockedResult.capture());

        // Verify attempt to block
        assertTrue(blockedResult.getValue());
    }



    /**
     * Test the mic controller requests to unblock the mic when blocked on invocation of setChecked.
     */
    @Test
    public void blocked_checkMicToggle_returnFalse() {
        // Starts off blocked
        doReturn(true).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_MICROPHONE);

        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle",
                mSensorPrivacyManagerHelper);
        verify(mSensorPrivacyManagerHelper, never()).setSensorBlocked(anyInt(), anyBoolean());

        // User set unblocked
        micToggleController.setChecked(true);

        ArgumentCaptor<Boolean> blockedResult = ArgumentCaptor.forClass(Boolean.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .setSensorBlocked(ArgumentMatchers.eq(SENSOR_MICROPHONE), blockedResult.capture());

        // Verify attempt to unblock
        assertFalse(blockedResult.getValue());
    }

    /**
     * Test the availability status when cam toggle is not supported.
     */
    @Test
    public void getAvailabilityStatus_CameraToggleNotSupported_returnUnsupported() {
        // Return not supported
        doReturn(false).when(mSensorPrivacyManagerHelper).supportsSensorToggle(SENSOR_CAMERA);
        CameraToggleController cameraToggleController = new CameraToggleController(mContext,
                "cam_toggle", mSensorPrivacyManagerHelper);
        // Verify not available
        assertEquals(UNSUPPORTED_ON_DEVICE, cameraToggleController.getAvailabilityStatus());
    }

    /**
     * Test the availability status when cam toggle is supported.
     */
    @Test
    public void getAvailabilityStatus_CameraToggleSupported_returnAvailable() {
        // Return supported
        doReturn(true).when(mSensorPrivacyManagerHelper).supportsSensorToggle(SENSOR_CAMERA);
        CameraToggleController cameraToggleController = new CameraToggleController(mContext,
                "cam_toggle", mSensorPrivacyManagerHelper);
        // Verify available
        assertEquals(AVAILABLE, cameraToggleController.getAvailabilityStatus());
    }

    /**
     * Test the initial state shows cam unblocked when created.
     */
    @Test
    public void isChecked_disableCameraSensorPrivacy_returnTrue() {
        // Starts off unblocked
        doReturn(false).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_CAMERA);
        CameraToggleController cameraToggleController = new CameraToggleController(mContext,
                "cam_toggle", mSensorPrivacyManagerHelper);
        // Verify the controller is checked
        assertTrue(cameraToggleController.isChecked());
    }

    /**
     * Test the initial state shows cam blocked when created.
     */
    @Test
    public void isChecked_enableCameraSensorPrivacy_returnFalse() {
        // Starts off blocked
        doReturn(true).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_CAMERA);
        CameraToggleController cameraToggleController = new CameraToggleController(mContext,
                "cam_toggle", mSensorPrivacyManagerHelper);
        // Verify the controller is unchecked
        assertFalse(cameraToggleController.isChecked());
    }

    @Test
    public void startCameraToggleController_invokeAddSensorBlockedListener() {
        CameraToggleController cameraToggleController =
                new CameraToggleController(mContext, "cam_toggle", mSensorPrivacyManagerHelper);
        cameraToggleController.onStart();
        verify(mSensorPrivacyManagerHelper, times(1))
                .addSensorBlockedListener(eq(SENSOR_CAMERA), any(), any());
    }

    @Test
    public void stopCameraToggleController_invokeRemoveSensorBlockedListener() {
        CameraToggleController cameraToggleController =
                new CameraToggleController(mContext, "cam_toggle", mSensorPrivacyManagerHelper);
        cameraToggleController.onStart();

        ArgumentCaptor<SensorPrivacyManagerHelper.Callback> callbackCaptor =
                ArgumentCaptor.forClass(SensorPrivacyManagerHelper.Callback.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .addSensorBlockedListener(eq(SENSOR_CAMERA), any(), callbackCaptor.capture());

        cameraToggleController.onStop();

        verify(mSensorPrivacyManagerHelper, times(1))
                .removeSensorBlockedListener(callbackCaptor.getValue());
    }

    /**
     * Test the state of the cam controller switches from unblocked to blocked when state is changed
     * externally.
     */
    @Test
    public void isChecked_disableCameraSensorPrivacyThenChanged_returnFalse() {
        // Starts off unblocked
        doReturn(false).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_CAMERA);
        CameraToggleController cameraToggleController = new CameraToggleController(mContext,
                "cam_toggle", mSensorPrivacyManagerHelper);
        // Preference is started
        cameraToggleController.displayPreference(mock(PreferenceScreen.class));
        cameraToggleController.onStart();

        ArgumentCaptor<SensorPrivacyManagerHelper.Callback> callbackCaptor =
                ArgumentCaptor.forClass(SensorPrivacyManagerHelper.Callback.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .addSensorBlockedListener(eq(SENSOR_CAMERA), any(), callbackCaptor.capture());

        // The state changed externally, update return value of isSensorBlocked and invoke callback
        doReturn(true).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_CAMERA);
        callbackCaptor.getValue().onSensorPrivacyChanged(TOGGLE_TYPE_SOFTWARE, SENSOR_CAMERA, true);
        assertFalse(cameraToggleController.isChecked());
    }


    /**
     * Test the state of the cam controller switches from blocked to unblocked when state is changed
     * externally.
     */
    @Test
    public void isChecked_enableCameraSensorPrivacyThenChanged_returnTrue() {
        // Starts off blocked
        doReturn(true).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_CAMERA);
        CameraToggleController cameraToggleController = new CameraToggleController(mContext,
                "cam_toggle", mSensorPrivacyManagerHelper);
        // Preference is started
        cameraToggleController.displayPreference(mock(PreferenceScreen.class));
        cameraToggleController.onStart();

        // The state changed externally, update return value of isSensorBlocked and invoke callback
        ArgumentCaptor<SensorPrivacyManagerHelper.Callback> callbackCaptor =
                ArgumentCaptor.forClass(SensorPrivacyManagerHelper.Callback.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .addSensorBlockedListener(eq(SENSOR_CAMERA), any(), callbackCaptor.capture());

        doReturn(false).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_CAMERA);
        callbackCaptor.getValue()
                .onSensorPrivacyChanged(TOGGLE_TYPE_SOFTWARE, SENSOR_CAMERA, false);
        assertTrue(cameraToggleController.isChecked());
    }


    /**
     * Test the cam controller requests to block the cam when unblocked on invocation of setChecked.
     */
    @Test
    public void blocked_uncheckCamToggle_returnTrue() {
        // Starts off unblocked
        doReturn(false).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_CAMERA);

        CameraToggleController cameraToggleController = new CameraToggleController(mContext,
                "cam_toggle", mSensorPrivacyManagerHelper);
        verify(mSensorPrivacyManagerHelper, never()).setSensorBlocked(anyInt(), anyBoolean());

        // User set blocked
        cameraToggleController.setChecked(false);

        ArgumentCaptor<Boolean> blockedResult = ArgumentCaptor.forClass(Boolean.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .setSensorBlocked(ArgumentMatchers.eq(SENSOR_CAMERA), blockedResult.capture());

        // Verify attempt to block
        assertTrue(blockedResult.getValue());
    }



    /**
     * Test the cam controller requests to unblock the cam when blocked on invocation of setChecked.
     */
    @Test
    public void blocked_checkCamToggle_returnFalse() {
        // Starts off blocked
        doReturn(true).when(mSensorPrivacyManagerHelper).isSensorBlocked(SENSOR_CAMERA);

        CameraToggleController cameraToggleController = new CameraToggleController(mContext,
                "cam_toggle", mSensorPrivacyManagerHelper);
        verify(mSensorPrivacyManagerHelper, never()).setSensorBlocked(anyInt(), anyBoolean());

        // User set unblocked
        cameraToggleController.setChecked(true);

        ArgumentCaptor<Boolean> blockedResult = ArgumentCaptor.forClass(Boolean.class);
        verify(mSensorPrivacyManagerHelper, times(1))
                .setSensorBlocked(ArgumentMatchers.eq(SENSOR_CAMERA), blockedResult.capture());

        // Verify attempt to unblock
        assertFalse(blockedResult.getValue());
    }
}
