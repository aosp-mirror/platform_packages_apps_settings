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

package com.android.settings.notification;

import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;

import android.annotation.Nullable;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedSwitchPreference;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

public class BubblePreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "BubblePrefContr";
    private static final String KEY = "bubble_pref";
    @VisibleForTesting
    static final int SYSTEM_WIDE_ON = 1;
    @VisibleForTesting
    static final int SYSTEM_WIDE_OFF = 0;

    private FragmentManager mFragmentManager;
    private boolean mIsAppPage;

    public BubblePreferenceController(Context context, @Nullable FragmentManager fragmentManager,
            NotificationBackend backend, boolean isAppPage) {
        super(context, backend);
        mFragmentManager = fragmentManager;
        mIsAppPage = isAppPage;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (!mIsAppPage && !isGloballyEnabled()) {
            return false;
        }
        if (mChannel != null) {
            if (isDefaultChannel()) {
                return true;
            } else {
                return mAppRow != null && mAppRow.allowBubbles;
            }
        }
        return true;
    }

    public void updateState(Preference preference) {
        if (mAppRow != null) {
            RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
            pref.setDisabledByAdmin(mAdmin);
            if (mChannel != null) {
                pref.setChecked(mChannel.canBubble() && isGloballyEnabled());
                pref.setEnabled(!pref.isDisabledByAdmin());
            } else {
                pref.setChecked(mAppRow.allowBubbles && isGloballyEnabled());
                pref.setSummary(mContext.getString(
                        R.string.bubbles_app_toggle_summary, mAppRow.label));
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean value = (Boolean) newValue && isGloballyEnabled();
        if (mChannel != null) {
            mChannel.setAllowBubbles(value);
            saveChannel();
            return true;
        } else if (mAppRow != null && mFragmentManager != null) {
            RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
            // if the global setting is off, toggling app level permission requires extra
            // confirmation
            if (!isGloballyEnabled() && !pref.isChecked()) {
                new BubbleWarningDialogFragment()
                        .setPkgInfo(mAppRow.pkg, mAppRow.uid)
                        .show(mFragmentManager, "dialog");
                return false;
            } else {
                mAppRow.allowBubbles = value;
                mBackend.setAllowBubbles(mAppRow.pkg, mAppRow.uid, value);
            }
        }
        return true;
    }

    private boolean isGloballyEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF) == SYSTEM_WIDE_ON;
    }

    // Used in app level prompt that confirms the user is ok with turning on bubbles
    // globally. If they aren't, undo what
    public static void revertBubblesApproval(Context mContext, String pkg, int uid) {
        NotificationBackend backend = new NotificationBackend();
        backend.setAllowBubbles(pkg, uid, false);
        // changing the global settings will cause the observer on the host page to reload
        // correct preference state
        Settings.Global.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);
    }

    // Apply global bubbles approval
    public static void applyBubblesApproval(Context mContext, String pkg, int uid) {
        NotificationBackend backend = new NotificationBackend();
        backend.setAllowBubbles(pkg, uid, true);
        // changing the global settings will cause the observer on the host page to reload
        // correct preference state
        Settings.Global.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
    }
}
