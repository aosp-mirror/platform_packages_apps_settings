/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.bluetooth;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settingslib.bluetooth.BluetoothBroadcastUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(AndroidJUnit4.class)
public class QrCodeScanModeActivityTest {

    @Mock
    private Intent mIntent;
    private QrCodeScanModeActivity mActivity;

    @Before
    public void setUp() {
        mIntent = new Intent(BluetoothBroadcastUtils.ACTION_BLUETOOTH_LE_AUDIO_QR_CODE_SCANNER);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                mActivity =
                        spy((QrCodeScanModeActivity) InstrumentationRegistry
                                .getInstrumentation().newActivity(
                                        getClass().getClassLoader(),
                                        QrCodeScanModeActivity.class.getName(), mIntent));
            } catch (Exception e) {
                throw new RuntimeException(e); // nothing to do
            }
        });
    }

    @Test
    public void handleIntent_noIntentAction_shouldFinish() {
        mIntent = new Intent();
        mActivity.handleIntent(mIntent);
        verify(mActivity).finish();
    }

    @Test
    public void handleIntent_hasIntentExtra_shouldShowFragment() {
        doNothing().when(mActivity).showQrCodeScannerFragment(mIntent);
        mActivity.handleIntent(mIntent);
        verify(mActivity).showQrCodeScannerFragment(mIntent);
    }

}
