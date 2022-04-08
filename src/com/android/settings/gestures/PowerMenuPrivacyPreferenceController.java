/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;

public class PowerMenuPrivacyPreferenceController extends TogglePreferenceController {

    private static final String SETTING_KEY = Settings.Secure.POWER_MENU_LOCKED_SHOW_CONTENT;
    private static final String CARDS_AVAILABLE_KEY =
            Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;
    private static final String CARDS_ENABLED_KEY = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    private static final String CONTROLS_ENABLED_KEY = Settings.Secure.CONTROLS_ENABLED;


    public PowerMenuPrivacyPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
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
        boolean cardsAvailable = Settings.Secure.getInt(mContext.getContentResolver(),
                CARDS_AVAILABLE_KEY, 0) != 0;
        boolean controlsAvailable = isControlsAvailable();
        final int res;
        if (!isSecure()) {
            res = R.string.power_menu_privacy_not_secure;
        } else if (cardsAvailable && controlsAvailable) {
            res = R.string.power_menu_privacy_show;
        } else if (!cardsAvailable && controlsAvailable) {
            res = R.string.power_menu_privacy_show_controls;
        } else if (cardsAvailable) {
            res = R.string.power_menu_privacy_show_cards;
        } else {
            // In this case, neither cards nor controls are available. This preference should not
            // be accessible as the power menu setting is not accessible
            return "";
        }
        return mContext.getText(res);
    }

    @Override
    public int getAvailabilityStatus() {
        // hide if lockscreen isn't secure for this user

        return isEnabled() && isSecure() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(getAvailabilityStatus() != DISABLED_DEPENDENT_SETTING);
        refreshSummary(preference);
    }

    private boolean isEnabled() {
        final ContentResolver resolver = mContext.getContentResolver();
        boolean cardsAvailable = Settings.Secure.getInt(resolver, CARDS_AVAILABLE_KEY, 0) != 0;
        boolean cardsEnabled = Settings.Secure.getInt(resolver, CARDS_ENABLED_KEY, 0) != 0;
        boolean controlsEnabled = Settings.Secure.getInt(resolver, CONTROLS_ENABLED_KEY, 1) != 0;
        return (cardsAvailable && cardsEnabled) || (isControlsAvailable() && controlsEnabled);
    }

    private boolean isSecure() {
        final LockPatternUtils utils = FeatureFactory.getFactory(mContext)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(mContext);
        int userId = UserHandle.myUserId();
        return utils.isSecure(userId);
    }

    private boolean isControlsAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CONTROLS);
    }
}
