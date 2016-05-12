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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.security.KeyStore;
import android.service.trust.TrustAgentService;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, DialogInterface.OnClickListener, Indexable,
        GearPreference.OnGearClickListener {

    private static final String TAG = "SecuritySettings";
    private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
    private static final Intent TRUST_AGENT_INTENT =
            new Intent(TrustAgentService.SERVICE_INTERFACE);

    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_UNLOCK_SET_OR_CHANGE_PROFILE = "unlock_set_or_change_profile";
    private static final String KEY_VISIBLE_PATTERN_PROFILE = "visiblepattern_profile";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_DEVICE_ADMIN_CATEGORY = "device_admin_category";
    private static final String KEY_ADVANCED_SECURITY = "advanced_security";
    private static final String KEY_MANAGE_TRUST_AGENTS = "manage_trust_agents";
    private static final String KEY_UNIFICATION = "unification";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CHANGE_TRUST_AGENT_SETTINGS = 126;
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE = 127;
    private static final int UNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 128;
    private static final int UNIFY_LOCK_CONFIRM_PROFILE_REQUEST = 129;
    private static final int UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 130;
    private static final String TAG_UNIFICATION_DIALOG = "unification_dialog";

    // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock";
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final String KEY_CREDENTIAL_STORAGE_TYPE = "credential_storage_type";
    private static final String KEY_USER_CREDENTIALS = "user_credentials";
    private static final String KEY_RESET_CREDENTIALS = "credentials_reset";
    private static final String KEY_CREDENTIALS_INSTALL = "credentials_install";
    private static final String KEY_TOGGLE_INSTALL_APPLICATIONS = "toggle_install_applications";
    private static final String KEY_CREDENTIALS_MANAGER = "credentials_management";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String KEY_TRUST_AGENT = "trust_agent";
    private static final String KEY_SCREEN_PINNING = "screen_pinning_settings";

    // These switch preferences need special handling since they're not all stored in Settings.
    private static final String SWITCH_PREFERENCE_KEYS[] = {
            KEY_SHOW_PASSWORD, KEY_TOGGLE_INSTALL_APPLICATIONS, KEY_UNIFICATION,
            KEY_VISIBLE_PATTERN_PROFILE
    };

    // Only allow one trust agent on the platform.
    private static final boolean ONLY_ONE_TRUST_AGENT = true;

    private static final int MY_USER_ID = UserHandle.myUserId();

    private DevicePolicyManager mDPM;
    private SubscriptionManager mSubscriptionManager;
    private UserManager mUm;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ManagedLockPasswordProvider mManagedPasswordProvider;

    private SwitchPreference mVisiblePatternProfile;
    private SwitchPreference mUnifyProfile;

    private SwitchPreference mShowPassword;

    private KeyStore mKeyStore;
    private RestrictedPreference mResetCredentials;

    private RestrictedSwitchPreference mToggleAppInstallation;
    private DialogInterface mWarnInstallApps;

    private boolean mIsAdmin;

    private Intent mTrustAgentClickIntent;

    private int mProfileChallengeUserId;

    private String mCurrentDevicePassword;
    private String mCurrentProfilePassword;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.SECURITY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSubscriptionManager = SubscriptionManager.from(getActivity());

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mManagedPasswordProvider = ManagedLockPasswordProvider.get(getActivity(), MY_USER_ID);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mUm = UserManager.get(getActivity());

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());

        if (savedInstanceState != null
                && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
            mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
        }
    }

    private static int getResIdForLockUnlockScreen(Context context,
            LockPatternUtils lockPatternUtils, ManagedLockPasswordProvider managedPasswordProvider,
            int userId) {
        final boolean isMyUser = userId == MY_USER_ID;
        int resid = 0;
        if (!lockPatternUtils.isSecure(userId)) {
            if (!isMyUser) {
                resid = R.xml.security_settings_lockscreen_profile;
            } else if (lockPatternUtils.isLockScreenDisabled(userId)) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else {
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality(userId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = isMyUser ? R.xml.security_settings_pattern
                            : R.xml.security_settings_pattern_profile;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    resid = isMyUser ? R.xml.security_settings_pin
                            : R.xml.security_settings_pin_profile;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = isMyUser ? R.xml.security_settings_password
                            : R.xml.security_settings_password_profile;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    resid = managedPasswordProvider.getResIdForLockUnlockScreen(!isMyUser);
                    break;
            }
        }
        return resid;
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
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Add options for lock/unlock screen
        final int resid = getResIdForLockUnlockScreen(getActivity(), mLockPatternUtils,
                mManagedPasswordProvider, MY_USER_ID);
        addPreferencesFromResource(resid);

        // DO or PO installed in the user may disallow to change password.
        disableIfPasswordQualityManaged(KEY_UNLOCK_SET_OR_CHANGE, MY_USER_ID);

        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, MY_USER_ID);
        if (mProfileChallengeUserId != UserHandle.USER_NULL
                && mLockPatternUtils.isSeparateProfileChallengeAllowed(mProfileChallengeUserId)) {
            addPreferencesFromResource(R.xml.security_settings_profile);
            addPreferencesFromResource(R.xml.security_settings_unification);
            final int profileResid = getResIdForLockUnlockScreen(
                    getActivity(), mLockPatternUtils, mManagedPasswordProvider,
                    mProfileChallengeUserId);
            addPreferencesFromResource(profileResid);
            maybeAddFingerprintPreference(root, mProfileChallengeUserId);
            if (!mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId)) {
                final Preference lockPreference =
                        root.findPreference(KEY_UNLOCK_SET_OR_CHANGE_PROFILE);
                final String summary = getContext().getString(
                        R.string.lock_settings_profile_unified_summary);
                lockPreference.setSummary(summary);
                lockPreference.setEnabled(false);
                // PO may disallow to change password for the profile, but screen lock and managed
                // profile's lock is the same. Disable main "Screen lock" menu.
                disableIfPasswordQualityManaged(KEY_UNLOCK_SET_OR_CHANGE, mProfileChallengeUserId);
            } else {
                // PO may disallow to change profile password, and the profile's password is
                // separated from screen lock password. Disable profile specific "Screen lock" menu.
                disableIfPasswordQualityManaged(KEY_UNLOCK_SET_OR_CHANGE_PROFILE,
                        mProfileChallengeUserId);
            }
        }

        Preference unlockSetOrChange = findPreference(KEY_UNLOCK_SET_OR_CHANGE);
        if (unlockSetOrChange instanceof GearPreference) {
            ((GearPreference) unlockSetOrChange).setOnGearClickListener(this);
        }

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

        // Fingerprint and trust agents
        PreferenceGroup securityCategory = (PreferenceGroup)
                root.findPreference(KEY_SECURITY_CATEGORY);
        if (securityCategory != null) {
            maybeAddFingerprintPreference(securityCategory, UserHandle.myUserId());
            addTrustAgentSettings(securityCategory);
        }

        mVisiblePatternProfile =
                (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN_PROFILE);
        mUnifyProfile = (SwitchPreference) root.findPreference(KEY_UNIFICATION);

        // Append the rest of the settings
        addPreferencesFromResource(R.xml.security_settings_misc);

        // Do not display SIM lock for devices without an Icc card
        TelephonyManager tm = TelephonyManager.getDefault();
        CarrierConfigManager cfgMgr = (CarrierConfigManager)
                getActivity().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = cfgMgr.getConfig();
        if (!mIsAdmin || !isSimIccReady() ||
                b.getBoolean(CarrierConfigManager.KEY_HIDE_SIM_LOCK_SETTINGS_BOOL)) {
            root.removePreference(root.findPreference(KEY_SIM_LOCK));
        } else {
            // Disable SIM lock if there is no ready SIM card.
            root.findPreference(KEY_SIM_LOCK).setEnabled(isSimReady());
        }
        if (Settings.System.getInt(getContentResolver(),
                Settings.System.LOCK_TO_APP_ENABLED, 0) != 0) {
            root.findPreference(KEY_SCREEN_PINNING).setSummary(
                    getResources().getString(R.string.switch_on_text));
        }

        // Show password
        mShowPassword = (SwitchPreference) root.findPreference(KEY_SHOW_PASSWORD);
        mResetCredentials = (RestrictedPreference) root.findPreference(KEY_RESET_CREDENTIALS);

        // Credential storage
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
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


        // Application install
        PreferenceGroup deviceAdminCategory = (PreferenceGroup)
                root.findPreference(KEY_DEVICE_ADMIN_CATEGORY);
        mToggleAppInstallation = (RestrictedSwitchPreference) findPreference(
                KEY_TOGGLE_INSTALL_APPLICATIONS);
        mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());
        // Side loading of apps.
        // Disable for restricted profiles. For others, check if policy disallows it.
        mToggleAppInstallation.setEnabled(!um.getUserInfo(MY_USER_ID).isRestricted());
        if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, MY_USER_ID)
                || RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                        UserManager.DISALLOW_INSTALL_APPS, MY_USER_ID)) {
            mToggleAppInstallation.setEnabled(false);
        }
        if (mToggleAppInstallation.isEnabled()) {
            mToggleAppInstallation.checkRestrictionAndSetDisabled(
                    UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
            if (!mToggleAppInstallation.isDisabledByAdmin()) {
                mToggleAppInstallation.checkRestrictionAndSetDisabled(
                        UserManager.DISALLOW_INSTALL_APPS);
            }
        }

        // Advanced Security features
        PreferenceGroup advancedCategory =
                (PreferenceGroup)root.findPreference(KEY_ADVANCED_SECURITY);
        if (advancedCategory != null) {
            Preference manageAgents = advancedCategory.findPreference(KEY_MANAGE_TRUST_AGENTS);
            if (manageAgents != null && !mLockPatternUtils.isSecure(MY_USER_ID)) {
                manageAgents.setEnabled(false);
                manageAgents.setSummary(R.string.disabled_because_no_backup_security);
            }
        }

        // The above preferences come and go based on security state, so we need to update
        // the index. This call is expected to be fairly cheap, but we may want to do something
        // smarter in the future.
        Index.getInstance(getActivity())
                .updateFromClassNameResource(SecuritySettings.class.getName(), true, true);

        for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
            final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }
        return root;
    }

    /*
     * Sets the preference as disabled by admin if PASSWORD_QUALITY_MANAGED is set.
     * The preference must be a RestrictedPreference.
     */
    private void disableIfPasswordQualityManaged(String preferenceKey, int userId) {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfPasswordQualityIsSet(
                getActivity(), userId);
        if (admin != null && mDPM.getPasswordQuality(admin.component, userId) ==
                DevicePolicyManager.PASSWORD_QUALITY_MANAGED) {
            final RestrictedPreference pref =
                    (RestrictedPreference) getPreferenceScreen().findPreference(preferenceKey);
            pref.setDisabledByAdmin(admin);
        }
    }

    private void maybeAddFingerprintPreference(PreferenceGroup securityCategory, int userId) {
        Preference fingerprintPreference =
                FingerprintSettings.getFingerprintPreferenceForUser(
                        securityCategory.getContext(), userId);
        if (fingerprintPreference != null) {
            securityCategory.addPreference(fingerprintPreference);
        }
    }

    private void addTrustAgentSettings(PreferenceGroup securityCategory) {
        final boolean hasSecurity = mLockPatternUtils.isSecure(MY_USER_ID);
        ArrayList<TrustAgentComponentInfo> agents =
                getActiveTrustAgents(getActivity(), mLockPatternUtils, mDPM);
        for (int i = 0; i < agents.size(); i++) {
            final TrustAgentComponentInfo agent = agents.get(i);
            RestrictedPreference trustAgentPreference =
                    new RestrictedPreference(securityCategory.getContext());
            trustAgentPreference.setKey(KEY_TRUST_AGENT);
            trustAgentPreference.setTitle(agent.title);
            trustAgentPreference.setSummary(agent.summary);
            // Create intent for this preference.
            Intent intent = new Intent();
            intent.setComponent(agent.componentName);
            intent.setAction(Intent.ACTION_MAIN);
            trustAgentPreference.setIntent(intent);
            // Add preference to the settings menu.
            securityCategory.addPreference(trustAgentPreference);

            trustAgentPreference.setDisabledByAdmin(agent.admin);
            if (!trustAgentPreference.isDisabledByAdmin() && !hasSecurity) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
            }
        }
    }

    /* Return true if a there is a Slot that has Icc.
     */
    private boolean isSimIccReady() {
        TelephonyManager tm = TelephonyManager.getDefault();
        final List<SubscriptionInfo> subInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();

        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (tm.hasIccCard(subInfo.getSimSlotIndex())) {
                    return true;
                }
            }
        }

        return false;
    }

    /* Return true if a SIM is ready for locking.
     * TODO: consider adding to TelephonyManager or SubscritpionManasger.
     */
    private boolean isSimReady() {
        int simState = TelephonyManager.SIM_STATE_UNKNOWN;
        final List<SubscriptionInfo> subInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                simState = TelephonyManager.getDefault().getSimState(subInfo.getSimSlotIndex());
                if((simState != TelephonyManager.SIM_STATE_ABSENT) &&
                            (simState != TelephonyManager.SIM_STATE_UNKNOWN)){
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<TrustAgentComponentInfo> getActiveTrustAgents(
            Context context, LockPatternUtils utils, DevicePolicyManager dpm) {
        PackageManager pm = context.getPackageManager();
        ArrayList<TrustAgentComponentInfo> result = new ArrayList<TrustAgentComponentInfo>();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT,
                PackageManager.GET_META_DATA);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents(MY_USER_ID);

        EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(context,
                DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS, UserHandle.myUserId());

        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                if (resolveInfo.serviceInfo == null) continue;
                if (!TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) continue;
                TrustAgentComponentInfo trustAgentComponentInfo =
                        TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                if (trustAgentComponentInfo.componentName == null ||
                        !enabledTrustAgents.contains(
                                TrustAgentUtils.getComponentName(resolveInfo)) ||
                        TextUtils.isEmpty(trustAgentComponentInfo.title)) continue;
                if (admin != null && dpm.getTrustAgentConfiguration(
                        null, TrustAgentUtils.getComponentName(resolveInfo)) == null) {
                    trustAgentComponentInfo.admin = admin;
                }
                result.add(trustAgentComponentInfo);
                if (ONLY_ONE_TRUST_AGENT) break;
            }
        }
        return result;
    }

    private boolean isNonMarketAppsAllowed() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.INSTALL_NON_MARKET_APPS, 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            return;
        }
        // Change the system setting
        Settings.Global.putInt(getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS,
                                enabled ? 1 : 0);
    }

    private void warnAppInstallation() {
        // TODO: DialogFragment?
        mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(
                getResources().getString(R.string.error_title))
                .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.install_all_warning))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, this)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mWarnInstallApps) {
            boolean turnOn = which == DialogInterface.BUTTON_POSITIVE;
            setNonMarketAppsAllowed(turnOn);
            if (mToggleAppInstallation != null) {
                mToggleAppInstallation.setChecked(turnOn);
            }
        }
    }

    @Override
    public void onGearClick(GearPreference p) {
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(p.getKey())) {
            startFragment(this, SecuritySubSettings.class.getName(), 0, 0, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWarnInstallApps != null) {
            mWarnInstallApps.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTrustAgentClickIntent != null) {
            outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        if (mVisiblePatternProfile != null) {
            mVisiblePatternProfile.setChecked(mLockPatternUtils.isVisiblePatternEnabled(
                    mProfileChallengeUserId));
        }

        updateUnificationPreference();

        if (mShowPassword != null) {
            mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
        }

        if (mResetCredentials != null && !mResetCredentials.isDisabledByAdmin()) {
            mResetCredentials.setEnabled(!mKeyStore.isEmpty());
        }
    }

    private void updateUnificationPreference() {
        if (mUnifyProfile != null) {
            mUnifyProfile.setChecked(!mLockPatternUtils.isSeparateProfileChallengeEnabled(
                    mProfileChallengeUserId));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_UNLOCK_SET_OR_CHANGE_PROFILE.equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                    mProfileChallengeUserId)) {
                return false;
            }
            Bundle extras = new Bundle();
            extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title_profile,
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE, extras);
        } else if (KEY_TRUST_AGENT.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            mTrustAgentClickIntent = preference.getIntent();
            boolean confirmationLaunched = helper.launchConfirmationActivity(
                    CHANGE_TRUST_AGENT_SETTINGS, preference.getTitle());
            if (!confirmationLaunched&&  mTrustAgentClickIntent != null) {
                // If this returns false, it means no password confirmation is required.
                startActivity(mTrustAgentClickIntent);
                mTrustAgentClickIntent = null;
            }
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHANGE_TRUST_AGENT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (mTrustAgentClickIntent != null) {
                startActivity(mTrustAgentClickIntent);
                mTrustAgentClickIntent = null;
            }
            return;
        } else if (requestCode == UNIFY_LOCK_CONFIRM_DEVICE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentDevicePassword =
                    data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            launchConfirmProfileLockForUnification();
            return;
        } else if (requestCode == UNIFY_LOCK_CONFIRM_PROFILE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentProfilePassword =
                    data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            unifyLocks();
            return;
        } else if (requestCode == UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            ununifyLocks();
            return;
        }
        createPreferenceHierarchy();
    }

    private void launchConfirmDeviceLockForUnification() {
        final String title = getActivity().getString(
                R.string.unlock_set_unlock_launch_picker_title);
        final ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(getActivity(), this);
        if (!helper.launchConfirmationActivity(
                UNIFY_LOCK_CONFIRM_DEVICE_REQUEST, title, true, MY_USER_ID)) {
            launchConfirmProfileLockForUnification();
        }
    }

    private void launchConfirmProfileLockForUnification() {
        final String title = getActivity().getString(
                R.string.unlock_set_unlock_launch_picker_title_profile);
        final ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(getActivity(), this);
        if (!helper.launchConfirmationActivity(
                UNIFY_LOCK_CONFIRM_PROFILE_REQUEST, title, true, mProfileChallengeUserId)) {
            unifyLocks();
            createPreferenceHierarchy();
        }
    }

    private void unifyLocks() {
        int profileQuality =
                mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileChallengeUserId);
        if (profileQuality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            mLockPatternUtils.saveLockPattern(
                    LockPatternUtils.stringToPattern(mCurrentProfilePassword),
                    mCurrentDevicePassword, MY_USER_ID);
        } else {
            mLockPatternUtils.saveLockPassword(
                    mCurrentProfilePassword, mCurrentDevicePassword,
                    profileQuality, MY_USER_ID);
        }
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileChallengeUserId, false,
                mCurrentProfilePassword);
        final boolean profilePatternVisibility =
                mLockPatternUtils.isVisiblePatternEnabled(mProfileChallengeUserId);
        mLockPatternUtils.setVisiblePatternEnabled(profilePatternVisibility, MY_USER_ID);
        mCurrentDevicePassword = null;
        mCurrentProfilePassword = null;
    }

    private void unifyUncompliantLocks() {
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileChallengeUserId, false,
                mCurrentProfilePassword);
        startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
    }

    private void ununifyLocks() {
        Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
        startFragment(this,
                "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                R.string.lock_settings_picker_title_profile,
                SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE, extras);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean result = true;
        final String key = preference.getKey();
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_VISIBLE_PATTERN_PROFILE.equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                    mProfileChallengeUserId)) {
                return false;
            }
            lockPatternUtils.setVisiblePatternEnabled((Boolean) value, mProfileChallengeUserId);
        } else if (KEY_UNIFICATION.equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                    mProfileChallengeUserId)) {
                return false;
            }
            if ((Boolean) value) {
                final boolean compliantForDevice =
                        (mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileChallengeUserId)
                                >= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                        && mLockPatternUtils.isSeparateProfileChallengeAllowedToUnify(
                                mProfileChallengeUserId));
                UnificationConfirmationDialog dialog =
                        UnificationConfirmationDialog.newIntance(compliantForDevice);
                dialog.show(getChildFragmentManager(), TAG_UNIFICATION_DIALOG);
            } else {
                final String title = getActivity().getString(
                        R.string.unlock_set_unlock_launch_picker_title);
                final ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(getActivity(), this);
                if(!helper.launchConfirmationActivity(
                        UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST, title, true, MY_USER_ID)) {
                    ununifyLocks();
                }
            }
        } else if (KEY_SHOW_PASSWORD.equals(key)) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    ((Boolean) value) ? 1 : 0);
            lockPatternUtils.setVisiblePasswordEnabled((Boolean) value, MY_USER_ID);
        } else if (KEY_TOGGLE_INSTALL_APPLICATIONS.equals(key)) {
            if ((Boolean) value) {
                mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
                // Don't change Switch status until user makes choice in dialog, so return false.
                result = false;
            } else {
                setNonMarketAppsAllowed(false);
            }
        }
        return result;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_security;
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
            final List<SearchIndexableResource> index = new ArrayList<SearchIndexableResource>();

            final LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            final ManagedLockPasswordProvider managedPasswordProvider =
                    ManagedLockPasswordProvider.get(context, MY_USER_ID);
            final DevicePolicyManager dpm = (DevicePolicyManager)
                    context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            final UserManager um = UserManager.get(context);
            final int profileUserId = Utils.getManagedProfileId(um, MY_USER_ID);

            // To add option for unlock screen, user's password must not be managed and
            // must not be unified with managed profile, whose password is managed.
            if (!isPasswordManaged(MY_USER_ID, context, dpm)
                    && (profileUserId == UserHandle.USER_NULL
                            || lockPatternUtils.isSeparateProfileChallengeAllowed(profileUserId)
                            || !isPasswordManaged(profileUserId, context, dpm))) {
                // Add options for lock/unlock screen
                final int resId = getResIdForLockUnlockScreen(context, lockPatternUtils,
                        managedPasswordProvider, MY_USER_ID);
                index.add(getSearchResource(context, resId));
            }

            if (profileUserId != UserHandle.USER_NULL
                    && lockPatternUtils.isSeparateProfileChallengeAllowed(profileUserId)
                    && !isPasswordManaged(profileUserId, context, dpm)) {
                index.add(getSearchResource(context, getResIdForLockUnlockScreen(context,
                        lockPatternUtils, managedPasswordProvider, profileUserId)));
            }

            if (um.isAdminUser()) {
                switch (dpm.getStorageEncryptionStatus()) {
                    case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                        // The device is currently encrypted.
                        index.add(getSearchResource(context, R.xml.security_settings_encrypted));
                        break;
                    case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                        // This device supports encryption but isn't encrypted.
                        index.add(getSearchResource(context, R.xml.security_settings_unencrypted));
                        break;
                }
            }

            final SearchIndexableResource sir = getSearchResource(context,
                    SecuritySubSettings.getResIdForLockUnlockSubScreen(context, lockPatternUtils,
                            managedPasswordProvider));
            sir.className = SecuritySubSettings.class.getName();
            index.add(sir);

            // Append the rest of the settings
            index.add(getSearchResource(context, R.xml.security_settings_misc));

            return index;
        }

        private SearchIndexableResource getSearchResource(Context context, int xmlResId) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = xmlResId;
            return sir;
        }

        private boolean isPasswordManaged(int userId, Context context, DevicePolicyManager dpm) {
            final EnforcedAdmin admin = RestrictedLockUtils.checkIfPasswordQualityIsSet(
                    context, userId);
            return admin != null && dpm.getPasswordQuality(admin.component, userId) ==
                    DevicePolicyManager.PASSWORD_QUALITY_MANAGED;
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            final String screenTitle = res.getString(R.string.security_settings_title);

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            result.add(data);

            final UserManager um = UserManager.get(context);
            if (!um.isAdminUser()) {
                int resId = um.isLinkedUser() ?
                        R.string.profile_info_settings_title : R.string.user_info_settings_title;

                data = new SearchIndexableRaw(context);
                data.title = res.getString(resId);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            // Fingerprint
            FingerprintManager fpm =
                    (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            if (fpm != null && fpm.isHardwareDetected()) {
                // This catches the title which can be overloaded in an overlay
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.security_settings_fingerprint_preference_title);
                data.screenTitle = screenTitle;
                result.add(data);
                // Fallback for when the above doesn't contain "fingerprint"
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.fingerprint_manage_category_title);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            final LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            final int profileUserId = Utils.getManagedProfileId(um, MY_USER_ID);
            if (profileUserId != UserHandle.USER_NULL
                    && lockPatternUtils.isSeparateProfileChallengeAllowed(profileUserId)) {
                if (lockPatternUtils.getKeyguardStoredPasswordQuality(profileUserId)
                        >= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                        && lockPatternUtils.isSeparateProfileChallengeAllowedToUnify(
                                profileUserId)) {
                    data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.lock_settings_profile_unification_title);
                    data.screenTitle = screenTitle;
                    result.add(data);
                }
            }

            // Credential storage
            if (!um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                KeyStore keyStore = KeyStore.getInstance();

                final int storageSummaryRes = keyStore.isHardwareBacked() ?
                        R.string.credential_storage_type_hardware :
                        R.string.credential_storage_type_software;

                data = new SearchIndexableRaw(context);
                data.title = res.getString(storageSummaryRes);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            // Advanced
            if (lockPatternUtils.isSecure(MY_USER_ID)) {
                ArrayList<TrustAgentComponentInfo> agents =
                        getActiveTrustAgents(context, lockPatternUtils,
                                context.getSystemService(DevicePolicyManager.class));
                for (int i = 0; i < agents.size(); i++) {
                    final TrustAgentComponentInfo agent = agents.get(i);
                    data = new SearchIndexableRaw(context);
                    data.title = agent.title;
                    data.screenTitle = screenTitle;
                    result.add(data);
                }
            }
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = new ArrayList<String>();

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);

            // Do not display SIM lock for devices without an Icc card
            final UserManager um = UserManager.get(context);
            final TelephonyManager tm = TelephonyManager.from(context);
            if (!um.isAdminUser() || !tm.hasIccCard()) {
                keys.add(KEY_SIM_LOCK);
            }

            if (um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                keys.add(KEY_CREDENTIALS_MANAGER);
            }

            // TrustAgent settings disappear when the user has no primary security.
            if (!lockPatternUtils.isSecure(MY_USER_ID)) {
                keys.add(KEY_TRUST_AGENT);
                keys.add(KEY_MANAGE_TRUST_AGENTS);
            }

            return keys;
        }
    }

    public static class SecuritySubSettings extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener {

        private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
        private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
        private static final String KEY_OWNER_INFO_SETTINGS = "owner_info_settings";
        private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";

        // These switch preferences need special handling since they're not all stored in Settings.
        private static final String SWITCH_PREFERENCE_KEYS[] = { KEY_LOCK_AFTER_TIMEOUT,
                KEY_VISIBLE_PATTERN, KEY_POWER_INSTANTLY_LOCKS };

        private TimeoutListPreference mLockAfter;
        private SwitchPreference mVisiblePattern;
        private SwitchPreference mPowerButtonInstantlyLocks;
        private RestrictedPreference mOwnerInfoPref;

        private LockPatternUtils mLockPatternUtils;
        private DevicePolicyManager mDPM;

        @Override
        protected int getMetricsCategory() {
            return MetricsEvent.SECURITY;
        }

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            mLockPatternUtils = new LockPatternUtils(getContext());
            mDPM = getContext().getSystemService(DevicePolicyManager.class);
            createPreferenceHierarchy();
        }

        @Override
        public void onResume() {
            super.onResume();

            createPreferenceHierarchy();

            if (mVisiblePattern != null) {
                mVisiblePattern.setChecked(mLockPatternUtils.isVisiblePatternEnabled(
                        MY_USER_ID));
            }
            if (mPowerButtonInstantlyLocks != null) {
                mPowerButtonInstantlyLocks.setChecked(mLockPatternUtils.getPowerButtonInstantlyLocks(
                        MY_USER_ID));
            }

            updateOwnerInfo();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            createPreferenceHierarchy();
        }

        private void createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            root = null;

            final int resid = getResIdForLockUnlockSubScreen(getActivity(),
                    new LockPatternUtils(getContext()),
                    ManagedLockPasswordProvider.get(getContext(), MY_USER_ID));
            addPreferencesFromResource(resid);

            // lock after preference
            mLockAfter = (TimeoutListPreference) findPreference(KEY_LOCK_AFTER_TIMEOUT);
            if (mLockAfter != null) {
                setupLockAfterPreference();
                updateLockAfterPreferenceSummary();
            }

            // visible pattern
            mVisiblePattern = (SwitchPreference) findPreference(KEY_VISIBLE_PATTERN);

            // lock instantly on power key press
            mPowerButtonInstantlyLocks = (SwitchPreference) findPreference(
                    KEY_POWER_INSTANTLY_LOCKS);
            Preference trustAgentPreference = findPreference(KEY_TRUST_AGENT);
            if (mPowerButtonInstantlyLocks != null &&
                    trustAgentPreference != null &&
                    trustAgentPreference.getTitle().length() > 0) {
                mPowerButtonInstantlyLocks.setSummary(getString(
                        R.string.lockpattern_settings_power_button_instantly_locks_summary,
                        trustAgentPreference.getTitle()));
            }

            mOwnerInfoPref = (RestrictedPreference) findPreference(KEY_OWNER_INFO_SETTINGS);
            if (mOwnerInfoPref != null) {
                if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
                    EnforcedAdmin admin = RestrictedLockUtils.getDeviceOwner(getActivity());
                    mOwnerInfoPref.setDisabledByAdmin(admin);
                } else {
                    mOwnerInfoPref.setDisabledByAdmin(null);
                    mOwnerInfoPref.setEnabled(!mLockPatternUtils.isLockScreenDisabled(MY_USER_ID));
                    if (mOwnerInfoPref.isEnabled()) {
                        mOwnerInfoPref.setOnPreferenceClickListener(
                                new OnPreferenceClickListener() {
                                    @Override
                                    public boolean onPreferenceClick(Preference preference) {
                                        OwnerInfoSettings.show(SecuritySubSettings.this);
                                        return true;
                                    }
                                });
                    }
                }
            }

            for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
                final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
                if (pref != null) pref.setOnPreferenceChangeListener(this);
            }
        }

        private void setupLockAfterPreference() {
            // Compatible with pre-Froyo
            long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
            mLockAfter.setValue(String.valueOf(currentTimeout));
            mLockAfter.setOnPreferenceChangeListener(this);
            if (mDPM != null) {
                final EnforcedAdmin admin = RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(
                        getActivity());
                final long adminTimeout = mDPM
                        .getMaximumTimeToLockForUserAndProfiles(UserHandle.myUserId());
                final long displayTimeout = Math.max(0,
                        Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
                // This setting is a slave to display timeout when a device policy is enforced.
                // As such, maxLockTimeout = adminTimeout - displayTimeout.
                // If there isn't enough time, shows "immediately" setting.
                final long maxTimeout = Math.max(0, adminTimeout - displayTimeout);
                mLockAfter.removeUnusableTimeouts(maxTimeout, admin);
            }
        }

        private void updateLockAfterPreferenceSummary() {
            final String summary;
            if (mLockAfter.isDisabledByAdmin()) {
                summary = getString(R.string.disabled_by_policy_title);
            } else {
                // Update summary message with current value
                long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
                final CharSequence[] entries = mLockAfter.getEntries();
                final CharSequence[] values = mLockAfter.getEntryValues();
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.valueOf(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }

                Preference preference = findPreference(KEY_TRUST_AGENT);
                if (preference != null && preference.getTitle().length() > 0) {
                    if (Long.valueOf(values[best].toString()) == 0) {
                        summary = getString(R.string.lock_immediately_summary_with_exception,
                                preference.getTitle());
                    } else {
                        summary = getString(R.string.lock_after_timeout_summary_with_exception,
                                entries[best], preference.getTitle());
                    }
                } else {
                    summary = getString(R.string.lock_after_timeout_summary, entries[best]);
                }
            }
            mLockAfter.setSummary(summary);
        }

        public void updateOwnerInfo() {
            if (mOwnerInfoPref != null) {
                if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
                    mOwnerInfoPref.setSummary(
                            mLockPatternUtils.getDeviceOwnerInfo());
                } else {
                    mOwnerInfoPref.setSummary(mLockPatternUtils.isOwnerInfoEnabled(MY_USER_ID)
                            ? mLockPatternUtils.getOwnerInfo(MY_USER_ID)
                            : getString(R.string.owner_info_settings_summary));
                }
            }
        }

        private static int getResIdForLockUnlockSubScreen(Context context,
                LockPatternUtils lockPatternUtils,
                ManagedLockPasswordProvider managedPasswordProvider) {
            if (lockPatternUtils.isSecure(MY_USER_ID)) {
                switch (lockPatternUtils.getKeyguardStoredPasswordQuality(MY_USER_ID)) {
                    case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                        return R.xml.security_settings_pattern_sub;
                    case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                    case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                        return R.xml.security_settings_pin_sub;
                    case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                    case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                    case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                        return R.xml.security_settings_password_sub;
                    case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                        return managedPasswordProvider.getResIdForLockUnlockSubScreen();
                }
            } else if (!lockPatternUtils.isLockScreenDisabled(MY_USER_ID)) {
                return R.xml.security_settings_slide_sub;
            }
            return 0;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String key = preference.getKey();
            if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
                mLockPatternUtils.setPowerButtonInstantlyLocks((Boolean) value, MY_USER_ID);
            } else if (KEY_LOCK_AFTER_TIMEOUT.equals(key)) {
                int timeout = Integer.parseInt((String) value);
                try {
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
                } catch (NumberFormatException e) {
                    Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
                }
                setupLockAfterPreference();
                updateLockAfterPreferenceSummary();
            } else if (KEY_VISIBLE_PATTERN.equals(key)) {
                mLockPatternUtils.setVisiblePatternEnabled((Boolean) value, MY_USER_ID);
            }
            return true;
        }
    }

    public static class UnificationConfirmationDialog extends DialogFragment {
        private static final String EXTRA_COMPLIANT = "compliant";

        public static UnificationConfirmationDialog newIntance(boolean compliant) {
            UnificationConfirmationDialog dialog = new UnificationConfirmationDialog();
            Bundle args = new Bundle();
            args.putBoolean(EXTRA_COMPLIANT, compliant);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void show(FragmentManager manager, String tag) {
            if (manager.findFragmentByTag(tag) == null) {
                // Prevent opening multiple dialogs if tapped on button quickly
                super.show(manager, tag);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SecuritySettings parentFragment = ((SecuritySettings) getParentFragment());
            final boolean compliant = getArguments().getBoolean(EXTRA_COMPLIANT);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lock_settings_profile_unification_dialog_title)
                    .setMessage(compliant ? R.string.lock_settings_profile_unification_dialog_body
                            : R.string.lock_settings_profile_unification_dialog_uncompliant_body)
                    .setPositiveButton(
                            compliant ? R.string.lock_settings_profile_unification_dialog_confirm
                            : R.string.lock_settings_profile_unification_dialog_uncompliant_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (compliant) {
                                        parentFragment.launchConfirmDeviceLockForUnification();
                                    }    else {
                                        parentFragment.unifyUncompliantLocks();
                                    }
                                }
                            }
                    )
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            ((SecuritySettings) getParentFragment()).updateUnificationPreference();
        }
    }

}
