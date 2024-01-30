/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quickaccesswallet.QuickAccessWalletClient;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/**
 * Preference for showing/hiding sensitive wallet content while the device is locked.
 */
public class WalletPrivacyPreferenceController extends TogglePreferenceController {

    private static final String SETTING_KEY = Settings.Secure.LOCKSCREEN_SHOW_WALLET;
    private final QuickAccessWalletClient mClient;

    public WalletPrivacyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mClient = initWalletClient();
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), SETTING_KEY, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SETTING_KEY,
                isChecked ? 1 : 0);
    }

    @Override
    public CharSequence getSummary() {
        final int res = isSecure() ? R.string.lockscreen_privacy_wallet_summary :
                R.string.lockscreen_privacy_not_secure;
        return mContext.getText(res);
    }

    @Override
    public int getAvailabilityStatus() {
        if (CustomizableLockScreenUtils.isFeatureEnabled(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        } else  if (!isEnabled()) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (!isSecure()) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(getAvailabilityStatus() == AVAILABLE);
        refreshSummary(preference);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    private boolean isEnabled() {
        return mClient.isWalletServiceAvailable();
    }

    private boolean isSecure() {
        final LockPatternUtils utils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(mContext);
        int userId = UserHandle.myUserId();
        return utils.isSecure(userId);
    }

    @VisibleForTesting
    QuickAccessWalletClient initWalletClient() {
        return QuickAccessWalletClient.create(mContext);
    }
}
