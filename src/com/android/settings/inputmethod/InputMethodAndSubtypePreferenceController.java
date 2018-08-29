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

package com.android.settings.inputmethod;

import android.content.Context;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeEnablerManagerCompat;

public class InputMethodAndSubtypePreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    private PreferenceFragmentCompat mFragment;
    private InputMethodAndSubtypeEnablerManagerCompat mManager;
    private String mTargetImi;

    public InputMethodAndSubtypePreferenceController(Context context, String key) {
        super(context, key);
    }

    public void initialize(PreferenceFragmentCompat fragment, String imi) {
        mFragment = fragment;
        mTargetImi = imi;
        mManager = new InputMethodAndSubtypeEnablerManagerCompat(mFragment);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mManager.init(mFragment, mTargetImi, screen);
    }

    @Override
    public void onStart() {
        mManager.refresh(mContext, mFragment);
    }

    @Override
    public void onStop() {
        mManager.save(mContext, mFragment);
    }
}
