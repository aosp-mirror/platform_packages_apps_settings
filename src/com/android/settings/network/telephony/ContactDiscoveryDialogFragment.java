/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsRcsManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Fragment for the "Contact Discovery" dialog that appears when the user enables
 * "Contact Discovery" in MobileNetworkSettings or an application starts MobileNetworkSettings with
 * {@link ImsRcsManager#ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN}.
 */
public class ContactDiscoveryDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    private static final String SUB_ID_KEY = "sub_id_key";
    private static final String CARRIER_NAME_KEY = "carrier_name_key";
    private static final String DIALOG_TAG = "discovery_dialog:";

    private int mSubId;
    private CharSequence mCarrierName;
    private ImsManager mImsManager;

    /**
     * Create a new Fragment, which will create a new Dialog when
     * {@link #show(FragmentManager, String)} is called.
     * @param subId The subscription ID to associate with this Dialog.
     * @return a new instance of ContactDiscoveryDialogFragment.
     */
    public static ContactDiscoveryDialogFragment newInstance(int subId, CharSequence carrierName) {
        final ContactDiscoveryDialogFragment dialogFragment = new ContactDiscoveryDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(SUB_ID_KEY, subId);
        args.putCharSequence(CARRIER_NAME_KEY, carrierName);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Bundle args = getArguments();
        mSubId = args.getInt(SUB_ID_KEY);
        mCarrierName = args.getCharSequence(CARRIER_NAME_KEY);
        mImsManager = getImsManager(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        CharSequence title;
        CharSequence message;
        if (!TextUtils.isEmpty(mCarrierName)) {
            title = getContext().getString(
                    R.string.contact_discovery_opt_in_dialog_title, mCarrierName);
            message = getContext().getString(
                    R.string.contact_discovery_opt_in_dialog_message, mCarrierName);
        } else {
            title = getContext().getString(
                    R.string.contact_discovery_opt_in_dialog_title_no_carrier_defined);
            message = getContext().getString(
                    R.string.contact_discovery_opt_in_dialog_message_no_carrier_defined);
        }
        builder.setMessage(message)
                .setTitle(title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(R.string.confirmation_turn_on, this)
                .setNegativeButton(android.R.string.cancel, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // let the host know that the positive button has been clicked
        if (which == dialog.BUTTON_POSITIVE) {
            MobileNetworkUtils.setContactDiscoveryEnabled(mImsManager, mSubId, true /*isEnabled*/);
        }
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    @VisibleForTesting
    public ImsManager getImsManager(Context context) {
        return context.getSystemService(ImsManager.class);
    }

    public static String getFragmentTag(int subId) {
        return DIALOG_TAG + subId;
    }
}
