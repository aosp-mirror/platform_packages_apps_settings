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

package com.android.settings.inputmethod;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.text.BidiFormatter;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class VirtualKeyboardPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private final InputMethodManager mImm;
    private final DevicePolicyManager mDpm;
    private final PackageManager mPm;

    public VirtualKeyboardPreferenceController(Context context) {
        super(context);
        mPm = mContext.getPackageManager();
        mDpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mImm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "virtual_keyboard_pref";
    }

    @Override
    public void updateState(Preference preference) {
        final List<InputMethodInfo> imis = mImm.getEnabledInputMethodList();
        if (imis == null) {
            preference.setSummary(R.string.summary_empty);
            return;
        }

        final List<String> permittedList = mDpm.getPermittedInputMethodsForCurrentUser();
        final List<String> labels = new ArrayList<>();

        for (InputMethodInfo imi : imis) {
            final boolean isAllowedByOrganization = permittedList == null
                    || permittedList.contains(imi.getPackageName());
            if (!isAllowedByOrganization) {
                continue;
            }
            labels.add(imi.loadLabel(mPm).toString());
        }
        if (labels.isEmpty()) {
            preference.setSummary(R.string.summary_empty);
            return;
        }

        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();

        String summary = null;
        for (String label : labels) {
            if (summary == null) {
                summary = bidiFormatter.unicodeWrap(label);
            } else {
                summary = mContext.getString(R.string.join_many_items_middle, summary,
                        bidiFormatter.unicodeWrap(label));
            }
        }
        preference.setSummary(summary);
    }
}
