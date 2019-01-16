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

package com.android.settings.display;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

public class VrDisplayPreferencePicker extends RadioButtonPickerFragment {

    static final String PREF_KEY_PREFIX = "vr_display_pref_";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.vr_display_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.VR_DISPLAY_PREFERENCE;
    }

    @Override
    protected List<VrCandidateInfo> getCandidates() {
        List<VrCandidateInfo> candidates = new ArrayList<>();
        final Context context = getContext();
        candidates.add(new VrCandidateInfo(context, 0, R.string.display_vr_pref_low_persistence));
        candidates.add(new VrCandidateInfo(context, 1, R.string.display_vr_pref_off));
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        int current = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                Settings.Secure.VR_DISPLAY_MODE, Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE,
                mUserId);
        return PREF_KEY_PREFIX + current;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        switch (key) {
            case PREF_KEY_PREFIX + 0:
                return Settings.Secure.putIntForUser(getContext().getContentResolver(),
                        Settings.Secure.VR_DISPLAY_MODE, 0, mUserId);
            case PREF_KEY_PREFIX + 1:
                return Settings.Secure.putIntForUser(getContext().getContentResolver(),
                        Settings.Secure.VR_DISPLAY_MODE, 1, mUserId);
        }
        return false;
    }

    static class VrCandidateInfo extends CandidateInfo {

        public final String label;
        public final int value;

        public VrCandidateInfo(Context context, int value, int resId) {
            super(true);
            this.value = value;
            label = context.getString(resId);
        }

        @Override
        public CharSequence loadLabel() {
            return label;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return PREF_KEY_PREFIX + value;
        }
    }
}
