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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.SettingsMainSwitchPreference;

public class BlockPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, OnCheckedChangeListener {

    private static final String KEY_BLOCK = "block";
    private NotificationSettings.DependentFieldListener mDependentFieldListener;

    public BlockPreferenceController(Context context,
            NotificationSettings.DependentFieldListener dependentFieldListener,
            NotificationBackend backend) {
        super(context, backend);
        mDependentFieldListener = dependentFieldListener;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BLOCK;
    }

    @Override
    public boolean isAvailable() {
        if (mAppRow == null) {
            return false;
        }
        if (mPreferenceFilter != null && !isIncludedInFilter()) {
            return false;
        }
        return true;
    }

    @Override
    boolean isIncludedInFilter() {
        return mPreferenceFilter.contains(NotificationChannel.EDIT_IMPORTANCE);
    }

    public void updateState(Preference preference) {
        SettingsMainSwitchPreference bar = (SettingsMainSwitchPreference) preference;
        if (bar != null) {
            String switchBarText = getSwitchBarText();
            bar.setTitle(switchBarText);
            bar.show();
            try {
                bar.addOnSwitchChangeListener(this);
            } catch (IllegalStateException e) {
                // an exception is thrown if you try to add the listener twice
            }
            bar.setDisabledByAdmin(mAdmin);

            if (mChannel != null && (!isChannelBlockable() || !isChannelConfigurable(mChannel))) {
                bar.setSwitchBarEnabled(false);
            }

            if (mChannelGroup != null && !isChannelGroupBlockable()) {
                bar.setSwitchBarEnabled(false);
            }

            if (mChannel == null && !isAppBlockable()) {
                bar.setSwitchBarEnabled(false);
            }

            if (mChannel != null) {
                bar.setChecked(!mAppRow.banned
                        && mChannel.getImportance() != NotificationManager.IMPORTANCE_NONE);
            } else if (mChannelGroup != null) {
                bar.setChecked(!mAppRow.banned && !mChannelGroup.isBlocked());
            } else {
                bar.setChecked(!mAppRow.banned);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        boolean blocked = !isChecked;
        if (mChannel != null) {
            final int originalImportance = mChannel.getImportance();
            // setting the initial state of the switch in updateState() triggers this callback.
            // It's always safe to override the importance if it's meant to be blocked or if
            // it was blocked and we are unblocking it.
            if (blocked || originalImportance == IMPORTANCE_NONE) {
                final int importance = blocked
                        ? IMPORTANCE_NONE
                        : isDefaultChannel()
                                ? IMPORTANCE_UNSPECIFIED
                                : Math.max(mChannel.getOriginalImportance(), IMPORTANCE_LOW);
                mChannel.setImportance(importance);
                saveChannel();
            }
            if (mBackend.onlyHasDefaultChannel(mAppRow.pkg, mAppRow.uid)) {
                if (mAppRow.banned != blocked) {
                    mAppRow.banned = blocked;
                    mBackend.setNotificationsEnabledForPackage(mAppRow.pkg, mAppRow.uid, !blocked);
                }
            }
        } else if (mChannelGroup != null) {
            mChannelGroup.setBlocked(blocked);
            mBackend.updateChannelGroup(mAppRow.pkg, mAppRow.uid, mChannelGroup);
        } else if (mAppRow != null) {
            mAppRow.banned = blocked;
            mBackend.setNotificationsEnabledForPackage(mAppRow.pkg, mAppRow.uid, !blocked);
        }
        mDependentFieldListener.onFieldValueChanged();
    }

    String getSwitchBarText() {
        if (mChannel != null) {
            return mContext.getString(R.string.notification_content_block_title);
        } else {
            CharSequence fieldContextName;
            if (mChannelGroup != null) {
                fieldContextName = mChannelGroup.getName();
            } else {
                fieldContextName = mAppRow.label;
            }
            return mContext.getString(R.string.notification_app_switch_label, fieldContextName);
        }
    }
}
