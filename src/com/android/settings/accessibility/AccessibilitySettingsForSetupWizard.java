/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.logging.MetricsProto;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

/**
 * Activity with the accessibility settings specific to Setup Wizard.
 */
public class AccessibilitySettingsForSetupWizard extends SettingsPreferenceFragment
        implements DialogCreatable, Preference.OnPreferenceChangeListener {

    private static final String TAG = AccessibilitySettingsForSetupWizard.class.getSimpleName();

    // Preferences.
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE =
            "screen_magnification_preference";
    private static final String SCREEN_READER_PREFERENCE = "talkback_preference";
    private static final String FONT_SIZE_PREFERENCE = "font_size_preference";

    // Time needed to let Talkback initialize its self before launching the tutorial.
    private static final long SCREEN_READER_INITIALIZATION_DELAY_MS = 3000;

    private String mTalkbackPackage;

    // Preference controls.
    private Preference mDisplayMagnificationPreference;
    private Preference mTalkbackPreference;

    private Runnable mStartTalkbackRunnable = new Runnable() {
        @Override
        public void run() {
            launchTalkbackTutorial();
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ACCESSIBILITY;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings_for_setup_wizard);

        mDisplayMagnificationPreference = findPreference(DISPLAY_MAGNIFICATION_PREFERENCE);
        mTalkbackPreference = findPreference(SCREEN_READER_PREFERENCE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    private void launchTalkbackTutorial() {
        try {
            Intent intent = new Intent(Settings.ACTION_SCREEN_READER_TUTORIAL);
            intent.setPackage(mTalkbackPackage);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // This can happen if either the build is misconfigued or an OEM removes the intent
            // filter for the Talkback tutorial from their implementation of Talkback.
            Log.e(TAG, "Can't find Talkback Tutorial: " + Settings.ACTION_SCREEN_READER_TUTORIAL);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mDisplayMagnificationPreference == preference) {
            Bundle extras = mDisplayMagnificationPreference.getExtras();
            extras.putString(AccessibilitySettings.EXTRA_TITLE,
                    getString(R.string.accessibility_screen_magnification_title));
            extras.putCharSequence(AccessibilitySettings.EXTRA_SUMMARY,
                    getText(R.string.accessibility_screen_magnification_summary));
            extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                    Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1);
        } else if (mTalkbackPreference == preference) {
            // Enable Talkback if disabled. The tutorial will automatically start when Talkback is
            // first activated.
            final ContentResolver resolver = getContentResolver();
            final int accessibilityEnabled =
                    Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (accessibilityEnabled == 0) {
                // Find the first installed screen reader.
                String serviceToEnable = null;
                final AccessibilityManager manager =
                        getActivity().getSystemService(AccessibilityManager.class);
                final List<AccessibilityServiceInfo> accessibilityServices =
                        manager.getInstalledAccessibilityServiceList();
                for (AccessibilityServiceInfo accessibilityService : accessibilityServices) {
                    if ((accessibilityService.feedbackType
                            & AccessibilityServiceInfo.FEEDBACK_SPOKEN) != 0) {
                        final ServiceInfo serviceInfo =
                                accessibilityService.getResolveInfo().serviceInfo;
                        mTalkbackPackage = serviceInfo.packageName;
                        final ComponentName componentName =
                                new ComponentName(serviceInfo.packageName, serviceInfo.name);

                        serviceToEnable = componentName.flattenToString();
                        break;
                    }
                }

                // Enable all accessibility services with spoken feedback type.
                Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        serviceToEnable);

                // Allow the services we just enabled to toggle touch exploration.
                Settings.Secure.putString(resolver,
                        Settings.Secure.TOUCH_EXPLORATION_GRANTED_ACCESSIBILITY_SERVICES,
                        serviceToEnable);

                // Enable touch exploration.
                Settings.Secure.putInt(resolver, Settings.Secure.TOUCH_EXPLORATION_ENABLED, 1);

                // Turn on accessibility mode last, since enabling accessibility with no
                // services has no effect.
                Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);

                // Since Talkback will display an error message if it's not active when the Tutorial
                // is launched, launch Talkbck Tutorial with a delay.
                new Handler().postDelayed(mStartTalkbackRunnable,
                        SCREEN_READER_INITIALIZATION_DELAY_MS);
            } else {
                launchTalkbackTutorial();
            }
        }

        return super.onPreferenceTreeClick(preference);
    }
}
