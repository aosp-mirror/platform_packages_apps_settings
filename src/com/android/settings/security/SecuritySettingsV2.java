package com.android.settings.security;

import static com.android.settings.security.EncryptionStatusPreferenceController
        .PREF_KEY_ENCRYPTION_SECURITY_PAGE;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.enterprise.EnterprisePrivacyPreferenceController;
import com.android.settings.enterprise.ManageDeviceAdminPreferenceController;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.location.LocationPreferenceController;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ManagedLockPasswordProvider;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.security.trustagent.ManageTrustAgentsPreferenceController;
import com.android.settings.security.trustagent.TrustAgentListPreferenceController;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.List;

public class SecuritySettingsV2 extends DashboardFragment
        implements Preference.OnPreferenceChangeListener,
        GearPreference.OnGearClickListener {

    private static final String TAG = "SecuritySettingsV2";

    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_UNLOCK_SET_OR_CHANGE_PROFILE = "unlock_set_or_change_profile";
    private static final String KEY_VISIBLE_PATTERN_PROFILE = "visiblepattern_profile";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_UNIFICATION = "unification";
    @VisibleForTesting
    static final String KEY_LOCKSCREEN_PREFERENCES = "lockscreen_preferences";
    private static final String KEY_ENCRYPTION_AND_CREDENTIALS = "encryption_and_credential";
    private static final String KEY_LOCATION_SCANNING = "location_scanning";
    private static final String KEY_LOCATION = "location";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    public static final int CHANGE_TRUST_AGENT_SETTINGS = 126;
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE = 127;
    private static final int UNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 128;
    private static final int UNIFY_LOCK_CONFIRM_PROFILE_REQUEST = 129;
    private static final int UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 130;
    private static final String TAG_UNIFICATION_DIALOG = "unification_dialog";

    // Security status
    private static final String KEY_SECURITY_STATUS = "security_status";
    private static final String SECURITY_STATUS_KEY_PREFIX = "security_status_";

    // Device management settings
    private static final String KEY_ENTERPRISE_PRIVACY = "enterprise_privacy";
    private static final String KEY_MANAGE_DEVICE_ADMIN = "manage_device_admin";

    // These switch preferences need special handling since they're not all stored in Settings.
    private static final String SWITCH_PREFERENCE_KEYS[] = {
            KEY_UNIFICATION, KEY_VISIBLE_PATTERN_PROFILE
    };

    private static final int MY_USER_ID = UserHandle.myUserId();

    private DashboardFeatureProvider mDashboardFeatureProvider;
    private DevicePolicyManager mDPM;
    private SecurityFeatureProvider mSecurityFeatureProvider;
    private UserManager mUm;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ManagedLockPasswordProvider mManagedPasswordProvider;

    private SwitchPreference mVisiblePatternProfile;
    private RestrictedSwitchPreference mUnifyProfile;

    private int mProfileChallengeUserId;

    private String mCurrentDevicePassword;
    private String mCurrentProfilePassword;

    private LocationPreferenceController mLocationController;
    private ManageDeviceAdminPreferenceController mManageDeviceAdminPreferenceController;
    private EnterprisePrivacyPreferenceController mEnterprisePrivacyPreferenceController;
    private EncryptionStatusPreferenceController mEncryptionStatusPreferenceController;
    private LockScreenNotificationPreferenceController mLockScreenNotificationPreferenceController;
    private ManageTrustAgentsPreferenceController mManageTrustAgentsPreferenceController;
    private ScreenPinningPreferenceController mScreenPinningPreferenceController;
    private SimLockPreferenceController mSimLockPreferenceController;
    private ShowPasswordPreferenceController mShowPasswordPreferenceController;
    private TrustAgentListPreferenceController mTrustAgentListPreferenceController;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SECURITY;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSecurityFeatureProvider = FeatureFactory.getFactory(context).getSecurityFeatureProvider();
        mLocationController = new LocationPreferenceController(context, getLifecycle());
        mLockPatternUtils = mSecurityFeatureProvider.getLockPatternUtils(context);
        mManagedPasswordProvider = ManagedLockPasswordProvider.get(context, MY_USER_ID);
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUm = UserManager.get(context);
        mDashboardFeatureProvider = FeatureFactory.getFactory(context)
                .getDashboardFeatureProvider(context);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_settings_v2;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        mManageDeviceAdminPreferenceController
                = new ManageDeviceAdminPreferenceController(context);
        mEnterprisePrivacyPreferenceController
                = new EnterprisePrivacyPreferenceController(context);
        mLockScreenNotificationPreferenceController
                = new LockScreenNotificationPreferenceController(context);
        mManageTrustAgentsPreferenceController = new ManageTrustAgentsPreferenceController(context);
        mScreenPinningPreferenceController = new ScreenPinningPreferenceController(context);
        mSimLockPreferenceController = new SimLockPreferenceController(context);
        mShowPasswordPreferenceController = new ShowPasswordPreferenceController(context);
        mEncryptionStatusPreferenceController = new EncryptionStatusPreferenceController(
                context, PREF_KEY_ENCRYPTION_SECURITY_PAGE);
        mTrustAgentListPreferenceController = new TrustAgentListPreferenceController(getActivity(),
                this /* host */, getLifecycle());
        return null;
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
        addPreferencesFromResource(R.xml.security_settings_v2);
        root = getPreferenceScreen();
        mTrustAgentListPreferenceController.displayPreference(root);

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


        // Fingerprint and trust agents
        PreferenceGroup securityCategory = (PreferenceGroup)
                root.findPreference(KEY_SECURITY_CATEGORY);
        if (securityCategory != null) {
            maybeAddFingerprintPreference(securityCategory, UserHandle.myUserId());
            setLockscreenPreferencesSummary(securityCategory);
        }

        mVisiblePatternProfile =
                (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN_PROFILE);
        mUnifyProfile = (RestrictedSwitchPreference) root.findPreference(KEY_UNIFICATION);

        mSimLockPreferenceController.displayPreference(root);
        mScreenPinningPreferenceController.displayPreference(root);

        // Advanced Security features
        mManageTrustAgentsPreferenceController.displayPreference(root);

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

        mLocationController.displayPreference(root);
        mManageDeviceAdminPreferenceController.updateState(
                root.findPreference(KEY_MANAGE_DEVICE_ADMIN));
        mEnterprisePrivacyPreferenceController.displayPreference(root);
        final Preference enterprisePrivacyPreference = root.findPreference(
                mEnterprisePrivacyPreferenceController.getPreferenceKey());
        mEnterprisePrivacyPreferenceController.updateState(enterprisePrivacyPreference);

        return root;
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
        final RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtils.checkIfPasswordQualityIsSet(
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

    @Override
    public void onGearClick(GearPreference p) {
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(p.getKey())) {
            startFragment(this, ScreenLockSettings.class.getName(), 0, 0, null);
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

        final Preference showPasswordPref = getPreferenceScreen().findPreference(
                mShowPasswordPreferenceController.getPreferenceKey());
        showPasswordPref.setOnPreferenceChangeListener(mShowPasswordPreferenceController);
        mShowPasswordPreferenceController.updateState(showPasswordPref);

        final Preference encryptionStatusPref = getPreferenceScreen().findPreference(
                mEncryptionStatusPreferenceController.getPreferenceKey());
        mEncryptionStatusPreferenceController.updateState(encryptionStatusPref);
        mTrustAgentListPreferenceController.onResume();
        mLocationController.updateSummary();
    }

    @VisibleForTesting
    void updateUnificationPreference() {
        if (mUnifyProfile != null) {
            final boolean separate =
                    mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId);
            mUnifyProfile.setChecked(!separate);
            if (separate) {
                mUnifyProfile.setDisabledByAdmin(RestrictedLockUtils.checkIfRestrictionEnforced(
                        getContext(), UserManager.DISALLOW_UNIFIED_PASSWORD,
                        mProfileChallengeUserId));
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mTrustAgentListPreferenceController.handlePreferenceTreeClick(preference)) {
            return true;
        }
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
            startFragment(this, ChooseLockGeneric.ChooseLockGenericFragment.class.getName(),
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_UNLOCK_SET_OR_CHANGE_PROFILE.equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                    mProfileChallengeUserId)) {
                return false;
            }
            Bundle extras = new Bundle();
            extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
            startFragment(this, ChooseLockGeneric.ChooseLockGenericFragment.class.getName(),
                    R.string.lock_settings_picker_title_profile,
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE, extras);
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
            mTrustAgentListPreferenceController.handleActivityResult(resultCode);
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

    void launchConfirmDeviceLockForUnification() {
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

    void unifyUncompliantLocks() {
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileChallengeUserId, false,
                mCurrentProfilePassword);
        startFragment(this, ChooseLockGeneric.ChooseLockGenericFragment.class.getName(),
                R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
    }

    private void ununifyLocks() {
        Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
        startFragment(this,
                ChooseLockGeneric.ChooseLockGenericFragment.class.getName(),
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
                if (!helper.launchConfirmationActivity(
                        UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST, title, true, MY_USER_ID)) {
                    ununifyLocks();
                }
            }
        }
        return result;
    }

    @Override
    public int getHelpResource() {
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
            final RestrictedLockUtils.EnforcedAdmin admin =
                    RestrictedLockUtils.checkIfPasswordQualityIsSet(
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
            data.key = "security_settings_screen";
            data.screenTitle = screenTitle;
            result.add(data);

            final UserManager um = UserManager.get(context);

            // Fingerprint
            final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(context);
            if (fpm != null && fpm.isHardwareDetected()) {
                // This catches the title which can be overloaded in an overlay
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.security_settings_fingerprint_preference_title);
                data.key = "security_fingerprint";
                data.screenTitle = screenTitle;
                result.add(data);
                // Fallback for when the above doesn't contain "fingerprint"
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.fingerprint_manage_category_title);
                data.key = "security_managed_fingerprint";
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
                    data.key = "security_use_one_lock";
                    data.screenTitle = screenTitle;
                    result.add(data);
                }
            }
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = super.getNonIndexableKeys(context);

            new SimLockPreferenceController(context).updateNonIndexableKeys(keys);

            if (!(new EnterprisePrivacyPreferenceController(context))
                    .isAvailable()) {
                keys.add(KEY_ENTERPRISE_PRIVACY);
            }

            // Duplicate in special app access
            keys.add(KEY_MANAGE_DEVICE_ADMIN);
            // Duplicates between parent-child
            keys.add(KEY_LOCATION);
            keys.add(KEY_ENCRYPTION_AND_CREDENTIALS);
            keys.add(KEY_LOCATION_SCANNING);

            return keys;
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
