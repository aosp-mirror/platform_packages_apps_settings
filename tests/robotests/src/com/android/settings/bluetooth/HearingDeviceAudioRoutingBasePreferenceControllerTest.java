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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.audiopolicy.AudioProductStrategy;

import androidx.preference.ListPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link HearingDeviceAudioRoutingBasePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class HearingDeviceAudioRoutingBasePreferenceControllerTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private static final String FAKE_KEY = "fake_key";
    private static final String TEST_SHARED_PREFERENCE = "test_bluetooth_settings";

    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private AudioProductStrategy mAudioProductStrategyMedia;
    private AudioDeviceAttributes mHearingDeviceAttribute;
    private ListPreference mListPreference;
    private TestHearingDeviceAudioRoutingBasePreferenceController mController;

    @Before
    public void setUp() {
        mListPreference = new ListPreference(mContext);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        mHearingDeviceAttribute = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_HEARING_AID,
                TEST_DEVICE_ADDRESS);
        when(mAudioProductStrategyMedia.getAudioAttributesForLegacyStreamType(
                AudioManager.STREAM_MUSIC))
                .thenReturn((new AudioAttributes.Builder()).build());
        doReturn(getSharedPreferences()).when(mContext).getSharedPreferences(anyString(), anyInt());

        mController = spy(
                new TestHearingDeviceAudioRoutingBasePreferenceController(mContext, FAKE_KEY));
        mController.setupForTesting(mCachedBluetoothDevice);
        doReturn(List.of(mAudioProductStrategyMedia)).when(mController).getAudioProductStrategies();
    }

    @Test
    public void onPreferenceChange_routingValueAuto_expectedListValue() {
        mController.onPreferenceChange(mListPreference, String.valueOf(
                HearingDeviceAudioRoutingBasePreferenceController.RoutingValue.AUTO));

        verify(mController).removePreferredDeviceForStrategies(any());
        assertThat(mListPreference.getValue()).isEqualTo(String.valueOf(
                HearingDeviceAudioRoutingBasePreferenceController.RoutingValue.AUTO));
    }

    @Test
    public void onPreferenceChange_routingValueHearingDevice_expectedListValue() {
        mController.onPreferenceChange(mListPreference, String.valueOf(
                HearingDeviceAudioRoutingBasePreferenceController.RoutingValue.HEARING_DEVICE));

        verify(mController).setPreferredDeviceForStrategies(any(), eq(mHearingDeviceAttribute));
        assertThat(mListPreference.getValue()).isEqualTo(String.valueOf(
                HearingDeviceAudioRoutingBasePreferenceController.RoutingValue.HEARING_DEVICE));
    }

    @Test
    public void onPreferenceChange_routingValueDeviceSpeaker_expectedListValue() {
        final AudioDeviceAttributes deviceSpeakerOut = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "");

        mController.onPreferenceChange(mListPreference, String.valueOf(
                HearingDeviceAudioRoutingBasePreferenceController.RoutingValue.DEVICE_SPEAKER));

        verify(mController).setPreferredDeviceForStrategies(any(), eq(deviceSpeakerOut));
        assertThat(mListPreference.getValue()).isEqualTo(String.valueOf(
                HearingDeviceAudioRoutingBasePreferenceController.RoutingValue.DEVICE_SPEAKER));

    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(TEST_SHARED_PREFERENCE, Context.MODE_PRIVATE);
    }

    private static class TestHearingDeviceAudioRoutingBasePreferenceController extends
            HearingDeviceAudioRoutingBasePreferenceController {

        private static CachedBluetoothDevice sCachedBluetoothDevice;
        private static int sSavedRoutingValue;

        TestHearingDeviceAudioRoutingBasePreferenceController(Context context,
                String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        protected int[] getSupportedAttributeList() {
            return new int[]{AudioAttributes.USAGE_MEDIA};
        }

        @Override
        protected CachedBluetoothDevice getHearingDevice() {
            return sCachedBluetoothDevice;
        }

        @Override
        protected void saveRoutingValue(Context context, int routingValue) {
            sSavedRoutingValue = routingValue;
        }

        @Override
        protected int restoreRoutingValue(Context context) {
            return sSavedRoutingValue;
        }

        public static void setupForTesting(CachedBluetoothDevice cachedBluetoothDevice) {
            sCachedBluetoothDevice = cachedBluetoothDevice;
        }
    }
}
