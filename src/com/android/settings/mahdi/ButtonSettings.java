/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.settings.mahdi;

import java.util.prefs.PreferenceChangeListener;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.media.AudioSystem;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.VolumePanel;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "ButtonSettings";

    private static final String CATEGORY_VOLUME = "button_volume_keys";
    private static final String BUTTON_VOLUME_WAKE = "button_volume_wake_screen";
    private static final String BUTTON_VOLUME_DEFAULT = "button_volume_default_screen";
    private static final String KEY_HARDWARE_KEYS_CATEGORY = "hardware_keys_category";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String CATEGORY_BACK = "button_back_key";    

    private CheckBoxPreference mVolumeWake;
    private ListPreference mVolumeDefault;
    private PreferenceScreen mHardwareKeys;    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();

        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);		

    // Only show the hardware keys config on a device that does not have a navbar
        mHardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        if (mHardwareKeys != null) {
            if (!res.getBoolean(R.bool.config_has_hardware_buttons)) {
                getPreferenceScreen().removePreference(mHardwareKeys);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(KEY_HARDWARE_KEYS_CATEGORY));
            }        
        } 

        if (hasVolumeRocker()) {
            mVolumeWake = (CheckBoxPreference) findPreference(BUTTON_VOLUME_WAKE);
            mVolumeDefault = (ListPreference) findPreference(BUTTON_VOLUME_DEFAULT);
            if (!res.getBoolean(R.bool.config_show_volumeRockerWake)) {
                prefScreen.removePreference(mVolumeWake);
            } else {
                mVolumeWake.setChecked(Settings.System.getInt(resolver,
                    Settings.System.VOLUME_WAKE_SCREEN, 0) != 0);
            }
            String currentDefault = Settings.System.getString(resolver, Settings.System.VOLUME_KEYS_DEFAULT);

            if (!Utils.isVoiceCapable(getActivity())) {
                removeListEntry(mVolumeDefault, String.valueOf(AudioSystem.STREAM_RING));
            }

            if (currentDefault == null) {
                currentDefault = mVolumeDefault.getEntryValues()[mVolumeDefault.getEntryValues().length - 1].toString();
            }
            mVolumeDefault.setValue(currentDefault);
            mVolumeDefault.setOnPreferenceChangeListener(this);
        } else {
            prefScreen.removePreference(volumeCategory);
        }
    }

    private boolean hasVolumeRocker() {
        return getActivity().getResources().getBoolean(R.bool.config_has_volume_rocker);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mVolumeWake) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.VOLUME_WAKE_SCREEN, checked ? 1:0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeDefault) {
            String value = (String)newValue;
            Settings.System.putString(getActivity().getContentResolver(), Settings.System.VOLUME_KEYS_DEFAULT, value);
            return true;
        }
        return false;
    }

    public void removeListEntry(ListPreference list, String valuetoRemove) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        for (int i = 0; i < list.getEntryValues().length; i++) {
            if (list.getEntryValues()[i].toString().equals(valuetoRemove)) {
                continue;
            } else {
                entries.add(list.getEntries()[i]);
                values.add(list.getEntryValues()[i]);
            }
        }

        list.setEntries(entries.toArray(new CharSequence[entries.size()]));
        list.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }
}
