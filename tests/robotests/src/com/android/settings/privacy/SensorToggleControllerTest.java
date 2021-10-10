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
import android.util.SparseArray;
import android.util.SparseBooleanArray;

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
    private SparseBooleanArray mMicState;
    private SparseBooleanArray mCamState;
    private SparseArray<Set<OnSensorPrivacyChangedListener>> mMicListeners;
    private SparseArray<Set<OnSensorPrivacyChangedListener>> mCamListeners;

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
        mMicState = new SparseBooleanArray();
        mCamState = new SparseBooleanArray();
        mMicState.put(0, false);
        mCamState.put(0, false);
        mMicState.put(10, false);
        mCamState.put(10, false);
        mMicListeners = new SparseArray<>();
        mCamListeners = new SparseArray<>();
        mMicListeners.put(0, new ArraySet<>());
        mMicListeners.put(10, new ArraySet<>());
        mCamListeners.put(0, new ArraySet<>());
        mCamListeners.put(10, new ArraySet<>());

        doReturn(0).when(mContext).getUserId();
        doReturn(mSensorPrivacyManager).when(mContext)
                .getSystemService(SensorPrivacyManager.class);

        doAnswer(invocation -> mMicState.get(0))
                .when(mSensorPrivacyManager).isSensorPrivacyEnabled(eq(MICROPHONE));
        doAnswer(invocation -> mCamState.get(0))
                .when(mSensorPrivacyManager).isSensorPrivacyEnabled(eq(CAMERA));
        doAnswer(invocation -> mMicState.get(invocation.getArgument(1)))
                .when(mSensorPrivacyManager).isSensorPrivacyEnabled(eq(MICROPHONE), anyInt());
        doAnswer(invocation -> mCamState.get(invocation.getArgument(1)))
                .when(mSensorPrivacyManager).isSensorPrivacyEnabled(eq(CAMERA), anyInt());

        doAnswer(invocation -> {
            mMicState.put(0, invocation.getArgument(2));
            mMicState.put(10, invocation.getArgument(2));
            for (OnSensorPrivacyChangedListener listener : mMicListeners.get(0)) {
                listener.onSensorPrivacyChanged(MICROPHONE, mMicState.get(0));
            }
            return null;
        }).when(mSensorPrivacyManager).setSensorPrivacy(anyInt(), eq(MICROPHONE), anyBoolean());
        doAnswer(invocation -> {
            mCamState.put(0, invocation.getArgument(2));
            mCamState.put(10, invocation.getArgument(2));
            for (OnSensorPrivacyChangedListener listener : mMicListeners.get(0)) {
                listener.onSensorPrivacyChanged(CAMERA, mMicState.get(0));
            }
            return null;
        }).when(mSensorPrivacyManager).setSensorPrivacy(anyInt(), eq(CAMERA), anyBoolean());

        doAnswer(invocation -> {
            mMicState.put(0, invocation.getArgument(2));
            mMicState.put(10, invocation.getArgument(2));
            for (OnSensorPrivacyChangedListener listener : mMicListeners.get(0)) {
                listener.onSensorPrivacyChanged(MICROPHONE, mMicState.get(0));
            }
            for (OnSensorPrivacyChangedListener listener : mMicListeners.get(10)) {
                listener.onSensorPrivacyChanged(MICROPHONE, mMicState.get(10));
            }
            return null;
        }).when(mSensorPrivacyManager)
                .setSensorPrivacyForProfileGroup(anyInt(), eq(MICROPHONE), anyBoolean());
        doAnswer(invocation -> {
            mCamState.put(0, invocation.getArgument(2));
            mCamState.put(10, invocation.getArgument(2));
            for (OnSensorPrivacyChangedListener listener : mCamListeners.get(0)) {
                listener.onSensorPrivacyChanged(CAMERA, mCamState.get(0));
            }
            for (OnSensorPrivacyChangedListener listener : mCamListeners.get(10)) {
                listener.onSensorPrivacyChanged(CAMERA, mCamState.get(10));
            }
            return null;
        }).when(mSensorPrivacyManager)
                .setSensorPrivacyForProfileGroup(anyInt(), eq(CAMERA), anyBoolean());

        doAnswer(invocation -> mMicListeners.get(0).add(invocation.getArgument(1)))
                .when(mSensorPrivacyManager).addSensorPrivacyListener(eq(MICROPHONE), any());
        doAnswer(invocation -> mCamListeners.get(0).add(invocation.getArgument(1)))
                .when(mSensorPrivacyManager).addSensorPrivacyListener(eq(CAMERA), any());

        doAnswer(invocation -> mMicListeners.get(invocation.getArgument(2))
                .add(invocation.getArgument(1))).when(mSensorPrivacyManager)
                .addSensorPrivacyListener(eq(MICROPHONE), anyInt(), any());
        doAnswer(invocation -> mCamListeners.get(invocation.getArgument(2))
                .add(invocation.getArgument(1))).when(mSensorPrivacyManager)
                .addSensorPrivacyListener(eq(CAMERA), anyInt(), any());
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
        assertTrue(mMicState.get(0));
    }

    @Test
    public void isMicrophoneSensorPrivacyEnabled_checkMicToggle_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, true);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        micToggleController.setChecked(true);
        assertFalse(mMicState.get(0));
    }

    @Test
    public void isMicrophoneSensorPrivacyEnabledForProfileUser_uncheckMicToggle_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, false);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        micToggleController.setChecked(false);
        assertTrue(mMicState.get(10));
    }

    @Test
    public void isMicrophoneSensorPrivacyEnabledProfileUser_checkMicToggle_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, MICROPHONE, true);
        MicToggleController micToggleController = new MicToggleController(mContext, "mic_toggle");
        micToggleController.setChecked(true);
        assertFalse(mMicState.get(10));
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
    public void isCameraSensorPrivacyEnabled_uncheckCanToggle_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, false);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        camToggleController.setChecked(false);
        assertTrue(mCamState.get(0));
    }

    @Test
    public void isCameraSensorPrivacyEnabled_checkCamToggle_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, true);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        camToggleController.setChecked(true);
        assertFalse(mCamState.get(0));
    }

    @Test
    public void isCameraSensorPrivacyEnabledForProfileUser_uncheckCamToggle_returnTrue() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, false);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        camToggleController.setChecked(false);
        assertTrue(mCamState.get(10));
    }

    @Test
    public void isCameraSensorPrivacyEnabledProfileUser_checkCamToggle_returnFalse() {
        mSensorPrivacyManager.setSensorPrivacy(OTHER, CAMERA, true);
        CameraToggleController camToggleController =
                new CameraToggleController(mContext, "cam_toggle");
        camToggleController.setChecked(true);
        assertFalse(mCamState.get(10));
    }
}
