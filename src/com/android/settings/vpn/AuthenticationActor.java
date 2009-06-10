/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.vpn;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.vpn.IVpnService;
import android.net.vpn.VpnManager;
import android.net.vpn.VpnProfile;
import android.net.vpn.VpnState;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

/**
 */
public class AuthenticationActor implements VpnProfileActor,
        DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private static final String TAG = AuthenticationActor.class.getName();
    private static final int ONE_SECOND = 1000; // ms

    private static final String STATE_IS_DIALOG_OPEN = "is_dialog_open";
    private static final String STATE_USERNAME = "username";
    private static final String STATE_PASSWORD = "password";

    private Context mContext;
    private TextView mUsernameView;
    private TextView mPasswordView;

    private VpnProfile mProfile;
    private View mView;
    private VpnManager mVpnManager;
    private AlertDialog mConnectDialog;
    private AlertDialog mDisconnectDialog;

    public AuthenticationActor(Context context, VpnProfile p) {
        mContext = context;
        mProfile = p;
        mVpnManager = new VpnManager(context);
    }

    //@Override
    public VpnProfile getProfile() {
        return mProfile;
    }

    //@Override
    public synchronized void connect() {
        connect("", "");
    }

    //@Override
    public void onClick(DialogInterface dialog, int which) {
        dismissConnectDialog();
        switch (which) {
        case DialogInterface.BUTTON1: // connect
            if (validateInputs()) {
                broadcastConnectivity(VpnState.CONNECTING);
                connectInternal();
            }
            break;

        case DialogInterface.BUTTON2: // cancel
            broadcastConnectivity(VpnState.CANCELLED);
            break;
        }
    }

    //@Override
    public void onCancel(DialogInterface dialog) {
        dismissConnectDialog();
        broadcastConnectivity(VpnState.CANCELLED);
    }

    private void connect(String username, String password) {
        Context c = mContext;
        mConnectDialog = new AlertDialog.Builder(c)
                .setView(createConnectView(username, password))
                .setTitle(c.getString(R.string.vpn_connect_to) + " "
                        + mProfile.getName())
                .setPositiveButton(c.getString(R.string.vpn_connect_button),
                        this)
                .setNegativeButton(c.getString(R.string.vpn_cancel_button),
                        this)
                .setOnCancelListener(this)
                .create();
        mConnectDialog.show();
    }

    //@Override
    public synchronized void onSaveState(Bundle outState) {
        outState.putBoolean(STATE_IS_DIALOG_OPEN, (mConnectDialog != null));
        if (mConnectDialog != null) {
            assert(mConnectDialog.isShowing());
            outState.putBoolean(STATE_IS_DIALOG_OPEN, (mConnectDialog != null));
            outState.putString(STATE_USERNAME,
                    mUsernameView.getText().toString());
            outState.putString(STATE_PASSWORD,
                    mPasswordView.getText().toString());
            dismissConnectDialog();
        }
    }

    //@Override
    public synchronized void onRestoreState(final Bundle savedState) {
        boolean isDialogOpen = savedState.getBoolean(STATE_IS_DIALOG_OPEN);
        if (isDialogOpen) {
            connect(savedState.getString(STATE_USERNAME),
                    savedState.getString(STATE_PASSWORD));
        }
    }

    private synchronized void dismissConnectDialog() {
        mConnectDialog.dismiss();
        mConnectDialog = null;
    }

    private void connectInternal() {
        mVpnManager.startVpnService();
        ServiceConnection c = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                    IBinder service) {
                boolean success = false;
                try {
                    success = IVpnService.Stub.asInterface(service)
                            .connect(mProfile,
                                    mUsernameView.getText().toString(),
                                    mPasswordView.getText().toString());
                    mPasswordView.setText("");
                } catch (Throwable e) {
                    Log.e(TAG, "connect()", e);
                    checkStatus();
                } finally {
                    mContext.unbindService(this);

                    if (!success) {
                        Log.d(TAG, "~~~~~~ connect() failed!");
                        // TODO: pop up a dialog
                        broadcastConnectivity(VpnState.IDLE);
                    } else {
                        Log.d(TAG, "~~~~~~ connect() succeeded!");
                    }
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                checkStatus();
            }
        };
        if (!bindService(c)) broadcastConnectivity(VpnState.IDLE);
    }

    //@Override
    public void disconnect() {
        ServiceConnection c = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                    IBinder service) {
                try {
                    IVpnService.Stub.asInterface(service).disconnect();
                } catch (RemoteException e) {
                    Log.e(TAG, "disconnect()", e);
                    checkStatus();
                } finally {
                    mContext.unbindService(this);
                    broadcastConnectivity(VpnState.IDLE);
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                checkStatus();
            }
        };
        bindService(c);
    }

    //@Override
    public void checkStatus() {
        ServiceConnection c = new ServiceConnection() {
            public synchronized void onServiceConnected(ComponentName className,
                    IBinder service) {
                try {
                    IVpnService.Stub.asInterface(service).checkStatus(mProfile);
                } catch (Throwable e) {
                    Log.e(TAG, "checkStatus()", e);
                } finally {
                    notify();
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                // do nothing
            }
        };
        if (bindService(c)) {
            // wait for a second, let status propagate
            wait(c, ONE_SECOND);
        }
        mContext.unbindService(c);
    }

    private boolean bindService(ServiceConnection c) {
        return mVpnManager.bindVpnService(c);
    }

    private void broadcastConnectivity(VpnState s) {
        mVpnManager.broadcastConnectivity(mProfile.getName(), s);
    }

    // returns true if inputs pass validation
    private boolean validateInputs() {
        Context c = mContext;
        String error = null;
        if (Util.isNullOrEmpty(mUsernameView.getText().toString())) {
            error = c.getString(R.string.vpn_username);
        } else if (Util.isNullOrEmpty(mPasswordView.getText().toString())) {
            error = c.getString(R.string.vpn_password);
        }
        if (error == null) {
            return true;
        } else {
            new AlertDialog.Builder(c)
                    .setTitle(c.getString(R.string.vpn_you_miss_a_field))
                    .setMessage(String.format(
                            c.getString(R.string.vpn_please_fill_up), error))
                    .setPositiveButton(c.getString(R.string.vpn_back_button),
                            createBackButtonListener())
                    .show();
            return false;
        }
    }

    private View createConnectView(String username, String password) {
        View v = View.inflate(mContext, R.layout.vpn_connect_dialog_view, null);
        mUsernameView = (TextView) v.findViewById(R.id.username_value);
        mPasswordView = (TextView) v.findViewById(R.id.password_value);
        mUsernameView.setText(username);
        mPasswordView.setText(password);
        copyFieldsFromOldView(v);
        mView = v;
        return v;
    }

    private void copyFieldsFromOldView(View newView) {
        if (mView == null) return;
        mUsernameView.setText(
                ((TextView) mView.findViewById(R.id.username_value)).getText());
        mPasswordView.setText(
                ((TextView) mView.findViewById(R.id.password_value)).getText());
    }

    private DialogInterface.OnClickListener createBackButtonListener() {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                connect();
            }
        };
    }

    private void wait(Object o, int ms) {
        synchronized (o) {
            try {
                o.wait(ms);
            } catch (Exception e) {}
        }
    }
}
