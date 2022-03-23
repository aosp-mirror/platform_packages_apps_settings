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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class BluetoothFilesPreferenceControllerTest {

    private Context mContext;
    private BluetoothFilesPreferenceController mController;
    private Preference mPreference;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = new BluetoothFilesPreferenceController(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(BluetoothFilesPreferenceController.KEY_RECEIVED_FILES);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    @Test
    public void testHandlePreferenceTreeClick_sendBroadcast() {
        mController.handlePreferenceTreeClick(mPreference);

        final Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction())
            .isEqualTo(BluetoothFilesPreferenceController.ACTION_OPEN_FILES);

        final Bundle bundle = intent.getExtras();
        assertThat(bundle.getInt(BluetoothFilesPreferenceController.EXTRA_DIRECTION)).isEqualTo(1);
        assertThat(bundle.getBoolean(BluetoothFilesPreferenceController.EXTRA_SHOW_ALL_FILES))
            .isTrue();
    }
}
