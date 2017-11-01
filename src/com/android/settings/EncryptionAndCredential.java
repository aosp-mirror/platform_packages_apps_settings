/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.security.KeyStore;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Encryption and Credential settings.
 * TODO: Extends this from {@link DashboardFragment} instead
 */
public class EncryptionAndCredential extends SettingsPreferenceFragment implements Indexable {

    private static final String TAG = "EncryptionAndCredential";

    // Misc Settings
    private static final String KEY_CREDENTIAL_STORAGE_TYPE = "credential_storage_type";
    private static final String KEY_USER_CREDENTIALS = "user_credentials";
    private static final String KEY_RESET_CREDENTIALS = "credentials_reset";
    private static final String KEY_CREDENTIALS_INSTALL = "credentials_install";
    private static final String KEY_CREDENTIALS_MANAGER = "credentials_management";

    private static final int MY_USER_ID = UserHandle.myUserId();

    private UserManager mUm;

    private KeyStore mKeyStore;
    private RestrictedPreference mResetCredentials;

    private boolean mIsAdmin;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ENCRYPTION_AND_CREDENTIAL;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();

        mUm = UserManager.get(activity);
    }

    /**
     * Important!
     *
     * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
     * logic or adding/removing preferences here.
     */
    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.encryption_and_credential);
        root = getPreferenceScreen();

        // Add options for device encryption
        mIsAdmin = mUm.isAdminUser();

        if (mIsAdmin) {
            if (LockPatternUtils.isDeviceEncryptionEnabled()) {
                // The device is currently encrypted.
                addPreferencesFromResource(R.xml.security_settings_encrypted);
            } else {
                // This device supports encryption but isn't encrypted.
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
            }
        }

        // Credential storage
        mKeyStore = KeyStore.getInstance(); // needs to be initialized for onResume()

        if (!RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_CONFIG_CREDENTIALS, MY_USER_ID)) {
            RestrictedPreference userCredentials = (RestrictedPreference) root.findPreference(
                    KEY_USER_CREDENTIALS);
            userCredentials.checkRestrictionAndSetDisabled(
                    UserManager.DISALLOW_CONFIG_CREDENTIALS);
            RestrictedPreference credentialStorageType = (RestrictedPreference) root.findPreference(
                    KEY_CREDENTIAL_STORAGE_TYPE);
            credentialStorageType.checkRestrictionAndSetDisabled(
                    UserManager.DISALLOW_CONFIG_CREDENTIALS);
            RestrictedPreference installCredentials = (RestrictedPreference) root.findPreference(
                    KEY_CREDENTIALS_INSTALL);
            installCredentials.checkRestrictionAndSetDisabled(
                    UserManager.DISALLOW_CONFIG_CREDENTIALS);
            mResetCredentials = (RestrictedPreference) root.findPreference(KEY_RESET_CREDENTIALS);
            mResetCredentials.checkRestrictionAndSetDisabled(
                    UserManager.DISALLOW_CONFIG_CREDENTIALS);

            final int storageSummaryRes =
                    mKeyStore.isHardwareBacked() ? R.string.credential_storage_type_hardware
                            : R.string.credential_storage_type_software;
            credentialStorageType.setSummary(storageSummaryRes);
        } else {
            PreferenceGroup credentialsManager = (PreferenceGroup)
                    root.findPreference(KEY_CREDENTIALS_MANAGER);
            credentialsManager.removePreference(root.findPreference(KEY_RESET_CREDENTIALS));
            credentialsManager.removePreference(root.findPreference(KEY_CREDENTIALS_INSTALL));
            credentialsManager.removePreference(root.findPreference(KEY_CREDENTIAL_STORAGE_TYPE));
            credentialsManager.removePreference(root.findPreference(KEY_USER_CREDENTIALS));
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        if (mResetCredentials != null && !mResetCredentials.isDisabledByAdmin()) {
            mResetCredentials.setEnabled(!mKeyStore.isEmpty());
        }
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        createPreferenceHierarchy();
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_encryption;
    }

    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SecuritySearchIndexProvider();

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final List<SearchIndexableResource> index = new ArrayList<>();

            // Add everything. We will suppress some of them in getNonIndexableKeys()
            index.add(getSearchResource(context, R.xml.encryption_and_credential));
            index.add(getSearchResource(context, R.xml.security_settings_encrypted));
            index.add(getSearchResource(context, R.xml.security_settings_unencrypted));

            return index;
        }

        @Override
        protected boolean isPageSearchEnabled(Context context) {
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            return um.isAdminUser();
        }

        private SearchIndexableResource getSearchResource(Context context, int xmlResId) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = xmlResId;
            return sir;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = super.getNonIndexableKeys(context);
            if (!isPageSearchEnabled(context)) {
                return keys;
            }
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);

            if (um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                keys.add(KEY_CREDENTIALS_MANAGER);
                keys.add(KEY_RESET_CREDENTIALS);
                keys.add(KEY_CREDENTIALS_INSTALL);
                keys.add(KEY_CREDENTIAL_STORAGE_TYPE);
                keys.add(KEY_USER_CREDENTIALS);
            }

            final DevicePolicyManager dpm = (DevicePolicyManager)
                    context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            switch (dpm.getStorageEncryptionStatus()) {
                case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                    // The device is currently encrypted. Disable security_settings_unencrypted
                    keys.addAll(getNonIndexableKeysFromXml(
                            context, R.xml.security_settings_unencrypted));
                    break;
                default:
                    // This device supports encryption but isn't encrypted.
                    keys.addAll(getNonIndexableKeysFromXml(
                            context, R.xml.security_settings_encrypted));
                    break;
            }

            return keys;
        }
    }

}
