/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.AbstractBluetoothA2dpPreferenceController
        .STREAMING_LABEL_ID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AbstractBluetoothA2dpPreferenceControllerTest {

    @Mock
    private BluetoothA2dp mBluetoothA2dp;
    @Mock
    private BluetoothCodecConfig mBluetoothCodecConfig;
    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;

    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private Context mContext;
    private AbstractBluetoothA2dpPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new AbstractBluetoothA2dpPreferenceControllerImpl(mContext, mLifecycle,
                mBluetoothA2dpConfigStore));
        doReturn(mBluetoothCodecConfig).when(mController).getCodecConfig(null);
        doNothing().when(mController).setCodecConfigPreference(any(), any());
        when(mBluetoothA2dpConfigStore.createCodecConfig()).thenReturn(mBluetoothCodecConfig);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    @Ignore
    public void onPreferenceChange_bluetoothConnected_shouldUpdateCodec() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.onPreferenceChange(mPreference, "" /* new value */);

        verify(mController).setCodecConfigPreference(any(), any());
    }

    @Test
    public void onPreferenceChange_bluetoothNotConnected_shouldNotUpdateCodec() {
        mController.onBluetoothServiceDisconnected();

        mController.onPreferenceChange(mPreference, "" /* new value */);

        verify(mController, never()).setCodecConfigPreference(any(), any());
    }

    @Test
    @Ignore
    public void updateState_option2Set_shouldUpdateToOption2() {
        when(mBluetoothCodecConfig.getSampleRate()).thenReturn(
                BluetoothCodecConfig.SAMPLE_RATE_48000);

        doReturn(2).when(mController).getCurrentA2dpSettingIndex(any());
        mController.updateState(mPreference);

        verify(mPreference).setValue(mController.getListValues()[2]);
        verify(mPreference).setSummary(mContext.getString(STREAMING_LABEL_ID,
            mController.getListSummaries()[2]));
    }

    @Test
    public void onBluetoothServiceConnected_shouldUpdateState() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        verify(mController).updateState(mPreference);
    }

    private static class AbstractBluetoothA2dpPreferenceControllerImpl
        extends AbstractBluetoothA2dpPreferenceController {

        private AbstractBluetoothA2dpPreferenceControllerImpl(Context context,
                Lifecycle lifecycle, BluetoothA2dpConfigStore store) {
            super(context, lifecycle, store);
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }

        @Override
        protected String[] getListValues() {
            return new String[]{"1", "2", "3"};
        }

        @Override
        protected String[] getListSummaries() {
            return new String[]{"foo", "bar", "foobar"};
        }

        @Override
        protected void writeConfigurationValues(Object newValue) {
        }

        @Override
        protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig config) {
            return 0;
        }

        @Override
        protected int getDefaultIndex() {
            return 0;
        }
    }
}
