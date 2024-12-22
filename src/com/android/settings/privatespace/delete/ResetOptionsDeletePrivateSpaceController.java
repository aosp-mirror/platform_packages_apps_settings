/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.privatespace.delete;

import static com.android.internal.app.SetScreenLockDialogActivity.LAUNCH_REASON_RESET_PRIVATE_SPACE_SETTINGS_ACCESS;
import static com.android.settings.system.ResetDashboardFragment.PRIVATE_SPACE_DELETE_CREDENTIAL_REQUEST;

import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.android.internal.annotations.Initializer;
import com.android.internal.app.SetScreenLockDialogActivity;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.privatespace.PrivateSpaceMaintainer;
import com.android.settings.system.ResetDashboardFragment;

/** Controller to delete private space from Settings Reset options after authentication. */
public class ResetOptionsDeletePrivateSpaceController extends BasePreferenceController {
    private static final String TAG = "PrivateSpaceResetCtrl";
    private ResetDashboardFragment mHostFragment;
    private KeyguardManager mKeyguardManager;

    public ResetOptionsDeletePrivateSpaceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Initializer
    public void setFragment(@NonNull ResetDashboardFragment hostFragment) {
        mHostFragment = hostFragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return android.multiuser.Flags.enablePrivateSpaceFeatures()
                        && android.multiuser.Flags.deletePrivateSpaceFromReset()
                        && isPrivateSpaceEntryPointEnabled()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        startAuthenticationForDelete();
        return true;
    }

    @VisibleForTesting
    boolean startAuthenticationForDelete() {
        if (getKeyguardManager().isDeviceSecure()) {
            final ChooseLockSettingsHelper.Builder builder = new ChooseLockSettingsHelper.Builder(
                    mHostFragment.getActivity(), mHostFragment);
            builder.setRequestCode(PRIVATE_SPACE_DELETE_CREDENTIAL_REQUEST).show();
        } else {
            promptToSetDeviceLock();
        }
        return true;
    }

    /** Method to handle onActivityResult */
    public boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PRIVATE_SPACE_DELETE_CREDENTIAL_REQUEST
                && resultCode == Activity.RESULT_OK
                && data != null) {
            DeletePrivateSpaceDialogFragment dialogFragment = getDeleteDialogFragment();
            dialogFragment.show(
                    getFragmentManager(), DeletePrivateSpaceDialogFragment.class.getName());
            return true;
        }
        return false;
    }

    @VisibleForTesting
    DeletePrivateSpaceDialogFragment getDeleteDialogFragment() {
        return new DeletePrivateSpaceDialogFragment();
    }

    @VisibleForTesting
    FragmentManager getFragmentManager() {
        return mHostFragment.getFragmentManager();
    }

    @VisibleForTesting
    boolean isPrivateSpaceEntryPointEnabled() {
        return PrivateSpaceMaintainer.getInstance(mContext).isPrivateSpaceEntryPointEnabled();
    }

    /* Dialog shown when deleting private space from Reset Options. */
    public static class DeletePrivateSpaceDialogFragment extends InstrumentedDialogFragment {
        private static final String TAG = "PrivateSpaceResetFrag";

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.RESET_DELETE_PRIVATE_SPACE_DIALOG;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            Context context = getContext();
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.private_space_delete_header)
                    .setMessage(R.string.reset_private_space_delete_dialog)
                    .setPositiveButton(
                            R.string.private_space_delete_button_label,
                            (DialogInterface dialog, int which) -> {
                                mMetricsFeatureProvider.action(
                                        context, SettingsEnums.RESET_DELETE_PRIVATE_SPACE_CONFIRM);
                                PrivateSpaceMaintainer privateSpaceMaintainer =
                                        PrivateSpaceMaintainer.getInstance(context);
                                privateSpaceMaintainer.deletePrivateSpace();
                                dialog.dismiss();
                            })
                    .setNegativeButton(
                            R.string.private_space_cancel_label,
                            (DialogInterface dialog, int which) -> {
                                mMetricsFeatureProvider.action(
                                        context, SettingsEnums.RESET_DELETE_PRIVATE_SPACE_CANCEL);
                                dialog.cancel();
                            })
                    .create();
        }
    }

    private KeyguardManager getKeyguardManager() {
        if (mKeyguardManager == null) {
            mKeyguardManager = mContext.getSystemService(KeyguardManager.class);
        }
        return mKeyguardManager;
    }

    private void promptToSetDeviceLock() {
        Intent setScreenLockPromptIntent = SetScreenLockDialogActivity.createBaseIntent(
                LAUNCH_REASON_RESET_PRIVATE_SPACE_SETTINGS_ACCESS);
        mContext.startActivity(setScreenLockPromptIntent);
    }
}
