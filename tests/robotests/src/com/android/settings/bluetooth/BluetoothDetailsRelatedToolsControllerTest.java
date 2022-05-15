/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BluetoothDetailsRelatedToolsController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsRelatedToolsControllerTest extends BluetoothDetailsControllerTestBase {
    private BluetoothDetailsRelatedToolsController mController;

    @Override
    public void setUp() {
        super.setUp();
        mController = new BluetoothDetailsRelatedToolsController(mContext, mFragment, mCachedDevice,
                mLifecycle);
    }

    @Test
    public void isAvailable_isHearingAidDevice_available() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notHearingAidDevice_notAvailable() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }
}
