package com.android.settings.mahdi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.mahdi.chameleonos.SeekBarPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NetworkUsageStats extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String STATUS_BAR_NETWORK_STATS = "status_bar_show_network_stats";
    private static final String NETWORK_STATS_UPDATE_FREQUENCY = "network_stats_update_frequency";
    private static final String STATUS_BAR_NETWORK_COLOR = "status_bar_network_color";
    private static final String STATUS_BAR_NETWORK_HIDE = "status_bar_network_hide";
    
    private SeekBarPreference mNetworkStatsUpdateFrequency;
    private CheckBoxPreference mStatusBarNetworkStats;
    private ColorPickerPreference mStatusBarNetworkColor;
    private CheckBoxPreference mStatusBarNetworkHide;
    
    private static final int MENU_RESET = Menu.FIRST;

    static final int DEFAULT_NETWORK_USAGE_COLOR = 0xffffffff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	refreshSettings();
    }

    public void refreshSettings() {
	PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        addPreferencesFromResource(R.xml.network_usage_stats);
        prefSet = getPreferenceScreen();

        mStatusBarNetworkStats = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NETWORK_STATS);
	mStatusBarNetworkStats.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                 Settings.System.STATUS_BAR_NETWORK_STATS, 0) == 1));

        mNetworkStatsUpdateFrequency = (SeekBarPreference)
                prefSet.findPreference(NETWORK_STATS_UPDATE_FREQUENCY);
        mNetworkStatsUpdateFrequency.setValue((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, 500)));
        mNetworkStatsUpdateFrequency.setOnPreferenceChangeListener(this);

	// custom colors
	mStatusBarNetworkColor = (ColorPickerPreference) prefSet.findPreference(STATUS_BAR_NETWORK_COLOR);
  	mStatusBarNetworkColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                   Settings.System.STATUS_BAR_NETWORK_COLOR, 0xff000000);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
        mStatusBarNetworkColor.setSummary(hexColor);
        mStatusBarNetworkColor.setNewPreviewColor(intColor);

	// hide if there's no traffic
	mStatusBarNetworkHide = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NETWORK_HIDE);
	mStatusBarNetworkHide.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_HIDE, 0) == 1));

  	setHasOptionsMenu(true);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.status_bar_network_usage_color_reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.status_bar_network_usage_color_reset);
        alertDialog.setMessage(R.string.status_bar_network_usage_color_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                NetworkStatsColorReset();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void NetworkStatsColorReset() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_COLOR, DEFAULT_NETWORK_USAGE_COLOR);
        
        mStatusBarNetworkColor.setNewPreviewColor(DEFAULT_NETWORK_USAGE_COLOR);
        String hexColor = String.format("#%08x", (0xffffffff & DEFAULT_NETWORK_USAGE_COLOR));
        mStatusBarNetworkColor.setSummary(hexColor);
    } 

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNetworkStatsUpdateFrequency) {
            int i = Integer.valueOf((Integer) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, i);
            return true;
	} else if (preference == mStatusBarNetworkColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_COLOR, intHex);
            return true;  
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mStatusBarNetworkStats) {
            value = mStatusBarNetworkStats.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS, value ? 1 : 0);
            return true;
	} else if (preference == mStatusBarNetworkHide) {
            value = mStatusBarNetworkHide.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_HIDE, value ? 1 : 0);
            return true;
        }
        return false;
    }
}
