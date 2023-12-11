/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace.autolock;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.privatespace.PrivateSpaceMaintainer;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.TopIntroPreference;

import java.util.ArrayList;
import java.util.List;

public class AutoLockSettingsFragment extends RadioButtonPickerFragment {
    private static final String TAG = "PSAutoLockSetting";
    private PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    private CharSequence[] mAutoLockRadioOptions;
    private CharSequence[] mAutoLockRadioValues;

    @Override
    public void onCreate(@NonNull Bundle icicle) {
        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.supportAutolockForPrivateSpace()) {
            super.onCreate(icicle);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPrivateSpaceMaintainer.isPrivateSpaceLocked()) {
            finish();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mPrivateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(context);
        mAutoLockRadioOptions =
                context.getResources().getStringArray(R.array.private_space_auto_lock_options);
        mAutoLockRadioValues =
                context.getResources()
                        .getStringArray(R.array.private_space_auto_lock_options_values);
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        final TopIntroPreference introPreference = new TopIntroPreference(screen.getContext());
        introPreference.setTitle(R.string.private_space_auto_lock_page_summary);
        screen.addPreference(introPreference);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.private_space_auto_lock_settings;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<CandidateInfo> candidates = new ArrayList<>();
        if (mAutoLockRadioValues != null) {
            for (int i = 0; i < mAutoLockRadioValues.length; ++i) {
                candidates.add(
                        new AutoLockCandidateInfo(
                                mAutoLockRadioOptions[i], mAutoLockRadioValues[i].toString()));
            }
        } else {
            Log.e(TAG, "Autolock options do not exist.");
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        return Integer.toString(mPrivateSpaceMaintainer.getPrivateSpaceAutoLockSetting());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        try {
            @Settings.Secure.PrivateSpaceAutoLockOption final int value = Integer.parseInt(key);
            mPrivateSpaceMaintainer.setPrivateSpaceAutoLockSetting(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist screen timeout setting", e);
        }
        return true;
    }

    private static class AutoLockCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        AutoLockCandidateInfo(CharSequence label, String key) {
            super(true);
            mLabel = label;
            mKey = key;
        }

        @NonNull
        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Nullable
        @Override
        public Drawable loadIcon() {
            return null;
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }
}
