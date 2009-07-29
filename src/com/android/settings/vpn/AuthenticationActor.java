/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.vpn.IVpnService;
import android.net.vpn.VpnManager;
import android.net.vpn.VpnProfile;
import android.net.vpn.VpnState;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.IOException;

/**
 * A {@link VpnProfileActor} that provides an authentication view for users to
 * input username and password before connecting to the VPN server.
 */
public class AuthenticationActor implements VpnProfileActor {
    private static final String TAG = AuthenticationActor.class.getName();

    private Context mContext;
    private VpnProfile mProfile;
    private VpnManager mVpnManager;

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
    public boolean isConnectDialogNeeded() {
        return true;
    }

    //@Override
    public String validateInputs(Dialog d) {
        TextView usernameView = (TextView) d.findViewById(R.id.username_value);
        TextView passwordView = (TextView) d.findViewById(R.id.password_value);
        Context c = mContext;
        if (TextUtils.isEmpty(usernameView.getText().toString())) {
            return c.getString(R.string.vpn_a_username);
        } else if (TextUtils.isEmpty(passwordView.getText().toString())) {
            return c.getString(R.string.vpn_a_password);
        } else {
            return null;
        }
    }

    //@Override
    public void connect(Dialog d) {
        TextView usernameView = (TextView) d.findViewById(R.id.username_value);
        TextView passwordView = (TextView) d.findViewById(R.id.password_value);
        CheckBox saveUsername = (CheckBox) d.findViewById(R.id.save_username);

        try {
            setSavedUsername(saveUsername.isChecked()
                    ? usernameView.getText().toString()
                    : "");
        } catch (IOException e) {
            Log.e(TAG, "setSavedUsername()", e);
        }

        connect(usernameView.getText().toString(),
                passwordView.getText().toString());
        passwordView.setText("");
    }

    //@Override
    public View createConnectView() {
        View v = View.inflate(mContext, R.layout.vpn_connect_dialog_view, null);
        TextView usernameView = (TextView) v.findViewById(R.id.username_value);
        TextView passwordView = (TextView) v.findViewById(R.id.password_value);
        CheckBox saveUsername = (CheckBox) v.findViewById(R.id.save_username);

        String username = mProfile.getSavedUsername();
        if (!TextUtils.isEmpty(username)) {
            usernameView.setText(username);
            saveUsername.setChecked(true);
            passwordView.requestFocus();
        }
        return v;
    }

    protected Context getContext() {
        return mContext;
    }

    private void connect(final String username, final String password) {
        mVpnManager.startVpnService();
        ServiceConnection c = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                    IBinder service) {
                try {
                    boolean success = IVpnService.Stub.asInterface(service)
                            .connect(mProfile, username, password);
                    if (!success) {
                        Log.d(TAG, "~~~~~~ connect() failed!");
                    } else {
                        Log.d(TAG, "~~~~~~ connect() succeeded!");
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "connect()", e);
                    broadcastConnectivity(VpnState.IDLE,
                            VpnManager.VPN_ERROR_CONNECTION_FAILED);
                } finally {
                    mContext.unbindService(this);
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                checkStatus();
            }
        };
        if (!bindService(c)) {
            broadcastConnectivity(VpnState.IDLE,
                    VpnManager.VPN_ERROR_CONNECTION_FAILED);
        }
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
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                checkStatus();
            }
        };
        if (!bindService(c)) {
            checkStatus();
        }
    }

    //@Override
    public void checkStatus() {
        final ConditionVariable cv = new ConditionVariable();
        cv.close();
        ServiceConnection c = new ServiceConnection() {
            public synchronized void onServiceConnected(ComponentName className,
                    IBinder service) {
                cv.open();
                try {
                    IVpnService.Stub.asInterface(service).checkStatus(mProfile);
                } catch (RemoteException e) {
                    Log.e(TAG, "checkStatus()", e);
                    broadcastConnectivity(VpnState.IDLE);
                } finally {
                    mContext.unbindService(this);
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                cv.open();
                broadcastConnectivity(VpnState.IDLE);
                mContext.unbindService(this);
            }
        };
        if (bindService(c)) {
            // wait for a second, let status propagate
            if (!cv.block(1000)) broadcastConnectivity(VpnState.IDLE);
        }
    }

    private boolean bindService(ServiceConnection c) {
        return mVpnManager.bindVpnService(c);
    }

    private void broadcastConnectivity(VpnState s) {
        mVpnManager.broadcastConnectivity(mProfile.getName(), s);
    }

    private void broadcastConnectivity(VpnState s, int errorCode) {
        mVpnManager.broadcastConnectivity(mProfile.getName(), s, errorCode);
    }

    private void setSavedUsername(String name) throws IOException {
        if (!name.equals(mProfile.getSavedUsername())) {
            mProfile.setSavedUsername(name);
            VpnSettings.saveProfileToStorage(mProfile);
        }
    }
}
