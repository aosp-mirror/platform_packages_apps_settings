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

package com.android.settings.vpn2;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.pm.PackageInfo;

import androidx.fragment.app.Fragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppDialogFragmentTest {

    @Mock
    private Fragment mParent;

    private PackageInfo mPackageInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPackageInfo = new PackageInfo();
    }

    @Test
    public void notManagingOrConnected_shouldNotShow() {
        AppDialogFragment
            .show(mParent, mPackageInfo, "label", false /* manage */, false /* connected */);

        verify(mParent, never()).isAdded();
    }

    @Test
    public void notManagingAndConnected_showShow() {
        AppDialogFragment
            .show(mParent, mPackageInfo, "label", false /* manage */, true /* connected */);

        verify(mParent).isAdded();
    }
}
