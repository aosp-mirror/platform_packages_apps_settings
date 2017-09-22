package com.android.settings.development;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settingslib.wrapper.PackageManagerWrapper;

public class LocalTerminalPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String ENABLE_TERMINAL_KEY = "enable_terminal";

    @VisibleForTesting
    static final String TERMINAL_APP_PACKAGE = "com.android.terminal";

    private PackageManagerWrapper mPackageManager;
    private SwitchPreference mPreference;
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

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
        mPackageManager = getPackageManagerWrapper();

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
        mPreference.setChecked(isTerminalEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isEnabled()) {
            mPreference.setEnabled(true);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        mPackageManager.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0 /* flags */);
        mPreference.setEnabled(false);
        mPreference.setChecked(false);
    }

    @VisibleForTesting
    PackageManagerWrapper getPackageManagerWrapper() {
        return new PackageManagerWrapper(mContext.getPackageManager());
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
