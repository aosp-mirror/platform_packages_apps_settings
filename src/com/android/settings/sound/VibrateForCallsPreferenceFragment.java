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
package com.android.settings.sound;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fragment for changing vibrate for calls options.
 */
public class VibrateForCallsPreferenceFragment extends RadioButtonPickerFragment {
    private static final String TAG = "VibrateForCallsPreferenceFragment";

    @VisibleForTesting
    static final String KEY_NEVER_VIBRATE = "never_vibrate";
    @VisibleForTesting
    static final String KEY_ALWAYS_VIBRATE = "always_vibrate";
    @VisibleForTesting
    static final String KEY_RAMPING_RINGER = "ramping_ringer";

    private static final int ON = 1;
    private static final int OFF = 0;

    private final Map<String, VibrateForCallsCandidateInfo> mCandidates;

    public VibrateForCallsPreferenceFragment() {
        mCandidates = new ArrayMap<>();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        loadCandidates(context);
    }

    private void loadCandidates(Context context) {
        mCandidates.put(KEY_NEVER_VIBRATE,
                new VibrateForCallsCandidateInfo(
                        KEY_NEVER_VIBRATE, R.string.vibrate_when_ringing_option_never_vibrate));
        mCandidates.put(KEY_ALWAYS_VIBRATE,
                new VibrateForCallsCandidateInfo(
                        KEY_ALWAYS_VIBRATE, R.string.vibrate_when_ringing_option_always_vibrate));
        mCandidates.put(KEY_RAMPING_RINGER,
                new VibrateForCallsCandidateInfo(
                        KEY_RAMPING_RINGER, R.string.vibrate_when_ringing_option_ramping_ringer));
    }

    private void updateSettings(VibrateForCallsCandidateInfo candidate) {
        final String key = candidate.getKey();
        if (TextUtils.equals(key, KEY_ALWAYS_VIBRATE)) {
            Settings.System.putInt(
                    getContext().getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, ON);
            Settings.Global.putInt(
                    getContext().getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, OFF);
        } else if (TextUtils.equals(key, KEY_RAMPING_RINGER)) {
            Settings.System.putInt(
                    getContext().getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, OFF);
            Settings.Global.putInt(
                    getContext().getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, ON);
        } else {
            Settings.System.putInt(
                    getContext().getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, OFF);
            Settings.Global.putInt(
                    getContext().getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, OFF);
        }
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<VibrateForCallsCandidateInfo> candidates = new ArrayList<>();
        candidates.add(mCandidates.get(KEY_NEVER_VIBRATE));
        candidates.add(mCandidates.get(KEY_ALWAYS_VIBRATE));
        candidates.add(mCandidates.get(KEY_RAMPING_RINGER));
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        if (Settings.Global.getInt(
                 getContext().getContentResolver(),
                 Settings.Global.APPLY_RAMPING_RINGER, OFF) == ON) {
            return KEY_RAMPING_RINGER;
        } else if (Settings.System.getInt(
                    getContext().getContentResolver(),
                    Settings.System.VIBRATE_WHEN_RINGING, OFF) == ON) {
            return KEY_ALWAYS_VIBRATE;
        } else {
            return KEY_NEVER_VIBRATE;
        }
    }

    @Override
    protected boolean setDefaultKey(String key) {
        final VibrateForCallsCandidateInfo candidate = mCandidates.get(key);
        if (candidate == null) {
            Log.e(TAG, "Unknown vibrate for calls candidate (key = " + key + ")!");
            return false;
        }
        updateSettings(candidate);
        return true;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.vibrate_for_calls_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.VIBRATE_FOR_CALLS;
    }

    @VisibleForTesting
    class VibrateForCallsCandidateInfo extends CandidateInfo {
        private final String mKey;
        private final int mLabelId;

        VibrateForCallsCandidateInfo(String key, int labelId) {
            super(true /* enabled */);
            mKey = key;
            mLabelId = labelId;
        }

        @Override
        public CharSequence loadLabel() {
            return getContext().getString(mLabelId);
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }
}
