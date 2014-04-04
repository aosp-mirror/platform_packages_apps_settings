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

import android.content.Context;
import android.net.IConnectivityManager;
import android.net.LinkAddress;
import android.net.RouteInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.security.Credentials;
import android.security.KeyStore;
import android.test.InstrumentationTestCase;
import android.test.InstrumentationTestRunner;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Legacy VPN connection tests
 *
 * To run the test, use command:
 * adb shell am instrument -e class com.android.settings.vpn2.VpnTests -e profile foo.xml
 * -w com.android.settings.tests/android.test.InstrumentationTestRunner
 *
 * VPN profiles are saved in an xml file and will be loaded through {@link VpnProfileParser}.
 * Push the profile (foo.xml) to the external storage, e.g adb push foo.xml /sdcard/ before running
 * the above command.
 *
 * A typical profile looks like the following:
 * <vpn>
 *   <name></name>
 *   <type></type>
 *   <server></server>
 *   <username></username>
 *   <password></password>
 *   <dnsServers></dnsServers>
 *   <searchDomains></searchDomains>
 *   <routes></routes>
 *   <l2tpSecret></l2tpSecret>
 *   <ipsecIdentifier></ipsecIdentifier>
 *   <ipsecSecret></ipsecSecret>
 *   <ipsecUserCert></ipsecUserCert>
 *   <ipsecCaCert></ipsecCaCert>
 *   <ipsecServerCert></ipsecServerCert>
 * </vpn>
 * VPN types include: TYPE_PPTP, TYPE_L2TP_IPSEC_PSK, TYPE_L2TP_IPSEC_RSA,
 * TYPE_IPSEC_XAUTH_PSK, TYPE_IPSEC_XAUTH_RSA, TYPE_IPSEC_HYBRID_RSA
 */
public class VpnTests extends InstrumentationTestCase {
    private static final String TAG = "VpnTests";
    /* Maximum time to wait for VPN connection */
    private static final long MAX_CONNECTION_TIME = 5 * 60 * 1000;
    private static final long VPN_STAY_TIME = 60 * 1000;
    private static final int MAX_DISCONNECTION_TRIES = 3;
    private static final String EXTERNAL_SERVER =
            "http://ip2country.sourceforge.net/ip2c.php?format=JSON";
    private static final String VPN_INTERFACE = "ppp0";
    private final IConnectivityManager mService = IConnectivityManager.Stub
        .asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
    private Map<Integer, VpnInfo> mVpnInfoPool = null;
    private Context mContext;
    private CertInstallerHelper mCertHelper = null;
    private KeyStore mKeyStore = KeyStore.getInstance();
    private String mPreviousIpAddress = null;
    private boolean DEBUG = false;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        InputStream in = null;
        InstrumentationTestRunner mRunner = (InstrumentationTestRunner)getInstrumentation();
        mContext = mRunner.getContext();
        Bundle arguments = mRunner.getArguments();
        String PROFILE_NAME = arguments.getString("profile");
        Assert.assertNotNull("Push profile to external storage and load with"
                + "'-e profile <filename>'", PROFILE_NAME);
        File profileFile = new File(Environment.getExternalStorageDirectory(), PROFILE_NAME);
        in = new FileInputStream(profileFile);
        mVpnInfoPool = VpnProfileParser.parse(in);
        Assert.assertNotNull("no VPN profiles are parsed", mVpnInfoPool);
        if (DEBUG) {
            Log.v(TAG, "print out the vpn profiles");
            for (Map.Entry<Integer, VpnInfo> profileEntrySet: mVpnInfoPool.entrySet()) {
                VpnInfo vpnInfo = profileEntrySet.getValue();
                printVpnProfile(vpnInfo.getVpnProfile());
                if (vpnInfo.getCertificateFile() != null) {
                    Log.d(TAG, "certificate file for this vpn is " + vpnInfo.getCertificateFile());
                }
                if (vpnInfo.getPassword() != null) {
                    Log.d(TAG, "password for the certificate file is: " + vpnInfo.getPassword());
                }
            }
        }
        // disconnect existing vpn if there is any
        LegacyVpnInfo oldVpn = mService.getLegacyVpnInfo();
        if (oldVpn != null) {
            Log.v(TAG, "disconnect legacy VPN");
            disconnect();
            // wait till the legacy VPN is disconnected.
            int tries = 0;
            while (tries < MAX_DISCONNECTION_TRIES && mService.getLegacyVpnInfo() != null) {
                tries++;
                Thread.sleep(10 * 1000);
                Log.v(TAG, "Wait for legacy VPN to be disconnected.");
            }
            Assert.assertNull("Failed to disconect VPN", mService.getLegacyVpnInfo());
            // wait for 30 seconds after the previous VPN is disconnected.
            sleep(30 * 1000);
        }
        // Create CertInstallerHelper to initialize the keystore
        mCertHelper = new CertInstallerHelper();
    }

    @Override
    protected void tearDown() throws Exception {
        sleep(VPN_STAY_TIME);
        super.tearDown();
    }

    private void printVpnProfile(VpnProfile profile) {
        Log.v(TAG, "profile: ");
        Log.v(TAG, "key: " + profile.key);
        Log.v(TAG, "name: " + profile.name);
        Log.v(TAG, "type: " + profile.type);
        Log.v(TAG, "server: " + profile.server);
        Log.v(TAG, "username: " + profile.username);
        Log.v(TAG, "password: " + profile.password);
        Log.v(TAG, "dnsServers: " + profile.dnsServers);
        Log.v(TAG, "searchDomains: " + profile.searchDomains);
        Log.v(TAG, "routes: " + profile.routes);
        Log.v(TAG, "mppe: " + profile.mppe);
        Log.v(TAG, "l2tpSecret: " + profile.l2tpSecret);
        Log.v(TAG, "ipsecIdentifier: " + profile.ipsecIdentifier);
        Log.v(TAG, "ipsecSecret: " + profile.ipsecSecret);
        Log.v(TAG, "ipsecUserCert: " + profile.ipsecUserCert);
        Log.v(TAG, "ipsecCaCert: " + profile.ipsecCaCert);
        Log.v(TAG, "ipsecServerCert: " + profile.ipsecServerCert);
    }

    private void printKeyStore(VpnProfile profile) {
        // print out the information from keystore
        String privateKey = "";
        String userCert = "";
        String caCert = "";
        String serverCert = "";
        if (!profile.ipsecUserCert.isEmpty()) {
            privateKey = Credentials.USER_PRIVATE_KEY + profile.ipsecUserCert;
            byte[] value = mKeyStore.get(Credentials.USER_CERTIFICATE + profile.ipsecUserCert);
            userCert = (value == null) ? null : new String(value, StandardCharsets.UTF_8);
        }
        if (!profile.ipsecCaCert.isEmpty()) {
            byte[] value = mKeyStore.get(Credentials.CA_CERTIFICATE + profile.ipsecCaCert);
            caCert = (value == null) ? null : new String(value, StandardCharsets.UTF_8);
        }
        if (!profile.ipsecServerCert.isEmpty()) {
            byte[] value = mKeyStore.get(Credentials.USER_CERTIFICATE + profile.ipsecServerCert);
            serverCert = (value == null) ? null : new String(value, StandardCharsets.UTF_8);
        }
        Log.v(TAG, "privateKey: \n" + ((privateKey == null) ? "" : privateKey));
        Log.v(TAG, "userCert: \n" + ((userCert == null) ? "" : userCert));
        Log.v(TAG, "caCert: \n" + ((caCert == null) ? "" : caCert));
        Log.v(TAG, "serverCert: \n" + ((serverCert == null) ? "" : serverCert));
    }

    /**
     * Connect legacy VPN
     */
    private void connect(VpnProfile profile) throws Exception {
        try {
            mService.startLegacyVpn(profile);
        } catch (IllegalStateException e) {
            fail(String.format("start legacy vpn: %s failed: %s", profile.name, e.toString()));
        }
    }

    /**
     * Disconnect legacy VPN
     */
    private void disconnect() throws Exception {
        try {
            mService.prepareVpn(VpnConfig.LEGACY_VPN, VpnConfig.LEGACY_VPN);
        } catch (RemoteException e) {
            Log.e(TAG, String.format("disconnect VPN exception: %s", e.toString()));
        }
    }

    /**
     * Get external IP address
     */
    private String getIpAddress() {
        String ip = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(EXTERNAL_SERVER);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            Log.i(TAG, "Response from httpget: " + httpResponse.getStatusLine().toString());

            String entityStr = EntityUtils.toString(httpResponse.getEntity());
            JSONObject json_data = new JSONObject(entityStr);
            ip = json_data.getString("ip");
            Log.v(TAG, "json_data: " + ip);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "exception while getting external IP: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "IOException while getting IP: " + e.toString());
        } catch (JSONException e) {
            Log.e(TAG, "exception while creating JSONObject: " + e.toString());
        }
        return ip;
    }

    /**
     * Verify the vpn connection by checking the VPN state and external IP
     */
    private void validateVpnConnection(VpnProfile profile) throws Exception {
        validateVpnConnection(profile, false);
    }

    /**
     * Verify the vpn connection by checking the VPN state, external IP or ping test
     */
    private void validateVpnConnection(VpnProfile profile, boolean pingTestFlag) throws Exception {
        LegacyVpnInfo legacyVpnInfo = mService.getLegacyVpnInfo();
        Assert.assertTrue(legacyVpnInfo != null);

        long start = System.currentTimeMillis();
        while (((System.currentTimeMillis() - start)  < MAX_CONNECTION_TIME) &&
                (legacyVpnInfo.state != LegacyVpnInfo.STATE_CONNECTED)) {
            Log.v(TAG, "vpn state: " + legacyVpnInfo.state);
            sleep(10 * 1000);
            legacyVpnInfo = mService.getLegacyVpnInfo();
        }

        // the vpn state should be CONNECTED
        Assert.assertTrue(legacyVpnInfo.state == LegacyVpnInfo.STATE_CONNECTED);
        if (pingTestFlag) {
            Assert.assertTrue(pingTest(profile.server));
        } else {
            String curIpAddress = getIpAddress();
            // the outgoing IP address should be the same as the VPN server address
            Assert.assertEquals(profile.server, curIpAddress);
        }
    }

    private boolean pingTest(String server) {
        final long PING_TIMER = 3 * 60 * 1000; // 3 minutes
        if (server == null || server.isEmpty()) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < PING_TIMER) {
            try {
                Log.v(TAG, "Start ping test, ping " + server);
                Process p = Runtime.getRuntime().exec("ping -c 10 -w 100 " + server);
                int status = p.waitFor();
                if (status == 0) {
                    // if any of the ping test is successful, return true
                    return true;
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "Ping test Fail: Unknown Host");
            } catch (IOException e) {
                Log.e(TAG, "Ping test Fail:  IOException");
            } catch (InterruptedException e) {
                Log.e(TAG, "Ping test Fail: InterruptedException");
            }
        }
        // ping test timeout
        return false;
    }

    /**
     * Install certificates from a file loaded in external stroage on the device
     * @param profile vpn profile
     * @param fileName certificate file name
     * @param password password to extract certificate file
     */
    private void installCertificatesFromFile(VpnProfile profile, String fileName, String password)
            throws Exception {
        if (profile == null || fileName == null || password == null) {
            throw new Exception ("vpn profile, certificate file name and password can not be null");
        }

        int curUid = mContext.getUserId();
        mCertHelper.installCertificate(profile, fileName, password);

        if (DEBUG) {
            printKeyStore(profile);
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted: " + e.toString());
        }
    }

    /**
     * Test PPTP VPN connection
     */
    @LargeTest
    public void testPPTPConnection() throws Exception {
        mPreviousIpAddress = getIpAddress();
        VpnInfo curVpnInfo = mVpnInfoPool.get(VpnProfile.TYPE_PPTP);
        VpnProfile vpnProfile = curVpnInfo.getVpnProfile();
        connect(vpnProfile);
        validateVpnConnection(vpnProfile);
    }

    /**
     * Test L2TP/IPSec PSK VPN connection
     */
    @LargeTest
    public void testL2tpIpsecPskConnection() throws Exception {
        mPreviousIpAddress = getIpAddress();
        VpnInfo curVpnInfo = mVpnInfoPool.get(VpnProfile.TYPE_L2TP_IPSEC_PSK);
        VpnProfile vpnProfile = curVpnInfo.getVpnProfile();
        connect(vpnProfile);
        validateVpnConnection(vpnProfile);
    }

    /**
     * Test L2TP/IPSec RSA VPN connection
     */
    @LargeTest
    public void testL2tpIpsecRsaConnection() throws Exception {
        mPreviousIpAddress = getIpAddress();
        VpnInfo curVpnInfo = mVpnInfoPool.get(VpnProfile.TYPE_L2TP_IPSEC_RSA);
        VpnProfile vpnProfile = curVpnInfo.getVpnProfile();
        if (DEBUG) {
            printVpnProfile(vpnProfile);
        }
        String certFile = curVpnInfo.getCertificateFile();
        String password = curVpnInfo.getPassword();
        installCertificatesFromFile(vpnProfile, certFile, password);
        connect(vpnProfile);
        validateVpnConnection(vpnProfile);
    }

    /**
     * Test IPSec Xauth RSA VPN connection
     */
    @LargeTest
    public void testIpsecXauthRsaConnection() throws Exception {
        mPreviousIpAddress = getIpAddress();
        VpnInfo curVpnInfo = mVpnInfoPool.get(VpnProfile.TYPE_IPSEC_XAUTH_RSA);
        VpnProfile vpnProfile = curVpnInfo.getVpnProfile();
        if (DEBUG) {
            printVpnProfile(vpnProfile);
        }
        String certFile = curVpnInfo.getCertificateFile();
        String password = curVpnInfo.getPassword();
        installCertificatesFromFile(vpnProfile, certFile, password);
        connect(vpnProfile);
        validateVpnConnection(vpnProfile);
    }

    /**
     * Test IPSec Xauth PSK VPN connection
     */
    @LargeTest
    public void testIpsecXauthPskConnection() throws Exception {
        VpnInfo curVpnInfo = mVpnInfoPool.get(VpnProfile.TYPE_IPSEC_XAUTH_PSK);
        VpnProfile vpnProfile = curVpnInfo.getVpnProfile();
        if (DEBUG) {
            printVpnProfile(vpnProfile);
        }
        connect(vpnProfile);
        validateVpnConnection(vpnProfile, true);
    }

    /**
     * Test IPSec Hybrid RSA VPN connection
     */
    @LargeTest
    public void testIpsecHybridRsaConnection() throws Exception {
        mPreviousIpAddress = getIpAddress();
        VpnInfo curVpnInfo = mVpnInfoPool.get(VpnProfile.TYPE_IPSEC_HYBRID_RSA);
        VpnProfile vpnProfile = curVpnInfo.getVpnProfile();
        if (DEBUG) {
            printVpnProfile(vpnProfile);
        }
        connect(vpnProfile);
        validateVpnConnection(vpnProfile);
    }
}
