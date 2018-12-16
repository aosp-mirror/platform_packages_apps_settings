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

package com.android.settings.deviceinfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.os.SystemProperties;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HardwareInfoDialogFragmentTest {

    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
    }

    @Test
    public void display_shouldShowHardwareRevision() {
        final String TEST_HARDWARE_REV = "123";
        SystemProperties.set("ro.boot.hardware.revision", TEST_HARDWARE_REV);

        final HardwareInfoDialogFragment fragment = spy(HardwareInfoDialogFragment.newInstance());
        doReturn("").when(fragment).getSerialNumber();
        fragment.show(mActivity.getSupportFragmentManager(), HardwareInfoDialogFragment.TAG);

        verify(fragment).setText(
                any(View.class), eq(R.id.model_label), eq(R.id.model_value),
                anyString());

        verify(fragment).setText(
                any(View.class), eq(R.id.hardware_rev_label), eq(R.id.hardware_rev_value),
                anyString());
    }
}
