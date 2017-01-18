/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gesture lock pattern settings.
 */
public class PrivacySettings extends SettingsPreferenceFragment implements Indexable {

    // Vendor specific
    private static final String GSETTINGS_PROVIDER = "com.google.settings";
    private static final String BACKUP_DATA = "backup_data";
    private static final String AUTO_RESTORE = "auto_restore";
    private static final String CONFIGURE_ACCOUNT = "configure_account";
    private static final String DATA_MANAGEMENT = "data_management";
    private static final String BACKUP_INACTIVE = "backup_inactive";
    private static final String FACTORY_RESET = "factory_reset";
    private static final String TAG = "PrivacySettings";
    private IBackupManager mBackupManager;
    private PreferenceScreen mBackup;
    private SwitchPreference mAutoRestore;
    private PreferenceScreen mConfigure;
    private PreferenceScreen mManageData;
    private boolean mEnabled;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.PRIVACY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Don't allow any access if this is not an admin user.
        // TODO: backup/restore currently only works with owner user b/22760572
        mEnabled = UserManager.get(getActivity()).isAdminUser();
        if (!mEnabled) {
            return;
        }

        addPreferencesFromResource(R.xml.privacy_settings);
        final PreferenceScreen screen = getPreferenceScreen();
        mBackupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));

        mBackup = (PreferenceScreen) screen.findPreference(BACKUP_DATA);

        mAutoRestore = (SwitchPreference) screen.findPreference(AUTO_RESTORE);
        mAutoRestore.setOnPreferenceChangeListener(preferenceChangeListener);

        mConfigure = (PreferenceScreen) screen.findPreference(CONFIGURE_ACCOUNT);
        mManageData = (PreferenceScreen) screen.findPreference(DATA_MANAGEMENT);

        Set<String> keysToRemove = new HashSet<>();
        getNonVisibleKeys(getActivity(), keysToRemove);
        final int screenPreferenceCount = screen.getPreferenceCount();
        for (int i = screenPreferenceCount - 1; i >= 0; --i) {
            Preference preference = screen.getPreference(i);
            if (keysToRemove.contains(preference.getKey())) {
                screen.removePreference(preference);
            }
        }

        updateToggles();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh UI
        if (mEnabled) {
            updateToggles();
        }
    }

    private OnPreferenceChangeListener preferenceChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (!(preference instanceof SwitchPreference)) {
                return true;
            }
            boolean nextValue = (Boolean) newValue;
            boolean result = false;
            if (preference == mAutoRestore) {
                try {
                    mBackupManager.setAutoRestore(nextValue);
                    result = true;
                } catch (RemoteException e) {
                    mAutoRestore.setChecked(!nextValue);
                }
            }
            return result;
        }
    };


    /*
     * Creates toggles for each backup/reset preference.
     */
    private void updateToggles() {
        ContentResolver res = getContentResolver();

        boolean backupEnabled = false;
        Intent configIntent = null;
        String configSummary = null;
        Intent manageIntent = null;
        String manageLabel = null;
        try {
            backupEnabled = mBackupManager.isBackupEnabled();
            String transport = mBackupManager.getCurrentTransport();
            configIntent = validatedActivityIntent(
                    mBackupManager.getConfigurationIntent(transport), "config");
            configSummary = mBackupManager.getDestinationString(transport);
            manageIntent = validatedActivityIntent(
                    mBackupManager.getDataManagementIntent(transport), "management");
            manageLabel = mBackupManager.getDataManagementLabel(transport);

            mBackup.setSummary(backupEnabled
                    ? R.string.accessibility_feature_state_on
                    : R.string.accessibility_feature_state_off);
        } catch (RemoteException e) {
            // leave it 'false' and disable the UI; there's no backup manager
            mBackup.setEnabled(false);
        }

        mAutoRestore.setChecked(Settings.Secure.getInt(res,
                Settings.Secure.BACKUP_AUTO_RESTORE, 1) == 1);
        mAutoRestore.setEnabled(backupEnabled);

        final boolean configureEnabled = (configIntent != null) && backupEnabled;
        mConfigure.setEnabled(configureEnabled);
        mConfigure.setIntent(configIntent);
        setConfigureSummary(configSummary);

        final boolean manageEnabled = (manageIntent != null) && backupEnabled;
        if (manageEnabled) {
            mManageData.setIntent(manageIntent);
            if (manageLabel != null) {
                mManageData.setTitle(manageLabel);
            }
        } else {
            // Hide the item if data management intent is not supported by transport.
            getPreferenceScreen().removePreference(mManageData);
        }
    }

    private Intent validatedActivityIntent(Intent intent, String logLabel) {
        if (intent != null) {
            PackageManager pm = getPackageManager();
            List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
            if (resolved == null || resolved.isEmpty()) {
                intent = null;
                Log.e(TAG, "Backup " + logLabel + " intent " + intent
                        + " fails to resolve; ignoring");
            }
        }
        return intent;
    }

    private void setConfigureSummary(String summary) {
        if (summary != null) {
            mConfigure.setSummary(summary);
        } else {
            mConfigure.setSummary(R.string.backup_configure_account_default_summary);
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_backup_reset;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new PrivacySearchIndexProvider();

    private static class PrivacySearchIndexProvider extends BaseSearchIndexProvider {

        boolean mIsPrimary;

        public PrivacySearchIndexProvider() {
            super();

            mIsPrimary = UserHandle.myUserId() == UserHandle.USER_SYSTEM;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {

            List<SearchIndexableResource> result = new ArrayList<SearchIndexableResource>();

            // For non-primary user, no backup or reset is available
            // TODO: http://b/22388012
            if (!mIsPrimary) {
                return result;
            }

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.privacy_settings;
            result.add(sir);

            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> nonVisibleKeys = new ArrayList<>();
            getNonVisibleKeys(context, nonVisibleKeys);
            return nonVisibleKeys;
        }
    }

    private static void getNonVisibleKeys(Context context, Collection<String> nonVisibleKeys) {
        final IBackupManager backupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));
        boolean isServiceActive = false;
        try {
            isServiceActive = backupManager.isBackupServiceActive(UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.w(TAG, "Failed querying backup manager service activity status. " +
                    "Assuming it is inactive.");
        }
        boolean vendorSpecific = context.getPackageManager().
                resolveContentProvider(GSETTINGS_PROVIDER, 0) == null;
        if (vendorSpecific || isServiceActive) {
            nonVisibleKeys.add(BACKUP_INACTIVE);
        }
        if (vendorSpecific || !isServiceActive) {
            nonVisibleKeys.add(BACKUP_DATA);
            nonVisibleKeys.add(AUTO_RESTORE);
            nonVisibleKeys.add(CONFIGURE_ACCOUNT);
        }
        if (RestrictedLockUtils.hasBaseUserRestriction(context,
                UserManager.DISALLOW_FACTORY_RESET, UserHandle.myUserId())) {
            nonVisibleKeys.add(FACTORY_RESET);
        }
    }
}
