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

package com.android.settings.notification.app;

import static android.app.NotificationManager.BUBBLE_PREFERENCE_NONE;
import static android.provider.Settings.Secure.NOTIFICATION_BUBBLES;

import android.app.NotificationChannel;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.BubbleHelper;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

/**
 * Preference controller for Bubbles. This is used as the app-specific page and conversation
 * settings.
 */
public class BubblePreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "BubblePrefContr";
    private static final String KEY = "bubble_pref";

    private FragmentManager mFragmentManager;
    private boolean mIsAppPage;
    private boolean mHasSentInvalidMsg;
    private int mNumConversations;
    private NotificationSettings.DependentFieldListener mListener;

    public BubblePreferenceController(Context context, @Nullable FragmentManager fragmentManager,
            NotificationBackend backend, boolean isAppPage,
            @Nullable NotificationSettings.DependentFieldListener listener) {
        super(context, backend);
        mFragmentManager = fragmentManager;
        mIsAppPage = isAppPage;
        mListener = listener;
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
        if (!mIsAppPage && !isEnabled()) {
            return false;
        }
        if (mChannel != null) {
            if (isDefaultChannel()) {
                return true;
            } else {
                return mAppRow != null &&  mAppRow.bubblePreference != BUBBLE_PREFERENCE_NONE;
            }
        }
        return true;
    }

    @Override
    boolean isIncludedInFilter() {
        return mPreferenceFilter.contains(NotificationChannel.EDIT_CONVERSATION);
    }

    @Override
    public void updateState(Preference preference) {
        if (mIsAppPage && mAppRow != null) {
            mHasSentInvalidMsg = mBackend.isInInvalidMsgState(mAppRow.pkg, mAppRow.uid);
            mNumConversations = mBackend.getConversations(
                    mAppRow.pkg, mAppRow.uid).getList().size();
            // We're on the app specific bubble page which displays a tri-state
            int backEndPref = mAppRow.bubblePreference;
            BubblePreference pref = (BubblePreference) preference;
            pref.setDisabledByAdmin(mAdmin);
            pref.setSelectedVisibility(!mHasSentInvalidMsg || mNumConversations > 0);
            if (!isEnabled()) {
                pref.setSelectedPreference(BUBBLE_PREFERENCE_NONE);
            } else {
                pref.setSelectedPreference(backEndPref);
            }
        } else if (mChannel != null) {
            // We're on the channel specific notification page which displays a toggle.
            RestrictedSwitchPreference switchpref = (RestrictedSwitchPreference) preference;
            switchpref.setDisabledByAdmin(mAdmin);
            switchpref.setChecked(mChannel.canBubble() && isEnabled());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel != null) {
            // Channel page is toggle
            mChannel.setAllowBubbles((boolean) newValue);
            saveChannel();
        } else if (mIsAppPage) {
            // App page is bubble preference
            BubblePreference pref = (BubblePreference) preference;
            if (mAppRow != null && mFragmentManager != null) {
                final int value = (int) newValue;
                if (!isEnabled()
                        && pref.getSelectedPreference() == BUBBLE_PREFERENCE_NONE) {
                    // if the global setting is off, toggling app level permission requires extra
                    // confirmation
                    new BubbleWarningDialogFragment()
                            .setPkgPrefInfo(mAppRow.pkg, mAppRow.uid, value)
                            .show(mFragmentManager, "dialog");
                    return false;
                } else {
                    mAppRow.bubblePreference = value;
                    mBackend.setAllowBubbles(mAppRow.pkg, mAppRow.uid, value);
                }
            }
            if (mListener != null) {
                mListener.onFieldValueChanged();
            }
        }
        return true;
    }

    private boolean isEnabled() {
        return BubbleHelper.isEnabledSystemWide(mContext);
    }

    /**
     * Used in app level prompt that confirms the user is ok with turning on bubbles
     * globally. If they aren't, undo that.
     */
    public static void revertBubblesApproval(Context context, String pkg, int uid) {
        NotificationBackend backend = new NotificationBackend();
        backend.setAllowBubbles(pkg, uid, BUBBLE_PREFERENCE_NONE);

        // changing the global settings will cause the observer on the host page to reload
        // correct preference state
        Settings.Secure.putInt(context.getContentResolver(),
                NOTIFICATION_BUBBLES,
                BubbleHelper.SYSTEM_WIDE_OFF);
    }

    /**
     * Apply global bubbles approval
     */
    public static void applyBubblesApproval(Context context, String pkg, int uid, int pref) {
        NotificationBackend backend = new NotificationBackend();
        backend.setAllowBubbles(pkg, uid, pref);
        // changing the global settings will cause the observer on the host page to reload
        // correct preference state
        Settings.Secure.putInt(context.getContentResolver(),
                NOTIFICATION_BUBBLES,
                BubbleHelper.SYSTEM_WIDE_ON);
    }
}
