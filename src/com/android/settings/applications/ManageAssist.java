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
 * limitations under the License
 */

package com.android.settings.applications;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.internal.app.AssistUtils;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.voice.VoiceInputListPreference;

/**
 * Settings screen to manage everything about assist.
 */
public class ManageAssist extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_DEFAULT_ASSIST = "default_assist";
    private static final String KEY_CONTEXT = "context";
    private static final String KEY_SCREENSHOT = "screenshot";
    private static final String KEY_VOICE_INPUT = "voice_input_settings";
    private static final String KEY_FLASH = "flash";

    private DefaultAssistPreference mDefaultAssitPref;
    private SwitchPreference mContextPref;
    private SwitchPreference mScreenshotPref;
    private SwitchPreference mFlashPref;
    private VoiceInputListPreference mVoiceInputPref;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.manage_assist);

        mDefaultAssitPref = (DefaultAssistPreference) findPreference(KEY_DEFAULT_ASSIST);
        mDefaultAssitPref.setOnPreferenceChangeListener(this);

        mContextPref = (SwitchPreference) findPreference(KEY_CONTEXT);
        mContextPref.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ASSIST_STRUCTURE_ENABLED, 1) != 0);
        mContextPref.setOnPreferenceChangeListener(this);

        mScreenshotPref = (SwitchPreference) findPreference(KEY_SCREENSHOT);
        mScreenshotPref.setOnPreferenceChangeListener(this);

        mFlashPref = (SwitchPreference) findPreference(KEY_FLASH);
        mFlashPref.setOnPreferenceChangeListener(this);

        mVoiceInputPref = (VoiceInputListPreference) findPreference(KEY_VOICE_INPUT);
        updateUi();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_MANAGE_ASSIST;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mContextPref) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSIST_STRUCTURE_ENABLED,
                    (boolean) newValue ? 1 : 0);
            mHandler.post(() -> {
                guardScreenshotPref();
                guardFlashPref();
            });
            return true;
        }
        if (preference == mScreenshotPref) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSIST_SCREENSHOT_ENABLED,
                    (boolean) newValue ? 1 : 0);
            return true;
        }
        if (preference == mFlashPref) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSIST_DISCLOSURE_ENABLED,
                    (boolean) newValue ? 1 : 0);
            return true;
        }
        if (preference == mDefaultAssitPref) {
            String newAssitPackage = (String)newValue;
            if (newAssitPackage == null ||
                    newAssitPackage.contentEquals(DefaultAssistPreference.ITEM_NONE_VALUE)) {
                setDefaultAssist(DefaultAssistPreference.ITEM_NONE_VALUE);
                return false;
            }

            final String currentPackage = mDefaultAssitPref.getValue();
            if (currentPackage == null || !newAssitPackage.contentEquals(currentPackage)) {
                confirmNewAssist(newAssitPackage);
            }
            return false;
        }
        return false;
    }

    private void guardScreenshotPref() {
        boolean isChecked = mContextPref.isChecked();
        boolean screenshotPrefWasSet = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.ASSIST_SCREENSHOT_ENABLED, 1) != 0;
        mScreenshotPref.setEnabled(isChecked);
        mScreenshotPref.setChecked(isChecked && screenshotPrefWasSet);
    }

    private void guardFlashPref() {
        ComponentName assistant = mDefaultAssitPref.getCurrentAssist();

        boolean isContextChecked = mContextPref.isChecked();
        boolean willShowFlash = AssistUtils.shouldDisclose(getContext(), assistant);
        boolean isSystemAssistant = AssistUtils.isPreinstalledAssistant(getContext(), assistant);

        mFlashPref.setEnabled(isContextChecked && isSystemAssistant);
        mFlashPref.setChecked(willShowFlash);
    }

    private void updateUi() {
        mDefaultAssitPref.refreshAssistApps();
        mVoiceInputPref.refreshVoiceInputs();

        final ComponentName currentAssist = mDefaultAssitPref.getCurrentAssist();
        final boolean hasAssistant = currentAssist != null;
        if (hasAssistant) {
            getPreferenceScreen().addPreference(mContextPref);
            getPreferenceScreen().addPreference(mScreenshotPref);
        } else {
            getPreferenceScreen().removePreference(mContextPref);
            getPreferenceScreen().removePreference(mScreenshotPref);
            getPreferenceScreen().removePreference(mFlashPref);
        }

        if (hasAssistant && AssistUtils.allowDisablingAssistDisclosure(getContext())) {
            getPreferenceScreen().addPreference(mFlashPref);
        } else {
            getPreferenceScreen().removePreference(mFlashPref);
        }

        if (isCurrentAssistVoiceService()) {
            getPreferenceScreen().removePreference(mVoiceInputPref);
        } else {
            getPreferenceScreen().addPreference(mVoiceInputPref);
            mVoiceInputPref.setAssistRestrict(currentAssist);
        }

        guardScreenshotPref();
        guardFlashPref();
    }

    private boolean isCurrentAssistVoiceService() {
        ComponentName currentAssist = mDefaultAssitPref.getCurrentAssist();
        ComponentName activeService = mVoiceInputPref.getCurrentService();
        return currentAssist == null && activeService == null ||
                currentAssist != null && currentAssist.equals(activeService);
    }

    private void confirmNewAssist(final String newAssitPackage) {
        final int selected = mDefaultAssitPref.findIndexOfValue(newAssitPackage);
        final CharSequence appLabel = mDefaultAssitPref.getEntries()[selected];

        final String title = getString(R.string.assistant_security_warning_title, appLabel);
        final String message = getString(R.string.assistant_security_warning, appLabel);

        final DialogInterface.OnClickListener onAgree = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setDefaultAssist(newAssitPackage);
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.assistant_security_warning_agree, onAgree)
                .setNegativeButton(R.string.assistant_security_warning_disagree, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setDefaultAssist(String assistPackage) {
        mDefaultAssitPref.setValue(assistPackage);
        updateUi();
    }
}
