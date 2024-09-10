/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.password;

import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
import static android.app.admin.DevicePolicyResources.Strings.Settings.PASSWORD_RECENTLY_USED;
import static android.app.admin.DevicePolicyResources.Strings.Settings.PIN_RECENTLY_USED;
import static android.app.admin.DevicePolicyResources.Strings.Settings.REENTER_WORK_PROFILE_PASSWORD_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.REENTER_WORK_PROFILE_PIN_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.SET_WORK_PROFILE_PASSWORD_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.SET_WORK_PROFILE_PIN_HEADER;
import static android.app.admin.DevicePolicyResources.UNDEFINED;
import static android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE;

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_NONE;
import static com.android.internal.widget.PasswordValidationError.CONTAINS_INVALID_CHARACTERS;
import static com.android.internal.widget.PasswordValidationError.CONTAINS_SEQUENCE;
import static com.android.internal.widget.PasswordValidationError.NOT_ENOUGH_DIGITS;
import static com.android.internal.widget.PasswordValidationError.NOT_ENOUGH_LETTERS;
import static com.android.internal.widget.PasswordValidationError.NOT_ENOUGH_LOWER_CASE;
import static com.android.internal.widget.PasswordValidationError.NOT_ENOUGH_NON_DIGITS;
import static com.android.internal.widget.PasswordValidationError.NOT_ENOUGH_NON_LETTER;
import static com.android.internal.widget.PasswordValidationError.NOT_ENOUGH_SYMBOLS;
import static com.android.internal.widget.PasswordValidationError.NOT_ENOUGH_UPPER_CASE;
import static com.android.internal.widget.PasswordValidationError.RECENTLY_USED;
import static com.android.internal.widget.PasswordValidationError.TOO_LONG;
import static com.android.internal.widget.PasswordValidationError.TOO_SHORT;
import static com.android.internal.widget.PasswordValidationError.TOO_SHORT_WHEN_ALL_NUMERIC;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManager.PasswordComplexity;
import android.app.admin.PasswordMetrics;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.ImeAwareEditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.PasswordValidationError;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.notification.RedactionInterstitial;
import com.android.settingslib.utils.StringUtil;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChooseLockPassword extends SettingsActivity {
    private static final String TAG = "ChooseLockPassword";

    static final String EXTRA_KEY_MIN_METRICS = "min_metrics";
    static final String EXTRA_KEY_MIN_COMPLEXITY = "min_complexity";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    public static class IntentBuilder {

        private final Intent mIntent;

        public IntentBuilder(Context context) {
            mIntent = new Intent(context, ChooseLockPassword.class);
            mIntent.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, false);
        }

        /**
         * Sets the intended credential type i.e. whether it's numeric PIN or general password
         * @param passwordType password type represented by one of the {@code PASSWORD_QUALITY_}
         *   constants.
         */
        public IntentBuilder setPasswordType(int passwordType) {
            mIntent.putExtra(LockPatternUtils.PASSWORD_TYPE_KEY, passwordType);
            return this;
        }

        public IntentBuilder setUserId(int userId) {
            mIntent.putExtra(Intent.EXTRA_USER_ID, userId);
            return this;
        }

        public IntentBuilder setRequestGatekeeperPasswordHandle(
                boolean requestGatekeeperPasswordHandle) {
            mIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE,
                    requestGatekeeperPasswordHandle);
            return this;
        }

        public IntentBuilder setPassword(LockscreenCredential password) {
            mIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, password);
            return this;
        }

        public IntentBuilder setForFingerprint(boolean forFingerprint) {
            mIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, forFingerprint);
            return this;
        }

        public IntentBuilder setForFace(boolean forFace) {
            mIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, forFace);
            return this;
        }

        public IntentBuilder setForBiometrics(boolean forBiometrics) {
            mIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, forBiometrics);
            return this;
        }

        /** Sets the minimum password requirement in terms of complexity and metrics */
        public IntentBuilder setPasswordRequirement(@PasswordComplexity int level,
                PasswordMetrics metrics) {
            mIntent.putExtra(EXTRA_KEY_MIN_COMPLEXITY, level);
            mIntent.putExtra(EXTRA_KEY_MIN_METRICS, metrics);
            return this;
        }

        /**
         * Configures the launch such that at the end of the password enrollment, one of its
         * managed profile (specified by {@code profileId}) will have its lockscreen unified
         * to the parent user. The profile's current lockscreen credential needs to be specified by
         * {@code credential}.
         */
        public IntentBuilder setProfileToUnify(int profileId, LockscreenCredential credential) {
            mIntent.putExtra(EXTRA_KEY_UNIFICATION_PROFILE_ID, profileId);
            mIntent.putExtra(EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL, credential);
            return this;
        }

        public Intent build() {
            return mIntent;
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ChooseLockPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    protected boolean isToolbarEnabled() {
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        return ChooseLockPasswordFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstanceState);
        findViewById(R.id.content_parent).setFitsSystemWindows(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static class ChooseLockPasswordFragment extends InstrumentedFragment
            implements OnEditorActionListener, TextWatcher, SaveAndFinishWorker.Listener {
        private static final String KEY_FIRST_PASSWORD = "first_password";
        private static final String KEY_UI_STAGE = "ui_stage";
        private static final String KEY_CURRENT_CREDENTIAL = "current_credential";
        private static final String FRAGMENT_TAG_SAVE_AND_FINISH = "save_and_finish_worker";
        private static final String KEY_IS_AUTO_CONFIRM_CHECK_MANUALLY_CHANGED =
                "auto_confirm_option_set_manually";

        private static final int MIN_AUTO_PIN_REQUIREMENT_LENGTH = 6;

        private LockscreenCredential mCurrentCredential;
        private LockscreenCredential mChosenPassword;
        private boolean mRequestGatekeeperPassword;
        private boolean mRequestWriteRepairModePassword;
        private ImeAwareEditText mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;

        // Minimum password metrics enforced by admins.
        private PasswordMetrics mMinMetrics;
        private List<PasswordValidationError> mValidationErrors;

        @PasswordComplexity private int mMinComplexity = PASSWORD_COMPLEXITY_NONE;
        protected int mUserId;
        private byte[] mPasswordHistoryHashFactor;
        private int mUnificationProfileId = UserHandle.USER_NULL;

        private LockPatternUtils mLockPatternUtils;
        private SaveAndFinishWorker mSaveAndFinishWorker;
        private int mPasswordType = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
        protected Stage mUiStage = Stage.Introduction;
        private PasswordRequirementAdapter mPasswordRequirementAdapter;
        private GlifLayout mLayout;
        protected boolean mForFingerprint;
        protected boolean mForFace;
        protected boolean mForBiometrics;

        private LockscreenCredential mFirstPassword;
        private RecyclerView mPasswordRestrictionView;
        protected boolean mIsAlphaMode;
        protected FooterButton mSkipOrClearButton;
        private FooterButton mNextButton;
        private TextView mMessage;
        protected CheckBox mAutoPinConfirmOption;
        protected TextView mAutoConfirmSecurityMessage;
        protected boolean mIsAutoPinConfirmOptionSetManually;

        private TextChangedHandler mTextChangedHandler;

        private static final int CONFIRM_EXISTING_REQUEST = 58;
        static final int RESULT_FINISHED = RESULT_FIRST_USER;
        private boolean mIsErrorTooShort = true;

        /** Used to store the profile type for which pin/password is being set */
        protected enum ProfileType {
            None,
            Managed,
            Private,
            Other
        };
        protected ProfileType mProfileType;

        /**
         * Keep track internally of where the user is in choosing a pattern.
         */
        protected enum Stage {

            Introduction(
                    R.string.lockpassword_choose_your_password_header, // password
                    SET_WORK_PROFILE_PASSWORD_HEADER,
                    R.string.lockpassword_choose_your_profile_password_header,
                    R.string.lockpassword_choose_your_password_header_for_fingerprint,
                    R.string.lockpassword_choose_your_password_header_for_face,
                    R.string.lockpassword_choose_your_password_header_for_biometrics,
                    R.string.private_space_choose_your_password_header, // private space password
                    R.string.lockpassword_choose_your_pin_header, // pin
                    SET_WORK_PROFILE_PIN_HEADER,
                    R.string.lockpassword_choose_your_profile_pin_header,
                    R.string.lockpassword_choose_your_pin_header_for_fingerprint,
                    R.string.lockpassword_choose_your_pin_header_for_face,
                    R.string.lockpassword_choose_your_pin_header_for_biometrics,
                    R.string.private_space_choose_your_pin_header, // private space pin
                    R.string.lock_settings_picker_biometrics_added_security_message,
                    R.string.lock_settings_picker_biometrics_added_security_message,
                    R.string.next_label),

            NeedToConfirm(
                    R.string.lockpassword_confirm_your_password_header,
                    REENTER_WORK_PROFILE_PASSWORD_HEADER,
                    R.string.lockpassword_reenter_your_profile_password_header,
                    R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    REENTER_WORK_PROFILE_PIN_HEADER,
                    R.string.lockpassword_reenter_your_profile_pin_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    0,
                    0,
                    R.string.lockpassword_confirm_label),

            ConfirmWrong(
                    R.string.lockpassword_confirm_passwords_dont_match,
                    UNDEFINED,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    UNDEFINED,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    0,
                    0,
                    R.string.lockpassword_confirm_label);

            Stage(int hintInAlpha,
                    String hintOverrideInAlphaForProfile,
                    int hintInAlphaForProfile,
                    int hintInAlphaForFingerprint,
                    int hintInAlphaForFace,
                    int hintInAlphaForBiometrics,
                    int hintInAlphaForPrivateProfile,
                    int hintInNumeric,
                    String hintOverrideInNumericForProfile,
                    int hintInNumericForProfile,
                    int hintInNumericForFingerprint,
                    int hintInNumericForFace,
                    int hintInNumericForBiometrics,
                    int hintInNumericForPrivateProfile,
                    int messageInAlphaForBiometrics,
                    int messageInNumericForBiometrics,
                    int nextButtonText) {

                this.alphaHint = hintInAlpha;
                this.alphaHintOverrideForProfile = hintOverrideInAlphaForProfile;
                this.alphaHintForManagedProfile = hintInAlphaForProfile;
                this.alphaHintForFingerprint = hintInAlphaForFingerprint;
                this.alphaHintForFace = hintInAlphaForFace;
                this.alphaHintForBiometrics = hintInAlphaForBiometrics;
                this.alphaHintForPrivateProfile = hintInAlphaForPrivateProfile;

                this.numericHint = hintInNumeric;
                this.numericHintOverrideForProfile = hintOverrideInNumericForProfile;
                this.numericHintForManagedProfile = hintInNumericForProfile;
                this.numericHintForFingerprint = hintInNumericForFingerprint;
                this.numericHintForFace = hintInNumericForFace;
                this.numericHintForBiometrics = hintInNumericForBiometrics;
                this.numericHintForPrivateProfile = hintInNumericForPrivateProfile;

                this.alphaMessageForBiometrics = messageInAlphaForBiometrics;
                this.numericMessageForBiometrics = messageInNumericForBiometrics;

                this.buttonText = nextButtonText;
            }

            public static final int TYPE_NONE = 0;
            public static final int TYPE_FINGERPRINT = 1;
            public static final int TYPE_FACE = 2;
            public static final int TYPE_BIOMETRIC = 3;

            // Password header
            public final int alphaHint;
            public final int alphaHintForPrivateProfile;
            public final String alphaHintOverrideForProfile;
            public final int alphaHintForManagedProfile;
            public final int alphaHintForFingerprint;
            public final int alphaHintForFace;
            public final int alphaHintForBiometrics;

            // PIN header
            public final int numericHint;
            public final int numericHintForPrivateProfile;
            public final String numericHintOverrideForProfile;
            public final int numericHintForManagedProfile;
            public final int numericHintForFingerprint;
            public final int numericHintForFace;
            public final int numericHintForBiometrics;

            // Password description
            public final int alphaMessageForBiometrics;

            // PIN description
            public final int numericMessageForBiometrics;

            public final int buttonText;

            public String getHint(Context context, boolean isAlpha, int type, ProfileType profile) {
                if (isAlpha) {
                    if (android.os.Flags.allowPrivateProfile()
                            && android.multiuser.Flags.enablePrivateSpaceFeatures()
                            && profile.equals(ProfileType.Private)) {
                        return context.getString(alphaHintForPrivateProfile);
                    } else if (type == TYPE_FINGERPRINT) {
                        return context.getString(alphaHintForFingerprint);
                    } else if (type == TYPE_FACE) {
                        return context.getString(alphaHintForFace);
                    } else if (type == TYPE_BIOMETRIC) {
                        return context.getString(alphaHintForBiometrics);
                    } else if (profile.equals(ProfileType.Managed)) {
                        return context.getSystemService(DevicePolicyManager.class).getResources()
                                .getString(alphaHintOverrideForProfile,
                                        () -> context.getString(alphaHintForManagedProfile));
                    } else {
                        return context.getString(alphaHint);
                    }
                } else {
                    if (android.os.Flags.allowPrivateProfile()
                            && android.multiuser.Flags.enablePrivateSpaceFeatures()
                            && profile.equals(ProfileType.Private)) {
                        return context.getString(numericHintForPrivateProfile);
                    } else if (type == TYPE_FINGERPRINT) {
                        return context.getString(numericHintForFingerprint);
                    } else if (type == TYPE_FACE) {
                        return context.getString(numericHintForFace);
                    } else if (type == TYPE_BIOMETRIC) {
                        return context.getString(numericHintForBiometrics);
                    } else if (profile.equals(ProfileType.Managed)) {
                        return context.getSystemService(DevicePolicyManager.class).getResources()
                                .getString(numericHintOverrideForProfile,
                                        () -> context.getString(numericHintForManagedProfile));
                    } else {
                        return context.getString(numericHint);
                    }
                }
            }

            public @StringRes int getMessage(boolean isAlpha, int type) {
                switch (type) {
                    case TYPE_FINGERPRINT:
                    case TYPE_FACE:
                    case TYPE_BIOMETRIC:
                        return isAlpha ? alphaMessageForBiometrics : numericMessageForBiometrics;

                    case TYPE_NONE:
                    default:
                        return 0;
                }
            }
        }

        // required constructor for fragments
        public ChooseLockPasswordFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
            Intent intent = getActivity().getIntent();
            if (!(getActivity() instanceof ChooseLockPassword)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
            // Only take this argument into account if it belongs to the current profile.
            mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras());
            mProfileType = getProfileType();
            mForFingerprint = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);
            mForFace = intent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, false);
            mForBiometrics = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, false);

            mPasswordType = intent.getIntExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY, PASSWORD_QUALITY_NUMERIC);
            mUnificationProfileId = intent.getIntExtra(
                    EXTRA_KEY_UNIFICATION_PROFILE_ID, UserHandle.USER_NULL);

            mMinComplexity = intent.getIntExtra(EXTRA_KEY_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE);
            mMinMetrics = intent.getParcelableExtra(EXTRA_KEY_MIN_METRICS);
            if (mMinMetrics == null) mMinMetrics = new PasswordMetrics(CREDENTIAL_TYPE_NONE);

            mTextChangedHandler = new TextChangedHandler();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.choose_lock_password, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mLayout = (GlifLayout) view;

            // Make the password container consume the optical insets so the edit text is aligned
            // with the sides of the parent visually.
            ViewGroup container = view.findViewById(R.id.password_container);
            container.setOpticalInsets(Insets.NONE);

            final FooterBarMixin mixin = mLayout.getMixin(FooterBarMixin.class);
            mixin.setSecondaryButton(
                    new FooterButton.Builder(getActivity())
                            .setText(R.string.lockpassword_clear_label)
                            .setListener(this::onSkipOrClearButtonClick)
                            .setButtonType(FooterButton.ButtonType.SKIP)
                            .setTheme(
                                    com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                            .build()
            );
            mixin.setPrimaryButton(
                    new FooterButton.Builder(getActivity())
                            .setText(R.string.next_label)
                            .setListener(this::onNextButtonClick)
                            .setButtonType(FooterButton.ButtonType.NEXT)
                            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                            .build()
            );
            mSkipOrClearButton = mixin.getSecondaryButton();
            mNextButton = mixin.getPrimaryButton();

            mMessage = view.findViewById(R.id.sud_layout_description);
            mLayout.setIcon(getActivity().getDrawable(R.drawable.ic_lock));

            mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == mPasswordType
                    || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == mPasswordType
                    || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == mPasswordType;

            final LinearLayout headerLayout = view.findViewById(
                    com.google.android.setupdesign.R.id.sud_layout_header);
            setupPasswordRequirementsView(headerLayout);

            mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mPasswordEntry = view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);
            mPasswordEntry.requestFocus();
            mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

            // Fetch the AutoPinConfirmOption
            mAutoPinConfirmOption = view.findViewById(R.id.auto_pin_confirm_enabler);
            mAutoConfirmSecurityMessage = view.findViewById(R.id.auto_pin_confirm_security_message);
            mIsAutoPinConfirmOptionSetManually = false;
            setOnAutoConfirmOptionClickListener();
            if (mAutoPinConfirmOption != null) {
                mAutoPinConfirmOption.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
                mAutoPinConfirmOption.setVisibility(View.GONE);
                mAutoPinConfirmOption.setChecked(false);
            }

            final Activity activity = getActivity();

            int currentType = mPasswordEntry.getInputType();
            mPasswordEntry.setInputType(mIsAlphaMode ? currentType
                    : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));
            if (mIsAlphaMode) {
                mPasswordEntry.setContentDescription(
                        getString(R.string.unlock_set_unlock_password_title));
            } else {
                mPasswordEntry.setContentDescription(
                        getString(R.string.unlock_set_unlock_pin_title));
            }
            // Can't set via XML since setInputType resets the fontFamily to null
            mPasswordEntry.setTypeface(Typeface.create(
                    getContext().getString(com.android.internal.R.string.config_headlineFontFamily),
                    Typeface.NORMAL));

            Intent intent = getActivity().getIntent();
            final boolean confirmCredentials = intent.getBooleanExtra(
                    ChooseLockGeneric.CONFIRM_CREDENTIALS, true);
            mCurrentCredential = intent.getParcelableExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            mRequestGatekeeperPassword = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, false);
            mRequestWriteRepairModePassword = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_WRITE_REPAIR_MODE_PW, false);
            if (savedInstanceState == null) {
                updateStage(Stage.Introduction);
                if (confirmCredentials) {
                    final ChooseLockSettingsHelper.Builder builder =
                            new ChooseLockSettingsHelper.Builder(getActivity());
                    builder.setRequestCode(CONFIRM_EXISTING_REQUEST)
                            .setTitle(getString(R.string.unlock_set_unlock_launch_picker_title))
                            .setReturnCredentials(true)
                            .setRequestGatekeeperPasswordHandle(mRequestGatekeeperPassword)
                            .setRequestWriteRepairModePassword(mRequestWriteRepairModePassword)
                            .setUserId(mUserId)
                            .show();
                }
            } else {

                // restore from previous state
                mFirstPassword = savedInstanceState.getParcelable(KEY_FIRST_PASSWORD);
                final String state = savedInstanceState.getString(KEY_UI_STAGE);
                if (state != null) {
                    mUiStage = Stage.valueOf(state);
                    updateStage(mUiStage);
                }
                mIsAutoPinConfirmOptionSetManually =
                        savedInstanceState.getBoolean(KEY_IS_AUTO_CONFIRM_CHECK_MANUALLY_CHANGED);

                mCurrentCredential = savedInstanceState.getParcelable(KEY_CURRENT_CREDENTIAL);

                // Re-attach to the exiting worker if there is one.
                mSaveAndFinishWorker = (SaveAndFinishWorker) getFragmentManager().findFragmentByTag(
                        FRAGMENT_TAG_SAVE_AND_FINISH);
            }

            if (activity instanceof SettingsActivity) {
                final SettingsActivity sa = (SettingsActivity) activity;
                String title = Stage.Introduction.getHint(
                        getContext(), mIsAlphaMode, getStageType(), mProfileType);
                sa.setTitle(title);
                mLayout.setHeaderText(title);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mCurrentCredential != null) {
                mCurrentCredential.zeroize();
            }
            // Force a garbage collection immediately to remove remnant of user password shards
            // from memory.
            System.gc();
            System.runFinalization();
            System.gc();
        }

        protected int getStageType() {
            if (mForFingerprint) {
                return Stage.TYPE_FINGERPRINT;
            } else if (mForFace) {
                return Stage.TYPE_FACE;
            } else if (mForBiometrics) {
                return Stage.TYPE_BIOMETRIC;
            } else {
                return Stage.TYPE_NONE;
            }
        }

        private void setupPasswordRequirementsView(@Nullable ViewGroup view) {
            if (view == null) {
                return;
            }

            createHintMessageView(view);
            mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mPasswordRequirementAdapter = new PasswordRequirementAdapter(getActivity());
            mPasswordRestrictionView.setAdapter(mPasswordRequirementAdapter);
            view.addView(mPasswordRestrictionView);
        }

        @VisibleForTesting
        View getPasswordRequirementsView() {
            return mPasswordRestrictionView;
        }

        private void createHintMessageView(ViewGroup view) {
            if (mPasswordRestrictionView != null) {
                return;
            }

            final TextView sucTitleView = view.findViewById(R.id.suc_layout_title);
            final ViewGroup.MarginLayoutParams titleLayoutParams =
                    (ViewGroup.MarginLayoutParams) sucTitleView.getLayoutParams();
            mPasswordRestrictionView = new RecyclerView(getActivity());
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(titleLayoutParams.leftMargin, getResources().getDimensionPixelSize(
                    R.dimen.password_requirement_view_margin_top), titleLayoutParams.leftMargin, 0);
            mPasswordRestrictionView.setLayoutParams(lp);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.CHOOSE_LOCK_PASSWORD;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(mUiStage);
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(this);
            } else {
                mPasswordEntry.requestFocus();
                mPasswordEntry.scheduleShowSoftInput();
            }
        }

        @Override
        public void onPause() {
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(null);
            }
            super.onPause();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(KEY_UI_STAGE, mUiStage.name());
            outState.putParcelable(KEY_FIRST_PASSWORD, mFirstPassword);
            if (mCurrentCredential != null) {
                outState.putParcelable(KEY_CURRENT_CREDENTIAL, mCurrentCredential.duplicate());
            }
            outState.putBoolean(KEY_IS_AUTO_CONFIRM_CHECK_MANUALLY_CHANGED,
                    mIsAutoPinConfirmOptionSetManually);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case CONFIRM_EXISTING_REQUEST:
                    if (resultCode != Activity.RESULT_OK) {
                        getActivity().setResult(RESULT_FINISHED);
                        getActivity().finish();
                    } else {
                        mCurrentCredential = data.getParcelableExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                    }
                    break;
            }
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context, mUserId);
        }

        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;
            mUiStage = stage;
            updateUi();

            // If the stage changed, announce the header for accessibility. This
            // is a no-op when accessibility is disabled.
            if (previousStage != stage) {
                mLayout.announceForAccessibility(mLayout.getHeaderText());
            }
        }

        /**
         * Validates PIN/Password and returns the validation result and updates mValidationErrors
         * to reflect validation results.
         *
         * @param credential credential the user typed in.
         * @return whether password satisfies all the requirements.
         */
        @VisibleForTesting
        boolean validatePassword(LockscreenCredential credential) {
            mValidationErrors = PasswordMetrics.validateCredential(mMinMetrics, mMinComplexity,
                    credential);
            if (mValidationErrors.isEmpty() && mLockPatternUtils.checkPasswordHistory(
                        credential.getCredential(), getPasswordHistoryHashFactor(), mUserId)) {
                mValidationErrors =
                        Collections.singletonList(new PasswordValidationError(RECENTLY_USED));
            }
            return mValidationErrors.isEmpty();
        }

        /**
         * Lazily compute and return the history hash factor of the current user (mUserId), used for
         * password history check.
         */
        private byte[] getPasswordHistoryHashFactor() {
            if (mPasswordHistoryHashFactor == null) {
                mPasswordHistoryHashFactor = mLockPatternUtils.getPasswordHistoryHashFactor(
                        mCurrentCredential != null ? mCurrentCredential
                                : LockscreenCredential.createNone(), mUserId);
            }
            return mPasswordHistoryHashFactor;
        }

        public void handleNext() {
            if (mSaveAndFinishWorker != null) return;
            // TODO(b/120484642): This is a point of entry for passwords from the UI
            final Editable passwordText = mPasswordEntry.getText();
            if (TextUtils.isEmpty(passwordText)) {
                return;
            }
            mChosenPassword = mIsAlphaMode ? LockscreenCredential.createPassword(passwordText)
                    : LockscreenCredential.createPin(passwordText);
            if (mUiStage == Stage.Introduction) {
                if (validatePassword(mChosenPassword)) {
                    mFirstPassword = mChosenPassword;
                    mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                } else {
                    mChosenPassword.zeroize();
                }
            } else if (mUiStage == Stage.NeedToConfirm) {
                if (mChosenPassword.equals(mFirstPassword)) {
                    startSaveAndFinish();
                } else {
                    CharSequence tmp = mPasswordEntry.getText();
                    if (tmp != null) {
                        Selection.setSelection((Spannable) tmp, 0, tmp.length());
                    }
                    updateStage(Stage.ConfirmWrong);
                    mChosenPassword.zeroize();
                }
            }
        }

        protected void setNextEnabled(boolean enabled) {
            mNextButton.setEnabled(enabled);
        }

        protected void setNextText(int text) {
            mNextButton.setText(getActivity(), text);
        }

        protected void onSkipOrClearButtonClick(View view) {
            mPasswordEntry.setText("");
        }

        protected void onNextButtonClick(View view) {
            handleNext();
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handleNext();
                return true;
            }
            return false;
        }

        /**
         * @param errorCode error code returned from password validation.
         * @return an array of messages describing the error, important messages come first.
         */
        String[] convertErrorCodeToMessages() {
            List<String> messages = new ArrayList<>();
            mIsErrorTooShort = false;
            for (PasswordValidationError error : mValidationErrors) {
                switch (error.errorCode) {
                    case CONTAINS_INVALID_CHARACTERS:
                        messages.add(getString(R.string.lockpassword_illegal_character));
                        break;
                    case NOT_ENOUGH_UPPER_CASE:
                        messages.add(StringUtil.getIcuPluralsString(getContext(), error.requirement,
                                R.string.lockpassword_password_requires_uppercase));
                        break;
                    case NOT_ENOUGH_LOWER_CASE:
                        messages.add(StringUtil.getIcuPluralsString(getContext(), error.requirement,
                                R.string.lockpassword_password_requires_lowercase));
                        break;
                    case NOT_ENOUGH_LETTERS:
                        messages.add(StringUtil.getIcuPluralsString(getContext(), error.requirement,
                                R.string.lockpassword_password_requires_letters));
                        break;
                    case NOT_ENOUGH_DIGITS:
                        messages.add(StringUtil.getIcuPluralsString(getContext(), error.requirement,
                                R.string.lockpassword_password_requires_numeric));
                        break;
                    case NOT_ENOUGH_SYMBOLS:
                        messages.add(StringUtil.getIcuPluralsString(getContext(), error.requirement,
                                R.string.lockpassword_password_requires_symbols));
                        break;
                    case NOT_ENOUGH_NON_LETTER:
                        messages.add(StringUtil.getIcuPluralsString(getContext(), error.requirement,
                                R.string.lockpassword_password_requires_nonletter));
                        break;
                    case NOT_ENOUGH_NON_DIGITS:
                        messages.add(StringUtil.getIcuPluralsString(getContext(), error.requirement,
                                R.string.lockpassword_password_requires_nonnumerical));
                        break;
                    case TOO_SHORT:
                        mIsErrorTooShort = true;
                        String message = StringUtil.getIcuPluralsString(getContext(),
                                error.requirement,
                                mIsAlphaMode
                                        ? R.string.lockpassword_password_too_short
                                        : R.string.lockpassword_pin_too_short);
                        if (LockPatternUtils.isAutoPinConfirmFeatureAvailable()
                                && !mIsAlphaMode
                                && error.requirement < MIN_AUTO_PIN_REQUIREMENT_LENGTH) {
                            Map<String, Object> arguments = new HashMap<>();
                            arguments.put("count", error.requirement);
                            arguments.put("minAutoConfirmLen", MIN_AUTO_PIN_REQUIREMENT_LENGTH);
                            message = StringUtil.getIcuPluralsString(getContext(),
                                    arguments,
                                    R.string.lockpassword_pin_too_short_autoConfirm_extra_message);
                        }
                        messages.add(message);
                        break;
                    case TOO_SHORT_WHEN_ALL_NUMERIC:
                        messages.add(
                                StringUtil.getIcuPluralsString(getContext(), error.requirement,
                                        R.string.lockpassword_password_too_short_all_numeric));
                        break;
                    case TOO_LONG:
                        messages.add(StringUtil.getIcuPluralsString(getContext(),
                                error.requirement + 1, mIsAlphaMode
                                        ? R.string.lockpassword_password_too_long
                                        : R.string.lockpassword_pin_too_long));
                        break;
                    case CONTAINS_SEQUENCE:
                        messages.add(getString(R.string.lockpassword_pin_no_sequential_digits));
                        break;
                    case RECENTLY_USED:
                        DevicePolicyManager devicePolicyManager =
                                getContext().getSystemService(DevicePolicyManager.class);
                        if (mIsAlphaMode) {
                            messages.add(devicePolicyManager.getResources().getString(
                                    PASSWORD_RECENTLY_USED,
                                    () -> getString(R.string.lockpassword_password_recently_used)));
                        } else {
                            messages.add(devicePolicyManager.getResources().getString(
                                    PIN_RECENTLY_USED,
                                    () -> getString(R.string.lockpassword_pin_recently_used)));
                        }
                        break;
                    default:
                        Log.wtf(TAG, "unknown error validating password: " + error);
                }
            }

            return messages.toArray(new String[0]);
        }

        /**
         * Update the hint based on current Stage and length of password entry
         */
        protected void updateUi() {
            final boolean canInput = mSaveAndFinishWorker == null;

            LockscreenCredential password = mIsAlphaMode
                    ? LockscreenCredential.createPassword(mPasswordEntry.getText())
                    : LockscreenCredential.createPin(mPasswordEntry.getText());
            final int length = password.size();

            if (mUiStage == Stage.Introduction) {
                mPasswordRestrictionView.setVisibility(View.VISIBLE);
                final boolean passwordCompliant = validatePassword(password);
                String[] messages = convertErrorCodeToMessages();
                // Update the fulfillment of requirements.
                mPasswordRequirementAdapter.setRequirements(messages, mIsErrorTooShort);
                // set the visibility of pin_auto_confirm option accordingly
                setAutoPinConfirmOption(passwordCompliant, length);
                // Enable/Disable the next button accordingly.
                setNextEnabled(passwordCompliant);
            } else {
                // Hide password requirement view when we are just asking user to confirm the pw.
                mPasswordRestrictionView.setVisibility(View.GONE);
                setHeaderText(mUiStage.getHint(getContext(), mIsAlphaMode, getStageType(),
                        mProfileType));
                setNextEnabled(canInput && length >= LockPatternUtils.MIN_LOCK_PASSWORD_SIZE);
                mSkipOrClearButton.setVisibility(toVisibility(canInput && length > 0));

                // Hide the pin_confirm option when we are just asking user to confirm the pwd.
                mAutoPinConfirmOption.setVisibility(View.GONE);
                mAutoConfirmSecurityMessage.setVisibility(View.GONE);
            }
            final int stage = getStageType();
            if (getStageType() != Stage.TYPE_NONE) {
                int message = mUiStage.getMessage(mIsAlphaMode, stage);
                if (message != 0) {
                    mMessage.setVisibility(View.VISIBLE);
                    mMessage.setText(message);
                } else {
                    mMessage.setVisibility(View.INVISIBLE);
                }
            } else {
                mMessage.setVisibility(View.GONE);
            }

            setNextText(mUiStage.buttonText);
            mPasswordEntryInputDisabler.setInputEnabled(canInput);
            password.zeroize();
        }

        protected int toVisibility(boolean visibleOrGone) {
            return visibleOrGone ? View.VISIBLE : View.GONE;
        }

        private void setAutoPinConfirmOption(boolean enabled, int length) {
            if (!LockPatternUtils.isAutoPinConfirmFeatureAvailable()
                    || mAutoPinConfirmOption == null) {
                return;
            }
            if (enabled && !mIsAlphaMode && isAutoPinConfirmPossible(length)) {
                mAutoPinConfirmOption.setVisibility(View.VISIBLE);
                mAutoConfirmSecurityMessage.setVisibility(View.VISIBLE);
                if (!mIsAutoPinConfirmOptionSetManually) {
                    mAutoPinConfirmOption.setChecked(length == MIN_AUTO_PIN_REQUIREMENT_LENGTH);
                }
            } else {
                mAutoPinConfirmOption.setVisibility(View.GONE);
                mAutoConfirmSecurityMessage.setVisibility(View.GONE);
                mAutoPinConfirmOption.setChecked(false);
            }
        }

        private boolean isAutoPinConfirmPossible(int currentPinLength) {
            return currentPinLength >= MIN_AUTO_PIN_REQUIREMENT_LENGTH;
        }

        private void setOnAutoConfirmOptionClickListener() {
            if (mAutoPinConfirmOption != null) {
                mAutoPinConfirmOption.setOnClickListener((v) -> {
                    mIsAutoPinConfirmOptionSetManually = true;
                });
            }
        }

        private void setHeaderText(String text) {
            // Only set the text if it is different than the existing one to avoid announcing again.
            if (!TextUtils.isEmpty(mLayout.getHeaderText())
                    && mLayout.getHeaderText().toString().equals(text)) {
                return;
            }
            mLayout.setHeaderText(text);
        }

        public void afterTextChanged(Editable s) {
            // Changing the text while error displayed resets to NeedToConfirm state
            if (mUiStage == Stage.ConfirmWrong) {
                mUiStage = Stage.NeedToConfirm;
            }
            // Schedule the UI update.
            mTextChangedHandler.notifyAfterTextChanged();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        private void startSaveAndFinish() {
            if (mSaveAndFinishWorker != null) {
                Log.w(TAG, "startSaveAndFinish with an existing SaveAndFinishWorker.");
                return;
            }

            ConfirmDeviceCredentialUtils.hideImeImmediately(
                    getActivity().getWindow().getDecorView());

            mPasswordEntryInputDisabler.setInputEnabled(false);
            mSaveAndFinishWorker = new SaveAndFinishWorker();
            mSaveAndFinishWorker
                    .setListener(this)
                    .setRequestGatekeeperPasswordHandle(mRequestGatekeeperPassword)
                    .setRequestWriteRepairModePassword(mRequestWriteRepairModePassword);

            getFragmentManager().beginTransaction().add(mSaveAndFinishWorker,
                    FRAGMENT_TAG_SAVE_AND_FINISH).commit();
            getFragmentManager().executePendingTransactions();

            final Intent intent = getActivity().getIntent();
            if (mUnificationProfileId != UserHandle.USER_NULL) {
                try (LockscreenCredential profileCredential = (LockscreenCredential)
                        intent.getParcelableExtra(EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL)) {
                    mSaveAndFinishWorker.setProfileToUnify(mUnificationProfileId,
                            profileCredential);
                }
            }
            // update the setting before triggering the password save workflow,
            // so that pinLength information is stored accordingly when setting is turned on.
            mLockPatternUtils.setAutoPinConfirm(
                    (mAutoPinConfirmOption != null && mAutoPinConfirmOption.isChecked()),
                    mUserId);

            mSaveAndFinishWorker.start(mLockPatternUtils,
                    mChosenPassword, mCurrentCredential, mUserId);
        }

        @Override
        public void onChosenLockSaveFinished(boolean wasSecureBefore, Intent resultData) {
            getActivity().setResult(RESULT_FINISHED, resultData);

            if (mChosenPassword != null) {
                mChosenPassword.zeroize();
            }
            if (mCurrentCredential != null) {
                mCurrentCredential.zeroize();
            }
            if (mFirstPassword != null) {
                mFirstPassword.zeroize();
            }

            mPasswordEntry.setText("");

            if (!wasSecureBefore) {
                Intent intent = getRedactionInterstitialIntent(getActivity());
                if (intent != null) {
                    startActivity(intent);
                }
            }

            if (mLayout != null) {
                mLayout.announceForAccessibility(
                        getString(R.string.accessibility_setup_password_complete));
            }

            getActivity().finish();
        }

        class TextChangedHandler extends Handler {
            private static final int ON_TEXT_CHANGED = 1;
            private static final int DELAY_IN_MILLISECOND = 100;

            /**
             * With the introduction of delay, we batch processing the text changed event to reduce
             * unnecessary UI updates.
             */
            private void notifyAfterTextChanged() {
                removeMessages(ON_TEXT_CHANGED);
                sendEmptyMessageDelayed(ON_TEXT_CHANGED, DELAY_IN_MILLISECOND);
            }

            @Override
            public void handleMessage(Message msg) {
                if (getActivity() == null) {
                    return;
                }
                if (msg.what == ON_TEXT_CHANGED) {
                    updateUi();
                }
            }
        }

        private ProfileType getProfileType() {
            UserManager userManager = getContext().createContextAsUser(UserHandle.of(mUserId),
                    /*flags=*/0).getSystemService(UserManager.class);
            if (userManager.isManagedProfile()) {
                return ProfileType.Managed;
            } else if (android.os.Flags.allowPrivateProfile()
                    && android.multiuser.Flags.enablePrivateSpaceFeatures()
                    && userManager.isPrivateProfile()) {
                return ProfileType.Private;
            } else if (userManager.isProfile()) {
                return ProfileType.Other;
            }
            return ProfileType.None;
        }
    }
}
