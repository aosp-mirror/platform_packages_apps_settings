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

package com.android.settings.display;

import static android.provider.Settings.System.FOLD_LOCK_BEHAVIOR;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.utils.CandidateInfoExtra;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment that is used to control fold setting.
 *
 * Keep the setting values in this class in sync with the values in
 * {@link com.android.server.utils.FoldSettingProvider}
 */
public class FoldLockBehaviorSettings extends RadioButtonPickerFragment implements
        HelpResourceProvider {

    public static final String SETTING_VALUE_STAY_AWAKE_ON_FOLD = "stay_awake_on_fold_key";
    public static final String SETTING_VALUE_SELECTIVE_STAY_AWAKE = "selective_stay_awake_key";
    public static final String SETTING_VALUE_SLEEP_ON_FOLD = "sleep_on_fold_key";
    public static final String TAG = "FoldLockBehaviorSetting";
    public static final HashSet<String> SETTING_VALUES = new HashSet<>(
            Set.of(SETTING_VALUE_STAY_AWAKE_ON_FOLD, SETTING_VALUE_SELECTIVE_STAY_AWAKE,
                    SETTING_VALUE_SLEEP_ON_FOLD));
    private static final String SETTING_VALUE_DEFAULT = SETTING_VALUE_SELECTIVE_STAY_AWAKE;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        setIllustrationLottieAnimation(getDefaultKey());
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        List<CandidateInfoExtra> candidates = new ArrayList<>();
        candidates.add(new CandidateInfoExtra(
                resourceToString(R.string.stay_awake_on_fold_title),
                resourceToString(R.string.stay_awake_on_fold_summary),
                SETTING_VALUE_STAY_AWAKE_ON_FOLD, /* enabled */ true));
        candidates.add(new CandidateInfoExtra(
                resourceToString(R.string.selective_stay_awake_title),
                resourceToString(R.string.selective_stay_awake_summary),
                SETTING_VALUE_SELECTIVE_STAY_AWAKE, /* enabled */ true));
        candidates.add(new CandidateInfoExtra(
                resourceToString(R.string.sleep_on_fold_title),
                resourceToString(R.string.sleep_on_fold_summary),
                SETTING_VALUE_SLEEP_ON_FOLD, /* enabled */ true));
        return candidates;
    }

    @Override
    public void bindPreferenceExtra(SelectorWithWidgetPreference pref,
            String key, CandidateInfo info, String defaultKey, String systemDefaultKey) {
        if (!(info instanceof CandidateInfoExtra)) {
            return;
        }

        pref.setSummary(((CandidateInfoExtra) info).loadSummary());
    }

    @Override
    protected String getDefaultKey() {
        String foldSettingValue = getCurrentFoldSettingValue();
        foldSettingValue = (foldSettingValue != null) ? foldSettingValue : SETTING_VALUE_DEFAULT;
        if (!SETTING_VALUES.contains(foldSettingValue)) {
            Log.e(TAG,
                    "getDefaultKey: Invalid setting value, returning default setting value");
            foldSettingValue = SETTING_VALUE_DEFAULT;
        }

        return foldSettingValue;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (!SETTING_VALUES.contains(key)) {
            Log.e(TAG, "setDefaultKey: Can not set invalid key: " + key);
            key = SETTING_VALUE_SELECTIVE_STAY_AWAKE;
        }
        setCurrentFoldSettingValue(key);
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FOLD_LOCK_BEHAVIOR;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.fold_lock_behavior_settings;
    }

    private String getCurrentFoldSettingValue() {
        return Settings.System.getStringForUser(mContext.getContentResolver(),
                FOLD_LOCK_BEHAVIOR,
                UserHandle.USER_CURRENT);
    }

    private void setCurrentFoldSettingValue(String key) {
        Settings.System.putStringForUser(mContext.getContentResolver(),
                FOLD_LOCK_BEHAVIOR,
                key,
                UserHandle.USER_CURRENT);
    }

    @Override
    protected void onSelectionPerformed(boolean success) {
        if (success) {
            setIllustrationLottieAnimation(getDefaultKey());
            updateCandidates();
        }
    }

    private void setIllustrationLottieAnimation(String foldSettingValue) {
        switch (foldSettingValue) {
            case SETTING_VALUE_STAY_AWAKE_ON_FOLD:
                setIllustration(R.raw.fold_setting_stay_awake_on_fold_lottie,
                        IllustrationType.LOTTIE_ANIMATION);
                break;
            case SETTING_VALUE_SELECTIVE_STAY_AWAKE:
                setIllustration(R.raw.fold_setting_selective_stay_awake_lottie,
                        IllustrationType.LOTTIE_ANIMATION);
                break;
            case SETTING_VALUE_SLEEP_ON_FOLD:
                setIllustration(R.raw.fold_setting_sleep_on_fold_lottie,
                        IllustrationType.LOTTIE_ANIMATION);
                break;
        }
    }

    private String resourceToString(int resource) {
        return mContext.getText(resource).toString();
    }
}
