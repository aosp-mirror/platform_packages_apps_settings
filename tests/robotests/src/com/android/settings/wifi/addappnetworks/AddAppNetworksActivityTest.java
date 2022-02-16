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

import static org.robolectric.Shadows.shadowOf;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AddAppNetworksActivityTest {

    @Test
    public void startActivity_withPackageName_bundleShouldHaveRightPackageName() {
        final String packageName = RuntimeEnvironment.application.getPackageName();
        final AddAppNetworksActivity activity =
                Robolectric.buildActivity(AddAppNetworksActivity.class).create().get();
        shadowOf(activity).setCallingPackage(packageName);

        activity.showAddNetworksFragment();

        assertThat(activity.mBundle.getString(AddAppNetworksActivity.KEY_CALLING_PACKAGE_NAME))
                .isEqualTo(packageName);
    }
}
