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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.audiopolicy.AudioProductStrategy;

import androidx.preference.ListPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingHelper;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/** Tests for {@link HearingDeviceAudioRoutingBasePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class HearingDeviceAudioRoutingBasePreferenceControllerTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private final ListPreference mListPreference = new ListPreference(mContext);

    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private AudioProductStrategy mAudioProductStrategyMedia;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Spy
    private HearingAidAudioRoutingHelper mAudioRoutingHelper =
            new HearingAidAudioRoutingHelper(mContext);
    @Mock
    private HearingAidHelper mHearingAidHelper;
    private TestHearingDeviceAudioRoutingBasePreferenceController mController;

    @Before
    public void setUp() {
        final AudioDeviceAttributes hearingDeviceAttribute = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_HEARING_AID,
                TEST_DEVICE_ADDRESS);

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getAnonymizedAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        doReturn(hearingDeviceAttribute).when(
                mAudioRoutingHelper).getMatchedHearingDeviceAttributes(any());
        when(mAudioProductStrategyMedia.getAudioAttributesForLegacyStreamType(
                AudioManager.STREAM_MUSIC)).thenReturn((new AudioAttributes.Builder()).build());
        when(mAudioRoutingHelper.getAudioProductStrategies()).thenReturn(
                List.of(mAudioProductStrategyMedia));

        mController = new TestHearingDeviceAudioRoutingBasePreferenceController(mContext,
                "test_key",
                mAudioRoutingHelper, mHearingAidHelper);
        mListPreference.setEntries(R.array.bluetooth_audio_routing_titles);
        mListPreference.setEntryValues(R.array.bluetooth_audio_routing_values);
        mListPreference.setSummary("%s");
    }

    @Test
    public void updateState_routingValueAuto_expectedSummary() {
        mController.saveRoutingValue(mContext, HearingAidAudioRoutingConstants.RoutingValue.AUTO);

        mController.updateState(mListPreference);

        assertThat(mListPreference.getSummary().toString()).isEqualTo(
                mListPreference.getEntries()[0].toString());
    }

    @Test
    public void onPreferenceChange_routingValueHearingDevice_restoreSameValue() {
        mController.onPreferenceChange(mListPreference, String.valueOf(
                HearingAidAudioRoutingConstants.RoutingValue.HEARING_DEVICE));

        assertThat(mController.restoreRoutingValue(mContext)).isEqualTo(
                HearingAidAudioRoutingConstants.RoutingValue.HEARING_DEVICE);
    }

    @Test
    public void onPreferenceChange_noMatchedDeviceAttributes_notCallSetStrategies() {
        when(mAudioRoutingHelper.getMatchedHearingDeviceAttributes(any())).thenReturn(null);

        verify(mAudioRoutingHelper, never()).setPreferredDeviceRoutingStrategies(any(), isNull(),
                anyInt());
    }

    private static class TestHearingDeviceAudioRoutingBasePreferenceController extends
            HearingDeviceAudioRoutingBasePreferenceController {

        private static int sSavedRoutingValue;

        TestHearingDeviceAudioRoutingBasePreferenceController(Context context,
                String preferenceKey, HearingAidAudioRoutingHelper audioRoutingHelper,
                HearingAidHelper hearingAidHelper) {
            super(context, preferenceKey, audioRoutingHelper, hearingAidHelper);
        }

        @Override
        protected int[] getSupportedAttributeList() {
            return new int[]{AudioAttributes.USAGE_MEDIA};
        }

        @Override
        protected void saveRoutingValue(Context context, int routingValue) {
            sSavedRoutingValue = routingValue;
        }

        @Override
        protected int restoreRoutingValue(Context context) {
            return sSavedRoutingValue;
        }
    }
}
