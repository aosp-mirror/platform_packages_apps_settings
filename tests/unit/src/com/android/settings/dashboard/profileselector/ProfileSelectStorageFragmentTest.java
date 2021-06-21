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

package com.android.settings.dashboard.profileselector;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ProfileSelectStorageFragmentTest {

    private ProfileSelectStorageFragment mFragment;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mFragment = new ProfileSelectStorageFragment();
    }

    @Test
    @UiThreadTest
    public void test_initializeOptionsMenuInvalidatesExistingMenu() {
        final Activity activity = mock(Activity.class);

        mFragment.initializeOptionsMenu(activity);

        verify(activity).invalidateOptionsMenu();
    }
}
