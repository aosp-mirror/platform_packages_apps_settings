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

package com.android.settings.panel;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.FeatureFlagUtils;

import androidx.annotation.Nullable;

import com.android.settings.Utils;
import com.android.settings.flags.Flags;

@Deprecated(forRemoval = true)
public class PanelFeatureProviderImpl implements PanelFeatureProvider {

    @Override
    @Nullable
    public PanelContent getPanel(Context context, Bundle bundle) {
        if (context == null) {
            return null;
        }

        final String panelType =
                bundle.getString(SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT);
        final String mediaPackageName =
                bundle.getString(SettingsPanelActivity.KEY_MEDIA_PACKAGE_NAME);

        switch (panelType) {
            case Settings.Panel.ACTION_INTERNET_CONNECTIVITY:
                // Redirect to the internet dialog in SystemUI.
                Intent intent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        .setPackage(Utils.SYSTEMUI_PACKAGE_NAME);
                context.sendBroadcast(intent);
                return null;
            case Settings.Panel.ACTION_NFC:
                if (Flags.slicesRetirement()) {
                    Intent nfcIntent = new Intent(Settings.ACTION_NFC_SETTINGS);
                    nfcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(nfcIntent);
                    return null;
                } else {
                    return NfcPanel.create(context);
                }
            case Settings.Panel.ACTION_WIFI:
                if (Flags.slicesRetirement()) {
                    Intent wifiIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    wifiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(wifiIntent);
                    return null;
                } else {
                    return WifiPanel.create(context);
                }
            case Settings.Panel.ACTION_VOLUME:
                if (FeatureFlagUtils.isEnabled(context,
                        FeatureFlagUtils.SETTINGS_VOLUME_PANEL_IN_SYSTEMUI)) {
                    // Redirect to the volume panel in SystemUI.
                    Intent volumeIntent = new Intent(Settings.Panel.ACTION_VOLUME);
                    volumeIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND).setPackage(
                            Utils.SYSTEMUI_PACKAGE_NAME);
                    context.sendBroadcast(volumeIntent);
                    return null;
                } else {
                    if (Flags.slicesRetirement()) {
                        Intent volIntent = new Intent(Settings.ACTION_SOUND_SETTINGS);
                        volIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(volIntent);
                        return null;
                    } else {
                        return VolumePanel.create(context);
                    }
                }
        }

        throw new IllegalStateException("No matching panel for: " + panelType);
    }
}
