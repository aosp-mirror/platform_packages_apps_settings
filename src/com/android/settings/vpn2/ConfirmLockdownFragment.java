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
package com.android.settings.vpn2;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ConfirmLockdownFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {
    public interface ConfirmLockdownListener {
        public void onConfirmLockdown(Bundle options, boolean isEnabled, boolean isLockdown);
    }

    private static final String TAG = "ConfirmLockdown";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_VPN_REPLACE_EXISTING;
    }

    private static final String ARG_REPLACING = "replacing";
    private static final String ARG_ALWAYS_ON = "always_on";
    private static final String ARG_LOCKDOWN_SRC = "lockdown_old";
    private static final String ARG_LOCKDOWN_DST = "lockdown_new";
    private static final String ARG_OPTIONS = "options";

    public static boolean shouldShow(boolean replacing, boolean fromLockdown, boolean toLockdown) {
        // We only need to show this if we are:
        //  - replacing an existing connection
        //  - switching on always-on mode with lockdown enabled where it was not enabled before.
        return replacing || (toLockdown && !fromLockdown);
    }

    public static void show(Fragment parent, boolean replacing, boolean alwaysOn,
            boolean fromLockdown, boolean toLockdown, Bundle options) {
        if (parent.getFragmentManager().findFragmentByTag(TAG) != null) {
            // Already exists. Don't show it twice.
            return;
        }
        final Bundle args = new Bundle();
        args.putBoolean(ARG_REPLACING, replacing);
        args.putBoolean(ARG_ALWAYS_ON, alwaysOn);
        args.putBoolean(ARG_LOCKDOWN_SRC, fromLockdown);
        args.putBoolean(ARG_LOCKDOWN_DST, toLockdown);
        args.putParcelable(ARG_OPTIONS, options);

        final ConfirmLockdownFragment frag = new ConfirmLockdownFragment();
        frag.setArguments(args);
        frag.setTargetFragment(parent, 0);
        frag.show(parent.getFragmentManager(), TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final boolean replacing = getArguments().getBoolean(ARG_REPLACING);
        final boolean alwaysOn = getArguments().getBoolean(ARG_ALWAYS_ON);
        final boolean wasLockdown = getArguments().getBoolean(ARG_LOCKDOWN_SRC);
        final boolean nowLockdown = getArguments().getBoolean(ARG_LOCKDOWN_DST);

        final int titleId = (nowLockdown ? R.string.vpn_require_connection_title :
                (replacing ? R.string.vpn_replace_vpn_title : R.string.vpn_set_vpn_title));
        final int actionId =
                (replacing ? R.string.vpn_replace :
                (nowLockdown ? R.string.vpn_turn_on : R.string.okay));
        final int messageId;
        if (nowLockdown) {
            messageId = replacing
                    ? R.string.vpn_replace_always_on_vpn_enable_message
                    : R.string.vpn_first_always_on_vpn_message;
        } else {
            messageId = wasLockdown
                    ? R.string.vpn_replace_always_on_vpn_disable_message
                    : R.string.vpn_replace_vpn_message;
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(titleId)
                .setMessage(messageId)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(actionId, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (getTargetFragment() instanceof ConfirmLockdownListener) {
            ((ConfirmLockdownListener) getTargetFragment()).onConfirmLockdown(
                    getArguments().getParcelable(ARG_OPTIONS),
                    getArguments().getBoolean(ARG_ALWAYS_ON),
                    getArguments().getBoolean(ARG_LOCKDOWN_DST));
        }
    }
}

