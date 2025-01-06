/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.virtual;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.companion.AssociationInfo;
import android.content.Context;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VirtualDeviceWrapperTest {

    private static final String PERSISTENT_DEVICE_ID = "PersistentDeviceIdForTest";
    private static final String DEVICE_NAME = "DEVICE NAME";

    @Mock
    private AssociationInfo mAssociationInfo;
    @Mock
    private Context mContext;
    private VirtualDeviceWrapper mVirtualDeviceWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mVirtualDeviceWrapper = new VirtualDeviceWrapper(mAssociationInfo, PERSISTENT_DEVICE_ID,
                Context.DEVICE_ID_INVALID);
    }

    @Test
    public void setDeviceId() {
        assertThat(mVirtualDeviceWrapper.getDeviceId()).isEqualTo(Context.DEVICE_ID_INVALID);
        mVirtualDeviceWrapper.setDeviceId(42);
        assertThat(mVirtualDeviceWrapper.getDeviceId()).isEqualTo(42);
    }

    @Test
    public void getDisplayName_fromAssociationInfo() {
        when(mAssociationInfo.getDisplayName()).thenReturn(DEVICE_NAME);
        assertThat(mVirtualDeviceWrapper.getDeviceName(mContext).toString()).isEqualTo(DEVICE_NAME);
    }

    @Test
    public void getDisplayName_fromResources() {
        when(mAssociationInfo.getDisplayName()).thenReturn(null);
        when(mContext.getString(R.string.virtual_device_unknown)).thenReturn(DEVICE_NAME);
        assertThat(mVirtualDeviceWrapper.getDeviceName(mContext).toString()).isEqualTo(DEVICE_NAME);
    }
}
