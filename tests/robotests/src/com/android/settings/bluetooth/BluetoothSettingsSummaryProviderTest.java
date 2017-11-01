/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothSettingsSummaryProviderTest {

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private SummaryLoader mSummaryLoader;

    private BluetoothSettings.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mSummaryProvider = new BluetoothSettings.SummaryProvider(mContext, mSummaryLoader,
                mBluetoothManager);
    }

    @Test
    public void setListening_shouldRegister() {
        mSummaryProvider.setListening(true);

        verify(mBluetoothManager.getEventManager()).registerCallback(
            mSummaryProvider.mSummaryUpdater);
    }

    @Test
    public void setNotListening_shouldUnregister() {
        mSummaryProvider.setListening(false);

        verify(mBluetoothManager.getEventManager()).unregisterCallback(
            mSummaryProvider.mSummaryUpdater);
    }

    @Test
    public void onSummaryChanged_shouldSetSummary() {
        final String summary = "Bluetooth summary";
        mSummaryProvider.onSummaryChanged(summary);

        verify(mSummaryLoader).setSummary(mSummaryProvider, summary);
    }

}
