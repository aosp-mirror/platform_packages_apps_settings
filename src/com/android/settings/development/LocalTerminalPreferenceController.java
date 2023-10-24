package com.android.settings.development;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class LocalTerminalPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String ENABLE_TERMINAL_KEY = "enable_terminal";

    @VisibleForTesting
    static final String TERMINAL_APP_PACKAGE = "com.android.terminal";

    private PackageManager mPackageManager;
    private UserManager mUserManager;

    public LocalTerminalPreferenceController(Context context) {
        super(context);

        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return isPackageInstalled(TERMINAL_APP_PACKAGE);
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_TERMINAL_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPackageManager = getPackageManager();

        if (isAvailable() && !isEnabled()) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean terminalEnabled = (Boolean) newValue;
        mPackageManager.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                terminalEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0 /* flags */);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isTerminalEnabled = mPackageManager.getApplicationEnabledSetting(
                TERMINAL_APP_PACKAGE) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ((TwoStatePreference) mPreference).setChecked(isTerminalEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isEnabled()) {
            mPreference.setEnabled(true);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        mPackageManager.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0 /* flags */);
        ((TwoStatePreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    PackageManager getPackageManager() {
        return mContext.getPackageManager();
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            return mContext.getPackageManager().getPackageInfo(packageName, 0 /* flags */) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isEnabled() {
        return mUserManager.isAdminUser();
    }
}
