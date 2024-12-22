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

package com.android.settings.development;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
public class RebootConfirmationDialogFragmentTest {

    private RebootConfirmationDialogFragment mFragment;

    @Mock
    RebootConfirmationDialogHost mRebootConfirmationDialogHost;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        RebootConfirmationDialogFragment dialogFragment =
                FragmentController.setupFragment(
                        new RebootConfirmationDialogFragment(
                                R.string.reboot_dialog_override_desktop_mode,
                                R.string.reboot_dialog_reboot_later,
                                mRebootConfirmationDialogHost),
                        FragmentActivity.class,
                        0 /* containerViewId= */, null /* bundle= */);

        mFragment = Mockito.spy(dialogFragment);
    }

    @Test
    public void onPause_shouldDismissDialog() {
        mFragment.onPause();

        Mockito.verify(mFragment).dismiss();
    }
}
