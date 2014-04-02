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

import android.util.Log;

import com.android.internal.net.VpnProfile;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parse VPN profiles from an XML file
 */
public class VpnProfileParser {
    private final static String TAG = "VpnProfileParser";
    private static Map<Integer, VpnInfo> mVpnPool = new HashMap<Integer, VpnInfo>();

    static DefaultHandler mHandler = new DefaultHandler() {
        boolean name;
        boolean type;
        boolean server;
        boolean username;
        boolean password;
        boolean dnsServers;
        boolean searchDomains;
        boolean routes;
        boolean mppe;
        boolean l2tpSecret;
        boolean ipsecIdentifier;
        boolean ipsecSecret;
        boolean ipsecUserCert;
        boolean ipsecCaCert;
        boolean ipsecServerCert;
        boolean certFile;
        boolean certFilePassword;
        VpnProfile profile = null;
        VpnInfo vpnInfo = null;


        @Override
        public void startElement(String uri, String localName, String tagName,
                Attributes attributes) throws SAXException {
            if (tagName.equalsIgnoreCase("vpn")) {
                //create a new VPN profile
                profile = new VpnProfile(Long.toHexString(System.currentTimeMillis()));
                vpnInfo = new VpnInfo(profile);
            }
            if (tagName.equalsIgnoreCase("name")) {
                name = true;
            }
            if (tagName.equalsIgnoreCase("type")) {
                type = true;
            }
            if (tagName.equalsIgnoreCase("server")) {
                server = true;
            }
            if (tagName.equalsIgnoreCase("username")) {
                username = true;
            }
            if (tagName.equalsIgnoreCase("password")) {
                password = true;
            }
            if (tagName.equalsIgnoreCase("dnsServers")) {
                dnsServers = true;
            }
            if (tagName.equalsIgnoreCase("searchDomains")) {
                searchDomains = true;
            }
            if (tagName.equalsIgnoreCase("mppe")) {
                mppe = true;
            }
            if (tagName.equalsIgnoreCase("l2tpSecret")) {
                l2tpSecret = true;
            }
            if (tagName.equalsIgnoreCase("ipsecIdentifier")) {
                ipsecIdentifier = true;
            }
            if (tagName.equalsIgnoreCase("ipsecSecret")) {
                ipsecSecret = true;
            }
            if (tagName.equalsIgnoreCase("ipsecUserCert")) {
                ipsecUserCert = true;
            }
            if (tagName.equalsIgnoreCase("ipsecCaCert")) {
                ipsecCaCert = true;
            }
            if (tagName.equalsIgnoreCase("ipsecServerCert")) {
                ipsecServerCert = true;
            }
            if (tagName.equalsIgnoreCase("routes")) {
                routes = true;
            }
            if (tagName.equalsIgnoreCase("cert-file")) {
                certFile = true;
            }
            if (tagName.equalsIgnoreCase("cert-file-password")) {
                certFilePassword = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String tagName) throws SAXException {
            if (tagName.equalsIgnoreCase("vpn")) {
                mVpnPool.put(profile.type, vpnInfo);
            }
        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            String strValue = new String(ch, start, length);
            if (name) {
                profile.name = strValue;
                name = false;
            }
            if (type) {
                int t = getVpnProfileType(strValue);
                if (t < 0) {
                    throw new SAXException("not a valid VPN type");
                } else {
                    profile.type = t;
                }
                type = false;
            }
            if (server) {
                profile.server = strValue;
                server = false;
            }
            if (username) {
                profile.username = strValue;
                username = false;
            }
            if (password) {
                profile.password = strValue;
                password = false;
            }
            if (dnsServers) {
                profile.dnsServers = strValue;
                dnsServers = false;
            }
            if (searchDomains) {
                profile.searchDomains = strValue;
                searchDomains = false;
            }
            if (mppe) {
                profile.mppe = Boolean.valueOf(strValue);
                mppe = false;
            }
            if (l2tpSecret) {
                profile.l2tpSecret = strValue;
                l2tpSecret = false;
            }
            if (ipsecIdentifier) {
                profile.ipsecIdentifier = strValue;
                ipsecIdentifier = false;
            }
            if (ipsecSecret) {
                profile.ipsecSecret = strValue;
                ipsecSecret = false;
            }
            if (ipsecUserCert) {
                profile.ipsecUserCert = strValue;
                ipsecUserCert = false;
            }
            if (ipsecCaCert) {
                profile.ipsecCaCert = strValue;
                ipsecCaCert = false;
            }
            if (ipsecServerCert) {
                profile.ipsecServerCert = strValue;
                ipsecServerCert = false;
            }
            if (routes) {
                profile.routes = strValue;
                routes = false;
            }
            if (certFile) {
                vpnInfo.setCertificateFile(strValue);
                certFile = false;
            }
            if (certFilePassword) {
                vpnInfo.setPassword(strValue);
                certFilePassword = false;
            }
        }

        private int getVpnProfileType(String type) {
            if (type.equalsIgnoreCase("TYPE_PPTP")) {
                return VpnProfile.TYPE_PPTP;
            } else if (type.equalsIgnoreCase("TYPE_L2TP_IPSEC_PSK")) {
                return VpnProfile.TYPE_L2TP_IPSEC_PSK;
            } else if (type.equalsIgnoreCase("TYPE_L2TP_IPSEC_RSA")) {
                return VpnProfile.TYPE_L2TP_IPSEC_RSA;
            } else if (type.equalsIgnoreCase("TYPE_IPSEC_XAUTH_PSK")) {
                return VpnProfile.TYPE_IPSEC_XAUTH_PSK;
            } else if (type.equalsIgnoreCase("TYPE_IPSEC_XAUTH_RSA")) {
                return VpnProfile.TYPE_IPSEC_XAUTH_RSA;
            } else if (type.equalsIgnoreCase("TYPE_IPSEC_HYBRID_RSA")) {
                return VpnProfile.TYPE_IPSEC_HYBRID_RSA;
            } else {
                Log.v(TAG, "Invalid VPN type: " + type);
                return -1;
            }
        }
    };

    public static Map<Integer, VpnInfo> parse(InputStream in) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(in, mHandler);
        } catch (SAXException e) {
            Log.e(TAG, "Parse vpn profile exception: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Parse vpn profile exception: " + e.toString());
        } catch (ParserConfigurationException e) {
            Log.e(TAG, "Parse vpn profile exception: " + e.toString());
        } finally {
            return mVpnPool;
        }
    }
}
