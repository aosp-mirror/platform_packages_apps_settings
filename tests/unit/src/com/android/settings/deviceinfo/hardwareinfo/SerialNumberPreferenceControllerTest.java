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

package com.android.settings.deviceinfo.hardwareinfo;

import static android.content.Context.CLIPBOARD_SERVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidJUnit4.class)
public class SerialNumberPreferenceControllerTest {

    private Context mContext;
    private SerialNumberPreferenceController mController;
    private ClipboardManager mClipboardManager;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        mClipboardManager = (ClipboardManager) spy(mContext.getSystemService(CLIPBOARD_SERVICE));
        doReturn(mClipboardManager).when(mContext).getSystemService(CLIPBOARD_SERVICE);
        mController = new SerialNumberPreferenceController(mContext, "test");
    }

    @Test
    @UiThreadTest
    public void copy_shouldPutSerialNumberToClipBoard() {
        ArgumentCaptor<ClipData> captor = ArgumentCaptor.forClass(ClipData.class);
        doNothing().when(mClipboardManager).setPrimaryClip(captor.capture());

        mController.copy();

        final ClipData data = captor.getValue();
        assertThat(data.getItemAt(0).getText().toString()).contains(Build.getSerial());
    }
}
