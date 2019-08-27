/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.deviceadmin;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.UserHandle;

import androidx.lifecycle.LifecycleOwner;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DeviceAdminListPreferenceControllerTest {
    private Context mContext;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        final LifecycleOwner lifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(lifecycleOwner);

        mLifecycle.addObserver(new DeviceAdminListPreferenceController(mContext, "test_key"));
    }

    @Test
    public void onStart_registerReceiver() {
        mLifecycle.handleLifecycleEvent(ON_START);

        verify(mContext).registerReceiverAsUser(any(BroadcastReceiver.class), eq(UserHandle.ALL),
                any(IntentFilter.class), isNull(), isNull());
    }

    @Test
    public void onStop_unregisterReceiver() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }
}
