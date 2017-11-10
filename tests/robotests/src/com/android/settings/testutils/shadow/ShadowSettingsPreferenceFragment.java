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

import android.os.Bundle;

import com.android.settings.SettingsPreferenceFragment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Shadow of {@link SettingsPreferenceFragment}.
 *
 * Override the {@link #onCreate(Bundle)} to skip a null pointer exception in
 * {@link android.content.res.Resources.Theme}, much the same as {@link ShadowDashboardFragment}.
 */
@Implements(SettingsPreferenceFragment.class)
public class ShadowSettingsPreferenceFragment {

    @Implementation
    public void onCreate(Bundle savedInstanceState) {
        // do nothing
    }
}