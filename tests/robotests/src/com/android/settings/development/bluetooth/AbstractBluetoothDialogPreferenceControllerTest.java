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

package com.android.settings.development.bluetooth;

import static android.bluetooth.BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AbstractBluetoothDialogPreferenceControllerTest {

    private static final String SUMMARY = "Test summary";
    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";

    @Mock
    private BluetoothA2dp mBluetoothA2dp;
    @Mock
    private PreferenceScreen mScreen;

    private AbstractBluetoothDialogPreferenceController mController;
    private BaseBluetoothDialogPreferenceImpl mPreference;
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private BluetoothCodecStatus mCodecStatus;
    private BluetoothCodecConfig mCodecConfigAAC;
    private BluetoothCodecConfig mCodecConfigSBC;
    private BluetoothCodecConfig[] mCodecConfigs = new BluetoothCodecConfig[2];
    private BluetoothDevice mActiveDevice;
    private Context mContext;
    private int mCurrentConfig;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mBluetoothA2dpConfigStore = spy(new BluetoothA2dpConfigStore());
        mActiveDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(DEVICE_ADDRESS);
        mController = spy(new AbstractBluetoothDialogPreferenceControllerImpl(mContext, mLifecycle,
                mBluetoothA2dpConfigStore));
        mPreference = spy(new BaseBluetoothDialogPreferenceImpl(mContext));

        mCodecConfigAAC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC);
        mCodecConfigSBC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC);
        mCodecConfigs[0] = mCodecConfigAAC;
        mCodecConfigs[1] = mCodecConfigSBC;

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        mCurrentConfig = mController.getCurrentConfigIndex();
        when(mPreference.generateSummary(mCurrentConfig)).thenReturn(SUMMARY);
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
    }

    @Test
    public void getSummary_generateSummary() {
        assertThat(mController.getSummary()).isEqualTo(SUMMARY);
    }
    @Test
    public void onIndexUpdated_A2dpNotReady() {
        mController.onIndexUpdated(mController.getCurrentConfigIndex());

        verify(mController, never()).writeConfigurationValues(mCurrentConfig);
    }

    @Test
    public void onIndexUpdated_checkFlow() {
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, null);
        when(mBluetoothA2dp.getCodecStatus(
            mActiveDevice)).thenReturn(mCodecStatus);
        when(mBluetoothA2dpConfigStore.createCodecConfig()).thenReturn(mCodecConfigAAC);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.onIndexUpdated(mCurrentConfig);

        verify(mController).writeConfigurationValues(mCurrentConfig);
        verify(mBluetoothA2dp).setCodecConfigPreference(
                mActiveDevice, mCodecConfigAAC);
        assertThat(mPreference.getSummary()).isEqualTo(SUMMARY);
    }

    @Test
    public void getCurrentConfigIndex_noCodecConfig_returnDefaultIndex() {
        when(mController.getCurrentCodecConfig()).thenReturn(null);

        assertThat(mController.getCurrentConfigIndex()).isEqualTo(mPreference.getDefaultIndex());
    }

    @Test
    public void getCurrentConfigIndex_returnCurrentIndex() {
        when(mController.getCurrentCodecConfig()).thenReturn(mCodecConfigAAC);
        mController.getCurrentConfigIndex();

        verify(mController).getCurrentIndexByConfig(mCodecConfigAAC);
    }

    @Test
    public void getCurrentCodecConfig_errorChecking() {
        mController.onBluetoothServiceConnected(null);
        assertThat(mController.getCurrentCodecConfig()).isNull();

        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(null);
        assertThat(mController.getCurrentCodecConfig()).isNull();
    }

    @Test
    public void getCurrentCodecConfig_verifyConfig() {
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, null);
        when(mBluetoothA2dp.getCodecStatus(
            mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        assertThat(mController.getCurrentCodecConfig()).isEqualTo(mCodecConfigAAC);
    }

    @Test
    public void getSelectableConfigs_verifyConfig() {
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(
            mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        assertThat(mController.getSelectableConfigs(null)).isEqualTo(mCodecConfigs);
    }

    @Test
    public void getSelectableByCodecType_verifyConfig() {
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(
            mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        assertThat(mController.getSelectableByCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC))
                .isEqualTo(mCodecConfigAAC);
    }

    @Test
    public void getSelectableByCodecType_unavailable() {
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(
            mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        assertThat(mController.getSelectableByCodecType(
                BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX)).isNull();
    }

    @Test
    public void onBluetoothServiceConnected_verifyBluetoothA2dpConfigStore() {
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(
            mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        verify(mBluetoothA2dpConfigStore).setCodecType(mCodecConfigAAC.getCodecType());
        verify(mBluetoothA2dpConfigStore).setSampleRate(mCodecConfigAAC.getSampleRate());
        verify(mBluetoothA2dpConfigStore).setBitsPerSample(mCodecConfigAAC.getBitsPerSample());
        verify(mBluetoothA2dpConfigStore).setChannelMode(mCodecConfigAAC.getChannelMode());
        verify(mBluetoothA2dpConfigStore).setCodecPriority(CODEC_PRIORITY_HIGHEST);
        verify(mBluetoothA2dpConfigStore).setCodecSpecific1Value(
                mCodecConfigAAC.getCodecSpecific1());
    }

    private static class AbstractBluetoothDialogPreferenceControllerImpl extends
            AbstractBluetoothDialogPreferenceController {

        private AbstractBluetoothDialogPreferenceControllerImpl(Context context,
                Lifecycle lifecycle, BluetoothA2dpConfigStore store) {
            super(context, lifecycle, store);
        }

        @Override
        public String getPreferenceKey() {
            return "KEY";
        }

        @Override
        protected void writeConfigurationValues(int newValue) {
        }

        @Override
        protected int getCurrentIndexByConfig(BluetoothCodecConfig config) {
            return 0;
        }

        @Override
        public List<Integer> getSelectableIndex() {
            return new ArrayList<>();
        }
    }

    private static class BaseBluetoothDialogPreferenceImpl extends BaseBluetoothDialogPreference {

        private BaseBluetoothDialogPreferenceImpl(Context context) {
            super(context);
            mSummaryStrings.add(SUMMARY);
        }

        @Override
        protected int getRadioButtonGroupId() {
            return 0;
        }
    }
}
