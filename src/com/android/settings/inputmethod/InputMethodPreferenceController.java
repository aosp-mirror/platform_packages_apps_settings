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
 * limitations under the License
 */

package com.android.settings.inputmethod;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.inputmethod.InputMethodPreference;

import com.google.common.annotations.VisibleForTesting;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

public class InputMethodPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart {

    @VisibleForTesting
    PreferenceScreen mScreen;
    private Preference mPreference;
    private InputMethodManager mImm;
    private DevicePolicyManager mDpm;

    public InputMethodPreferenceController(Context context, String key) {
        super(context, key);
        mImm = context.getSystemService(InputMethodManager.class);
        mDpm = context.getSystemService(DevicePolicyManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        mPreference = mScreen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        updateInputMethodPreferenceViews();
    }

    private void updateInputMethodPreferenceViews() {
        final List<InputMethodPreference> preferenceList = new ArrayList<>();

        final List<String> permittedList = mDpm.getPermittedInputMethodsForCurrentUser();
        final List<InputMethodInfo> imis = mImm.getEnabledInputMethodList();
        final int N = (imis == null ? 0 : imis.size());
        for (int i = 0; i < N; ++i) {
            final InputMethodInfo imi = imis.get(i);
            final boolean isAllowedByOrganization = permittedList == null
                    || permittedList.contains(imi.getPackageName());
            final Drawable icon = imi.loadIcon(mContext.getPackageManager());
            final InputMethodPreference pref = new InputMethodPreference(
                    mScreen.getContext(),
                    imi,
                    false,  /* isImeEnabler */
                    isAllowedByOrganization,
                    null  /* this can be null since isImeEnabler is false */);
            pref.setIcon(icon);
            preferenceList.add(pref);
        }
        final Collator collator = Collator.getInstance();
        preferenceList.sort((lhs, rhs) -> lhs.compareTo(rhs, collator));
        mScreen.removeAll();
        for (int i = 0; i < N; ++i) {
            final InputMethodPreference pref = preferenceList.get(i);
            pref.setOrder(i);
            mScreen.addPreference(pref);
            pref.updatePreferenceViews();
        }
        mScreen.addPreference(mPreference);
    }
}
