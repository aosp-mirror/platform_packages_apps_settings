package com.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

public class BatterySaverStickyPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    public static final String LOW_POWER_STICKY_AUTO_DISABLE_ENABLED =
            "low_power_sticky_auto_disable_enabled";

    public BatterySaverStickyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void updateState(Preference preference) {
        int setting = Settings.Global.getInt(mContext.getContentResolver(),
                LOW_POWER_STICKY_AUTO_DISABLE_ENABLED, 1);

        ((SwitchPreference) preference).setChecked(setting == 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean keepActive = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                LOW_POWER_STICKY_AUTO_DISABLE_ENABLED,
                keepActive ? 0 : 1);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
