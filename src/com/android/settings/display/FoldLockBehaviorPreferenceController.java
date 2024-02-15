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

import static com.android.settings.display.FoldLockBehaviorSettings.SETTING_VALUES;
import static com.android.settings.display.FoldLockBehaviorSettings.SETTING_VALUE_SELECTIVE_STAY_AWAKE;
import static com.android.settings.display.FoldLockBehaviorSettings.SETTING_VALUE_SLEEP_ON_FOLD;
import static com.android.settings.display.FoldLockBehaviorSettings.SETTING_VALUE_STAY_AWAKE_ON_FOLD;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.foldables.FoldGracePeriodProvider;
import com.android.internal.foldables.FoldLockSettingAvailabilityProvider;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.HashMap;
import java.util.Map;

/**
 * A preference controller for the @link android.provider.Settings.System#FOLD_LOCK_BEHAVIOR
 * setting.
 *
 * This preference controller allows users to control whether or not the device
 * stays awake when it is folded.
 */
public class FoldLockBehaviorPreferenceController extends BasePreferenceController {

    private static final Map<String, String> KEY_TO_TEXT = new HashMap<>();
    private final FoldLockSettingAvailabilityProvider mFoldLockSettingAvailabilityProvider;

    public FoldLockBehaviorPreferenceController(Context context, String key) {
        this(context, key, new FoldLockSettingAvailabilityProvider(context.getResources()));
    }

    public FoldLockBehaviorPreferenceController(Context context, String key,
            FoldLockSettingAvailabilityProvider foldLockSettingAvailabilityProvider) {
        super(context, key);
        mFoldLockSettingAvailabilityProvider = foldLockSettingAvailabilityProvider;
        KEY_TO_TEXT.put(SETTING_VALUE_STAY_AWAKE_ON_FOLD,
                resourceToString(R.string.stay_awake_on_fold_title));
        if (new FoldGracePeriodProvider().isEnabled()) {
            KEY_TO_TEXT.put(SETTING_VALUE_SELECTIVE_STAY_AWAKE,
                    resourceToString(R.string.stay_awake_on_lockscreen_title));
        } else {
            KEY_TO_TEXT.put(SETTING_VALUE_SELECTIVE_STAY_AWAKE,
                    resourceToString(R.string.selective_stay_awake_title));
        }
        KEY_TO_TEXT.put(SETTING_VALUE_SLEEP_ON_FOLD,
                resourceToString(R.string.sleep_on_fold_title));
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isFoldLockBehaviorAvailable =
                mFoldLockSettingAvailabilityProvider.isFoldLockBehaviorAvailable();
        return isFoldLockBehaviorAvailable
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        String summary = KEY_TO_TEXT.get(getFoldSettingValue());
        preference.setSummary(summary);
    }

    private String getFoldSettingValue() {
        String foldSettingValue = Settings.System.getStringForUser(mContext.getContentResolver(),
                FOLD_LOCK_BEHAVIOR, UserHandle.USER_CURRENT);
        return (foldSettingValue != null && SETTING_VALUES.contains(foldSettingValue))
                ? foldSettingValue : SETTING_VALUE_SELECTIVE_STAY_AWAKE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    private String resourceToString(int resource) {
        return mContext.getText(resource).toString();
    }

}
