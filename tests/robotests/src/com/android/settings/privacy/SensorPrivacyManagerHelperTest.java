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

package com.android.settings.privacy;

import static android.hardware.SensorPrivacyManager.Sources.SETTINGS;

import static com.android.settings.utils.SensorPrivacyManagerHelper.SENSOR_CAMERA;
import static com.android.settings.utils.SensorPrivacyManagerHelper.SENSOR_MICROPHONE;
import static com.android.settings.utils.SensorPrivacyManagerHelper.TOGGLE_TYPE_HARDWARE;
import static com.android.settings.utils.SensorPrivacyManagerHelper.TOGGLE_TYPE_SOFTWARE;

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
import android.hardware.SensorPrivacyManager;
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener;
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener.SensorPrivacyChangedParams;

import com.android.settings.utils.SensorPrivacyManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
public class SensorPrivacyManagerHelperTest {

    private MockitoSession mMockitoSession;

    /** Execute synchronously */
    private Executor mExecutor = r -> r.run();

    @Mock
    private Context mContext;
    @Mock
    private SensorPrivacyManager mSensorPrivacyManager;

    private SensorPrivacyManagerHelper mSensorPrivacyManagerHelper;

    @Before
    public void setUp() {
        mMockitoSession = Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.WARN)
                .startMocking();

        doReturn(mExecutor).when(mContext)
                .getMainExecutor();
        doReturn(mSensorPrivacyManager).when(mContext)
                .getSystemService(eq(SensorPrivacyManager.class));

        mSensorPrivacyManagerHelper = new SensorPrivacyManagerHelper(mContext);
    }

    @After
    public void tearDown() {
        mMockitoSession.finishMocking();
    }

    /**
     * Verify that a sensor privacy listener is added in constructor.
     */
    @Test
    public void constructor_invokeAddSensorPrivacyListener() {
        verify(mSensorPrivacyManager, times(1)).addSensorPrivacyListener(eq(mExecutor),
                any(OnSensorPrivacyChangedListener.class));
    }

    /**
     * Verify when SensorPrivacyManagerHelper#setSensorBlocked(microphone, true) called,
     * SensorPrivacyManager#setSensorPrivacy(microphone, true) is invoked.
     */
    @Test
    public void invokeSetMicrophoneBlocked_invokeSetMicrophonePrivacyTrue() {
        mSensorPrivacyManagerHelper.setSensorBlocked(SENSOR_MICROPHONE, true);
        verify(mSensorPrivacyManager, times(1))
                .setSensorPrivacy(eq(SETTINGS), eq(SENSOR_MICROPHONE), eq(true));
    }

    /**
     * Verify when SensorPrivacyManagerHelper#setSensorBlocked(microphone, false) called,
     * SensorPrivacyManager#setSensorPrivacy(microphone, false) is invoked.
     */
    @Test
    public void invokeSetMicrophoneUnBlocked_invokeSetMicrophonePrivacyFalse() {
        mSensorPrivacyManagerHelper.setSensorBlocked(SENSOR_MICROPHONE, false);
        verify(mSensorPrivacyManager, times(1))
                .setSensorPrivacy(eq(SETTINGS), eq(SENSOR_MICROPHONE), eq(false));
    }

    /**
     * Verify when a callback is added with no toggleType and no sensor filter, then the
     * callback is invoked on changes to all states.
     */
    @Test
    public void addCallbackNoFilter_invokeCallback() {
        SensorPrivacyManager.OnSensorPrivacyChangedListener listener = getServiceListener();

        SensorPrivacyManagerHelper.Callback callback =
                mock(SensorPrivacyManagerHelper.Callback.class);
        mSensorPrivacyManagerHelper.addSensorBlockedListener(mExecutor, callback);

        verifyAllCases(listener, (t, s, e, i) -> {
            verify(callback, times(1)).onSensorPrivacyChanged(eq(t), eq(s), eq(e));
            verify(callback, times(i + 1)).onSensorPrivacyChanged(anyInt(), anyInt(), anyBoolean());
        });
    }

    /**
     * Verify when a callback is added with a filter to only dispatch microphone events, then the
     * callback is only invoked on changes to microphone state.
     */
    @Test
    public void addCallbackMicrophoneOnlyFilter_invokeCallbackMicrophoneOnly() {
        SensorPrivacyManager.OnSensorPrivacyChangedListener listener = getServiceListener();

        SensorPrivacyManagerHelper.Callback callback =
                mock(SensorPrivacyManagerHelper.Callback.class);
        mSensorPrivacyManagerHelper.addSensorBlockedListener(SENSOR_MICROPHONE, mExecutor,
                callback);
        mSensorPrivacyManagerHelper.removeSensorBlockedListener(callback);

        verifyAllCases(listener, (t, s, e, i) -> {
            verify(callback, never()).onSensorPrivacyChanged(anyInt(), eq(SENSOR_MICROPHONE),
                    anyBoolean());
        });
    }

    /**
     * Verify when a callback is added with a filter to only dispatch camera events, then the
     * callback is only invoked on changes to camera state.
     */
    @Test
    public void addCallbackCameraOnlyFilter_invokeCallbackCameraOnly() {
        SensorPrivacyManager.OnSensorPrivacyChangedListener listener = getServiceListener();

        SensorPrivacyManagerHelper.Callback callback =
                mock(SensorPrivacyManagerHelper.Callback.class);
        mSensorPrivacyManagerHelper.addSensorBlockedListener(SENSOR_CAMERA, mExecutor,
                callback);
        mSensorPrivacyManagerHelper.removeSensorBlockedListener(callback);

        verifyAllCases(listener, (t, s, e, i) -> {
            verify(callback, never()).onSensorPrivacyChanged(anyInt(), eq(SENSOR_CAMERA),
                    anyBoolean());
        });
    }

    /**
     * Verify when a callback is added with a filter to only dispatch software_toggle+microphone
     * events, then the callback is only invoked on changes to microphone state.
     */
    @Test
    public void addCallbackSoftwareMicrophoneOnlyFilter_invokeCallbackSoftwareMicrophoneOnly() {
        SensorPrivacyManager.OnSensorPrivacyChangedListener listener = getServiceListener();

        SensorPrivacyManagerHelper.Callback callback =
                mock(SensorPrivacyManagerHelper.Callback.class);
        mSensorPrivacyManagerHelper.addSensorBlockedListener(TOGGLE_TYPE_SOFTWARE,
                SENSOR_MICROPHONE, mExecutor, callback);
        mSensorPrivacyManagerHelper.removeSensorBlockedListener(callback);

        verifyAllCases(listener, (t, s, e, i) -> {
            verify(callback, never()).onSensorPrivacyChanged(eq(TOGGLE_TYPE_SOFTWARE),
                    eq(SENSOR_MICROPHONE), anyBoolean());
        });
    }

    /**
     * Verify when a callback is added with a filter to only dispatch software_toggle+camera
     * events, then the callback is only invoked on changes to camera state.
     */
    @Test
    public void addCallbackSoftwareCameraOnlyFilter_invokeCallbackSoftwareCameraOnly() {
        SensorPrivacyManager.OnSensorPrivacyChangedListener listener = getServiceListener();

        SensorPrivacyManagerHelper.Callback callback =
                mock(SensorPrivacyManagerHelper.Callback.class);
        mSensorPrivacyManagerHelper.addSensorBlockedListener(TOGGLE_TYPE_SOFTWARE,
                SENSOR_CAMERA, mExecutor, callback);
        mSensorPrivacyManagerHelper.removeSensorBlockedListener(callback);

        verifyAllCases(listener, (t, s, e, i) -> {
            verify(callback, never()).onSensorPrivacyChanged(eq(TOGGLE_TYPE_SOFTWARE),
                    eq(SENSOR_CAMERA), anyBoolean());
        });
    }

    /**
     * Verify when a callback is added with a filter to only dispatch hardware_toggle+microphone
     * events, then the callback is only invoked on changes to microphone state.
     */
    @Test
    public void addCallbackHardwareMicrophoneOnlyFilter_invokeCallbackHardwareMicrophoneOnly() {
        SensorPrivacyManager.OnSensorPrivacyChangedListener listener = getServiceListener();

        SensorPrivacyManagerHelper.Callback callback =
                mock(SensorPrivacyManagerHelper.Callback.class);
        mSensorPrivacyManagerHelper.addSensorBlockedListener(TOGGLE_TYPE_HARDWARE,
                SENSOR_MICROPHONE, mExecutor, callback);
        mSensorPrivacyManagerHelper.removeSensorBlockedListener(callback);

        verifyAllCases(listener, (t, s, e, i) -> {
            verify(callback, never()).onSensorPrivacyChanged(eq(TOGGLE_TYPE_HARDWARE),
                    eq(SENSOR_MICROPHONE), anyBoolean());
        });
    }

    /**
     * Verify when a callback is added with a filter to only dispatch hardware_toggle+camera
     * events, then the callback is only invoked on changes to camera state.
     */
    @Test
    public void addCallbackHardwareCameraOnlyFilter_invokeCallbackHardwareCameraOnly() {
        SensorPrivacyManager.OnSensorPrivacyChangedListener listener = getServiceListener();

        SensorPrivacyManagerHelper.Callback callback =
                mock(SensorPrivacyManagerHelper.Callback.class);
        mSensorPrivacyManagerHelper.addSensorBlockedListener(TOGGLE_TYPE_HARDWARE,
                SENSOR_CAMERA, mExecutor, callback);
        mSensorPrivacyManagerHelper.removeSensorBlockedListener(callback);

        verifyAllCases(listener, (t, s, e, i) -> {
            verify(callback, never()).onSensorPrivacyChanged(eq(TOGGLE_TYPE_HARDWARE),
                    eq(SENSOR_CAMERA), anyBoolean());
        });
    }

    /**
     * Verify when a callback is removed, then the callback is never invoked on changes to state.
     */
    @Test
    public void removeCallback_noInvokeCallback() {
        SensorPrivacyManager.OnSensorPrivacyChangedListener listener = getServiceListener();

        SensorPrivacyManagerHelper.Callback callback =
                mock(SensorPrivacyManagerHelper.Callback.class);
        mSensorPrivacyManagerHelper.addSensorBlockedListener(mExecutor, callback);
        mSensorPrivacyManagerHelper.removeSensorBlockedListener(callback);

        verifyAllCases(listener, (t, s, e, i) -> {
            verify(callback, never()).onSensorPrivacyChanged(anyInt(), anyInt(), anyBoolean());
        });
    }

    private interface Verifier {

        /**
         * This method should throw in the fail case.
         */
        void verifyCallback(int toggleType, int sensor, boolean isEnabled, int iterationNumber);
    }

    private void verifyAllCases(SensorPrivacyManager.OnSensorPrivacyChangedListener listener,
            Verifier verifier) {
        int[] toggleTypes = {TOGGLE_TYPE_SOFTWARE, TOGGLE_TYPE_HARDWARE};
        int[] sensors = {SENSOR_MICROPHONE, SENSOR_CAMERA};
        boolean[] enabledValues = {false, true};

        int i = 0;
        for (int t : toggleTypes) {
            for (int s : sensors) {
                for (boolean e : enabledValues) {
                    listener.onSensorPrivacyChanged(createParams(t, s, e));

                    verifier.verifyCallback(t, s, e, i++);
                }
            }
        }
    }

    private OnSensorPrivacyChangedListener getServiceListener() {
        ArgumentCaptor<OnSensorPrivacyChangedListener> captor =
                ArgumentCaptor.forClass(OnSensorPrivacyChangedListener.class);
        verify(mSensorPrivacyManager).addSensorPrivacyListener(eq(mExecutor),
                captor.capture());

        OnSensorPrivacyChangedListener listener = captor.getValue();
        return listener;
    }

    private SensorPrivacyChangedParams createParams(int toggleType, int sensor, boolean enabled) {
        SensorPrivacyChangedParams params = mock(SensorPrivacyChangedParams.class);
        doReturn(toggleType).when(params).getToggleType();
        doReturn(sensor).when(params).getSensor();
        doReturn(enabled).when(params).isEnabled();
        return params;
    }
}
