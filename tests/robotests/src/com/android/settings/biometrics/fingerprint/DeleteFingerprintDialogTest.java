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

package com.android.settings.biometrics.fingerprint;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Dialog;
import android.hardware.fingerprint.Fingerprint;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import com.android.settings.biometrics.fingerprint.FingerprintSettings
        .FingerprintSettingsFragment.DeleteFingerprintDialog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = DeleteFingerprintDialogTest.ShadowFragment.class)
public class DeleteFingerprintDialogTest {

    @Mock
    private Fingerprint mFingerprint;
    @Mock
    private FingerprintSettingsFragment mTarget;

    private DeleteFingerprintDialog mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = spy(DeleteFingerprintDialog.newInstance(mFingerprint, mTarget));
    }

    @Test
    public void launchDialog_clickPositive_shouldDeleteFingerprint() {
        FragmentController.setupFragment(mFragment, FragmentActivity.class, 0 /* containerViewId */,
                null /* bundle */);

        mFragment.onClick(mFragment.getDialog(), Dialog.BUTTON_POSITIVE);

        verify(mTarget).deleteFingerPrint(mFingerprint);
    }

    @Test
    public void launchDialog_clickNegative_shouldDoNothing() {
        FragmentController.setupFragment(mFragment, FragmentActivity.class, 0 /* containerViewId */,
                null /* bundle */);

        mFragment.onClick(mFragment.getDialog(), Dialog.BUTTON_NEGATIVE);

        verify(mTarget, never()).deleteFingerPrint(mFingerprint);
    }

    @Implements(Fragment.class)
    public static class ShadowFragment {
        private Fragment mTargetFragment;

        @Implementation
        protected void setTargetFragment(Fragment fragment, int requestCode) {
            mTargetFragment = fragment;
        }

        @Implementation
        protected Fragment getTargetFragment() {
            return mTargetFragment;
        }
    }
}
