/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import android.annotation.Nullable;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.chooser.DisplayResolveInfo;
import com.android.internal.app.chooser.TargetInfo;
import com.android.settings.R;
import com.android.settingslib.qrcode.QrCodeGenerator;

import com.google.zxing.WriterException;

/**
 * After sharing a saved Wi-Fi network, {@code WifiDppConfiguratorActivity} start with this fragment
 * to generate a Wi-Fi DPP QR code for other device to initiate as an enrollee.
 */
public class WifiDppQrCodeGeneratorFragment extends WifiDppQrCodeBaseFragment {
    private static final String TAG = "WifiDppQrCodeGeneratorFragment";

    private ImageView mQrCodeView;
    private String mQrCode;

    private static final String CHIP_LABEL_METADATA_KEY = "android.service.chooser.chip_label";
    private static final String CHIP_ICON_METADATA_KEY = "android.service.chooser.chip_icon";
    private static final String EXTRA_WIFI_CREDENTIALS_BUNDLE =
            "android.intent.extra.WIFI_CREDENTIALS_BUNDLE";
    private static final String EXTRA_SSID = "android.intent.extra.SSID";
    private static final String EXTRA_PASSWORD = "android.intent.extra.PASSWORD";
    private static final String EXTRA_SECURITY_TYPE = "android.intent.extra.SECURITY_TYPE";
    private static final String EXTRA_HIDDEN_SSID = "android.intent.extra.HIDDEN_SSID";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setTitle for TalkBack
        final WifiNetworkConfig wifiNetworkConfig = getWifiNetworkConfigFromHostActivity();
        if (getActivity() != null) {
            if (wifiNetworkConfig.isHotspot()) {
                getActivity().setTitle(R.string.wifi_dpp_share_hotspot);
            } else {
                getActivity().setTitle(R.string.wifi_dpp_share_wifi);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final MenuItem menuItem = menu.findItem(Menu.FIRST);
        if (menuItem != null) {
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_dpp_qrcode_generator_fragment, container,
                /* attachToRoot */ false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQrCodeView = view.findViewById(R.id.qrcode_view);

        final WifiNetworkConfig wifiNetworkConfig = getWifiNetworkConfigFromHostActivity();
        if (wifiNetworkConfig.isHotspot()) {
            setHeaderTitle(R.string.wifi_dpp_share_hotspot);
        } else {
            setHeaderTitle(R.string.wifi_dpp_share_wifi);
        }

        final String password = wifiNetworkConfig.getPreSharedKey();
        TextView passwordView = view.findViewById(R.id.password);
        passwordView.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        if (TextUtils.isEmpty(password)) {
            mSummary.setText(getString(
                    R.string.wifi_dpp_scan_open_network_qr_code_with_another_device,
                    wifiNetworkConfig.getSsid()));

            passwordView.setVisibility(View.GONE);
        } else {
            mSummary.setText(getString(R.string.wifi_dpp_scan_qr_code_with_another_device,
                    wifiNetworkConfig.getSsid()));

            if (wifiNetworkConfig.isHotspot()) {
                passwordView.setText(getString(R.string.wifi_dpp_hotspot_password, password));
            } else {
                passwordView.setText(getString(R.string.wifi_dpp_wifi_password, password));
            }
        }

        final Intent intent = new Intent().setComponent(getNearbySharingComponent());
        addActionButton(view.findViewById(R.id.wifi_dpp_layout), createNearbyButton(intent, v -> {
            intent.setAction(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            Bundle wifiCredentialBundle = new Bundle();

            String ssid = WifiDppUtils.removeFirstAndLastDoubleQuotes(wifiNetworkConfig.getSsid());

            String passwordExtra = wifiNetworkConfig.getPreSharedKey();
            String securityType = wifiNetworkConfig.getSecurity();
            boolean hiddenSsid = wifiNetworkConfig.getHiddenSsid();

            wifiCredentialBundle.putString(EXTRA_SSID, ssid);
            wifiCredentialBundle.putString(EXTRA_PASSWORD, passwordExtra);
            wifiCredentialBundle.putString(EXTRA_SECURITY_TYPE, securityType);
            wifiCredentialBundle.putBoolean(EXTRA_HIDDEN_SSID, hiddenSsid);

            intent.putExtra(EXTRA_WIFI_CREDENTIALS_BUNDLE, wifiCredentialBundle);
            startActivity(intent);
        }));

        mQrCode = wifiNetworkConfig.getQrCode();
        setQrCode();
    }

    @VisibleForTesting
    ComponentName getNearbySharingComponent() {
        String nearbyComponent = Settings.Secure.getString(
                getContext().getContentResolver(),
                Settings.Secure.NEARBY_SHARING_COMPONENT);
        if (TextUtils.isEmpty(nearbyComponent)) {
            nearbyComponent = getString(
                    com.android.internal.R.string.config_defaultNearbySharingComponent);
        }
        if (TextUtils.isEmpty(nearbyComponent)) {
            return null;
        }
        return ComponentName.unflattenFromString(nearbyComponent);
    }

    private TargetInfo getNearbySharingTarget(Intent originalIntent) {
        final ComponentName cn = getNearbySharingComponent();
        if (cn == null) return null;

        final Intent resolveIntent = new Intent(originalIntent);
        resolveIntent.setComponent(cn);
        PackageManager pm = getContext().getPackageManager();
        final ResolveInfo resolveInfo = pm.resolveActivity(
                resolveIntent, PackageManager.GET_META_DATA);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            Log.e(TAG, "Device-specified nearby sharing component (" + cn
                    + ") not available");
            return null;
        }

        // Allow the nearby sharing component to provide a more appropriate icon and label
        // for the chip.
        CharSequence name = null;
        Drawable icon = null;
        final Bundle metaData = resolveInfo.activityInfo.metaData;
        if (metaData != null) {
            try {
                final Resources pkgRes = pm.getResourcesForActivity(cn);
                final int nameResId = metaData.getInt(CHIP_LABEL_METADATA_KEY);
                name = pkgRes.getString(nameResId);
                final int resId = metaData.getInt(CHIP_ICON_METADATA_KEY);
                icon = pkgRes.getDrawable(resId);
            } catch (Resources.NotFoundException ex) {
            } catch (PackageManager.NameNotFoundException ex) {
            }
        }
        if (TextUtils.isEmpty(name)) {
            name = resolveInfo.loadLabel(pm);
        }
        if (icon == null) {
            icon = resolveInfo.loadIcon(pm);
        }

        final DisplayResolveInfo dri = new DisplayResolveInfo(
                originalIntent, resolveInfo, name, "", resolveIntent, null);
        dri.setDisplayIcon(icon);
        return dri;
    }

    private Button createActionButton(Drawable icon, CharSequence title, View.OnClickListener r) {
        final Button b = (Button) LayoutInflater.from(getContext()).inflate(
                R.layout.action_button, null);
        if (icon != null) {
            final int size = getResources().getDimensionPixelSize(R.dimen.action_button_icon_size);
            icon.setBounds(0, 0, size, size);
            b.setCompoundDrawablesRelative(icon, null, null, null);
        }
        b.setText(title);
        b.setOnClickListener(r);
        return b;
    }

    private void addActionButton(ViewGroup parent, Button b) {
        if (b == null) return;
        final ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        final int gap = getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.resolver_icon_margin) / 2;
        lp.setMarginsRelative(gap, 0, gap, 0);
        parent.addView(b, lp);
    }

    @VisibleForTesting
    @Nullable
    Button createNearbyButton(Intent originalIntent, View.OnClickListener r) {
        final TargetInfo ti = getNearbySharingTarget(originalIntent);
        if (ti == null) return null;
        final Button button = createActionButton(ti.getDisplayIcon(getContext()),
                ti.getDisplayLabel(), r);
        button.setAllCaps(false);
        return button;
    }

    private void setQrCode() {
        try {
            final int qrcodeSize = getContext().getResources().getDimensionPixelSize(
                    R.dimen.qrcode_size);
            final Bitmap bmp = QrCodeGenerator.encodeQrCode(mQrCode, qrcodeSize);
            mQrCodeView.setImageBitmap(bmp);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code bitmap " + e);
        }
    }

    private WifiNetworkConfig getWifiNetworkConfigFromHostActivity() {
        final WifiNetworkConfig wifiNetworkConfig = ((WifiNetworkConfig.Retriever) getActivity())
                .getWifiNetworkConfig();
        if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
            throw new IllegalStateException("Invalid Wi-Fi network for configuring");
        }

        return wifiNetworkConfig;
    }

    @Override
    protected boolean isFooterAvailable() {
        return false;
    }
}
