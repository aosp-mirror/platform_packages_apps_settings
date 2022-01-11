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
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImeAwareEditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.PasswordValidationError;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.EncryptionInterstitial;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.notification.RedactionInterstitial;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            mIntent.putExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, false);
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
    }

    public static class ChooseLockPasswordFragment extends InstrumentedFragment
            implements OnEditorActionListener, TextWatcher, SaveAndFinishWorker.Listener {
        private static final String KEY_FIRST_PASSWORD = "first_password";
        private static final String KEY_UI_STAGE = "ui_stage";
        private static final String KEY_CURRENT_CREDENTIAL = "current_credential";
        private static final String FRAGMENT_TAG_SAVE_AND_FINISH = "save_and_finish_worker";

        private LockscreenCredential mCurrentCredential;
        private LockscreenCredential mChosenPassword;
        private boolean mRequestGatekeeperPassword;
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
        protected boolean mIsManagedProfile;
        protected FooterButton mSkipOrClearButton;
        private FooterButton mNextButton;
        private TextView mMessage;

        private TextChangedHandler mTextChangedHandler;

        private static final int CONFIRM_EXISTING_REQUEST = 58;
        static final int RESULT_FINISHED = RESULT_FIRST_USER;

        /**
         * Keep track internally of where the user is in choosing a pattern.
         */
        protected enum Stage {

            Introduction(
                    R.string.lockpassword_choose_your_password_header, // password
                    R.string.lockpassword_choose_your_profile_password_header,
                    R.string.lockpassword_choose_your_password_header_for_fingerprint,
                    R.string.lockpassword_choose_your_password_header_for_face,
                    R.string.lockpassword_choose_your_password_header_for_biometrics,
                    R.string.lockpassword_choose_your_pin_header, // pin
                    R.string.lockpassword_choose_your_profile_pin_header,
                    R.string.lockpassword_choose_your_pin_header_for_fingerprint,
                    R.string.lockpassword_choose_your_pin_header_for_face,
                    R.string.lockpassword_choose_your_pin_header_for_biometrics,
                    R.string.lockpassword_choose_password_description,
                    R.string.lock_settings_picker_biometrics_added_security_message,
                    R.string.lockpassword_choose_pin_description,
                    R.string.lock_settings_picker_biometrics_added_security_message,
                    R.string.next_label),

            NeedToConfirm(
                    R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_reenter_your_profile_password_header,
                    R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    R.string.lockpassword_reenter_your_profile_pin_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    0,
                    0,
                    0,
                    0,
                    R.string.lockpassword_confirm_label),

            ConfirmWrong(
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    0,
                    0,
                    0,
                    0,
                    R.string.lockpassword_confirm_label);

            Stage(int hintInAlpha,
                    int hintInAlphaForProfile,
                    int hintInAlphaForFingerprint,
                    int hintInAlphaForFace,
                    int hintInAlphaForBiometrics,
                    int hintInNumeric,
                    int hintInNumericForProfile,
                    int hintInNumericForFingerprint,
                    int hintInNumericForFace,
                    int hintInNumericForBiometrics,
                    int messageInAlpha,
                    int messageInAlphaForBiometrics,
                    int messageInNumeric,
                    int messageInNumericForBiometrics,
                    int nextButtonText) {

                this.alphaHint = hintInAlpha;
                this.alphaHintForProfile = hintInAlphaForProfile;
                this.alphaHintForFingerprint = hintInAlphaForFingerprint;
                this.alphaHintForFace = hintInAlphaForFace;
                this.alphaHintForBiometrics = hintInAlphaForBiometrics;

                this.numericHint = hintInNumeric;
                this.numericHintForProfile = hintInNumericForProfile;
                this.numericHintForFingerprint = hintInNumericForFingerprint;
                this.numericHintForFace = hintInNumericForFace;
                this.numericHintForBiometrics = hintInNumericForBiometrics;

                this.alphaMessage = messageInAlpha;
                this.alphaMessageForBiometrics = messageInAlphaForBiometrics;

                this.numericMessage = messageInNumeric;
                this.numericMessageForBiometrics = messageInNumericForBiometrics;

                this.buttonText = nextButtonText;
            }

            public static final int TYPE_NONE = 0;
            public static final int TYPE_FINGERPRINT = 1;
            public static final int TYPE_FACE = 2;
            public static final int TYPE_BIOMETRIC = 3;

            // Password header
            public final int alphaHint;
            public final int alphaHintForProfile;
            public final int alphaHintForFingerprint;
            public final int alphaHintForFace;
            public final int alphaHintForBiometrics;

            // PIN header
            public final int numericHint;
            public final int numericHintForProfile;
            public final int numericHintForFingerprint;
            public final int numericHintForFace;
            public final int numericHintForBiometrics;

            // Password description
            public final int alphaMessage;
            public final int alphaMessageForBiometrics;

            // PIN description
            public final int numericMessage;
            public final int numericMessageForBiometrics;

            public final int buttonText;

            public @StringRes int getHint(boolean isAlpha, int type, boolean isProfile) {
                if (isAlpha) {
                    if (type == TYPE_FINGERPRINT) {
                        return alphaHintForFingerprint;
                    } else if (type == TYPE_FACE) {
                        return alphaHintForFace;
                    } else if (type == TYPE_BIOMETRIC) {
                        return alphaHintForBiometrics;
                    } else {
                        return isProfile ? alphaHintForProfile : alphaHint;
                    }
                } else {
                    if (type == TYPE_FINGERPRINT) {
                        return numericHintForFingerprint;
                    } else if (type == TYPE_FACE) {
                        return numericHintForFace;
                    } else if (type == TYPE_BIOMETRIC) {
                        return numericHintForBiometrics;
                    } else {
                        return isProfile ? numericHintForProfile : numericHint;
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
                        return isAlpha ? alphaMessage : numericMessage;
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
            mIsManagedProfile = UserManager.get(getActivity()).isManagedProfile(mUserId);
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

            if (intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_CHANGE_CRED_REQUIRED_FOR_BOOT, false)) {
                SaveAndFinishWorker w = new SaveAndFinishWorker();
                final boolean required = getActivity().getIntent().getBooleanExtra(
                        EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true);
                LockscreenCredential currentCredential = intent.getParcelableExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);

                final LockPatternUtils utils = new LockPatternUtils(getActivity());

                w.setBlocking(true);
                w.setListener(this);
                w.start(utils, required, false /* requestGatekeeperPassword */, currentCredential,
                        currentCredential, mUserId);
            }
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
                            .setTheme(R.style.SudGlifButton_Secondary)
                            .build()
            );
            mixin.setPrimaryButton(
                    new FooterButton.Builder(getActivity())
                            .setText(R.string.next_label)
                            .setListener(this::onNextButtonClick)
                            .setButtonType(FooterButton.ButtonType.NEXT)
                            .setTheme(R.style.SudGlifButton_Primary)
                            .build()
            );
            mSkipOrClearButton = mixin.getSecondaryButton();
            mNextButton = mixin.getPrimaryButton();

            mMessage = view.findViewById(R.id.sud_layout_description);
            mLayout.setIcon(getActivity().getDrawable(R.drawable.ic_lock));

            mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == mPasswordType
                    || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == mPasswordType
                    || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == mPasswordType;

            setupPasswordRequirementsView(view);

            mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mPasswordEntry = view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);
            mPasswordEntry.requestFocus();
            mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

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
            if (savedInstanceState == null) {
                updateStage(Stage.Introduction);
                if (confirmCredentials) {
                    final ChooseLockSettingsHelper.Builder builder =
                            new ChooseLockSettingsHelper.Builder(getActivity());
                    builder.setRequestCode(CONFIRM_EXISTING_REQUEST)
                            .setTitle(getString(R.string.unlock_set_unlock_launch_picker_title))
                            .setReturnCredentials(true)
                            .setRequestGatekeeperPasswordHandle(mRequestGatekeeperPassword)
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

                mCurrentCredential = savedInstanceState.getParcelable(KEY_CURRENT_CREDENTIAL);

                // Re-attach to the exiting worker if there is one.
                mSaveAndFinishWorker = (SaveAndFinishWorker) getFragmentManager().findFragmentByTag(
                        FRAGMENT_TAG_SAVE_AND_FINISH);
            }

            if (activity instanceof SettingsActivity) {
                final SettingsActivity sa = (SettingsActivity) activity;
                int title = Stage.Introduction.getHint(mIsAlphaMode, getStageType(),
                        mIsManagedProfile);
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

        private void setupPasswordRequirementsView(View view) {
            mPasswordRestrictionView = view.findViewById(R.id.password_requirements_view);
            mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mPasswordRequirementAdapter = new PasswordRequirementAdapter();
            mPasswordRestrictionView.setAdapter(mPasswordRequirementAdapter);
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
         * and mPasswordReused to reflect validation results.
         *
         * @param credential credential the user typed in.
         * @return whether password satisfies all the requirements.
         */
        @VisibleForTesting
        boolean validatePassword(LockscreenCredential credential) {
            final byte[] password = credential.getCredential();
            mValidationErrors = PasswordMetrics.validatePassword(
                    mMinMetrics, mMinComplexity, !mIsAlphaMode, password);
            if (mValidationErrors.isEmpty() &&  mLockPatternUtils.checkPasswordHistory(
                        password, getPasswordHistoryHashFactor(), mUserId)) {
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
            for (PasswordValidationError error : mValidationErrors) {
                switch (error.errorCode) {
                    case CONTAINS_INVALID_CHARACTERS:
                        messages.add(getString(R.string.lockpassword_illegal_character));
                        break;
                    case NOT_ENOUGH_UPPER_CASE:
                        messages.add(getResources().getQuantityString(
                                R.plurals.lockpassword_password_requires_uppercase,
                                error.requirement, error.requirement));
                        break;
                    case NOT_ENOUGH_LOWER_CASE:
                        messages.add(getResources().getQuantityString(
                                R.plurals.lockpassword_password_requires_lowercase,
                                error.requirement, error.requirement));
                        break;
                    case NOT_ENOUGH_LETTERS:
                        messages.add(getResources().getQuantityString(
                                R.plurals.lockpassword_password_requires_letters,
                                error.requirement, error.requirement));
                        break;
                    case NOT_ENOUGH_DIGITS:
                        messages.add(getResources().getQuantityString(
                                R.plurals.lockpassword_password_requires_numeric,
                                error.requirement, error.requirement));
                        break;
                    case NOT_ENOUGH_SYMBOLS:
                        messages.add(getResources().getQuantityString(
                                R.plurals.lockpassword_password_requires_symbols,
                                error.requirement, error.requirement));
                        break;
                    case NOT_ENOUGH_NON_LETTER:
                        messages.add(getResources().getQuantityString(
                                R.plurals.lockpassword_password_requires_nonletter,
                                error.requirement, error.requirement));
                        break;
                    case NOT_ENOUGH_NON_DIGITS:
                        messages.add(getResources().getQuantityString(
                                R.plurals.lockpassword_password_requires_nonnumerical,
                                error.requirement, error.requirement));
                        break;
                    case TOO_SHORT:
                        messages.add(getResources().getQuantityString(
                                mIsAlphaMode
                                        ? R.plurals.lockpassword_password_too_short
                                        : R.plurals.lockpassword_pin_too_short,
                                error.requirement, error.requirement));
                        break;
                    case TOO_LONG:
                        messages.add(getResources().getQuantityString(
                                mIsAlphaMode
                                        ? R.plurals.lockpassword_password_too_long
                                        : R.plurals.lockpassword_pin_too_long,
                                error.requirement + 1, error.requirement + 1));
                        break;
                    case CONTAINS_SEQUENCE:
                        messages.add(getString(R.string.lockpassword_pin_no_sequential_digits));
                        break;
                    case RECENTLY_USED:
                        messages.add(getString(mIsAlphaMode
                                ? R.string.lockpassword_password_recently_used
                                : R.string.lockpassword_pin_recently_used));
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
                    ? LockscreenCredential.createPasswordOrNone(mPasswordEntry.getText())
                    : LockscreenCredential.createPinOrNone(mPasswordEntry.getText());
            final int length = password.size();
            if (mUiStage == Stage.Introduction) {
                mPasswordRestrictionView.setVisibility(View.VISIBLE);
                final boolean passwordCompliant = validatePassword(password);
                String[] messages = convertErrorCodeToMessages();
                // Update the fulfillment of requirements.
                mPasswordRequirementAdapter.setRequirements(messages);
                // Enable/Disable the next button accordingly.
                setNextEnabled(passwordCompliant);
            } else {
                // Hide password requirement view when we are just asking user to confirm the pw.
                mPasswordRestrictionView.setVisibility(View.GONE);
                setHeaderText(getString(mUiStage.getHint(mIsAlphaMode, getStageType(),
                        mIsManagedProfile)));
                setNextEnabled(canInput && length >= LockPatternUtils.MIN_LOCK_PASSWORD_SIZE);
                mSkipOrClearButton.setVisibility(toVisibility(canInput && length > 0));
            }
            int message = mUiStage.getMessage(mIsAlphaMode, getStageType());
            if (message != 0) {
                mMessage.setVisibility(View.VISIBLE);
                mMessage.setText(message);
            } else {
                mMessage.setVisibility(View.INVISIBLE);
            }

            setNextText(mUiStage.buttonText);
            mPasswordEntryInputDisabler.setInputEnabled(canInput);
            password.zeroize();
        }

        protected int toVisibility(boolean visibleOrGone) {
            return visibleOrGone ? View.VISIBLE : View.GONE;
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

            mPasswordEntryInputDisabler.setInputEnabled(false);
            setNextEnabled(false);

            mSaveAndFinishWorker = new SaveAndFinishWorker();
            mSaveAndFinishWorker.setListener(this);

            getFragmentManager().beginTransaction().add(mSaveAndFinishWorker,
                    FRAGMENT_TAG_SAVE_AND_FINISH).commit();
            getFragmentManager().executePendingTransactions();

            final Intent intent = getActivity().getIntent();
            final boolean required = intent.getBooleanExtra(
                    EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true);
            if (mUnificationProfileId != UserHandle.USER_NULL) {
                try (LockscreenCredential profileCredential = (LockscreenCredential)
                        intent.getParcelableExtra(EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL)) {
                    mSaveAndFinishWorker.setProfileToUnify(mUnificationProfileId,
                            profileCredential);
                }
            }
            mSaveAndFinishWorker.start(mLockPatternUtils, required, mRequestGatekeeperPassword,
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
    }

    public static class SaveAndFinishWorker extends SaveChosenLockWorkerBase {

        private LockscreenCredential mChosenPassword;
        private LockscreenCredential mCurrentCredential;

        public void start(LockPatternUtils utils, boolean required,
                boolean requestGatekeeperPassword, LockscreenCredential chosenPassword,
                LockscreenCredential currentCredential, int userId) {
            prepare(utils, required, requestGatekeeperPassword, userId);

            mChosenPassword = chosenPassword;
            mCurrentCredential = currentCredential != null ? currentCredential
                    : LockscreenCredential.createNone();
            mUserId = userId;

            start();
        }

        @Override
        protected Pair<Boolean, Intent> saveAndVerifyInBackground() {
            final boolean success = mUtils.setLockCredential(
                    mChosenPassword, mCurrentCredential, mUserId);
            if (success) {
                unifyProfileCredentialIfRequested();
            }
            Intent result = null;
            if (success && mRequestGatekeeperPassword) {
                // If a Gatekeeper Password was requested, invoke the LockSettingsService code
                // path to return a Gatekeeper Password based on the credential that the user
                // chose. This should only be run if the credential was successfully set.
                final VerifyCredentialResponse response = mUtils.verifyCredential(mChosenPassword,
                        mUserId, LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE);

                if (!response.isMatched() || !response.containsGatekeeperPasswordHandle()) {
                    Log.e(TAG, "critical: bad response or missing GK PW handle for known good"
                            + " password: " + response.toString());
                }

                result = new Intent();
                result.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                        response.getGatekeeperPasswordHandle());
            }
            return Pair.create(success, result);
        }
    }
}
