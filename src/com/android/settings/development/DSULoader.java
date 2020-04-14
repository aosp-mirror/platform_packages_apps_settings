/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.development;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.settings.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * DSU Loader is a front-end that offers developers the ability to boot into GSI with one-click. It
 * also offers the flexibility to overwrite the default setting and load OEMs owned images.
 */
public class DSULoader extends ListActivity {
    public static final String PROPERTY_KEY_FEATURE_FLAG =
            "persist.sys.fflag.override.settings_dynamic_system";
    private static final int Q_VNDK_BASE = 28;
    private static final int Q_OS_BASE = 10;

    private static final boolean DEBUG = false;
    private static final String TAG = "DSULOADER";
    private static final String PROPERTY_KEY_CPU = "ro.product.cpu.abi";
    private static final String PROPERTY_KEY_OS = "ro.system.build.version.release";
    private static final String PROPERTY_KEY_VNDK = "ro.vndk.version";
    private static final String PROPERTY_KEY_LIST =
            "persist.sys.fflag.override.settings_dynamic_system.list";
    private static final String PROPERTY_KEY_SPL = "ro.build.version.security_patch";
    private static final String DSU_LIST =
            "https://dl.google.com/developers/android/gsi/gsi-src.json";

    private static final int TIMEOUT_MS = 10 * 1000;
    private List<Object> mDSUList = new ArrayList<Object>();
    private ArrayAdapter<Object> mAdapter;

    private static String readAll(InputStream in) throws IOException {
        int n;
        StringBuilder list = new StringBuilder();
        byte[] bytes = new byte[4096];
        while ((n = in.read(bytes, 0, 4096)) != -1) {
            list.append(new String(Arrays.copyOf(bytes, n)));
        }
        return list.toString();
    }

    private static String readAll(URL url) throws IOException {
        InputStream in = null;
        HttpsURLConnection connection = null;
        Slog.i(TAG, "fetch " + url.toString());
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            in = new BufferedInputStream(connection.getInputStream());
            return readAll(in);
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch (IOException e) {
                // ignore
            }
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }
    }
    // Fetcher fetches mDSUList in backgroud
    private class Fetcher implements Runnable {
        private URL mDsuList;

        Fetcher(URL dsuList) {
            mDsuList = dsuList;
        }

        private void fetch(URL url)
                throws IOException, JSONException, MalformedURLException, ParseException {
            String content = readAll(url);
            JSONObject jsn = new JSONObject(content);
            // The include primitive is like below
            // "include": [
            //   "https:/...json",
            //    ...
            // ]
            if (jsn.has("include")) {
                JSONArray include = jsn.getJSONArray("include");
                int len = include.length();
                for (int i = 0; i < len; i++) {
                    if (include.isNull(i)) {
                        continue;
                    }
                    fetch(new URL(include.getString(i)));
                }
            }
            //  "images":[
            //    {
            //      "name":"...",
            //      "os_version":"10",
            //      "cpu_abi":"...",
            //      "details":"...",
            //      "vndk":[],
            //      "spl":"...",
            //      "pubkey":"",
            //      "uri":"https://...zip"
            //    },
            //     ...
            //  ]
            if (jsn.has("images")) {
                JSONArray images = jsn.getJSONArray("images");
                int len = images.length();
                for (int i = 0; i < len; i++) {
                    DSUPackage dsu = new DSUPackage(images.getJSONObject(i));
                    if (dsu.isSupported()) {
                        mDSUList.add(dsu);
                    }
                }
            }
        }

        public void run() {
            try {
                fetch(mDsuList);
            } catch (IOException e) {
                Slog.e(TAG, e.toString());
                mDSUList.add(0, "Network Error");
            } catch (Exception e) {
                Slog.e(TAG, e.toString());
                mDSUList.add(0, "Metadata Error");
            }
            if (mDSUList.size() == 0) {
                mDSUList.add(0, "No DSU available for this device");
            }
            runOnUiThread(
                    new Runnable() {
                        public void run() {
                            mAdapter.clear();
                            mAdapter.addAll(mDSUList);
                        }
                    });
        }
    }

    private class DSUPackage {
        private static final String NAME = "name";
        private static final String DETAILS = "details";
        private static final String CPU_ABI = "cpu_abi";
        private static final String URI = "uri";
        private static final String OS_VERSION = "os_version";
        private static final String VNDK = "vndk";
        private static final String PUBKEY = "pubkey";
        private static final String SPL = "spl";
        private static final String SPL_FORMAT = "yyyy-MM-dd";
        private static final String TOS = "tos";

        String mName = null;
        String mDetails = null;
        String mCpuAbi = null;
        int mOsVersion = -1;
        int[] mVndk = null;
        String mPubKey = "";
        Date mSPL = null;
        URL mTosUrl = null;
        URL mUri;

        DSUPackage(JSONObject jsn) throws JSONException, MalformedURLException, ParseException {
            Slog.i(TAG, "DSUPackage: " + jsn.toString());
            mName = jsn.getString(NAME);
            mDetails = jsn.getString(DETAILS);
            mCpuAbi = jsn.getString(CPU_ABI);
            mUri = new URL(jsn.getString(URI));
            if (jsn.has(OS_VERSION)) {
                mOsVersion = dessertNumber(jsn.getString(OS_VERSION), Q_OS_BASE);
            }
            if (jsn.has(VNDK)) {
                JSONArray vndks = jsn.getJSONArray(VNDK);
                mVndk = new int[vndks.length()];
                for (int i = 0; i < vndks.length(); i++) {
                    mVndk[i] = vndks.getInt(i);
                }
            }
            if (jsn.has(PUBKEY)) {
                mPubKey = jsn.getString(PUBKEY);
            }
            if (jsn.has(TOS)) {
                mTosUrl = new URL(jsn.getString(TOS));
            }
            if (jsn.has(SPL)) {
                mSPL = new SimpleDateFormat(SPL_FORMAT).parse(jsn.getString(SPL));
            }
        }

        int dessertNumber(String s, int base) {
            if (s == null || s.isEmpty()) {
                return -1;
            }
            if (Character.isDigit(s.charAt(0))) {
                return Integer.parseInt(s);
            } else {
                s = s.toUpperCase();
                return ((int) s.charAt(0) - (int) 'Q') + base;
            }
        }

        int getDeviceVndk() {
            if (DEBUG) {
                return Q_VNDK_BASE;
            }
            return dessertNumber(SystemProperties.get(PROPERTY_KEY_VNDK), Q_VNDK_BASE);
        }

        int getDeviceOs() {
            if (DEBUG) {
                return Q_OS_BASE;
            }
            return dessertNumber(SystemProperties.get(PROPERTY_KEY_OS), Q_OS_BASE);
        }

        String getDeviceCpu() {
            String cpu = SystemProperties.get(PROPERTY_KEY_CPU);
            cpu = cpu.toLowerCase();
            if (cpu.startsWith("aarch64")) {
                cpu = "arm64-v8a";
            }
            return cpu;
        }

        Date getDeviceSPL() {
            String spl = SystemProperties.get(PROPERTY_KEY_SPL);
            if (TextUtils.isEmpty(spl)) {
                return null;
            }
            try {
                return new SimpleDateFormat(SPL_FORMAT).parse(spl);
            } catch (ParseException e) {
                return null;
            }
        }

        boolean isSupported() {
            boolean supported = true;
            String cpu = getDeviceCpu();
            if (!mCpuAbi.equals(cpu)) {
                Slog.i(TAG, mCpuAbi + " != " + cpu);
                supported = false;
            }
            if (mOsVersion > 0) {
                int os = getDeviceOs();
                if (os < 0) {
                    Slog.i(TAG, "Failed to getDeviceOs");
                    supported = false;
                } else if (mOsVersion < os) {
                    Slog.i(TAG, mOsVersion + " < " + os);
                    supported = false;
                }
            }
            if (mVndk != null) {
                int vndk = getDeviceVndk();
                if (vndk < 0) {
                    Slog.i(TAG, "Failed to getDeviceVndk");
                    supported = false;
                } else {
                    boolean found_vndk = false;
                    for (int i = 0; i < mVndk.length; i++) {
                        if (mVndk[i] == vndk) {
                            found_vndk = true;
                            break;
                        }
                    }
                    if (!found_vndk) {
                        Slog.i(TAG, "vndk:" + vndk + " not found");
                        supported = false;
                    }
                }
            }
            if (mSPL != null) {
                Date spl = getDeviceSPL();
                if (spl == null) {
                    Slog.i(TAG, "Failed to getDeviceSPL");
                    supported = false;
                } else if (spl.getTime() > mSPL.getTime()) {
                    Slog.i(TAG, "Device SPL:" + spl.toString() + " > " + mSPL.toString());
                    supported = false;
                }
            }
            Slog.i(TAG, mName + " isSupported " + supported);
            return supported;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SystemProperties.set(PROPERTY_KEY_FEATURE_FLAG, "1");
        String dsuList = SystemProperties.get(PROPERTY_KEY_LIST);
        Slog.e(TAG, "Try to get DSU list from: " + PROPERTY_KEY_LIST);
        if (dsuList == null || dsuList.isEmpty()) {
            dsuList = DSU_LIST;
        }
        Slog.e(TAG, "DSU list: " + dsuList);
        URL url = null;
        try {
            url = new URL(dsuList);
        } catch (MalformedURLException e) {
            Slog.e(TAG, e.toString());
            return;
        }
        mAdapter = new DSUPackageListAdapter(this);
        setListAdapter(mAdapter);
        mAdapter.add(getResources().getString(R.string.dsu_loader_loading));
        new Thread(new Fetcher(url)).start();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Object selected = mAdapter.getItem(position);
        if (selected instanceof DSUPackage) {
            DSUPackage dsu = (DSUPackage) selected;
            mAdapter.clear();
            mAdapter.add(getResources().getString(R.string.dsu_loader_loading));
            new Thread(new Runnable() {
                public void run() {
                    String termsOfService = "";
                    if (dsu.mTosUrl != null) {
                        try {
                            termsOfService = readAll(dsu.mTosUrl);
                        } catch (IOException e) {
                            Slog.e(TAG, e.toString());
                        }
                    }
                    Intent intent = new Intent(DSULoader.this, DSUTermsOfServiceActivity.class);
                    intent.putExtra(DSUTermsOfServiceActivity.KEY_TOS, termsOfService);
                    intent.setData(Uri.parse(dsu.mUri.toString()));
                    intent.putExtra("KEY_PUBKEY", dsu.mPubKey);
                    startActivity(intent);
                }
            }).start();
        }
        finish();
    }

    private class DSUPackageListAdapter extends ArrayAdapter<Object> {
        private final LayoutInflater mInflater;

        DSUPackageListAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder = AppViewHolder.createOrRecycle(mInflater, convertView);
            convertView = holder.rootView;
            Object item = getItem(position);
            if (item instanceof DSUPackage) {
                DSUPackage dsu = (DSUPackage) item;
                holder.appName.setText(dsu.mName);
                holder.summary.setText(dsu.mDetails);
            } else {
                String msg = (String) item;
                holder.summary.setText(msg);
            }
            holder.appIcon.setImageDrawable(null);
            holder.disabled.setVisibility(View.GONE);
            return convertView;
        }
    }
}
