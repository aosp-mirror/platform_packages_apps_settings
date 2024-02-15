/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;


import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.app.admin.FactoryResetProtectionPolicy;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.oemlock.OemLockManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.template.FooterButton.ButtonType;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;

/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
public class MainClearConfirm extends InstrumentedFragment {
    private static final String TAG = "MainClearConfirm";

    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";

    @VisibleForTesting View mContentView;
    private boolean mEraseSdCard;
    @VisibleForTesting boolean mEraseEsims;

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and invoke the Checkin Service to reset the device to its factory-default
     * state (rebooting in the process).
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }

            final PersistentDataBlockManager pdbManager;
            // pre-flight check hardware support PersistentDataBlockManager
            if (!SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals("")) {
                pdbManager = (PersistentDataBlockManager)
                    getActivity().getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
            } else {
                pdbManager = null;
            }

            if (shouldWipePersistentDataBlock(pdbManager)) {

                new AsyncTask<Void, Void, Void>() {
                    int mOldOrientation;
                    ProgressDialog mProgressDialog;

                    @Override
                    protected Void doInBackground(Void... params) {
                        pdbManager.wipe();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        mProgressDialog.hide();
                        if (getActivity() != null) {
                            getActivity().setRequestedOrientation(mOldOrientation);
                            doMainClear();
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        mProgressDialog = getProgressDialog();
                        mProgressDialog.show();

                        // need to prevent orientation changes as we're about to go into
                        // a long IO request, so we won't be able to access inflate resources on
                        // flash
                        mOldOrientation = getActivity().getRequestedOrientation();
                        getActivity().setRequestedOrientation(
                                ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    }
                }.execute();
            } else {
                doMainClear();
            }
        }

        private ProgressDialog getProgressDialog() {
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(
                    getActivity().getString(R.string.main_clear_progress_title));
            progressDialog.setMessage(
                    getActivity().getString(R.string.main_clear_progress_text));
            return progressDialog;
        }
    };

    @VisibleForTesting
    boolean shouldWipePersistentDataBlock(PersistentDataBlockManager pdbManager) {
        if (pdbManager == null) {
            return false;
        }
        // The persistent data block will persist if the device is still being provisioned.
        if (isDeviceStillBeingProvisioned()) {
            return false;
        }
        // If OEM unlock is allowed, the persistent data block will be wiped during FR
        // process. If disabled, it will be wiped here instead.
        if (isOemUnlockedAllowed()) {
            return false;
        }
        final DevicePolicyManager dpm = (DevicePolicyManager) getActivity()
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        // Do not erase the factory reset protection data (from Settings) if factory reset
        // protection policy is not supported on the device.
        if (!dpm.isFactoryResetProtectionPolicySupported()) {
            return false;
        }
        // Do not erase the factory reset protection data (from Settings) if the
        // device is an organization-owned managed profile device and a factory
        // reset protection policy has been set.
        FactoryResetProtectionPolicy frpPolicy = dpm.getFactoryResetProtectionPolicy(null);
        if (dpm.isOrganizationOwnedDeviceWithManagedProfile() && frpPolicy != null
                && frpPolicy.isNotEmpty()) {
            return false;
        }
        return true;
    }

    @VisibleForTesting
    boolean isOemUnlockedAllowed() {
        return ((OemLockManager) getActivity().getSystemService(
                Context.OEM_LOCK_SERVICE)).isOemUnlockAllowed();
    }

    @VisibleForTesting
    boolean isDeviceStillBeingProvisioned() {
        return !WizardManagerHelper.isDeviceProvisioned(getActivity());
    }

    private void doMainClear() {
        Intent intent = new Intent(Intent.ACTION_FACTORY_RESET);
        intent.setPackage("android");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(Intent.EXTRA_REASON, "MainClearConfirm");
        intent.putExtra(Intent.EXTRA_WIPE_EXTERNAL_STORAGE, mEraseSdCard);
        intent.putExtra(Intent.EXTRA_WIPE_ESIMS, mEraseEsims);
        getActivity().sendBroadcast(intent);
        // Intent handling is asynchronous -- assume it will happen soon.
    }

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        final GlifLayout layout = mContentView.findViewById(R.id.setup_wizard_layout);

        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getActivity())
                        .setText(R.string.main_clear_button_text)
                        .setListener(mFinalClickListener)
                        .setButtonType(ButtonType.OTHER)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build()
        );
    }

    private void setUpActionBarAndTitle() {
        final Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "No activity attached, skipping setUpActionBarAndTitle");
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (actionBar == null) {
            Log.e(TAG, "No actionbar, skipping setUpActionBarAndTitle");
            return;
        }
        actionBar.hide();
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_FACTORY_RESET, UserHandle.myUserId());
        if (RestrictedLockUtilsInternal.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_FACTORY_RESET, UserHandle.myUserId())) {
            return inflater.inflate(R.layout.main_clear_disallowed_screen, null);
        } else if (admin != null) {
            new ActionDisabledByAdminDialogHelper(getActivity())
                    .prepareDialogBuilder(UserManager.DISALLOW_FACTORY_RESET, admin)
                    .setOnDismissListener(__ -> getActivity().finish())
                    .show();
            return new View(getActivity());
        }
        mContentView = inflater.inflate(R.layout.main_clear_confirm, null);
        setUpActionBarAndTitle();
        establishFinalConfirmationState();
        setAccessibilityTitle();
        setSubtitle();
        return mContentView;
    }

    private void setAccessibilityTitle() {
        CharSequence currentTitle = getActivity().getTitle();
        TextView confirmationMessage = mContentView.findViewById(R.id.sud_layout_description);
        if (confirmationMessage != null) {
            String accessibleText = new StringBuilder(currentTitle).append(",").append(
                    confirmationMessage.getText()).toString();
            getActivity().setTitle(Utils.createAccessibleSequence(currentTitle, accessibleText));
        }
    }

    @VisibleForTesting
    void setSubtitle() {
        if (mEraseEsims) {
            TextView confirmationMessage = mContentView.findViewById(R.id.sud_layout_description);
            if (confirmationMessage != null) {
                confirmationMessage.setText(R.string.main_clear_final_desc_esim);
                }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mEraseSdCard = args != null
                && args.getBoolean(MainClear.ERASE_EXTERNAL_EXTRA);
        mEraseEsims = args != null
                && args.getBoolean(MainClear.ERASE_ESIMS_EXTRA);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MASTER_CLEAR_CONFIRM;
    }
}
