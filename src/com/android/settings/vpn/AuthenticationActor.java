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
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * A {@link VpnProfileActor} that provides an authentication view for users to
 * input username and password before connecting to the VPN server.
 */
public class AuthenticationActor implements VpnProfileActor {
    private static final String TAG = AuthenticationActor.class.getName();
    private static final int ONE_SECOND = 1000; // ms

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
        if (Util.isNullOrEmpty(usernameView.getText().toString())) {
            return c.getString(R.string.vpn_username);
        } else if (Util.isNullOrEmpty(passwordView.getText().toString())) {
            return c.getString(R.string.vpn_password);
        } else {
            return null;
        }
    }

    //@Override
    public void connect(Dialog d) {
        TextView usernameView = (TextView) d.findViewById(R.id.username_value);
        TextView passwordView = (TextView) d.findViewById(R.id.password_value);
        CheckBox saveUsername = (CheckBox) d.findViewById(R.id.save_username);

        // save username
        if (saveUsername.isChecked()) {
            mProfile.setSavedUsername(usernameView.getText().toString());
        } else {
            mProfile.setSavedUsername("");
        }
        connect(usernameView.getText().toString(),
                passwordView.getText().toString());
        passwordView.setText("");
    }

    //@Override
    public View createConnectView() {
        return View.inflate(mContext, R.layout.vpn_connect_dialog_view, null);
    }

    //@Override
    public void updateConnectView(Dialog d) {
        String username = mProfile.getSavedUsername();
        if (username == null) username = "";
        updateConnectView(d, username, "", !Util.isNullOrEmpty(username));
    }

    private void connect(final String username, final String password) {
        mVpnManager.startVpnService();
        ServiceConnection c = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                    IBinder service) {
                boolean success = false;
                try {
                    success = IVpnService.Stub.asInterface(service)
                            .connect(mProfile, username, password);
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

    private void updateConnectView(Dialog d, String username,
            String password, boolean toSaveUsername) {
        TextView usernameView = (TextView) d.findViewById(R.id.username_value);
        TextView passwordView = (TextView) d.findViewById(R.id.password_value);
        CheckBox saveUsername = (CheckBox) d.findViewById(R.id.save_username);
        usernameView.setText(username);
        passwordView.setText(password);
        saveUsername.setChecked(toSaveUsername);
    }

    private void broadcastConnectivity(VpnState s) {
        mVpnManager.broadcastConnectivity(mProfile.getName(), s);
    }

    private void wait(Object o, int ms) {
        synchronized (o) {
            try {
                o.wait(ms);
            } catch (Exception e) {}
        }
    }
}
