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

import android.app.Dialog;
import android.net.vpn.VpnProfile;
import android.view.View;

/**
 * The interface to act on a {@link VpnProfile}.
 */
public interface VpnProfileActor {
    VpnProfile getProfile();

    /**
     * Returns true if a connect dialog is needed before establishing a
     * connection.
     */
    boolean isConnectDialogNeeded();

    /**
     * Creates the view in the connect dialog.
     */
    View createConnectView();

    /**
     * Validates the inputs in the dialog.
     * @param dialog the connect dialog
     * @return an error message if the inputs are not valid
     */
    String validateInputs(Dialog dialog);

    /**
     * Establishes a VPN connection.
     * @param dialog the connect dialog
     */
    void connect(Dialog dialog);

    /**
     * Tears down the connection.
     */
    void disconnect();

    /**
     * Checks the current status. The result is expected to be broadcast.
     * Use {@link VpnManager#registerConnectivityReceiver()} to register a
     * broadcast receiver and to receives the broadcast events.
     */
    void checkStatus();
}
