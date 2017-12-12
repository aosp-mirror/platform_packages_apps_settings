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

import static android.provider.Settings.Secure.ASSIST_GESTURE_ENABLED;
import static android.provider.Settings.Secure.ASSIST_GESTURE_SILENCE_ALERTS_ENABLED;

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

public class AssistGestureSettingsPreferenceController extends GesturePreferenceController
        implements OnResume {

    private static final String PREF_KEY_VIDEO = "gesture_assist_video";

    private static final String SECURE_KEY_ASSIST = ASSIST_GESTURE_ENABLED;
    private static final String SECURE_KEY_SILENCE = ASSIST_GESTURE_SILENCE_ALERTS_ENABLED;
    private static final int ON = 1;
    private static final int OFF = 0;

    private final String mAssistGesturePrefKey;
    private final AssistGestureFeatureProvider mFeatureProvider;
    private boolean mWasAvailable;

    private PreferenceScreen mScreen;
    private Preference mPreference;

    @VisibleForTesting
    boolean mAssistOnly;

    public AssistGestureSettingsPreferenceController(Context context, Lifecycle lifecycle,
            String key, boolean assistOnly) {
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
        // Call super last or AbstractPreferenceController might remove the preference from the
        // screen (if !isAvailable()) before we can save a reference to it.
        super.displayPreference(screen);
    }

    @Override
    public void onResume() {
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

        if (isAvailable()) {
            if (mScreen.findPreference(getPreferenceKey()) == null) {
                mScreen.addPreference(mPreference);
            }
        } else {
            mScreen.removePreference(mPreference);
        }
    }

    private boolean isAssistGestureEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                SECURE_KEY_ASSIST, ON) != 0;
    }

    private boolean isSilenceGestureEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                SECURE_KEY_SILENCE, ON) != 0;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = isAssistGestureEnabled() && mFeatureProvider.isSupported(mContext);

        if (!mAssistOnly) {
            isEnabled = isEnabled || isSilenceGestureEnabled();
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
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY_ASSIST,
                enabled ? ON : OFF);
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
        // Does nothing
        return true;
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                AssistGestureSettings.class.getName(), mAssistGesturePrefKey,
                mContext.getString(R.string.display_settings));

        return new InlineSwitchPayload(SECURE_KEY_ASSIST, ResultPayload.SettingsSource.SECURE,
                ON /* onValue */, intent, isAvailable(), ON /* defaultValue */);
    }
}
