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

import android.annotation.Nullable;
import android.app.IActivityManager;
import android.os.RemoteException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AddAppNetworksActivityTest {

    @Mock
    private IActivityManager mIActivityManager;

    private AddAppNetworksActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mActivity = Robolectric.buildActivity(AddAppNetworksActivity.class).create().get();
        mActivity.mActivityManager = mIActivityManager;
    }

    @Test
    public void getCallingAppPackageName_nullPackageName_returnNotNull() {
        fakeCallingPackage("com.android.settings");

        assertThat(mActivity.getCallingAppPackageName()).isNotNull();
    }

    @Test
    public void getCallingAppPackageName_withPackageName_returnNull()  {
        fakeCallingPackage(null);

        assertThat(mActivity.getCallingAppPackageName()).isNull();
    }

    @Test
    public void showAddNetworksFragment_nullPackageName_returnFalse()  {
        fakeCallingPackage(null);

        assertThat(mActivity.showAddNetworksFragment()).isFalse();
    }

    @Test
    public void showAddNetworksFragment_withPackageName_returnTrue()  {
        fakeCallingPackage("com.android.settings");

        assertThat(mActivity.showAddNetworksFragment()).isTrue();
    }

    private void fakeCallingPackage(@Nullable String packageName) {
        try {
            when(mIActivityManager.getLaunchedFromPackage(any())).thenReturn(packageName);
        } catch (RemoteException e) {
            // Do nothing.
        }
    }
}
