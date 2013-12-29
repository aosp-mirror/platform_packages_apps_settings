package com.android.settings.gnome;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class LockscreenNotifications extends SettingsPreferenceFragment {

    private static final String KEY_LOCKSCREEN_NOTIFICATIONS = "lockscreen_notifications";
    private static final String KEY_POCKET_MODE = "pocket_mode";
    private static final String KEY_SHOW_ALWAYS = "show_always";
    private static final String KEY_HIDE_LOW_PRIORITY = "hide_low_priority";
    private static final String KEY_HIDE_NON_CLEARABLE = "hide_non_clearable";
    private static final String KEY_DISMISS_ALL = "dismiss_all";
    private static final String KEY_EXPANDED_VIEW = "expanded_view";

    private CheckBoxPreference mLockscreenNotifications;
    private CheckBoxPreference mPocketMode;
    private CheckBoxPreference mShowAlways;
    private CheckBoxPreference mHideLowPriority;
    private CheckBoxPreference mHideNonClearable;
    private CheckBoxPreference mDismissAll;
    private CheckBoxPreference mExpandedView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_notifications);
        PreferenceScreen prefs = getPreferenceScreen();
        final ContentResolver cr = getActivity().getContentResolver();

        mLockscreenNotifications = (CheckBoxPreference) prefs.findPreference(KEY_LOCKSCREEN_NOTIFICATIONS);
        mLockscreenNotifications.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS, 1) == 1);

        mPocketMode = (CheckBoxPreference) prefs.findPreference(KEY_POCKET_MODE);
        mPocketMode.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_POCKET_MODE, 1) == 1);
        mPocketMode.setEnabled(mLockscreenNotifications.isChecked());

        mShowAlways = (CheckBoxPreference) prefs.findPreference(KEY_SHOW_ALWAYS);
        mShowAlways.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_SHOW_ALWAYS, 1) == 1);
        mShowAlways.setEnabled(mPocketMode.isChecked() && mPocketMode.isEnabled());

        mHideLowPriority = (CheckBoxPreference) prefs.findPreference(KEY_HIDE_LOW_PRIORITY);
        mHideLowPriority.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_LOW_PRIORITY, 0) == 1);
        mHideLowPriority.setEnabled(mLockscreenNotifications.isChecked());

        mHideNonClearable = (CheckBoxPreference) prefs.findPreference(KEY_HIDE_NON_CLEARABLE);
        mHideNonClearable.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_NON_CLEARABLE, 0) == 1);
        mHideNonClearable.setEnabled(mLockscreenNotifications.isChecked());

        mDismissAll = (CheckBoxPreference) prefs.findPreference(KEY_DISMISS_ALL);
        mDismissAll.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_DISMISS_ALL, 1) == 1);
        mDismissAll.setEnabled(!mHideNonClearable.isChecked() && mLockscreenNotifications.isChecked());

	mExpandedView = (CheckBoxPreference) prefs.findPreference(KEY_EXPANDED_VIEW);
        mExpandedView.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_EXPANDED_VIEW, 1) == 1);
        mExpandedView.setEnabled(mLockscreenNotifications.isChecked());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mLockscreenNotifications) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS,
                    mLockscreenNotifications.isChecked() ? 1 : 0);
            mPocketMode.setEnabled(mLockscreenNotifications.isChecked());
            mShowAlways.setEnabled(mPocketMode.isChecked() && mPocketMode.isEnabled());
            mHideLowPriority.setEnabled(mLockscreenNotifications.isChecked());
            mHideNonClearable.setEnabled(mLockscreenNotifications.isChecked());
            mDismissAll.setEnabled(!mHideNonClearable.isChecked() && mLockscreenNotifications.isChecked());
	    mExpandedView.setEnabled(mLockscreenNotifications.isChecked());
        } else if (preference == mPocketMode) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_POCKET_MODE,
                    mPocketMode.isChecked() ? 1 : 0);
            mShowAlways.setEnabled(mPocketMode.isChecked());
        } else if (preference == mShowAlways) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_SHOW_ALWAYS,
                    mShowAlways.isChecked() ? 1 : 0);
        } else if (preference == mHideLowPriority) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_LOW_PRIORITY,
                    mHideLowPriority.isChecked() ? 1 : 0);
        } else if (preference == mHideNonClearable) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_NON_CLEARABLE,
                    mHideNonClearable.isChecked() ? 1 : 0);
            mDismissAll.setEnabled(!mHideNonClearable.isChecked());
        } else if (preference == mDismissAll) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_DISMISS_ALL,
                    mDismissAll.isChecked() ? 1 : 0);
        } else if (preference == mExpandedView) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_EXPANDED_VIEW,
                    mExpandedView.isChecked() ? 1 : 0);
        }else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }
}
