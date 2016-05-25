/*
 * Copyright (C) 2015 The Android Open Source Project
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

import java.util.Arrays;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;

/**
 * Fragment wrapper around a {@link ConfigDialog}.
 */
public class ConfigDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {
    private static final String TAG_CONFIG_DIALOG = "vpnconfigdialog";
    private static final String TAG = "ConfigDialogFragment";

    private static final String ARG_PROFILE = "profile";
    private static final String ARG_EDITING = "editing";
    private static final String ARG_EXISTS = "exists";

    private final IConnectivityManager mService = IConnectivityManager.Stub.asInterface(
            ServiceManager.getService(Context.CONNECTIVITY_SERVICE));

    private boolean mUnlocking = false;

    public static void show(VpnSettings parent, VpnProfile profile, boolean edit, boolean exists) {
        if (!parent.isAdded()) return;

        Bundle args = new Bundle();
        args.putParcelable(ARG_PROFILE, profile);
        args.putBoolean(ARG_EDITING, edit);
        args.putBoolean(ARG_EXISTS, exists);

        final ConfigDialogFragment frag = new ConfigDialogFragment();
        frag.setArguments(args);
        frag.setTargetFragment(parent, 0);
        frag.show(parent.getFragmentManager(), TAG_CONFIG_DIALOG);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check KeyStore here, so others do not need to deal with it.
        if (!KeyStore.getInstance().isUnlocked()) {
            if (!mUnlocking) {
                // Let us unlock KeyStore. See you later!
                Credentials.getInstance().unlock(getActivity());
            } else {
                // We already tried, but it is still not working!
                dismiss();
            }
            mUnlocking = !mUnlocking;
            return;
        }

        // Now KeyStore is always unlocked. Reset the flag.
        mUnlocking = false;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        VpnProfile profile = (VpnProfile) args.getParcelable(ARG_PROFILE);
        boolean editing = args.getBoolean(ARG_EDITING);
        boolean exists = args.getBoolean(ARG_EXISTS);

        return new ConfigDialog(getActivity(), this, profile, editing, exists);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        ConfigDialog dialog = (ConfigDialog) getDialog();
        VpnProfile profile = dialog.getProfile();

        if (button == DialogInterface.BUTTON_POSITIVE) {
            // Update KeyStore entry
            KeyStore.getInstance().put(Credentials.VPN + profile.key, profile.encode(),
                    KeyStore.UID_SELF, /* flags */ 0);

            // Flush out old version of profile
            disconnect(profile);

            updateLockdownVpn(dialog.isVpnAlwaysOn(), profile);

            // If we are not editing, connect!
            if (!dialog.isEditing() && !VpnUtils.isVpnLockdown(profile.key)) {
                try {
                    connect(profile);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to connect", e);
                }
            }
        } else if (button == DialogInterface.BUTTON_NEUTRAL) {
            // Disable profile if connected
            disconnect(profile);

            // Delete from KeyStore
            KeyStore keyStore = KeyStore.getInstance();
            keyStore.delete(Credentials.VPN + profile.key, KeyStore.UID_SELF);

            updateLockdownVpn(false, profile);
        }
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dismiss();
        super.onCancel(dialog);
    }

    private void updateLockdownVpn(boolean isVpnAlwaysOn, VpnProfile profile) {
        // Save lockdown vpn
        if (isVpnAlwaysOn) {
            // Show toast if vpn profile is not valid
            if (!profile.isValidLockdownProfile()) {
                Toast.makeText(getContext(), R.string.vpn_lockdown_config_error,
                        Toast.LENGTH_LONG).show();
                return;
            }

            final ConnectivityManager conn = ConnectivityManager.from(getActivity());
            conn.setAlwaysOnVpnPackageForUser(UserHandle.myUserId(), null,
                    /* lockdownEnabled */ false);
            VpnUtils.setLockdownVpn(getContext(), profile.key);
        } else {
            // update only if lockdown vpn has been changed
            if (VpnUtils.isVpnLockdown(profile.key)) {
                VpnUtils.clearLockdownVpn(getContext());
            }
        }
    }

    private void connect(VpnProfile profile) throws RemoteException {
        try {
            mService.startLegacyVpn(profile);
        } catch (IllegalStateException e) {
            Toast.makeText(getActivity(), R.string.vpn_no_network, Toast.LENGTH_LONG).show();
        }
    }

    private void disconnect(VpnProfile profile) {
        try {
            LegacyVpnInfo connected = mService.getLegacyVpnInfo(UserHandle.myUserId());
            if (connected != null && profile.key.equals(connected.key)) {
                VpnUtils.clearLockdownVpn(getContext());
                mService.prepareVpn(VpnConfig.LEGACY_VPN, VpnConfig.LEGACY_VPN,
                        UserHandle.myUserId());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to disconnect", e);
        }
    }
}
