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

package com.android.settings.notification;

import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.accounts.AccountRestrictionHelper;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.RestrictedPreference;

/**
 * Base class for preference controller that handles preference that enforce adjust volume
 * restriction
 */
public abstract class AdjustVolumeRestrictedPreferenceController extends
        SliderPreferenceController {

    private AccountRestrictionHelper mHelper;

    public AdjustVolumeRestrictedPreferenceController(Context context, String key) {
        this(context, new AccountRestrictionHelper(context), key);
    }

    @VisibleForTesting
    AdjustVolumeRestrictedPreferenceController(Context context, AccountRestrictionHelper helper,
            String key) {
        super(context, key);
        mHelper = helper;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        mHelper.enforceRestrictionOnPreference((RestrictedPreference) preference,
                UserManager.DISALLOW_ADJUST_VOLUME, UserHandle.myUserId());
    }

    @Override
    public IntentFilter getIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
        filter.addAction(AudioManager.STREAM_MUTE_CHANGED_ACTION);
        filter.addAction(AudioManager.MASTER_MUTE_CHANGED_ACTION);
        filter.addAction(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
        return filter;
    }
}
