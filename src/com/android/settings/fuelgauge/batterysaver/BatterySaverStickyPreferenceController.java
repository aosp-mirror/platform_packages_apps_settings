package com.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.Global;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;

public class BatterySaverStickyPreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {
    private static final int DEFAULT_STICKY_SHUTOFF_LEVEL = 90;

    private Context mContext;

    public BatterySaverStickyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Global.LOW_POWER_MODE_STICKY_AUTO_DISABLE_ENABLED,
                        1)
                == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Global.LOW_POWER_MODE_STICKY_AUTO_DISABLE_ENABLED,
                isChecked ? 1 : 0);
        return true;
    }

    @Override
    protected void refreshSummary(Preference preference) {
        super.refreshSummary(preference);
        final int stickyShutoffLevel =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Global.LOW_POWER_MODE_STICKY_AUTO_DISABLE_LEVEL,
                        DEFAULT_STICKY_SHUTOFF_LEVEL);
        final String formatPercentage = Utils.formatPercentage(stickyShutoffLevel);
        preference.setTitle(
                mContext.getString(
                        R.string.battery_saver_sticky_title_percentage, formatPercentage));
        preference.setSummary(
                mContext.getString(
                        R.string.battery_saver_sticky_description_new, formatPercentage));
    }

    @Override
    public void updateState(Preference preference) {
        int setting =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Global.LOW_POWER_MODE_STICKY_AUTO_DISABLE_ENABLED,
                        1);

        ((TwoStatePreference) preference).setChecked(setting == 1);
        refreshSummary(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }
}
