package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;

public class WifiTetherSecurityPreferenceController extends WifiTetherBasePreferenceController {

    private static final String PREF_KEY = "wifi_tether_security";

    private final String[] mSecurityEntries;
    private int mSecurityValue;

    public WifiTetherSecurityPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mSecurityEntries = mContext.getResources().getStringArray(R.array.wifi_tether_security);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final WifiConfiguration config = mWifiManager.getWifiApConfiguration();
        if (config != null && config.getAuthType() == WifiConfiguration.KeyMgmt.NONE) {
            mSecurityValue = WifiConfiguration.KeyMgmt.NONE;

        } else {
            mSecurityValue = WifiConfiguration.KeyMgmt.WPA2_PSK;
        }

        final ListPreference preference = (ListPreference) mPreference;
        preference.setSummary(getSummaryForSecurityType(mSecurityValue));
        preference.setValue(String.valueOf(mSecurityValue));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mSecurityValue = Integer.parseInt((String) newValue);
        preference.setSummary(getSummaryForSecurityType(mSecurityValue));
        mListener.onTetherConfigUpdated();
        return true;
    }

    public int getSecurityType() {
        return mSecurityValue;
    }

    private String getSummaryForSecurityType(int securityType) {
        if (securityType == WifiConfiguration.KeyMgmt.NONE) {
            return mSecurityEntries[1];
        }
        // WPA2 PSK
        return mSecurityEntries[0];
    }
}
