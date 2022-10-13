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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.NotificationAssistantService;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;

public class NotificationAssistantPreferenceController extends TogglePreferenceController {
    private static final String TAG = "NASPreferenceController";
    static final String KEY_NAS = "notification_assistant";

    private static final int AVAILABLE = 1;
    private final UserManager mUserManager;
    private final PackageManager mPackageManager;
    private Fragment mFragment;
    private int mUserId = UserHandle.myUserId();

    @VisibleForTesting
    protected NotificationBackend mNotificationBackend;
    private ComponentName mDefaultNASComponent;
    private Intent mNASSettingIntent;

    public NotificationAssistantPreferenceController(Context context) {
        super(context, KEY_NAS);
        mUserManager = UserManager.get(context);
        mNotificationBackend = new NotificationBackend();
        mPackageManager = context.getPackageManager();
        getDefaultNASIntent();
    }


    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        ComponentName acn = mNotificationBackend.getAllowedNotificationAssistant();
        return (acn != null && acn.equals(mDefaultNASComponent));
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        ComponentName cn = isChecked
                ? mDefaultNASComponent : null;
        if (isChecked) {
            if (mFragment == null) {
                throw new IllegalStateException("No fragment to start activity");
            }
            showDialog(cn);
            return false;
        } else {
            setNotificationAssistantGranted(null);
            return true;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_notifications;
    }

    protected void setNotificationAssistantGranted(ComponentName cn) {
        if (Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAS_SETTINGS_UPDATED, 0, mUserId) == 0) {
            mNotificationBackend.setNASMigrationDoneAndResetDefault(mUserId, cn != null);
        }
        mNotificationBackend.setNotificationAssistantGranted(cn);
    }

    protected void showDialog(ComponentName cn) {
        NotificationAssistantDialogFragment dialogFragment =
                NotificationAssistantDialogFragment.newInstance(mFragment, cn);
        dialogFragment.show(mFragment.getFragmentManager(), TAG);
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @VisibleForTesting
    void setBackend(NotificationBackend backend) {
        mNotificationBackend = backend;
    }

    @VisibleForTesting
    void getDefaultNASIntent() {
        mDefaultNASComponent = mNotificationBackend.getDefaultNotificationAssistant();
        if (mDefaultNASComponent != null) {
            mNASSettingIntent = new Intent(
                    NotificationAssistantService.ACTION_NOTIFICATION_ASSISTANT_DETAIL_SETTINGS);
            mNASSettingIntent.setPackage(mDefaultNASComponent.getPackageName());
            mNASSettingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }

    @Override
    public boolean isSliceable() {
        return (mFragment != null && mFragment instanceof ConfigureNotificationSettings);
    }

    private boolean isNASSettingActivityAvailable() {
        final List<ResolveInfo> resolved = mPackageManager.queryIntentActivities(mNASSettingIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL));
        return (resolved != null && !resolved.isEmpty());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mDefaultNASComponent == null) {
            preference.setEnabled(false);
            ((PrimarySwitchPreference) preference).setSwitchEnabled(false);
        } else if (isNASSettingActivityAvailable()) {
            preference.setIntent(mNASSettingIntent);
        } else {
            // Cannot find settings activity from the default NAS app
            preference.setIntent(null);
            preference.setOnPreferenceClickListener(
                    preference1 -> {
                        onPreferenceChange(preference1, !isChecked());
                        ((PrimarySwitchPreference) preference1).setChecked(isChecked());
                        return true;
                    }
            );
        }
    }
}
