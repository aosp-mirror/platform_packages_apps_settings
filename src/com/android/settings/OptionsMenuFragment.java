/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings;

import android.os.Bundle;

import com.android.settings.core.InstrumentedPreferenceFragment;

/**
 * Base class for a fragment that has the options menu.
 * SettingsPreferenceFragment automatically sets this, but some activities do not use
 * preferences, and we need to call setHasOptionsMenu(true) for the back button on action bar.
 * For preference fragments, use SettingsPreferenceFragment.
 */
public abstract class OptionsMenuFragment extends InstrumentedPreferenceFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }
}
