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

package com.android.settings.gestures;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.events.OnResume;

import static android.provider.Settings.Secure.ASSIST_GESTURE_ENABLED;

public class AssistGesturePreferenceController extends GesturePreferenceController
        implements OnResume {

    private final int ON = 1;
    private final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_assist_video";
    private final String mAssistGesturePrefKey;

    private final String SECURE_KEY = ASSIST_GESTURE_ENABLED;

    private final AssistGestureFeatureProvider mFeatureProvider;
    private boolean mWasAvailable;

    private PreferenceScreen mScreen;
    private Preference mPreference;

    @VisibleForTesting
    boolean mAssistOnly;

    public AssistGesturePreferenceController(Context context, Lifecycle lifecycle, String key,
            boolean assistOnly) {
        super(context, lifecycle);
        mFeatureProvider = FeatureFactory.getFactory(context).getAssistGestureFeatureProvider();
        mWasAvailable = isAvailable();
        mAssistGesturePrefKey = key;
        mAssistOnly = assistOnly;
    }

    @Override
    public boolean isAvailable() {
        if (mAssistOnly) {
            return mFeatureProvider.isSupported(mContext);
        } else {
            return mFeatureProvider.isSensorAvailable(mContext);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
        mPreference = screen.findPreference(getPreferenceKey());
        if (!mFeatureProvider.isSensorAvailable(mContext)) {
            removePreference(mScreen, getPreferenceKey());
            return;
        }
        if (!mFeatureProvider.isSupported(mContext)) {
            mScreen.removePreference(mPreference);
            return;
        }
        // Call super last or AbstractPreferenceController might remove the preference from the
        // screen (if !isAvailable()) before we can save a reference to it.
        super.displayPreference(screen);
    }

    @Override
    public void onResume() {
        // This check must be done in case the user disables Assistant while still on the settings
        // page. This check is slightly different than isAvailable() in some cases due to this
        // setting being in multiple places that require different behavior
        if (mScreen != null && !mFeatureProvider.isSupported(mContext)) {
            mScreen.removePreference(mPreference);
            mWasAvailable = false;
            return;
        }
        if (mWasAvailable != isAvailable()) {
            // Only update the preference visibility if the availability has changed -- otherwise
            // the preference may be incorrectly added to screens with collapsed sections.
            updatePreference();
            mWasAvailable = isAvailable();
        }
    }

    private void updatePreference() {
        if (mPreference == null) {
            return;
        }

        if (mFeatureProvider.isSupported(mContext)) {
            if (mScreen.findPreference(getPreferenceKey()) == null) {
                mScreen.addPreference(mPreference);
            }
        } else {
            mScreen.removePreference(mPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = isSwitchPrefEnabled() && mFeatureProvider.isSupported(mContext);

        if (!mAssistOnly) {
            boolean assistGestureSilenceEnabled = Settings.Secure.getInt(
                    mContext.getContentResolver(),
                    Settings.Secure.ASSIST_GESTURE_SILENCE_ALERTS_ENABLED, 1) != 0;
            isEnabled = isEnabled || assistGestureSilenceEnabled;
        }

        if (preference != null) {
            if (preference instanceof TwoStatePreference) {
                ((TwoStatePreference) preference).setChecked(isSwitchPrefEnabled());
            } else {
                preference.setSummary(isEnabled
                        ? R.string.gesture_setting_on
                        : R.string.gesture_setting_off);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean enabled = (boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY, enabled ? ON : OFF);
        updateState(preference);
        return true;
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mAssistGesturePrefKey;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        final int assistGestureEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                SECURE_KEY, ON);
        return assistGestureEnabled != 0;
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                AssistGestureSettings.class.getName(), mAssistGesturePrefKey,
                mContext.getString(R.string.display_settings));

        return new InlineSwitchPayload(SECURE_KEY, ResultPayload.SettingsSource.SECURE,
                ON /* onValue */, intent, isAvailable(), ON /* defaultValue */);
    }
}
