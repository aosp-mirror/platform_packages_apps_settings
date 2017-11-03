/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.security.KeyStore;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encryption and Credential settings.
 * TODO: Extends this from {@link DashboardFragment} instead
 */
public class EncryptionAndCredential extends DashboardFragment {

    private static final String TAG = "EncryptionAndCredential";

    // Misc Settings
    private static final String KEY_CREDENTIAL_STORAGE_TYPE = "credential_storage_type";
    private static final String KEY_USER_CREDENTIALS = "user_credentials";
    private static final String KEY_RESET_CREDENTIALS = "credentials_reset";
    private static final String KEY_CREDENTIALS_INSTALL = "credentials_install";
    private static final String KEY_CREDENTIALS_MANAGER = "credentials_management";

    private static final int MY_USER_ID = UserHandle.myUserId();

    private KeyStore mKeyStore;
    private RestrictedPreference mResetCredentials;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ENCRYPTION_AND_CREDENTIAL;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.encryption_and_credential;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final EncryptionStatusPreferenceController encryptStatusController =
                new EncryptionStatusPreferenceController(context);
        controllers.add(encryptStatusController);
        controllers.add(new PreferenceCategoryController(context,
                "encryption_and_credentials_status_category",
                Arrays.asList(encryptStatusController)));
        return controllers;
    }

    /**
     * Important!
     *
     * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
     * logic or adding/removing preferences here.
     */
    private PreferenceScreen createPreferenceHierarchy() {
        final PreferenceScreen root = getPreferenceScreen();
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
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.encryption_and_credential;
            return Arrays.asList(sir);
        }

        @Override
        public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
            return buildPreferenceControllers(context);
        }

        @Override
        protected boolean isPageSearchEnabled(Context context) {
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            return um.isAdminUser();
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

            return keys;
        }
    }

}
