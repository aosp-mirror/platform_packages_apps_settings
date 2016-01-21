/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.android.internal.net.VpnProfile;

/**
 * Wrapper for VPN Profile and associated certificate files
 */
public class VpnInfo {
    // VPN Profile
    private VpnProfile mVpnProfile;
    // Certificate file in PC12 format for user certificates and private keys
    private String mCertificateFile = null;
    // Password to extract certificates from the file
    private String mPassword = null;

    public VpnInfo(VpnProfile vpnProfile, String certFile, String password) {
        mVpnProfile = vpnProfile;
        mCertificateFile = certFile;
        mPassword = password;
    }

    public VpnInfo(VpnProfile vpnProfile) {
        mVpnProfile = vpnProfile;
    }

    public void setVpnProfile(VpnProfile vpnProfile) {
        mVpnProfile = vpnProfile;
    }

    public void setCertificateFile(String certFile) {
        mCertificateFile = certFile;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public VpnProfile getVpnProfile() {
        return mVpnProfile;
    }

    public String getCertificateFile() {
        return mCertificateFile;
    }

    public String getPassword() {
        return mPassword;
    }
}
