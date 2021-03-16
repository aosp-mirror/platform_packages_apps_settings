/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class BluetoothPermissionActivityTest {

    private BluetoothPermissionActivity mActivity;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mActivity = new BluetoothPermissionActivity();
    }

    @Test
    public void callingPackageIsEqualToReturnPackage_sendBroadcastToReturnPackage() {
        mActivity.mReturnPackage = "com.android.settings";
        mActivity.mReturnClass = "com.android.settings.bluetooth.BluetoothPermissionActivity";
        mActivity.mCallingAppPackageName = "com.android.settings";
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ReflectionHelpers.setField(mActivity, "mBase", mContext);

        mActivity.sendReplyIntentToReceiver(true, true);

        verify(mContext).sendBroadcast(intentCaptor.capture(),
                eq("android.permission.BLUETOOTH_ADMIN"));
        assertThat(intentCaptor.getValue().getComponent().getPackageName())
                .isEqualTo("com.android.settings");
    }

    @Test
    public void callingPackageIsNotEqualToReturnPackage_broadcastNotSend() {
        mActivity.mReturnPackage = "com.fake.settings";
        mActivity.mReturnClass = "com.android.settings.bluetooth.BluetoothPermissionActivity";
        mActivity.mCallingAppPackageName = "com.android.settings";
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ReflectionHelpers.setField(mActivity, "mBase", mContext);

        mActivity.sendReplyIntentToReceiver(true, true);

        verify(mContext, never()).sendBroadcast(intentCaptor.capture(),
                eq("android.permission.BLUETOOTH_ADMIN"));
    }
}
