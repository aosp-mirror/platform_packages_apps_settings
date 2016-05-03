/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.dashboard;

import android.app.AutomaticZenRule;
import android.app.IWallpaperManager;
import android.app.IWallpaperManager.Stub;
import android.app.IWallpaperManagerCallback;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.ims.ImsManager;
import com.android.settings.Settings.FingerprintEnrollSuggestionActivity;
import com.android.settings.Settings.FingerprintSuggestionActivity;
import com.android.settings.Settings.ScreenLockSuggestionActivity;
import com.android.settings.Settings.WallpaperSuggestionActivity;
import com.android.settings.Settings.WifiCallingSuggestionActivity;
import com.android.settings.Settings.ZenModeAutomationSuggestionActivity;
import com.android.settingslib.drawer.Tile;

import java.util.Collection;

/**
 * The Home of all stupidly dynamic Settings Suggestions checks.
 */
public class SuggestionsChecks {

    private final Context mContext;

    public SuggestionsChecks(Context context) {
        mContext = context.getApplicationContext();
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
            return isNotSingleFingerprintEnrolled();
        } else if (className.equals(ScreenLockSuggestionActivity.class.getName())
                || className.equals(FingerprintEnrollSuggestionActivity.class.getName())) {
            return isDeviceSecured();
        }
        return false;
    }

    private boolean isDeviceSecured() {
        KeyguardManager km = mContext.getSystemService(KeyguardManager.class);
        return km.isKeyguardSecure();
    }

    private boolean isNotSingleFingerprintEnrolled() {
        FingerprintManager manager = mContext.getSystemService(FingerprintManager.class);
        return manager == null || manager.getEnrolledFingerprints().size() != 1;
    }

    public boolean isWifiCallingUnavailableOrEnabled() {
        if (!ImsManager.isWfcEnabledByPlatform(mContext)) {
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

    private boolean hasWallpaperSet() {
        IBinder b = ServiceManager.getService(Context.WALLPAPER_SERVICE);
        IWallpaperManager service = Stub.asInterface(b);
        try {
            return service.getWallpaper(mCallback, WallpaperManager.FLAG_SYSTEM,
                    new Bundle(), mContext.getUserId()) != null;
        } catch (RemoteException e) {
        }
        return false;
    }

    private final IWallpaperManagerCallback mCallback = new IWallpaperManagerCallback.Stub() {
        @Override
        public void onWallpaperChanged() throws RemoteException {
             // Don't care.
        }
    };
}
