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

package com.android.settings.inputmethod;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.settings.R;
import com.android.settings.InstrumentedFragment;
import com.android.settings.SettingsPreferenceFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class AvailableVirtualKeyboardFragment extends SettingsPreferenceFragment
        implements InputMethodPreference.OnSavePreferenceListener {

    private static final Drawable NO_ICON = new ColorDrawable(Color.TRANSPARENT);

    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList = new ArrayList<>();
    private InputMethodSettingValuesWrapper mInputMethodSettingValues;
    private InputMethodManager mImm;
    private DevicePolicyManager mDpm;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Activity activity = getActivity();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(activity);
        screen.setTitle(activity.getString(R.string.available_virtual_keyboard_category));
        setPreferenceScreen(screen);
        mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(activity);
        mImm = activity.getSystemService(InputMethodManager.class);
        mDpm = activity.getSystemService(DevicePolicyManager.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh internal states in mInputMethodSettingValues to keep the latest
        // "InputMethodInfo"s and "InputMethodSubtype"s
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        updateInputMethodPreferenceViews();
    }

    @Override
    public void onSaveInputMethodPreference(final InputMethodPreference pref) {
        final boolean hasHardwareKeyboard = getResources().getConfiguration().keyboard
                == Configuration.KEYBOARD_QWERTY;
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(),
                mImm.getInputMethodList(), hasHardwareKeyboard);
        // Update input method settings and preference list.
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        for (final InputMethodPreference p : mInputMethodPreferenceList) {
            p.updatePreferenceViews();
        }
    }

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.ENABLE_VIRTUAL_KEYBOARDS;
    }

    private void updateInputMethodPreferenceViews() {
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        // Clear existing "InputMethodPreference"s
        mInputMethodPreferenceList.clear();
        List<String> permittedList = mDpm.getPermittedInputMethodsForCurrentUser();
        final Context context = getPrefContext();
        final List<InputMethodInfo> imis = mInputMethodSettingValues.getInputMethodList();
        final int N = (imis == null ? 0 : imis.size());
        for (int i = 0; i < N; ++i) {
            final InputMethodInfo imi = imis.get(i);
            final boolean isAllowedByOrganization = permittedList == null
                    || permittedList.contains(imi.getPackageName());
            Drawable icon;
            try {
                icon = getActivity().getPackageManager().getApplicationIcon(imi.getPackageName());
            } catch (Exception e) {
                icon = NO_ICON;
            }
            final InputMethodPreference pref = new InputMethodPreference(
                    context, imi, true, isAllowedByOrganization, this);
            pref.setIcon(icon);
            mInputMethodPreferenceList.add(pref);
        }
        final Collator collator = Collator.getInstance();
        Collections.sort(mInputMethodPreferenceList, new Comparator<InputMethodPreference>() {
            @Override
            public int compare(InputMethodPreference lhs, InputMethodPreference rhs) {
                return lhs.compareTo(rhs, collator);
            }
        });
        getPreferenceScreen().removeAll();
        for (int i = 0; i < N; ++i) {
            final InputMethodPreference pref = mInputMethodPreferenceList.get(i);
            pref.setOrder(i);
            getPreferenceScreen().addPreference(pref);
            InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(pref);
            pref.updatePreferenceViews();
        }
    }
}
