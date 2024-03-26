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

package com.android.settings.development.bluetooth;

import static android.bluetooth.BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
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
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AbstractBluetoothListPreferenceControllerTest {

    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";

    private static String DEFAULT_ENTRY;
    private static final String DEFAULT_ENTRY_VALUE = "1000";

    @Mock private BluetoothA2dp mBluetoothA2dp;
    @Mock private BluetoothAdapter mBluetoothAdapter;
    @Mock private PreferenceScreen mScreen;

    private AbstractBluetoothListPreferenceController mController;
    private ListPreference mPreference;
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private BluetoothCodecStatus mCodecStatus;
    private BluetoothCodecConfig mCodecConfigAAC;
    private BluetoothCodecConfig mCodecConfigSBC;
    private BluetoothCodecConfig[] mCodecConfigs = new BluetoothCodecConfig[2];
    private BluetoothDevice mActiveDevice;
    private Context mContext;
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
        mController =
                spy(
                        new AbstractBluetoothListPreferenceControllerImpl(
                                mContext, mLifecycle, mBluetoothA2dpConfigStore));
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mPreference = spy(new ListPreference(mContext));

        mCodecConfigAAC =
                new BluetoothCodecConfig.Builder()
                        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC)
                        .build();
        mCodecConfigSBC =
                new BluetoothCodecConfig.Builder()
                        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC)
                        .build();
        mCodecConfigs[0] = mCodecConfigAAC;
        mCodecConfigs[1] = mCodecConfigSBC;

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        when(mBluetoothAdapter.getActiveDevices(eq(BluetoothProfile.A2DP)))
                .thenReturn(Arrays.asList(mActiveDevice));

        DEFAULT_ENTRY = mContext.getString(R.string.bluetooth_audio_codec_default_selection);
    }

    private void verifySetupDefaultListPreference() {
        List<String> entries = new ArrayList<>(1);
        entries.add(DEFAULT_ENTRY);
        List<String> entryValues = new ArrayList<>(1);
        entryValues.add(DEFAULT_ENTRY_VALUE);

        verify(mPreference).setEntries(entries.toArray(new String[entries.size()]));
        verify(mPreference).setEntryValues(entryValues.toArray(new String[entryValues.size()]));
        verify(mPreference).setValue(DEFAULT_ENTRY_VALUE);
        verify(mPreference).setSummary(DEFAULT_ENTRY);
    }

    @Test
    public void onPreferenceChange_shouldSetupDefaultListPreference() {
        mController.onPreferenceChange(mPreference, "" /* new value */);
        verifySetupDefaultListPreference();
    }

    @Test
    public void setupListPreference_wrongSize_shouldSetupDefaultListPreference() {
        List<String> entries = new ArrayList<>(1);
        entries.add(DEFAULT_ENTRY);
        List<String> entryValues = new ArrayList<>(2);
        entryValues.add(DEFAULT_ENTRY_VALUE);
        entryValues.add(DEFAULT_ENTRY_VALUE);

        mController.setupListPreference(entries, entryValues, "", "");
        verifySetupDefaultListPreference();
    }

    @Test
    public void setupListPreference_listEmpty_shouldSetupDefaultListPreference() {
        List<String> entries = new ArrayList<>(1);
        entries.add(DEFAULT_ENTRY);
        List<String> entryValues = new ArrayList<>();

        mController.setupListPreference(entries, entryValues, "", "");
        verifySetupDefaultListPreference();
    }

    @Test
    public void getBluetoothCodecStatus_errorChecking() {
        mController.onBluetoothServiceConnected(null);
        assertThat(mController.getBluetoothCodecStatus()).isNull();

        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(null);
        assertThat(mController.getBluetoothCodecStatus()).isNull();
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
        mCodecStatus = new BluetoothCodecStatus.Builder().setCodecConfig(mCodecConfigAAC).build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        assertThat(mController.getCurrentCodecConfig()).isEqualTo(mCodecConfigAAC);
    }

    @Test
    public void isHDAudioEnabled_errorChecking() {
        mController.onBluetoothServiceConnected(null);
        assertFalse(mController.isHDAudioEnabled());

        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED);
        assertFalse(mController.isHDAudioEnabled());
    }

    @Test
    public void isHDAudioEnabled_verifyEnabled() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        assertTrue(mController.isHDAudioEnabled());
    }

    @Test
    public void onBluetoothServiceConnected_verifyBluetoothA2dpConfigStore() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigAAC)
                        .setCodecsSelectableCapabilities(Arrays.asList(mCodecConfigs))
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        verify(mBluetoothA2dpConfigStore).setCodecType(mCodecConfigAAC.getExtendedCodecType());
        verify(mBluetoothA2dpConfigStore).setSampleRate(mCodecConfigAAC.getSampleRate());
        verify(mBluetoothA2dpConfigStore).setBitsPerSample(mCodecConfigAAC.getBitsPerSample());
        verify(mBluetoothA2dpConfigStore).setChannelMode(mCodecConfigAAC.getChannelMode());
        verify(mBluetoothA2dpConfigStore).setCodecPriority(CODEC_PRIORITY_HIGHEST);
        verify(mBluetoothA2dpConfigStore)
                .setCodecSpecific1Value(mCodecConfigAAC.getCodecSpecific1());
    }

    private static class AbstractBluetoothListPreferenceControllerImpl
            extends AbstractBluetoothListPreferenceController {

        private AbstractBluetoothListPreferenceControllerImpl(
                Context context, Lifecycle lifecycle, BluetoothA2dpConfigStore store) {
            super(context, lifecycle, store);
        }

        @Override
        public String getPreferenceKey() {
            return "KEY";
        }

        @Override
        protected void writeConfigurationValues(String entryValue) {}
    }
}
