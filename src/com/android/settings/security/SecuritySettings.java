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

package com.android.settings.security;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.enterprise.EnterprisePrivacyPreferenceController;
import com.android.settings.enterprise.ManageDeviceAdminPreferenceController;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.location.LocationPreferenceController;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ManagedLockPasswordProvider;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settings.security.trustagent.TrustAgentManager.TrustAgentComponentInfo;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, Indexable,
        GearPreference.OnGearClickListener {

    private static final String TAG = "SecuritySettings";

    private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";

    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_UNLOCK_SET_OR_CHANGE_PROFILE = "unlock_set_or_change_profile";
    private static final String KEY_VISIBLE_PATTERN_PROFILE = "visiblepattern_profile";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    @VisibleForTesting
    static final String KEY_MANAGE_TRUST_AGENTS = "manage_trust_agents";
    private static final String KEY_UNIFICATION = "unification";
    @VisibleForTesting
    static final String KEY_LOCKSCREEN_PREFERENCES = "lockscreen_preferences";
    private static final String KEY_ENCRYPTION_AND_CREDENTIALS = "encryption_and_credential";
    private static final String KEY_LOCATION_SCANNING  = "location_scanning";
    private static final String KEY_LOCATION = "location";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CHANGE_TRUST_AGENT_SETTINGS = 126;
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE = 127;
    private static final int UNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 128;
    private static final int UNIFY_LOCK_CONFIRM_PROFILE_REQUEST = 129;
    private static final int UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 130;
    private static final String TAG_UNIFICATION_DIALOG = "unification_dialog";

    // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock_settings";
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final String KEY_TRUST_AGENT = "trust_agent";
    private static final String KEY_SCREEN_PINNING = "screen_pinning_settings";

    // Security status
    private static final String KEY_SECURITY_STATUS = "security_status";
    private static final String SECURITY_STATUS_KEY_PREFIX = "security_status_";

    // Device management settings
    private static final String KEY_ENTERPRISE_PRIVACY = "enterprise_privacy";
    private static final String KEY_MANAGE_DEVICE_ADMIN = "manage_device_admin";

    // These switch preferences need special handling since they're not all stored in Settings.
    private static final String SWITCH_PREFERENCE_KEYS[] = {
            KEY_SHOW_PASSWORD, KEY_UNIFICATION, KEY_VISIBLE_PATTERN_PROFILE
    };

    private static final int MY_USER_ID = UserHandle.myUserId();

    private DashboardFeatureProvider mDashboardFeatureProvider;
    private DevicePolicyManager mDPM;
    private SecurityFeatureProvider mSecurityFeatureProvider;
    private TrustAgentManager mTrustAgentManager;
    private SubscriptionManager mSubscriptionManager;
    private UserManager mUm;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ManagedLockPasswordProvider mManagedPasswordProvider;

    private SwitchPreference mVisiblePatternProfile;
    private SwitchPreference mUnifyProfile;

    private SwitchPreference mShowPassword;

    private boolean mIsAdmin;

    private Intent mTrustAgentClickIntent;

    private int mProfileChallengeUserId;

    private String mCurrentDevicePassword;
    private String mCurrentProfilePassword;

    private LocationPreferenceController mLocationcontroller;
    private ManageDeviceAdminPreferenceController mManageDeviceAdminPreferenceController;
    private EnterprisePrivacyPreferenceController mEnterprisePrivacyPreferenceController;
    private LockScreenNotificationPreferenceController mLockScreenNotificationPreferenceController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SECURITY;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mLocationcontroller = new LocationPreferenceController(context, getLifecycle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();

        mSubscriptionManager = SubscriptionManager.from(activity);

        mLockPatternUtils = new LockPatternUtils(activity);

        mManagedPasswordProvider = ManagedLockPasswordProvider.get(activity, MY_USER_ID);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mUm = UserManager.get(activity);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(activity);

        mDashboardFeatureProvider = FeatureFactory.getFactory(activity)
                .getDashboardFeatureProvider(activity);

        mSecurityFeatureProvider = FeatureFactory.getFactory(activity).getSecurityFeatureProvider();

        mTrustAgentManager = mSecurityFeatureProvider.getTrustAgentManager();

        if (savedInstanceState != null
                && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
            mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
        }

        mManageDeviceAdminPreferenceController
                = new ManageDeviceAdminPreferenceController(activity);
        mEnterprisePrivacyPreferenceController
                = new EnterprisePrivacyPreferenceController(activity);
        mLockScreenNotificationPreferenceController
                = new LockScreenNotificationPreferenceController(activity);
    }

    private static int getResIdForLockUnlockScreen(LockPatternUtils lockPatternUtils,
            ManagedLockPasswordProvider managedPasswordProvider, int userId) {
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

        // Add category for security status
        addPreferencesFromResource(R.xml.security_settings_status);

        // Add options for lock/unlock screen
        final int resid = getResIdForLockUnlockScreen(mLockPatternUtils,
                mManagedPasswordProvider, MY_USER_ID);
        addPreferencesFromResource(resid);

        // DO or PO installed in the user may disallow to change password.
        disableIfPasswordQualityManaged(KEY_UNLOCK_SET_OR_CHANGE, MY_USER_ID);

        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, MY_USER_ID);
        if (mProfileChallengeUserId != UserHandle.USER_NULL
                && mLockPatternUtils.isSeparateProfileChallengeAllowed(mProfileChallengeUserId)) {
            addPreferencesFromResource(R.xml.security_settings_profile);
            addPreferencesFromResource(R.xml.security_settings_unification);
            final int profileResid = getResIdForLockUnlockScreen(mLockPatternUtils,
                    mManagedPasswordProvider, mProfileChallengeUserId);
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

        mIsAdmin = mUm.isAdminUser();

        // Fingerprint and trust agents
        int numberOfTrustAgent = 0;
        PreferenceGroup securityCategory = (PreferenceGroup)
                root.findPreference(KEY_SECURITY_CATEGORY);
        if (securityCategory != null) {
            maybeAddFingerprintPreference(securityCategory, UserHandle.myUserId());
            numberOfTrustAgent = addTrustAgentSettings(securityCategory);
            setLockscreenPreferencesSummary(securityCategory);
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

        // Encryption status of device
        if (LockPatternUtils.isDeviceEncryptionEnabled()) {
            root.findPreference(KEY_ENCRYPTION_AND_CREDENTIALS).setSummary(
                R.string.encryption_and_credential_settings_summary);
        } else {
            root.findPreference(KEY_ENCRYPTION_AND_CREDENTIALS).setSummary(
                R.string.summary_placeholder);
        }

        // Show password
        mShowPassword = (SwitchPreference) root.findPreference(KEY_SHOW_PASSWORD);

        // Credential storage
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);

        // Advanced Security features
        initTrustAgentPreference(root, numberOfTrustAgent);

        PreferenceGroup securityStatusPreferenceGroup =
                (PreferenceGroup) root.findPreference(KEY_SECURITY_STATUS);
        final List<Preference> tilePrefs = mDashboardFeatureProvider.getPreferencesForCategory(
            getActivity(), getPrefContext(), getMetricsCategory(),
            CategoryKey.CATEGORY_SECURITY);
        int numSecurityStatusPrefs = 0;
        if (tilePrefs != null && !tilePrefs.isEmpty()) {
            for (Preference preference : tilePrefs) {
                if (!TextUtils.isEmpty(preference.getKey())
                    && preference.getKey().startsWith(SECURITY_STATUS_KEY_PREFIX)) {
                    // Injected security status settings are placed under the Security status
                    // category.
                    securityStatusPreferenceGroup.addPreference(preference);
                    numSecurityStatusPrefs++;
                } else {
                    // Other injected settings are placed under the Security preference screen.
                    root.addPreference(preference);
                }
            }
        }

        if (numSecurityStatusPrefs == 0) {
            root.removePreference(securityStatusPreferenceGroup);
        } else if (numSecurityStatusPrefs > 0) {
            // Update preference data with tile data. Security feature provider only updates the
            // data if it actually needs to be changed.
            mSecurityFeatureProvider.updatePreferences(getActivity(), root,
                mDashboardFeatureProvider.getTilesForCategory(
                    CategoryKey.CATEGORY_SECURITY));
        }

        for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
            final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }

        mLocationcontroller.displayPreference(root);
        mManageDeviceAdminPreferenceController.updateState(
                root.findPreference(KEY_MANAGE_DEVICE_ADMIN));
        mEnterprisePrivacyPreferenceController.displayPreference(root);
        final Preference enterprisePrivacyPreference = root.findPreference(
                mEnterprisePrivacyPreferenceController.getPreferenceKey());
        mEnterprisePrivacyPreferenceController.updateState(enterprisePrivacyPreference);

        return root;
    }

    @VisibleForTesting
    void initTrustAgentPreference(PreferenceScreen root, int numberOfTrustAgent) {
        Preference manageAgents = root.findPreference(KEY_MANAGE_TRUST_AGENTS);
        if (manageAgents != null) {
            if (!mLockPatternUtils.isSecure(MY_USER_ID)) {
                manageAgents.setEnabled(false);
                manageAgents.setSummary(R.string.disabled_because_no_backup_security);
            } else if (numberOfTrustAgent > 0) {
                manageAgents.setSummary(getActivity().getResources().getQuantityString(
                    R.plurals.manage_trust_agents_summary_on,
                    numberOfTrustAgent, numberOfTrustAgent));
            } else {
                manageAgents.setSummary(R.string.manage_trust_agents_summary);
            }
        }
    }

    @VisibleForTesting
    void setLockscreenPreferencesSummary(PreferenceGroup group) {
        final Preference lockscreenPreferences = group.findPreference(KEY_LOCKSCREEN_PREFERENCES);
        if (lockscreenPreferences != null) {
            lockscreenPreferences.setSummary(
                mLockScreenNotificationPreferenceController.getSummaryResource());
        }
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

    // Return the number of trust agents being added
    private int addTrustAgentSettings(PreferenceGroup securityCategory) {
        final boolean hasSecurity = mLockPatternUtils.isSecure(MY_USER_ID);
        final List<TrustAgentComponentInfo> agents = mTrustAgentManager.getActiveTrustAgents(
                getActivity(), mLockPatternUtils);
        for (TrustAgentComponentInfo agent : agents) {
            final RestrictedPreference trustAgentPreference =
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
        return agents.size();
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

    @Override
    public void onGearClick(GearPreference p) {
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(p.getKey())) {
            startFragment(this, ScreenLockSettings.class.getName(), 0, 0, null);
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

        mLocationcontroller.updateSummary();
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
            // TODO(b/35930129): Remove once existing password can be passed into vold directly.
            // Currently we need this logic to ensure that the QUIET_MODE is off for any work
            // profile with unified challenge on FBE-enabled devices. Otherwise, vold would not be
            // able to complete the operation due to the lack of (old) encryption key.
            if (mProfileChallengeUserId != UserHandle.USER_NULL
                    && !mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId)
                    && StorageManager.isFileEncryptedNativeOnly()) {
                if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                        mProfileChallengeUserId)) {
                    return false;
                }
            }
            startFragment(this, ChooseLockGenericFragment.class.getName(),
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_UNLOCK_SET_OR_CHANGE_PROFILE.equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                    mProfileChallengeUserId)) {
                return false;
            }
            Bundle extras = new Bundle();
            extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
            startFragment(this, ChooseLockGenericFragment.class.getName(),
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
        startFragment(this, ChooseLockGenericFragment.class.getName(),
                R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
    }

    private void ununifyLocks() {
        Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
        startFragment(this,
                ChooseLockGenericFragment.class.getName(),
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

        // TODO (b/68001777) Refactor indexing to include all XML and block other settings.

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final List<SearchIndexableResource> index = new ArrayList<>();

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
                final int resId = getResIdForLockUnlockScreen(lockPatternUtils,
                        managedPasswordProvider, MY_USER_ID);
                index.add(getSearchResource(context, resId));
            }

            if (profileUserId != UserHandle.USER_NULL
                    && lockPatternUtils.isSeparateProfileChallengeAllowed(profileUserId)
                    && !isPasswordManaged(profileUserId, context, dpm)) {
                index.add(getSearchResource(context, getResIdForLockUnlockScreen(
                        lockPatternUtils, managedPasswordProvider, profileUserId)));
            }

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

            // Fingerprint
            final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(context);
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

            // Advanced
            if (lockPatternUtils.isSecure(MY_USER_ID)) {
                final TrustAgentManager trustAgentManager =
                    FeatureFactory.getFactory(context).getSecurityFeatureProvider()
                        .getTrustAgentManager();
                final List<TrustAgentComponentInfo> agents =
                        trustAgentManager.getActiveTrustAgents(context, lockPatternUtils);
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
            final List<String> keys = super.getNonIndexableKeys(context);

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);

            // Do not display SIM lock for devices without an Icc card
            final UserManager um = UserManager.get(context);
            final TelephonyManager tm = TelephonyManager.from(context);
            if (!um.isAdminUser() || !tm.hasIccCard()) {
                keys.add(KEY_SIM_LOCK);
            }

            // TrustAgent settings disappear when the user has no primary security.
            if (!lockPatternUtils.isSecure(MY_USER_ID)) {
                keys.add(KEY_TRUST_AGENT);
                keys.add(KEY_MANAGE_TRUST_AGENTS);
            }

            if (!(new EnterprisePrivacyPreferenceController(context))
                    .isAvailable()) {
                keys.add(KEY_ENTERPRISE_PRIVACY);
            }

            // Duplicate in special app access
            keys.add(KEY_MANAGE_DEVICE_ADMIN);
            // Duplicates between parent-child
            keys.add(KEY_LOCATION);
            keys.add(KEY_ENCRYPTION_AND_CREDENTIALS);
            keys.add(KEY_SCREEN_PINNING);
            keys.add(KEY_LOCATION_SCANNING);

            return keys;
        }
    }

    public static class UnificationConfirmationDialog extends InstrumentedDialogFragment {
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

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_UNIFICATION_CONFIRMATION;
        }
    }

    static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                final FingerprintManager fpm =
                    Utils.getFingerprintManagerOrNull(mContext);
                if (fpm != null && fpm.isHardwareDetected()) {
                    mSummaryLoader.setSummary(this,
                        mContext.getString(R.string.security_dashboard_summary));
                } else {
                    mSummaryLoader.setSummary(this, mContext.getString(
                        R.string.security_dashboard_summary_no_fingerprint));
                }
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY =
            new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

}
