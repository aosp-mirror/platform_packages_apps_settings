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

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.VolumePanel;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "ButtonSettings";

    private static final String CATEGORY_VOLUME = "button_volume_keys";
    private static final String BUTTON_VOLUME_WAKE = "button_volume_wake_screen";
    private static final String KEY_VOLBTN_MUSIC_CTRL = "volbtn_music_controls";
    private static final String KEY_HARDWARE_KEYS_CATEGORY = "hardware_keys_category";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String CATEGORY_BACK = "button_back_key";    

    private CheckBoxPreference mVolumeWake;
    private CheckBoxPreference mVolBtnMusicCtrl;
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

	mVolBtnMusicCtrl = (CheckBoxPreference) findPreference(KEY_VOLBTN_MUSIC_CTRL);
        mVolBtnMusicCtrl.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.VOLUME_MUSIC_CONTROLS, 1) != 0);
        mVolBtnMusicCtrl.setOnPreferenceChangeListener(this);	

    // Only show the hardware keys config on a device that does not have a navbar
        mHardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        if (mHardwareKeys != null) {
            if (!res.getBoolean(R.bool.config_has_hardware_buttons)) {
                getPreferenceScreen().removePreference(mHardwareKeys);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(KEY_HARDWARE_KEYS_CATEGORY));
            }        
        } 

        if (hasVolumeRocker()) {
            if (!res.getBoolean(R.bool.config_show_volumeRockerWake)) {
                prefScreen.removePreference(volumeCategory);
            } else {
                mVolumeWake = (CheckBoxPreference) findPreference(BUTTON_VOLUME_WAKE);
                mVolumeWake.setChecked(Settings.System.getInt(resolver,
                        Settings.System.VOLUME_WAKE_SCREEN, 0) != 0);
            }
        } else {
            prefScreen.removePreference(volumeCategory);
        }
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

    private boolean hasVolumeRocker() {
        return getActivity().getResources().getBoolean(R.bool.config_has_volume_rocker);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_VOLBTN_MUSIC_CTRL.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.VOLUME_MUSIC_CONTROLS,
                    (Boolean) objValue ? 1 : 0);
                
	    return true;
        }
	return false;
    }
}
