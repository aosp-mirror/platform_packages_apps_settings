/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;


import static android.app.admin.DevicePolicyResources.Strings.Settings.FINGERPRINT_UNLOCK_DISABLED_EXPLANATION;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_FINGERPRINT_LAST_DELETE_MESSAGE;
import static android.app.admin.DevicePolicyResources.UNDEFINED;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY;

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImeAwareEditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.transition.SettingsTransitionHelper;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.TwoTargetPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Settings screen for fingerprints
 */
public class FingerprintSettings extends SubSettings {

    private static final String TAG = "FingerprintSettings";

    private static final long LOCKOUT_DURATION = 30000; // time we have to wait for fp to reset, ms

    public static final String ANNOTATION_URL = "url";
    public static final String ANNOTATION_ADMIN_DETAILS = "admin_details";

    private static final int RESULT_FINISHED = BiometricEnrollBase.RESULT_FINISHED;
    private static final int RESULT_SKIP = BiometricEnrollBase.RESULT_SKIP;
    private static final int RESULT_TIMEOUT = BiometricEnrollBase.RESULT_TIMEOUT;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintSettingsFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.security_settings_fingerprint_preference_title);
        setTitle(msg);
    }

    /**
     * @param context
     * @return true if the Fingerprint hardware is detected.
     */
    public static boolean isFingerprintHardwareDetected(Context context) {
        FingerprintManager manager = Utils.getFingerprintManagerOrNull(context);
        boolean isHardwareDetected = false;
        if (manager == null) {
            Log.d(TAG, "FingerprintManager is null");
        } else {
            isHardwareDetected = manager.isHardwareDetected();
            Log.d(TAG, "FingerprintManager is not null. Hardware detected: " + isHardwareDetected);
        }
        return manager != null && isHardwareDetected;
    }

    /**
     *
     */
    public static class FingerprintSettingsFragment extends DashboardFragment
            implements OnPreferenceChangeListener, FingerprintPreference.OnDeleteClickListener {

        private static class FooterColumn {
            CharSequence mTitle = null;
            CharSequence mLearnMoreOverrideText = null;
            View.OnClickListener mLearnMoreClickListener = null;
        }

        private static final int RESET_HIGHLIGHT_DELAY_MS = 500;

        private static final String TAG = "FingerprintSettings";
        private static final String KEY_FINGERPRINT_ITEM_PREFIX = "key_fingerprint_item";
        private static final String KEY_FINGERPRINT_ADD = "key_fingerprint_add";
        private static final String KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE =
                "fingerprint_enable_keyguard_toggle";
        private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";
        private static final String KEY_HAS_FIRST_ENROLLED = "has_first_enrolled";
        private static final String KEY_IS_ENROLLING = "is_enrolled";
        private static final String KEY_REQUIRE_SCREEN_ON_TO_AUTH =
                "security_settings_require_screen_on_to_auth";
        private static final String KEY_FINGERPRINT_UNLOCK_CATEGORY =
                "security_settings_fingerprint_unlock_category";

        private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
        private static final int MSG_FINGER_AUTH_SUCCESS = 1001;
        private static final int MSG_FINGER_AUTH_FAIL = 1002;
        private static final int MSG_FINGER_AUTH_ERROR = 1003;
        private static final int MSG_FINGER_AUTH_HELP = 1004;

        private static final int CONFIRM_REQUEST = 101;
        private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

        private static final int ADD_FINGERPRINT_REQUEST = 10;
        private static final int AUTO_ADD_FIRST_FINGERPRINT_REQUEST = 11;

        protected static final boolean DEBUG = false;

        private List<AbstractPreferenceController> mControllers;
        private FingerprintSettingsRequireScreenOnToAuthPreferenceController
                mRequireScreenOnToAuthPreferenceController;
        private RestrictedSwitchPreference mRequireScreenOnToAuthPreference;
        private PreferenceCategory mFingerprintUnlockCategory;

        private FingerprintManager mFingerprintManager;
        private FingerprintUpdater mFingerprintUpdater;
        private List<FingerprintSensorPropertiesInternal> mSensorProperties;
        private boolean mInFingerprintLockout;
        private byte[] mToken;
        private boolean mLaunchedConfirm;
        private boolean mHasFirstEnrolled = true;
        private Drawable mHighlightDrawable;
        private int mUserId;
        private final List<FooterColumn> mFooterColumns = new ArrayList<>();
        private boolean mIsEnrolling;

        private long mChallenge;

        private static final String TAG_AUTHENTICATE_SIDECAR = "authenticate_sidecar";
        private static final String TAG_REMOVAL_SIDECAR = "removal_sidecar";
        private FingerprintAuthenticateSidecar mAuthenticateSidecar;
        private FingerprintRemoveSidecar mRemovalSidecar;
        private HashMap<Integer, String> mFingerprintsRenaming;

        FingerprintAuthenticateSidecar.Listener mAuthenticateListener =
                new FingerprintAuthenticateSidecar.Listener() {
                    @Override
                    public void onAuthenticationSucceeded(
                            FingerprintManager.AuthenticationResult result) {
                        int fingerId = result.getFingerprint().getBiometricId();
                        mHandler.obtainMessage(MSG_FINGER_AUTH_SUCCESS, fingerId, 0).sendToTarget();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        mHandler.obtainMessage(MSG_FINGER_AUTH_FAIL).sendToTarget();
                    }

                    @Override
                    public void onAuthenticationError(int errMsgId, CharSequence errString) {
                        mHandler.obtainMessage(MSG_FINGER_AUTH_ERROR, errMsgId, 0, errString)
                                .sendToTarget();
                    }

                    @Override
                    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                        mHandler.obtainMessage(MSG_FINGER_AUTH_HELP, helpMsgId, 0, helpString)
                                .sendToTarget();
                    }
                };

        FingerprintRemoveSidecar.Listener mRemovalListener =
                new FingerprintRemoveSidecar.Listener() {
                    public void onRemovalSucceeded(Fingerprint fingerprint) {
                        mHandler.obtainMessage(MSG_REFRESH_FINGERPRINT_TEMPLATES,
                                fingerprint.getBiometricId(), 0).sendToTarget();
                        updateDialog();
                    }

                    public void onRemovalError(Fingerprint fp, int errMsgId,
                            CharSequence errString) {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            Toast.makeText(activity, errString, Toast.LENGTH_SHORT);
                        }
                        updateDialog();
                    }

                    private void updateDialog() {
                        if (isSfps()) {
                            setRequireScreenOnToAuthVisibility();
                        }
                        RenameDialog renameDialog = (RenameDialog) getFragmentManager().
                                findFragmentByTag(RenameDialog.class.getName());
                        if (renameDialog != null) {
                            renameDialog.enableDelete();
                        }
                    }
                };

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH_FINGERPRINT_TEMPLATES:
                        removeFingerprintPreference(msg.arg1);
                        updateAddPreference();
                        retryFingerprint();
                        break;
                    case MSG_FINGER_AUTH_SUCCESS:
                        highlightFingerprintItem(msg.arg1);
                        retryFingerprint();
                        break;
                    case MSG_FINGER_AUTH_FAIL:
                        // No action required... fingerprint will allow up to 5 of these
                        break;
                    case MSG_FINGER_AUTH_ERROR:
                        handleError(msg.arg1 /* errMsgId */, (CharSequence) msg.obj /* errStr */);
                        break;
                    case MSG_FINGER_AUTH_HELP: {
                        // Not used
                    }
                    break;
                }
            }
        };

        /**
         *
         */
        protected void handleError(int errMsgId, CharSequence msg) {
            switch (errMsgId) {
                case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                case FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED:
                    // Only happens if we get preempted by another activity, or canceled by the
                    // user (e.g. swipe up to home). Ignored.
                    return;
                case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                    mInFingerprintLockout = true;
                    // We've been locked out.  Reset after 30s.
                    if (!mHandler.hasCallbacks(mFingerprintLockoutReset)) {
                        mHandler.postDelayed(mFingerprintLockoutReset,
                                LOCKOUT_DURATION);
                    }
                    break;
                case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT:
                    mInFingerprintLockout = true;
                    break;
            }

            if (mInFingerprintLockout) {
                // Activity can be null on a screen rotation.
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                }
            }
            retryFingerprint(); // start again
        }

        private void retryFingerprint() {
            if (isUdfps()) {
                // Do not authenticate for UDFPS devices.
                return;
            }

            if (mRemovalSidecar.inProgress()
                    || 0 == mFingerprintManager.getEnrolledFingerprints(mUserId).size()) {
                return;
            }
            // Don't start authentication if ChooseLockGeneric is showing, otherwise if the user
            // is in FP lockout, a toast will show on top
            if (mLaunchedConfirm) {
                return;
            }
            if (!mInFingerprintLockout) {
                mAuthenticateSidecar.startAuthentication(mUserId);
                mAuthenticateSidecar.setListener(mAuthenticateListener);
            }
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.FINGERPRINT;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Activity activity = getActivity();
            mFingerprintManager = Utils.getFingerprintManagerOrNull(activity);
            mFingerprintUpdater = new FingerprintUpdater(activity, mFingerprintManager);
            mSensorProperties = mFingerprintManager.getSensorPropertiesInternal();

            mToken = getIntent().getByteArrayExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
            mChallenge = activity.getIntent()
                    .getLongExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, -1L);

            mAuthenticateSidecar = (FingerprintAuthenticateSidecar)
                    getFragmentManager().findFragmentByTag(TAG_AUTHENTICATE_SIDECAR);
            if (mAuthenticateSidecar == null) {
                mAuthenticateSidecar = new FingerprintAuthenticateSidecar();
                getFragmentManager().beginTransaction()
                        .add(mAuthenticateSidecar, TAG_AUTHENTICATE_SIDECAR).commit();
            }
            mAuthenticateSidecar.setFingerprintManager(mFingerprintManager);

            mRemovalSidecar = (FingerprintRemoveSidecar)
                    getFragmentManager().findFragmentByTag(TAG_REMOVAL_SIDECAR);
            if (mRemovalSidecar == null) {
                mRemovalSidecar = new FingerprintRemoveSidecar();
                getFragmentManager().beginTransaction()
                        .add(mRemovalSidecar, TAG_REMOVAL_SIDECAR).commit();
            }
            mRemovalSidecar.setFingerprintUpdater(mFingerprintUpdater);
            mRemovalSidecar.setListener(mRemovalListener);

            RenameDialog renameDialog = (RenameDialog) getFragmentManager().
                    findFragmentByTag(RenameDialog.class.getName());
            if (renameDialog != null) {
                renameDialog.setDeleteInProgress(mRemovalSidecar.inProgress());
            }

            mFingerprintsRenaming = new HashMap<Integer, String>();
            mUserId = getActivity().getIntent().getIntExtra(
                    Intent.EXTRA_USER_ID, UserHandle.myUserId());
            mHasFirstEnrolled = mFingerprintManager.hasEnrolledFingerprints(mUserId);

            if (savedInstanceState != null) {
                mFingerprintsRenaming = (HashMap<Integer, String>)
                        savedInstanceState.getSerializable("mFingerprintsRenaming");
                mToken = savedInstanceState.getByteArray(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                mLaunchedConfirm = savedInstanceState.getBoolean(
                        KEY_LAUNCHED_CONFIRM, false);
                mIsEnrolling = savedInstanceState.getBoolean(KEY_IS_ENROLLING, mIsEnrolling);
                mHasFirstEnrolled = savedInstanceState.getBoolean(KEY_HAS_FIRST_ENROLLED,
                        mHasFirstEnrolled);
            }

            // (mLaunchedConfirm or mIsEnrolling) means that we are waiting an activity result.
            if (!mLaunchedConfirm && !mIsEnrolling) {
                // Need to authenticate a session token if none
                if (mToken == null) {
                    mLaunchedConfirm = true;
                    launchChooseOrConfirmLock();
                } else if (!mHasFirstEnrolled) {
                    mIsEnrolling = true;
                    addFirstFingerprint();
                }
            }
            updateFooterColumns(activity);
        }

        private void updateFooterColumns(@NonNull Activity activity) {
            final EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                    activity, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, mUserId);
            final Intent helpIntent = HelpUtils.getHelpIntent(
                    activity, getString(getHelpResource()), activity.getClass().getName());
            final View.OnClickListener learnMoreClickListener = (v) ->
                    activity.startActivityForResult(helpIntent, 0);

            mFooterColumns.clear();
            if (admin != null) {
                final DevicePolicyManager devicePolicyManager =
                        getSystemService(DevicePolicyManager.class);
                final FooterColumn column1 = new FooterColumn();
                column1.mTitle = devicePolicyManager.getResources().getString(
                        FINGERPRINT_UNLOCK_DISABLED_EXPLANATION,
                        () -> getString(
                                R.string.security_fingerprint_disclaimer_lockscreen_disabled_1
                        )
                );
                column1.mLearnMoreClickListener = (v) -> RestrictedLockUtils
                        .sendShowAdminSupportDetailsIntent(activity, admin);
                column1.mLearnMoreOverrideText = getText(R.string.admin_support_more_info);
                mFooterColumns.add(column1);

                final FooterColumn column2 = new FooterColumn();
                column2.mTitle = getText(
                        R.string.security_fingerprint_disclaimer_lockscreen_disabled_2
                );
                column2.mLearnMoreClickListener = learnMoreClickListener;
                mFooterColumns.add(column2);
            } else {
                final FooterColumn column = new FooterColumn();
                column.mTitle = getText(
                        R.string.security_settings_fingerprint_enroll_introduction_v2_message);
                column.mLearnMoreClickListener = learnMoreClickListener;
                mFooterColumns.add(column);
            }
        }

        private boolean isUdfps() {
            for (FingerprintSensorPropertiesInternal prop : mSensorProperties) {
                if (prop.isAnyUdfpsType()) {
                    return true;
                }
            }
            return false;
        }

        private boolean isSfps() {
            for (FingerprintSensorPropertiesInternal prop : mSensorProperties) {
                if (prop.isAnySidefpsType()) {
                    return true;
                }
            }
            return false;
        }

        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = findPreference(name);
            if (prefToRemove != null) {
                if (!getPreferenceScreen().removePreference(prefToRemove)) {
                    Log.w(TAG, "Failed to remove preference with key " + name);
                }
            } else {
                Log.w(TAG, "Can't find preference to remove: " + name);
            }
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
            root = getPreferenceScreen();
            addFingerprintItemPreferences(root);
            addPreferencesFromResource(getPreferenceScreenResId());
            mRequireScreenOnToAuthPreference = findPreference(KEY_REQUIRE_SCREEN_ON_TO_AUTH);
            mFingerprintUnlockCategory = findPreference(KEY_FINGERPRINT_UNLOCK_CATEGORY);
            for (AbstractPreferenceController controller : mControllers) {
                ((FingerprintSettingsPreferenceController) controller).setUserId(mUserId);
            }
            mRequireScreenOnToAuthPreference.setChecked(
                    mRequireScreenOnToAuthPreferenceController.isChecked());
            mRequireScreenOnToAuthPreference.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = ((SwitchPreference) preference).isChecked();
                        mRequireScreenOnToAuthPreferenceController.setChecked(!isChecked);
                        return true;
                    });
            mFingerprintUnlockCategory.setVisible(false);
            if (isSfps()) {
                setRequireScreenOnToAuthVisibility();
            }
            setPreferenceScreen(root);
            return root;
        }

        private void setRequireScreenOnToAuthVisibility() {
            int fingerprintsEnrolled = mFingerprintManager.getEnrolledFingerprints(mUserId).size();
            final boolean removalInProgress = mRemovalSidecar.inProgress();
            // Removing last remaining fingerprint
            if (fingerprintsEnrolled == 0 && removalInProgress) {
                mFingerprintUnlockCategory.setVisible(false);
            } else {
                mFingerprintUnlockCategory.setVisible(true);
            }
        }

        private void addFingerprintItemPreferences(PreferenceGroup root) {
            root.removeAll();
            final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints(mUserId);
            final int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                final Fingerprint item = items.get(i);
                FingerprintPreference pref = new FingerprintPreference(root.getContext(),
                        this /* onDeleteClickListener */);
                pref.setKey(genKey(item.getBiometricId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                pref.setIcon(R.drawable.ic_fingerprint_24dp);
                if (mRemovalSidecar.isRemovingFingerprint(item.getBiometricId())) {
                    pref.setEnabled(false);
                }
                if (mFingerprintsRenaming.containsKey(item.getBiometricId())) {
                    pref.setTitle(mFingerprintsRenaming.get(item.getBiometricId()));
                }
                root.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }

            Preference addPreference = new Preference(root.getContext());
            addPreference.setKey(KEY_FINGERPRINT_ADD);
            addPreference.setTitle(R.string.fingerprint_add_title);
            addPreference.setIcon(R.drawable.ic_add_24dp);
            root.addPreference(addPreference);
            addPreference.setOnPreferenceChangeListener(this);
            updateAddPreference();
            createFooterPreference(root);
        }

        private void updateAddPreference() {
            if (getActivity() == null) return; // Activity went away

            final Preference addPreference = findPreference(KEY_FINGERPRINT_ADD);

            /* Disable preference if too many fingerprints added */
            final int max = getContext().getResources().getInteger(
                    com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
            boolean tooMany = mFingerprintManager.getEnrolledFingerprints(mUserId).size() >= max;
            // retryFingerprint() will be called when remove finishes
            // need to disable enroll or have a way to determine if enroll is in progress
            final boolean removalInProgress = mRemovalSidecar.inProgress();
            CharSequence maxSummary = tooMany ?
                    getContext().getString(R.string.fingerprint_add_max, max) : "";
            addPreference.setSummary(maxSummary);
            addPreference.setEnabled(!tooMany && !removalInProgress && mToken != null);
        }

        private void createFooterPreference(PreferenceGroup root) {
            final Context context = getActivity();
            if (context == null) {
                return;
            }
            for (int i = 0; i < mFooterColumns.size(); ++i) {
                final FooterColumn column = mFooterColumns.get(i);
                final FooterPreference footer = new FooterPreference.Builder(context)
                        .setTitle(column.mTitle).build();
                if (i > 0) {
                    footer.setIconVisibility(View.GONE);
                }
                if (column.mLearnMoreClickListener != null) {
                    footer.setLearnMoreAction(column.mLearnMoreClickListener);
                    if (!TextUtils.isEmpty(column.mLearnMoreOverrideText)) {
                        footer.setLearnMoreText(column.mLearnMoreOverrideText);
                    }
                }
                root.addPreference(footer);
            }
        }

        private static String genKey(int id) {
            return KEY_FINGERPRINT_ITEM_PREFIX + "_" + id;
        }

        @Override
        public void onResume() {
            super.onResume();
            mInFingerprintLockout = false;
            // Make sure we reload the preference hierarchy since fingerprints may be added,
            // deleted or renamed.
            updatePreferences();
            if (mRemovalSidecar != null) {
                mRemovalSidecar.setListener(mRemovalListener);
            }
        }

        private void updatePreferences() {
            createPreferenceHierarchy();
            retryFingerprint();
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mRemovalSidecar != null) {
                mRemovalSidecar.setListener(null);
            }
            if (mAuthenticateSidecar != null) {
                mAuthenticateSidecar.setListener(null);
                mAuthenticateSidecar.stopAuthentication();
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            if (!getActivity().isChangingConfigurations() && !mLaunchedConfirm && !mIsEnrolling) {
                getActivity().finish();
            }
        }

        @Override
        protected int getPreferenceScreenResId() {
            return R.xml.security_settings_fingerprint;
        }

        @Override
        protected String getLogTag() {
            return TAG;
        }

        @Override
        public void onSaveInstanceState(final Bundle outState) {
            outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                    mToken);
            outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
            outState.putSerializable("mFingerprintsRenaming", mFingerprintsRenaming);
            outState.putBoolean(KEY_IS_ENROLLING, mIsEnrolling);
            outState.putBoolean(KEY_HAS_FIRST_ENROLLED, mHasFirstEnrolled);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference pref) {
            final String key = pref.getKey();
            if (KEY_FINGERPRINT_ADD.equals(key)) {
                mIsEnrolling = true;
                Intent intent = new Intent();
                intent.setClassName(SETTINGS_PACKAGE_NAME,
                        FingerprintEnrollEnrolling.class.getName());
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startActivityForResult(intent, ADD_FINGERPRINT_REQUEST);
            } else if (pref instanceof FingerprintPreference) {
                FingerprintPreference fpref = (FingerprintPreference) pref;
                final Fingerprint fp = fpref.getFingerprint();
                showRenameDialog(fp);
            }
            return super.onPreferenceTreeClick(pref);
        }

        @Override
        public void onDeleteClick(FingerprintPreference p) {
            final boolean hasMultipleFingerprint =
                    mFingerprintManager.getEnrolledFingerprints(mUserId).size() > 1;
            final Fingerprint fp = p.getFingerprint();

            if (hasMultipleFingerprint) {
                if (mRemovalSidecar.inProgress()) {
                    Log.d(TAG, "Fingerprint delete in progress, skipping");
                    return;
                }
                DeleteFingerprintDialog.newInstance(fp, this /* target */)
                        .show(getFragmentManager(), DeleteFingerprintDialog.class.getName());
            } else {
                ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                final boolean isProfileChallengeUser =
                        UserManager.get(getContext()).isManagedProfile(mUserId);
                final Bundle args = new Bundle();
                args.putParcelable("fingerprint", fp);
                args.putBoolean("isProfileChallengeUser", isProfileChallengeUser);
                lastDeleteDialog.setArguments(args);
                lastDeleteDialog.setTargetFragment(this, 0);
                lastDeleteDialog.show(getFragmentManager(),
                        ConfirmLastDeleteDialog.class.getName());
            }
        }

        private void showRenameDialog(final Fingerprint fp) {
            RenameDialog renameDialog = new RenameDialog();
            Bundle args = new Bundle();
            if (mFingerprintsRenaming.containsKey(fp.getBiometricId())) {
                final Fingerprint f = new Fingerprint(
                        mFingerprintsRenaming.get(fp.getBiometricId()),
                        fp.getGroupId(), fp.getBiometricId(), fp.getDeviceId());
                args.putParcelable("fingerprint", f);
            } else {
                args.putParcelable("fingerprint", fp);
            }
            renameDialog.setOnDismissListener((dialogInterface) -> {
                retryFingerprint();
            });
            renameDialog.setDeleteInProgress(mRemovalSidecar.inProgress());
            renameDialog.setArguments(args);
            renameDialog.setTargetFragment(this, 0);
            renameDialog.show(getFragmentManager(), RenameDialog.class.getName());
            mAuthenticateSidecar.stopAuthentication();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean result = true;
            final String key = preference.getKey();
            if (KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE.equals(key)) {
                // TODO
            } else {
                Log.v(TAG, "Unknown key:" + key);
            }
            return result;
        }

        @Override
        public int getHelpResource() {
            return R.string.help_url_fingerprint;
        }

        @Override
        protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            if (!isFingerprintHardwareDetected(context)) {
                return null;
            }

            mControllers = buildPreferenceControllers(context);
            return mControllers;
        }

        private List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
            final List<AbstractPreferenceController> controllers = new ArrayList<>();
            mRequireScreenOnToAuthPreferenceController =
                    new FingerprintSettingsRequireScreenOnToAuthPreferenceController(
                            context,
                            KEY_REQUIRE_SCREEN_ON_TO_AUTH
                    );
            controllers.add(mRequireScreenOnToAuthPreferenceController);
            return controllers;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CONFIRM_REQUEST || requestCode == CHOOSE_LOCK_GENERIC_REQUEST) {
                mLaunchedConfirm = false;
                if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                    if (data != null && BiometricUtils.containsGatekeeperPasswordHandle(data)) {
                        if (!mHasFirstEnrolled && !mIsEnrolling) {
                            final Activity activity = getActivity();
                            if (activity != null) {
                                // Apply pending transition for auto adding first fingerprint case
                                activity.overridePendingTransition(R.anim.sud_slide_next_in,
                                        R.anim.sud_slide_next_out);
                            }
                        }
                        mFingerprintManager.generateChallenge(mUserId,
                                (sensorId, userId, challenge) -> {
                                    mToken = BiometricUtils.requestGatekeeperHat(getActivity(),
                                            data,
                                            mUserId, challenge);
                                    mChallenge = challenge;
                                    BiometricUtils.removeGatekeeperPasswordHandle(getActivity(),
                                            data);
                                    updateAddPreference();
                                    if (!mHasFirstEnrolled && !mIsEnrolling) {
                                        mIsEnrolling = true;
                                        addFirstFingerprint();
                                    }
                        });
                    } else {
                        Log.d(TAG, "Data null or GK PW missing");
                        finish();
                    }
                } else {
                    Log.d(TAG, "Password not confirmed");
                    finish();
                }
            } else if (requestCode == ADD_FINGERPRINT_REQUEST) {
                mIsEnrolling = false;
                if (resultCode == RESULT_TIMEOUT) {
                    Activity activity = getActivity();
                    activity.setResult(resultCode);
                    activity.finish();
                }
            } else if (requestCode == AUTO_ADD_FIRST_FINGERPRINT_REQUEST) {
                mIsEnrolling = false;
                mHasFirstEnrolled = true;
                if (resultCode != RESULT_FINISHED) {
                    Log.d(TAG, "Add first fingerprint fail, result:" + resultCode);
                    finish();
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (getActivity().isFinishing()) {
                mFingerprintManager.revokeChallenge(mUserId, mChallenge);
            }
        }

        private Drawable getHighlightDrawable() {
            if (mHighlightDrawable == null) {
                final Activity activity = getActivity();
                if (activity != null) {
                    mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
                }
            }
            return mHighlightDrawable;
        }

        private void highlightFingerprintItem(int fpId) {
            String prefName = genKey(fpId);
            FingerprintPreference fpref = (FingerprintPreference) findPreference(prefName);
            final Drawable highlight = getHighlightDrawable();
            if (highlight != null && fpref != null) {
                final View view = fpref.getView();
                if (view == null) {
                    // FingerprintPreference is not bound to UI yet, so view is null.
                    return;
                }
                final int centerX = view.getWidth() / 2;
                final int centerY = view.getHeight() / 2;
                highlight.setHotspot(centerX, centerY);
                view.setBackground(highlight);
                view.setPressed(true);
                view.setPressed(false);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackground(null);
                    }
                }, RESET_HIGHLIGHT_DELAY_MS);
            }
        }

        private void launchChooseOrConfirmLock() {
            final Intent intent = new Intent();
            final ChooseLockSettingsHelper.Builder builder =
                    new ChooseLockSettingsHelper.Builder(getActivity(), this);
            final boolean launched = builder.setRequestCode(CONFIRM_REQUEST)
                    .setTitle(getString(R.string.security_settings_fingerprint_preference_title))
                    .setRequestGatekeeperPasswordHandle(true)
                    .setUserId(mUserId)
                    .setForegroundOnly(true)
                    .setReturnCredentials(true)
                    .show();

            if (!launched) {
                // TODO: This should be cleaned up. ChooseLockGeneric should provide a way of
                //  specifying arguments/requests, instead of relying on callers setting extras.
                intent.setClassName(SETTINGS_PACKAGE_NAME, ChooseLockGeneric.class.getName());
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS,
                        true);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
            }
        }

        private void addFirstFingerprint() {
            Intent intent = new Intent();
            intent.setClassName(SETTINGS_PACKAGE_NAME,
                    FingerprintEnrollIntroductionInternal.class.getName());

            intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true);
            intent.putExtra(SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                    SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE);

            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            startActivityForResult(intent, AUTO_ADD_FIRST_FINGERPRINT_REQUEST);
        }

        @VisibleForTesting
        void deleteFingerPrint(Fingerprint fingerPrint) {
            mRemovalSidecar.startRemove(fingerPrint, mUserId);
            String name = genKey(fingerPrint.getBiometricId());
            Preference prefToRemove = findPreference(name);
            if (prefToRemove != null) {
                prefToRemove.setEnabled(false);
            }
            updateAddPreference();
        }

        private void renameFingerPrint(int fingerId, String newName) {
            mFingerprintManager.rename(fingerId, mUserId, newName);
            if (!TextUtils.isEmpty(newName)) {
                mFingerprintsRenaming.put(fingerId, newName);
            }
            updatePreferences();
        }

        private final Runnable mFingerprintLockoutReset = new Runnable() {
            @Override
            public void run() {
                mInFingerprintLockout = false;
                retryFingerprint();
            }
        };

        public static class DeleteFingerprintDialog extends InstrumentedDialogFragment
                implements DialogInterface.OnClickListener {

            private static final String KEY_FINGERPRINT = "fingerprint";
            private Fingerprint mFp;
            private AlertDialog mAlertDialog;

            public static DeleteFingerprintDialog newInstance(Fingerprint fp,
                    FingerprintSettingsFragment target) {
                final DeleteFingerprintDialog dialog = new DeleteFingerprintDialog();
                final Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_FINGERPRINT, fp);
                dialog.setArguments(bundle);
                dialog.setTargetFragment(target, 0 /* requestCode */);
                return dialog;
            }

            @Override
            public int getMetricsCategory() {
                return SettingsEnums.DIALOG_FINGERPINT_EDIT;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable(KEY_FINGERPRINT);
                final String title = getString(R.string.fingerprint_delete_title, mFp.getName());
                final String message =
                        getString(R.string.fingerprint_v2_delete_message, mFp.getName());

                mAlertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(
                                R.string.security_settings_fingerprint_enroll_dialog_delete,
                                this /* onClickListener */)
                        .setNegativeButton(R.string.cancel, null /* onClickListener */)
                        .create();
                return mAlertDialog;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    final int fingerprintId = mFp.getBiometricId();
                    Log.v(TAG, "Removing fpId=" + fingerprintId);
                    mMetricsFeatureProvider.action(getContext(),
                            SettingsEnums.ACTION_FINGERPRINT_DELETE,
                            fingerprintId);
                    FingerprintSettingsFragment parent
                            = (FingerprintSettingsFragment) getTargetFragment();
                    parent.deleteFingerPrint(mFp);
                }
            }
        }

        private static InputFilter[] getFilters() {
            InputFilter filter = new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end,
                        Spanned dest, int dstart, int dend) {
                    for (int index = start; index < end; index++) {
                        final char c = source.charAt(index);
                        // KXMLSerializer does not allow these characters,
                        // see KXmlSerializer.java:162.
                        if (c < 0x20) {
                            return "";
                        }
                    }
                    return null;
                }
            };
            return new InputFilter[]{filter};
        }

        public static class RenameDialog extends InstrumentedDialogFragment {

            private Fingerprint mFp;
            private ImeAwareEditText mDialogTextField;
            private AlertDialog mAlertDialog;
            private @Nullable DialogInterface.OnDismissListener mDismissListener;
            private boolean mDeleteInProgress;

            public void setDeleteInProgress(boolean deleteInProgress) {
                mDeleteInProgress = deleteInProgress;
            }

            @Override
            public void onCancel(DialogInterface dialog) {
                super.onCancel(dialog);
                if (mDismissListener != null) {
                    mDismissListener.onDismiss(dialog);
                }
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                final String fingerName;
                final int textSelectionStart;
                final int textSelectionEnd;
                if (savedInstanceState != null) {
                    fingerName = savedInstanceState.getString("fingerName");
                    textSelectionStart = savedInstanceState.getInt("startSelection", -1);
                    textSelectionEnd = savedInstanceState.getInt("endSelection", -1);
                } else {
                    fingerName = null;
                    textSelectionStart = -1;
                    textSelectionEnd = -1;
                }
                mAlertDialog = new AlertDialog.Builder(getActivity())
                        .setView(R.layout.fingerprint_rename_dialog)
                        .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String newName =
                                                mDialogTextField.getText().toString();
                                        final CharSequence name = mFp.getName();
                                        if (!TextUtils.equals(newName, name)) {
                                            Log.d(TAG, "rename " + name + " to " + newName);
                                            mMetricsFeatureProvider.action(getContext(),
                                                    SettingsEnums.ACTION_FINGERPRINT_RENAME,
                                                    mFp.getBiometricId());
                                            FingerprintSettingsFragment parent
                                                    = (FingerprintSettingsFragment)
                                                    getTargetFragment();
                                            parent.renameFingerPrint(mFp.getBiometricId(),
                                                    newName);
                                        }
                                        if (mDismissListener != null) {
                                            mDismissListener.onDismiss(dialog);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        mDialogTextField = mAlertDialog.findViewById(R.id.fingerprint_rename_field);
                        CharSequence name = fingerName == null ? mFp.getName() : fingerName;
                        mDialogTextField.setText(name);
                        mDialogTextField.setFilters(getFilters());
                        if (textSelectionStart != -1 && textSelectionEnd != -1) {
                            mDialogTextField.setSelection(textSelectionStart, textSelectionEnd);
                        } else {
                            mDialogTextField.selectAll();
                        }
                        if (mDeleteInProgress) {
                            mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                        }
                        mDialogTextField.requestFocus();
                        mDialogTextField.scheduleShowSoftInput();
                    }
                });
                return mAlertDialog;
            }

            public void setOnDismissListener(@NonNull DialogInterface.OnDismissListener listener) {
                mDismissListener = listener;
            }

            public void enableDelete() {
                mDeleteInProgress = false;
                if (mAlertDialog != null) {
                    mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                }
            }

            @Override
            public void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
                if (mDialogTextField != null) {
                    outState.putString("fingerName", mDialogTextField.getText().toString());
                    outState.putInt("startSelection", mDialogTextField.getSelectionStart());
                    outState.putInt("endSelection", mDialogTextField.getSelectionEnd());
                }
            }

            @Override
            public int getMetricsCategory() {
                return SettingsEnums.DIALOG_FINGERPINT_EDIT;
            }
        }

        public static class ConfirmLastDeleteDialog extends InstrumentedDialogFragment {

            private Fingerprint mFp;

            @Override
            public int getMetricsCategory() {
                return SettingsEnums.DIALOG_FINGERPINT_DELETE_LAST;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                final boolean isProfileChallengeUser =
                        getArguments().getBoolean("isProfileChallengeUser");

                final String title = getString(R.string.fingerprint_delete_title, mFp.getName());
                final String message =
                        getString(R.string.fingerprint_v2_delete_message, mFp.getName()) + ".";

                DevicePolicyManager devicePolicyManager =
                        getContext().getSystemService(DevicePolicyManager.class);
                String messageId =
                        isProfileChallengeUser ? WORK_PROFILE_FINGERPRINT_LAST_DELETE_MESSAGE
                        : UNDEFINED;
                int defaultMessageId = isProfileChallengeUser
                        ? R.string.fingerprint_last_delete_message_profile_challenge
                        : R.string.fingerprint_last_delete_message;

                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setMessage(devicePolicyManager.getResources().getString(
                                messageId,
                                () ->  message + "\n\n" + getContext().getString(defaultMessageId)))
                        .setPositiveButton(
                                R.string.security_settings_fingerprint_enroll_dialog_delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FingerprintSettingsFragment parent
                                                = (FingerprintSettingsFragment) getTargetFragment();
                                        parent.deleteFingerPrint(mFp);
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                return alertDialog;
            }
        }
    }

    public static class FingerprintPreference extends TwoTargetPreference {

        private final OnDeleteClickListener mOnDeleteClickListener;

        private Fingerprint mFingerprint;
        private View mView;
        private View mDeleteView;

        public interface OnDeleteClickListener {
            void onDeleteClick(FingerprintPreference p);
        }

        public FingerprintPreference(Context context, OnDeleteClickListener onDeleteClickListener) {
            super(context);
            mOnDeleteClickListener = onDeleteClickListener;
        }

        public View getView() {
            return mView;
        }

        public void setFingerprint(Fingerprint item) {
            mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @Override
        protected int getSecondTargetResId() {
            return R.layout.preference_widget_delete;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            mView = view.itemView;
            mDeleteView = view.itemView.findViewById(R.id.delete_button);
            mDeleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnDeleteClickListener != null) {
                        mOnDeleteClickListener.onDeleteClick(FingerprintPreference.this);
                    }
                }
            });
        }
    }
}
