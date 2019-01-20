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
package com.android.settings.users;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.core.AbstractPreferenceController;

public class AutoSyncDataPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "AutoSyncDataController";
    private static final String TAG_CONFIRM_AUTO_SYNC_CHANGE = "confirmAutoSyncChange";
    private static final String KEY_AUTO_SYNC_ACCOUNT = "auto_sync_account_data";

    protected final UserManager mUserManager;
    private final Fragment mParentFragment;

    protected UserHandle mUserHandle;

    public AutoSyncDataPreferenceController(Context context, Fragment parent) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mParentFragment = parent;
        mUserHandle = Process.myUserHandle();
    }

    @Override
    public void updateState(Preference preference) {
        SwitchPreference switchPreference = (SwitchPreference) preference;
        switchPreference.setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                mUserHandle.getIdentifier()));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            SwitchPreference switchPreference = (SwitchPreference) preference;
            boolean checked = switchPreference.isChecked();
            switchPreference.setChecked(!checked);
            if (ActivityManager.isUserAMonkey()) {
                Log.d(TAG, "ignoring monkey's attempt to flip sync state");
            } else {
                ConfirmAutoSyncChangeFragment.show(mParentFragment, checked, mUserHandle,
                        switchPreference);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        return !mUserManager.isManagedProfile()
                && (mUserManager.isRestrictedProfile()
                || mUserManager.getProfiles(UserHandle.myUserId()).size() == 1);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_SYNC_ACCOUNT;
    }

    /**
     * Dialog to inform user about changing auto-sync setting
     */
    public static class ConfirmAutoSyncChangeFragment extends InstrumentedDialogFragment implements
            DialogInterface.OnClickListener {
        private static final String SAVE_ENABLING = "enabling";
        private static final String SAVE_USER_HANDLE = "userHandle";
        boolean mEnabling;
        UserHandle mUserHandle;
        SwitchPreference mPreference;

        public static void show(Fragment parent, boolean enabling, UserHandle userHandle,
                SwitchPreference preference) {
            if (!parent.isAdded()) return;

            final ConfirmAutoSyncChangeFragment dialog = new ConfirmAutoSyncChangeFragment();
            dialog.mEnabling = enabling;
            dialog.mUserHandle = userHandle;
            dialog.setTargetFragment(parent, 0);
            dialog.mPreference = preference;
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_AUTO_SYNC_CHANGE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            if (savedInstanceState != null) {
                mEnabling = savedInstanceState.getBoolean(SAVE_ENABLING);
                mUserHandle = (UserHandle) savedInstanceState.getParcelable(SAVE_USER_HANDLE);
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (!mEnabling) {
                builder.setTitle(R.string.data_usage_auto_sync_off_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_off_dialog);
            } else {
                builder.setTitle(R.string.data_usage_auto_sync_on_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_on_dialog);
            }

            builder.setPositiveButton(android.R.string.ok, this);
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_ENABLING, mEnabling);
            outState.putParcelable(SAVE_USER_HANDLE, mUserHandle);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_CONFIRM_AUTO_SYNC_CHANGE;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                ContentResolver.setMasterSyncAutomaticallyAsUser(mEnabling,
                        mUserHandle.getIdentifier());
                if (mPreference != null) {
                    mPreference.setChecked(mEnabling);
                }
            }
        }
    }

}
