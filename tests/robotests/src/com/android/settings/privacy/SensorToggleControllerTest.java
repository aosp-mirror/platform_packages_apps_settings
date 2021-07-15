/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.hardware.SensorPrivacyManager.Sensors.CAMERA;
import static android.hardware.SensorPrivacyManager.Sensors.MICROPHONE;
import static android.hardware.SensorPrivacyManager.Sources.OTHER;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener;
import android.util.ArraySet;

import com.android.settings.utils.SensorPrivacyManagerHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class SensorToggleControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private SensorPrivacyManager mSensorPrivacyManager;
    private boolean mMicState;
    private boolean mCamState;
    private Set<OnSensorPrivacyChangedListener> mMicListeners;
    private Set<OnSensorPrivacyChangedListener> mCamListeners;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Mockito.mock(Context.class);
        mSensorPrivacyManager = Mockito.mock(SensorPrivacyManager.class);

        try {
            Method clearInstance =
                    SensorPrivacyManagerHelper.class.getDeclaredMethod("clearInstance");
            clearInstance.setAccessible(true);
            clearInstance.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mMicState = false;
        mCamState = false;
        mMicListeners = new ArraySet<>();
        mCamListeners = new ArraySet<>();

        doReturn(0).when(mContext).getUserId();
        doReturn(mSensorPrivacyManager).when(mContext)
                .getSystemService(SensorPrivacyManager.class);

        doAnswer(invocation -> mMicState)
                .when(mSensorPrivacyManager).isSensorPrivacyEnabled(eq(MICROPHONE));
        doAnswer(invocation -> mCamState)
                .when(mSensorPrivacyManager).isSensorPrivacyEnabled(eq(CAMERA));

        doAnswer(invocation -> {
            mMicState = invocation.getArgument(1);
            for (OnSensorPrivacyChangedListener listener : mMicListeners) {
                listener.onSensorPrivacyChanged(MICROPHONE, mMicState);
            }
            return null;
        }).when(mSensorPrivacyManager).setSensorPrivacy(anyInt(), eq(MICROPHONE), anyBoolean());
        doAnswer(invocation -> {
            mCamState = invocation.getArgument(1);
            for (OnSensorPrivacyChangedListener listener : mMicListeners) {
                listener.onSensorPrivacyChanged(CAMERA, mMicState);
            }
            return null;
        }).when(mSensorPrivacyManager).setSensorPrivacy(anyInt(), eq(CAMERA), anyBoolean());

        doAnswer(invocation -> mMicListeners.add(invocation.getArgument(1)))
                .when(mSensorPrivacyManager).addSensorPrivacyListener(eq(MICROPHONE), any());
        doAnswer(invocation -> mCamListeners.add(invocation.getArgument(1)))
                .when(mSensorPrivacyManager).addSensorPrivacyListener(eq(CAMERA), any());
    }

    @Test
    public void isChecked_disableMicrophoneSensorPrivacy_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, false);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        assertTrue(micToggleController.isChecked());
    }

    @Test
    public void isChecked_enableMicrophoneSensorPrivacy_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, true);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        assertFalse(micToggleController.isChecked());
    }

    @Test
    public void isChecked_disableMicrophoneSensorPrivacyThenChanged_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, false);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, true);
        assertFalse(micToggleController.isChecked());
    }

    @Test
    public void isChecked_enableMicrophoneSensorPrivacyThenChanged_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, true);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, false);
        assertTrue(micToggleController.isChecked());
    }

    @Test
    public void isMicrophoneSensorPrivacyEnabled_uncheckMicToggle_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, false);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        micToggleController.setChecked(false);
        assertTrue(mMicState);
    }

    @Test
    public void isMicrophoneSensorPrivacyEnabled_checkMicToggle_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, true);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        micToggleController.setChecked(true);
        assertFalse(mMicState);
    }

    @Test
    public void isChecked_disableCameraSensorPrivacy_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, false);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        assertTrue(camToggleController.isChecked());
    }

    @Test
    public void isChecked_enableCameraSensorPrivacy_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, true);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        assertFalse(camToggleController.isChecked());
    }

    @Test
    public void isChecked_disableCameraSensorPrivacyThenChanged_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, false);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, true);
        assertFalse(camToggleController.isChecked());
    }

    @Test
    public void isChecked_enableCameraSensorPrivacyThenChanged_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, true);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, false);
        assertTrue(camToggleController.isChecked());
    }

    @Test
    public void isCameraSensorPrivacyEnabled_uncheckMicToggle_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, false);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        camToggleController.setChecked(false);
        assertTrue(mCamState);
    }

    @Test
    public void isCameraSensorPrivacyEnabled_checkMicToggle_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, true);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        camToggleController.setChecked(true);
        assertFalse(mCamState);
    }
}
