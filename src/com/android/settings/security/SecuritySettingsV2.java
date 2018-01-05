package com.android.settings.security;

import static com.android.settings.security.EncryptionStatusPreferenceController.PREF_KEY_ENCRYPTION_SECURITY_PAGE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.enterprise.EnterprisePrivacyPreferenceController;
import com.android.settings.enterprise.ManageDeviceAdminPreferenceController;
import com.android.settings.location.LocationPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.trustagent.ManageTrustAgentsPreferenceController;
import com.android.settings.security.trustagent.TrustAgentListPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class SecuritySettingsV2 extends DashboardFragment {

    private static final String TAG = "SecuritySettingsV2";

    public static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    public static final int CHANGE_TRUST_AGENT_SETTINGS = 126;
    public static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE = 127;
    public static final int UNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 128;
    public static final int UNIFY_LOCK_CONFIRM_PROFILE_REQUEST = 129;
    public static final int UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 130;


    // Security status
    private static final String KEY_SECURITY_STATUS = "security_status";
    private static final String SECURITY_STATUS_KEY_PREFIX = "security_status_";

    private static final int MY_USER_ID = UserHandle.myUserId();

    private DashboardFeatureProvider mDashboardFeatureProvider;
    private SecurityFeatureProvider mSecurityFeatureProvider;
    private UserManager mUm;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;

    private int mProfileChallengeUserId;

    private LocationPreferenceController mLocationController;
    private ManageDeviceAdminPreferenceController mManageDeviceAdminPreferenceController;
    private EnterprisePrivacyPreferenceController mEnterprisePrivacyPreferenceController;
    private EncryptionStatusPreferenceController mEncryptionStatusPreferenceController;
    private ManageTrustAgentsPreferenceController mManageTrustAgentsPreferenceController;
    private ScreenPinningPreferenceController mScreenPinningPreferenceController;
    private SimLockPreferenceController mSimLockPreferenceController;
    private ShowPasswordPreferenceController mShowPasswordPreferenceController;
    private TrustAgentListPreferenceController mTrustAgentListPreferenceController;
    private LockScreenPreferenceController mLockScreenPreferenceController;
    private ChangeScreenLockPreferenceController mChangeScreenLockPreferenceController;
    private ChangeProfileScreenLockPreferenceController
            mChangeProfileScreenLockPreferenceController;
    private LockUnificationPreferenceController mLockUnificationPreferenceController;
    private VisiblePatternProfilePreferenceController mVisiblePatternProfilePreferenceController;
    private FingerprintStatusPreferenceController mFingerprintStatusPreferenceController;
    private FingerprintProfileStatusPreferenceController
            mFingerprintProfileStatusPreferenceController;

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
    public int getHelpResource() {
        return R.string.help_url_security;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        mManageDeviceAdminPreferenceController
                = new ManageDeviceAdminPreferenceController(context);
        mEnterprisePrivacyPreferenceController
                = new EnterprisePrivacyPreferenceController(context);
        mManageTrustAgentsPreferenceController = new ManageTrustAgentsPreferenceController(context);
        mScreenPinningPreferenceController = new ScreenPinningPreferenceController(context);
        mSimLockPreferenceController = new SimLockPreferenceController(context);
        mShowPasswordPreferenceController = new ShowPasswordPreferenceController(context);
        mEncryptionStatusPreferenceController = new EncryptionStatusPreferenceController(
                context, PREF_KEY_ENCRYPTION_SECURITY_PAGE);
        mTrustAgentListPreferenceController = new TrustAgentListPreferenceController(getActivity(),
                this /* host */, getLifecycle());
        mLockScreenPreferenceController = new LockScreenPreferenceController(context);
        mChangeScreenLockPreferenceController = new ChangeScreenLockPreferenceController(context,
                this /* host */);
        mChangeProfileScreenLockPreferenceController =
                new ChangeProfileScreenLockPreferenceController(context, this /* host */);
        mLockUnificationPreferenceController = new LockUnificationPreferenceController(context,
                this /* host */);
        mVisiblePatternProfilePreferenceController =
                new VisiblePatternProfilePreferenceController(context);
        mFingerprintStatusPreferenceController = new FingerprintStatusPreferenceController(context);
        mFingerprintProfileStatusPreferenceController =
                new FingerprintProfileStatusPreferenceController(context);
        return null;
    }

    /**
     * Important!
     *
     * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
     * logic or adding/removing preferences here.
     */
    private PreferenceScreen createPreferenceHierarchy() {
        final PreferenceScreen root = getPreferenceScreen();
        mTrustAgentListPreferenceController.displayPreference(root);
        mLockScreenPreferenceController.displayPreference(root);
        mChangeScreenLockPreferenceController.displayPreference(root);
        mChangeProfileScreenLockPreferenceController.displayPreference(root);
        mLockUnificationPreferenceController.displayPreference(root);
        mVisiblePatternProfilePreferenceController.displayPreference(root);
        mFingerprintStatusPreferenceController.displayPreference(root);
        mFingerprintProfileStatusPreferenceController.displayPreference(root);

        mSimLockPreferenceController.displayPreference(root);
        mScreenPinningPreferenceController.displayPreference(root);

        // Advanced Security features
        mManageTrustAgentsPreferenceController.displayPreference(root);

//        PreferenceGroup securityStatusPreferenceGroup =
//                (PreferenceGroup) root.findPreference(KEY_SECURITY_STATUS);
//        final List<Preference> tilePrefs = mDashboardFeatureProvider.getPreferencesForCategory(
//                getActivity(), getPrefContext(), getMetricsCategory(),
//                CategoryKey.CATEGORY_SECURITY);
//        int numSecurityStatusPrefs = 0;
//        if (tilePrefs != null && !tilePrefs.isEmpty()) {
//            for (Preference preference : tilePrefs) {
//                if (!TextUtils.isEmpty(preference.getKey())
//                        && preference.getKey().startsWith(SECURITY_STATUS_KEY_PREFIX)) {
//                    // Injected security status settings are placed under the Security status
//                    // category.
//                    securityStatusPreferenceGroup.addPreference(preference);
//                    numSecurityStatusPrefs++;
//                } else {
//                    // Other injected settings are placed under the Security preference screen.
//                    root.addPreference(preference);
//                }
//            }
//        }
//
//        if (numSecurityStatusPrefs == 0) {
//            root.removePreference(securityStatusPreferenceGroup);
//        } else if (numSecurityStatusPrefs > 0) {
//            // Update preference data with tile data. Security feature provider only updates the
//            // data if it actually needs to be changed.
//            mSecurityFeatureProvider.updatePreferences(getActivity(), root,
//                    mDashboardFeatureProvider.getTilesForCategory(
//                            CategoryKey.CATEGORY_SECURITY));
//        }

        mLocationController.displayPreference(root);
        mManageDeviceAdminPreferenceController.updateState(
                root.findPreference(mManageDeviceAdminPreferenceController.getPreferenceKey()));
        mEnterprisePrivacyPreferenceController.displayPreference(root);
        final Preference enterprisePrivacyPreference = root.findPreference(
                mEnterprisePrivacyPreferenceController.getPreferenceKey());
        mEnterprisePrivacyPreferenceController.updateState(enterprisePrivacyPreference);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        final Preference visiblePatternProfilePref = getPreferenceScreen().findPreference(
                mVisiblePatternProfilePreferenceController.getPreferenceKey());
        if (visiblePatternProfilePref != null) {
            visiblePatternProfilePref
                    .setOnPreferenceChangeListener(mVisiblePatternProfilePreferenceController);
            mVisiblePatternProfilePreferenceController.updateState(visiblePatternProfilePref);
        }

        final Preference showPasswordPref = getPreferenceScreen().findPreference(
                mShowPasswordPreferenceController.getPreferenceKey());
        showPasswordPref.setOnPreferenceChangeListener(mShowPasswordPreferenceController);
        mShowPasswordPreferenceController.updateState(showPasswordPref);

        final Preference lockUnificationPref = getPreferenceScreen().findPreference(
                mLockUnificationPreferenceController.getPreferenceKey());
        lockUnificationPref.setOnPreferenceChangeListener(mLockUnificationPreferenceController);
        mLockUnificationPreferenceController.updateState(lockUnificationPref);

        final Preference changeDeviceLockPref = getPreferenceScreen().findPreference(
                mChangeScreenLockPreferenceController.getPreferenceKey());
        mChangeScreenLockPreferenceController.updateState(changeDeviceLockPref);

        mFingerprintStatusPreferenceController.updateState(
                getPreferenceScreen().findPreference(
                        mFingerprintStatusPreferenceController.getPreferenceKey()));

        mFingerprintProfileStatusPreferenceController.updateState(
                getPreferenceScreen().findPreference(
                        mFingerprintProfileStatusPreferenceController.getPreferenceKey()));

        final Preference changeProfileLockPref = getPreferenceScreen().findPreference(
                mChangeProfileScreenLockPreferenceController.getPreferenceKey());
        mChangeProfileScreenLockPreferenceController.updateState(changeProfileLockPref);

        final Preference encryptionStatusPref = getPreferenceScreen().findPreference(
                mEncryptionStatusPreferenceController.getPreferenceKey());
        mEncryptionStatusPreferenceController.updateState(encryptionStatusPref);
        mTrustAgentListPreferenceController.onResume();
        mLocationController.updateSummary();
    }


    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mTrustAgentListPreferenceController.handlePreferenceTreeClick(preference)) {
            return true;
        }
        if (mChangeScreenLockPreferenceController.handlePreferenceTreeClick(preference)) {
            return true;
        }
        if (mChangeProfileScreenLockPreferenceController.handlePreferenceTreeClick(preference)) {
            return true;
        }
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preference);
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mTrustAgentListPreferenceController.handleActivityResult(requestCode, resultCode)) {
            return;
        }
        if (mLockUnificationPreferenceController.handleActivityResult(
                requestCode, resultCode, data)) {
            return;
        }
        createPreferenceHierarchy();
    }

    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SecuritySearchIndexProvider();

    void launchConfirmDeviceLockForUnification() {
        mLockUnificationPreferenceController.launchConfirmDeviceLockForUnification();
    }

    void unifyUncompliantLocks() {
        mLockUnificationPreferenceController.unifyUncompliantLocks();
    }

    void updateUnificationPreference() {
        mLockUnificationPreferenceController.updateState(null);
    }

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {

        // TODO (b/68001777) Refactor indexing to include all XML and block other settings.

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final List<SearchIndexableResource> index = new ArrayList<>();
            // Append the rest of the settings
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.security_settings_v2;
            index.add(sir);
            return index;
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
