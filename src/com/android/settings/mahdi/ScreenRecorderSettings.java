/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
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

package com.android.settings.mahdi;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ScreenRecorderSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_VIDEO_SIZE = "screen_recorder_size";
    private static final String KEY_VIDEO_BITRATE = "screen_recorder_bitrate";
    private static final String KEY_RECORD_AUDIO = "screen_recorder_record_audio";

    private ListPreference mVideoSizePref;
    private ListPreference mVideoBitratePref;
    private CheckBoxPreference mRecordAudioPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.screen_recorder_settings);

        final ContentResolver resolver = getContentResolver();
        mVideoSizePref = (ListPreference) findPreference(KEY_VIDEO_SIZE);
        mVideoSizePref.setOnPreferenceChangeListener(this);
        String size = Settings.System.getString(resolver,
                Settings.System.SCREEN_RECORDER_OUTPUT_DIMENSIONS);
        updateVideoSizePreference(size);

        mVideoBitratePref = (ListPreference) findPreference(KEY_VIDEO_BITRATE);
        mVideoBitratePref.setOnPreferenceChangeListener(this);
        String rate= Settings.System.getString(resolver,
                Settings.System.SCREEN_RECORDER_BITRATE);
        updateVideoBitratePreference(rate);

        mRecordAudioPref = (CheckBoxPreference) findPreference(KEY_RECORD_AUDIO);
        mRecordAudioPref.setChecked(Settings.System.getInt(resolver,
                Settings.System.SCREEN_RECORDER_RECORD_AUDIO, 0) == 1);
        mRecordAudioPref.setOnPreferenceChangeListener(this);
        if (!hasMicrophone()) getPreferenceScreen().removePreference(mRecordAudioPref);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mVideoSizePref) {
            updateVideoSizePreference((String) o);
            return true;
        } else if (preference == mVideoBitratePref) {
            updateVideoBitratePreference((String) o);
            return true;
        } else if (preference == mRecordAudioPref) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SCREEN_RECORDER_RECORD_AUDIO,
                    Boolean.TRUE.equals((Boolean)o) ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateVideoSizePreference(String value) {
        if (TextUtils.isEmpty(value)) value = getString(R.string.screen_recorder_size_720x1280);
        mVideoSizePref.setSummary(mVideoSizePref
                .getEntries()[mVideoSizePref.findIndexOfValue(value)]);
        Settings.System.putString(getContentResolver(),
                Settings.System.SCREEN_RECORDER_OUTPUT_DIMENSIONS,
                value);
    }

    private void updateVideoBitratePreference(String value) {
        if (TextUtils.isEmpty(value)) value = getString(R.string.screen_recorder_bitrate_4000000);
        mVideoBitratePref.setSummary(mVideoBitratePref
                .getEntries()[mVideoBitratePref.findIndexOfValue(value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_RECORDER_BITRATE,
                Integer.valueOf(value));
    }

    private boolean hasMicrophone() {
        return getActivity().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }
}
