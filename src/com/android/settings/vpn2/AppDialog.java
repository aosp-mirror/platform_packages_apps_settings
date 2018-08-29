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

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;

/**
 * UI for managing the connection controlled by an app.
 *
 * Among the actions available are (depending on context):
 * <ul>
 *   <li><strong>Forget</strong>: revoke the managing app's VPN permission</li>
 *   <li><strong>Dismiss</strong>: continue to use the VPN</li>
 * </ul>
 *
 * {@see ConfigDialog}
 */
class AppDialog extends AlertDialog implements DialogInterface.OnClickListener {
    private final Listener mListener;
    private final PackageInfo mPackageInfo;
    private final String mLabel;

    AppDialog(Context context, Listener listener, PackageInfo pkgInfo, String label) {
        super(context);

        mListener = listener;
        mPackageInfo = pkgInfo;
        mLabel = label;
    }

    public final PackageInfo getPackageInfo() {
        return mPackageInfo;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        setTitle(mLabel);
        setMessage(getContext().getString(R.string.vpn_version, mPackageInfo.versionName));

        createButtons();
        super.onCreate(savedState);
    }

    protected void createButtons() {
        Context context = getContext();

        // Forget the network
        setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(R.string.vpn_forget), this);

        // Dismiss
        setButton(DialogInterface.BUTTON_POSITIVE,
                context.getString(R.string.vpn_done), this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_NEGATIVE) {
            mListener.onForget(dialog);
        }
        dismiss();
    }

    public interface Listener {
        public void onForget(DialogInterface dialog);
    }
}
