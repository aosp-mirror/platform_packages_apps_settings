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
package com.android.settings.display;

import static android.provider.Settings.Secure.DOZE_ENABLED;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_AMBIENT_DISPLAY;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import android.text.TextUtils;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class AmbientDisplayNotificationsPreferenceController extends
        TogglePreferenceController implements Preference.OnPreferenceChangeListener {

    private final int ON = 1;
    private final int OFF = 0;

    @VisibleForTesting
    static final String KEY_AMBIENT_DISPLAY_NOTIFICATIONS = "ambient_display_notification";
    private static final int MY_USER = UserHandle.myUserId();

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private AmbientDisplayConfiguration mConfig;

    public AmbientDisplayNotificationsPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    /**
     * Set AmbientDisplayConfiguration for this controller, please call in onAttach of fragment
     *
     * @param config AmbientDisplayConfiguration for this controller
     */
    public AmbientDisplayNotificationsPreferenceController setConfig(
            AmbientDisplayConfiguration config) {
        mConfig = config;
        return this;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AMBIENT_DISPLAY_NOTIFICATIONS.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, ACTION_AMBIENT_DISPLAY);
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        return mConfig.pulseOnNotificationEnabled(MY_USER);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(), DOZE_ENABLED, isChecked ? ON : OFF);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mConfig == null) {
            mConfig = new AmbientDisplayConfiguration(mContext);
        }
        return mConfig.pulseOnNotificationAvailable() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "ambient_display_notification");
    }

    @Override
    //TODO (b/69808376): Remove result payload
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSearchResultPageIntent(mContext,
                AmbientDisplaySettings.class.getName(), KEY_AMBIENT_DISPLAY_NOTIFICATIONS,
                mContext.getString(R.string.ambient_display_screen_title));

        return new InlineSwitchPayload(Settings.Secure.DOZE_ENABLED,
                ResultPayload.SettingsSource.SECURE, ON /* onValue */, intent, isAvailable(),
                ON /* defaultValue */);
    }
}
