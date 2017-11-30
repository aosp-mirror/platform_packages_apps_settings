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

package com.android.settings.testutils.shadow;

import android.app.Fragment;
import android.app.FragmentManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Override the {@link #setTargetFragment(Fragment, int)} to skip an illegal state exception
 * in SDK 26. SDK 26 requires that the target fragment be in the same {@link FragmentManager} as
 * the current {@link Fragment}. This is infeasible with our current framework.
 */
@Implements(
        value = Fragment.class,
        minSdk = 26
)
public class ShadowFragment {

    private Fragment mTargetFragment;

    @Implementation
    public void setTargetFragment(Fragment fragment, int requestCode) {
        mTargetFragment = fragment;
    }

    @Implementation
    final public Fragment getTargetFragment() {
        return mTargetFragment;
    }
}
