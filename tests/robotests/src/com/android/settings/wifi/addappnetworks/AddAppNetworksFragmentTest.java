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

package com.android.settings.wifi.addappnetworks;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
public class AddAppNetworksFragmentTest {
    private static final String FAKE_APP_NAME = "fake_app_name";
    private FragmentActivity mActivity;
    private AddAppNetworksFragment mAddAppNetworksFragment;
    private Context mContext;

    @Before
    public void setUp() {
        mAddAppNetworksFragment = spy(new AddAppNetworksFragment());
        MockitoAnnotations.initMocks(this);

        // Set up bundle
        final Bundle bundle = new Bundle();
        bundle.putString(AddAppNetworksActivity.KEY_CALLING_PACKAGE_NAME, FAKE_APP_NAME);
        doReturn(bundle).when(mAddAppNetworksFragment).getArguments();

        FragmentController.setupFragment(mAddAppNetworksFragment);
    }

    @Test
    public void callingPackageName_onCreateView_shouldBeCorrect() {
        assertThat(mAddAppNetworksFragment.mCallingPackageName).isEqualTo(FAKE_APP_NAME);
    }

    @Test
    public void launchFragment_shouldShowSaveButton() {
        assertThat(mAddAppNetworksFragment.mSaveButton).isNotNull();
    }

    @Test
    public void launchFragment_shouldShowCancelButton() {
        assertThat(mAddAppNetworksFragment.mCancelButton).isNotNull();
    }
}
