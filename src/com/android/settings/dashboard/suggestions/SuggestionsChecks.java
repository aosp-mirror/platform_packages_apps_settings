/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dashboard.suggestions;

import android.app.AutomaticZenRule;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.support.annotation.VisibleForTesting;

import com.android.ims.ImsManager;
import com.android.settings.Settings.FingerprintEnrollSuggestionActivity;
import com.android.settings.Settings.FingerprintSuggestionActivity;
import com.android.settings.Settings.ScreenLockSuggestionActivity;
import com.android.settings.Settings.WifiCallingSuggestionActivity;
import com.android.settings.Settings.ZenModeAutomationSuggestionActivity;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wallpaper.WallpaperSuggestionActivity;
import com.android.settingslib.drawer.Tile;

import java.util.Collection;

/**
 * The Home of all stupidly dynamic Settings Suggestions checks.
 */
public class SuggestionsChecks {

    private static final String TAG = "SuggestionsChecks";
    private final Context mContext;

    private final WallpaperManagerWrapper mWallpaperManager;

    public SuggestionsChecks(Context context) {
        mContext = context.getApplicationContext();
        mWallpaperManager = new WallpaperManagerWrapper(mContext);
    }

    public boolean isSuggestionComplete(Tile suggestion) {
        String className = suggestion.intent.getComponent().getClassName();
        if (className.equals(ZenModeAutomationSuggestionActivity.class.getName())) {
            return hasEnabledZenAutoRules();
        } else if (className.equals(WallpaperSuggestionActivity.class.getName())) {
            return hasWallpaperSet();
        } else if (className.equals(WifiCallingSuggestionActivity.class.getName())) {
            return isWifiCallingUnavailableOrEnabled();
        } else if (className.equals(FingerprintSuggestionActivity.class.getName())) {
            return isNotSingleFingerprintEnrolled() || !isFingerprintEnabled();
        } else if (className.equals(ScreenLockSuggestionActivity.class.getName())) {
            return isDeviceSecured();
        } else if (className.equals(FingerprintEnrollSuggestionActivity.class.getName())) {
            FingerprintManager manager = Utils.getFingerprintManagerOrNull(mContext);
            if (manager == null || !isFingerprintEnabled()) {
                return true;
            }
            return manager.hasEnrolledFingerprints();
        }

        SuggestionFeatureProvider provider =
                FeatureFactory.getFactory(mContext).getSuggestionFeatureProvider(mContext);
        if (provider != null && provider.isPresent(className)) {
            return provider.isSuggestionCompleted(mContext);
        }

        return false;
    }

    private boolean isDeviceSecured() {
        KeyguardManager km = mContext.getSystemService(KeyguardManager.class);
        return km.isKeyguardSecure();
    }

    private boolean isNotSingleFingerprintEnrolled() {
        FingerprintManager manager = Utils.getFingerprintManagerOrNull(mContext);
        return manager == null || manager.getEnrolledFingerprints().size() != 1;
    }

    public boolean isWifiCallingUnavailableOrEnabled() {
        if (!ImsManager.isWfcEnabledByPlatform(mContext) ||
                !ImsManager.isWfcProvisionedOnDevice(mContext)) {
            return true;
        }
        return ImsManager.isWfcEnabledByUser(mContext)
                && ImsManager.isNonTtyOrTtyOnVolteEnabled(mContext);
    }

    private boolean hasEnabledZenAutoRules() {
        Collection<AutomaticZenRule> zenRules =
                NotificationManager.from(mContext).getAutomaticZenRules().values();
        for (AutomaticZenRule rule : zenRules) {
            if (rule.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    boolean hasWallpaperSet() {
        return mWallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM) > 0;
    }

    private boolean isFingerprintEnabled() {
        DevicePolicyManager dpManager =
                (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final int dpmFlags = dpManager.getKeyguardDisabledFeatures(null, /* admin */
                mContext.getUserId());
        return (dpmFlags & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) == 0;
    }
}
