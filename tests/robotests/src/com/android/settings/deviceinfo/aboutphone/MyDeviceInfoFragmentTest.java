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

package com.android.settings.deviceinfo.aboutphone;

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.os.Bundle;

import com.android.settings.deviceinfo.BuildNumberPreferenceController;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Ignore
public class MyDeviceInfoFragmentTest {

    private MyDeviceInfoFragment mMyDeviceInfoFragment;

    @Test
    public void onCreate_fromSearch_shouldExpandAllPreferences() {
        final Bundle args = new Bundle();
        args.putString(EXTRA_FRAGMENT_ARG_KEY, "search_key");
        mMyDeviceInfoFragment = FragmentController.of(new MyDeviceInfoFragment(), args)
                .create()
                .get();

        assertThat(mMyDeviceInfoFragment.getPreferenceScreen().getInitialExpandedChildrenCount())
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void onActivityResult_shouldCallBuildNumberPreferenceController() {
        mMyDeviceInfoFragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .get();

        final BuildNumberPreferenceController controller =
            mock(BuildNumberPreferenceController.class);
        ReflectionHelpers.setField(
                mMyDeviceInfoFragment, "mBuildNumberPreferenceController", controller);

        final int requestCode = 1;
        final int resultCode = 2;
        final Intent data = new Intent();
        mMyDeviceInfoFragment.onActivityResult(requestCode, resultCode, data);

        verify(controller).onActivityResult(requestCode, resultCode, data);
    }
}
