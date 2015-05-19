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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.net.VpnConfig;
import com.android.settings.R;

/**
 * Fragment wrapper around an {@link AppDialog}.
 */
public class AppDialogFragment extends DialogFragment implements AppDialog.Listener {
    private static final String TAG_APP_DIALOG = "vpnappdialog";
    private static final String TAG = "AppDialogFragment";

    private static final String ARG_MANAGING = "managing";
    private static final String ARG_LABEL = "label";
    private static final String ARG_CONNECTED = "connected";
    private static final String ARG_PACKAGE = "package";

    private PackageInfo mPackageInfo;

    private final IConnectivityManager mService = IConnectivityManager.Stub.asInterface(
            ServiceManager.getService(Context.CONNECTIVITY_SERVICE));

    public static void show(VpnSettings parent, PackageInfo packageInfo, String label,
            boolean managing, boolean connected) {
        if (!parent.isAdded()) return;

        Bundle args = new Bundle();
        args.putParcelable(ARG_PACKAGE, packageInfo);
        args.putString(ARG_LABEL, label);
        args.putBoolean(ARG_MANAGING, managing);
        args.putBoolean(ARG_CONNECTED, connected);

        final AppDialogFragment frag = new AppDialogFragment();
        frag.setArguments(args);
        frag.setTargetFragment(parent, 0);
        frag.show(parent.getFragmentManager(), TAG_APP_DIALOG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        final String label = args.getString(ARG_LABEL);
        boolean managing = args.getBoolean(ARG_MANAGING);
        boolean connected = args.getBoolean(ARG_CONNECTED);
        mPackageInfo = (PackageInfo) args.getParcelable(ARG_PACKAGE);

        if (managing) {
            return new AppDialog(getActivity(), this, mPackageInfo, label);
        } else {
            // Build an AlertDialog with an option to disconnect.
            AlertDialog.Builder dlog = new AlertDialog.Builder(getActivity())
                    .setTitle(label)
                    .setMessage(getActivity().getString(R.string.vpn_disconnect_confirm))
                    .setNegativeButton(getActivity().getString(R.string.vpn_cancel), null);

            if (connected) {
                dlog.setPositiveButton(getActivity().getString(R.string.vpn_disconnect),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onDisconnect(dialog);
                            }
                        });
            }
            return dlog.create();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dismiss();
        super.onCancel(dialog);
    }

    @Override
    public void onForget(final DialogInterface dialog) {
        final int userId = UserHandle.getUserId(mPackageInfo.applicationInfo.uid);
        try {
            mService.setVpnPackageAuthorization(mPackageInfo.packageName, userId, false);
            onDisconnect(dialog);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to forget authorization of " + mPackageInfo.packageName +
                    " for user " + userId, e);
        }
    }

    private void onDisconnect(final DialogInterface dialog) {
        final int userId = UserHandle.getUserId(mPackageInfo.applicationInfo.uid);
        try {
            final VpnConfig vpnConfig = mService.getVpnConfig(userId);
            if (vpnConfig == null || vpnConfig.legacy) {
                return;
            }
            if (mPackageInfo.packageName.equals(vpnConfig.user)) {
                mService.prepareVpn(mPackageInfo.packageName, VpnConfig.LEGACY_VPN, userId);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to disconnect package " + mPackageInfo.packageName +
                    " for user " + userId, e);
        }
    }
}
