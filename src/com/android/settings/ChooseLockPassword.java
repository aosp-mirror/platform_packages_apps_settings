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

package com.android.settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;

import android.app.Activity;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ChooseLockPassword extends PreferenceActivity {
    public static final String PASSWORD_MIN_KEY = "lockscreen.password_min";
    public static final String PASSWORD_MAX_KEY = "lockscreen.password_max";
    public static final String PASSWORD_MIN_LETTERS_KEY = "lockscreen.password_min_letters";
    public static final String PASSWORD_MIN_LOWERCASE_KEY = "lockscreen.password_min_lowercase";
    public static final String PASSWORD_MIN_UPPERCASE_KEY = "lockscreen.password_min_uppercase";
    public static final String PASSWORD_MIN_NUMERIC_KEY = "lockscreen.password_min_numeric";
    public static final String PASSWORD_MIN_SYMBOLS_KEY = "lockscreen.password_min_symbols";
    public static final String PASSWORD_MIN_NONLETTER_KEY = "lockscreen.password_min_nonletter";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ChooseLockPasswordFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO: Fix on phones
        // Disable IME on our window since we provide our own keyboard
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                //WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_password_header);
        showBreadCrumbs(msg, msg);
    }

    public static class ChooseLockPasswordFragment extends Fragment
            implements OnClickListener, OnEditorActionListener,  TextWatcher {
        private static final String KEY_FIRST_PIN = "first_pin";
        private static final String KEY_UI_STAGE = "ui_stage";
        private TextView mPasswordEntry;
        private int mPasswordMinLength = 4;
        private int mPasswordMaxLength = 16;
        private int mPasswordMinLetters = 0;
        private int mPasswordMinUpperCase = 0;
        private int mPasswordMinLowerCase = 0;
        private int mPasswordMinSymbols = 0;
        private int mPasswordMinNumeric = 0;
        private int mPasswordMinNonLetter = 0;
        private LockPatternUtils mLockPatternUtils;
        private int mRequestedQuality = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private Stage mUiStage = Stage.Introduction;
        private TextView mHeaderText;
        private String mFirstPin;
        private KeyboardView mKeyboardView;
        private PasswordEntryKeyboardHelper mKeyboardHelper;
        private boolean mIsAlphaMode;
        private Button mCancelButton;
        private Button mNextButton;
        private static final int CONFIRM_EXISTING_REQUEST = 58;
        static final int RESULT_FINISHED = RESULT_FIRST_USER;
        private static final long ERROR_MESSAGE_TIMEOUT = 3000;
        private static final int MSG_SHOW_ERROR = 1;

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_SHOW_ERROR) {
                    updateStage((Stage) msg.obj);
                }
            }
        };

        /**
         * Keep track internally of where the user is in choosing a pattern.
         */
        protected enum Stage {

            Introduction(R.string.lockpassword_choose_your_password_header,
                    R.string.lockpassword_choose_your_pin_header,
                    R.string.lockpassword_continue_label),

            NeedToConfirm(R.string.lockpassword_confirm_your_password_header,
                    R.string.lockpassword_confirm_your_pin_header,
                    R.string.lockpassword_ok_label),

            ConfirmWrong(R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_continue_label);

            /**
             * @param headerMessage The message displayed at the top.
             */
            Stage(int hintInAlpha, int hintInNumeric, int nextButtonText) {
                this.alphaHint = hintInAlpha;
                this.numericHint = hintInNumeric;
                this.buttonText = nextButtonText;
            }

            public final int alphaHint;
            public final int numericHint;
            public final int buttonText;
        }

        // required constructor for fragments
        public ChooseLockPasswordFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
            Intent intent = getActivity().getIntent();
            mRequestedQuality = Math.max(intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY,
                    mRequestedQuality), mLockPatternUtils.getRequestedPasswordQuality());
            mPasswordMinLength = Math.max(
                    intent.getIntExtra(PASSWORD_MIN_KEY, mPasswordMinLength), mLockPatternUtils
                            .getRequestedMinimumPasswordLength());
            mPasswordMaxLength = intent.getIntExtra(PASSWORD_MAX_KEY, mPasswordMaxLength);
            mPasswordMinLetters = Math.max(intent.getIntExtra(PASSWORD_MIN_LETTERS_KEY,
                    mPasswordMinLetters), mLockPatternUtils.getRequestedPasswordMinimumLetters());
            mPasswordMinUpperCase = Math.max(intent.getIntExtra(PASSWORD_MIN_UPPERCASE_KEY,
                    mPasswordMinUpperCase), mLockPatternUtils.getRequestedPasswordMinimumUpperCase());
            mPasswordMinLowerCase = Math.max(intent.getIntExtra(PASSWORD_MIN_LOWERCASE_KEY,
                    mPasswordMinLowerCase), mLockPatternUtils.getRequestedPasswordMinimumLowerCase());
            mPasswordMinNumeric = Math.max(intent.getIntExtra(PASSWORD_MIN_NUMERIC_KEY,
                    mPasswordMinNumeric), mLockPatternUtils.getRequestedPasswordMinimumNumeric());
            mPasswordMinSymbols = Math.max(intent.getIntExtra(PASSWORD_MIN_SYMBOLS_KEY,
                    mPasswordMinSymbols), mLockPatternUtils.getRequestedPasswordMinimumSymbols());
            mPasswordMinNonLetter = Math.max(intent.getIntExtra(PASSWORD_MIN_NONLETTER_KEY,
                    mPasswordMinNonLetter), mLockPatternUtils.getRequestedPasswordMinimumNonLetter());

            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.choose_lock_password, null);

            mCancelButton = (Button) view.findViewById(R.id.cancel_button);
            mCancelButton.setOnClickListener(this);
            mNextButton = (Button) view.findViewById(R.id.next_button);
            mNextButton.setOnClickListener(this);

            mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == mRequestedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == mRequestedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == mRequestedQuality;
            mKeyboardView = (PasswordEntryKeyboardView) view.findViewById(R.id.keyboard);
            mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);

            final Activity activity = getActivity();
            mKeyboardHelper = new PasswordEntryKeyboardHelper(activity,
                    mKeyboardView, mPasswordEntry);
            mKeyboardHelper.setKeyboardMode(mIsAlphaMode ?
                    PasswordEntryKeyboardHelper.KEYBOARD_MODE_ALPHA
                    : PasswordEntryKeyboardHelper.KEYBOARD_MODE_NUMERIC);

            mHeaderText = (TextView) view.findViewById(R.id.headerText);
            mKeyboardView.requestFocus();

            int currentType = mPasswordEntry.getInputType();
            mPasswordEntry.setInputType(mIsAlphaMode ? currentType
                    : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));

            Intent intent = getActivity().getIntent();
            final boolean confirmCredentials = intent.getBooleanExtra("confirm_credentials", true);
            if (savedInstanceState == null) {
                updateStage(Stage.Introduction);
                if (confirmCredentials) {
                    mChooseLockSettingsHelper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST,
                            null, null);
                }
            } else {
                mFirstPin = savedInstanceState.getString(KEY_FIRST_PIN);
                final String state = savedInstanceState.getString(KEY_UI_STAGE);
                if (state != null) {
                    mUiStage = Stage.valueOf(state);
                    updateStage(mUiStage);
                }
            }
            // Update the breadcrumb (title) if this is embedded in a PreferenceActivity
            if (activity instanceof PreferenceActivity) {
                final PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
                int id = mIsAlphaMode ? R.string.lockpassword_choose_your_password_header
                        : R.string.lockpassword_choose_your_pin_header;
                CharSequence title = getText(id);
                preferenceActivity.showBreadCrumbs(title, title);
            }

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(mUiStage);
            mKeyboardView.requestFocus();
        }

        @Override
        public void onPause() {
            mHandler.removeMessages(MSG_SHOW_ERROR);

            super.onPause();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(KEY_UI_STAGE, mUiStage.name());
            outState.putString(KEY_FIRST_PIN, mFirstPin);
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
                    }
                    break;
            }
        }

        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;
            mUiStage = stage;
            updateUi();

            // If the stage changed, announce the header for accessibility. This
            // is a no-op when accessibility is disabled.
            if (previousStage != stage) {
                mHeaderText.announceForAccessibility(mHeaderText.getText());
            }
        }

        /**
         * Validates PIN and returns a message to display if PIN fails test.
         * @param password the raw password the user typed in
         * @return error message to show to user or null if password is OK
         */
        private String validatePassword(String password) {
            if (password.length() < mPasswordMinLength) {
                return getString(mIsAlphaMode ?
                        R.string.lockpassword_password_too_short
                        : R.string.lockpassword_pin_too_short, mPasswordMinLength);
            }
            if (password.length() > mPasswordMaxLength) {
                return getString(mIsAlphaMode ?
                        R.string.lockpassword_password_too_long
                        : R.string.lockpassword_pin_too_long, mPasswordMaxLength + 1);
            }
            int letters = 0;
            int numbers = 0;
            int lowercase = 0;
            int symbols = 0;
            int uppercase = 0;
            int nonletter = 0;
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
                // allow non control Latin-1 characters only
                if (c < 32 || c > 127) {
                    return getString(R.string.lockpassword_illegal_character);
                }
                if (c >= '0' && c <= '9') {
                    numbers++;
                    nonletter++;
                } else if (c >= 'A' && c <= 'Z') {
                    letters++;
                    uppercase++;
                } else if (c >= 'a' && c <= 'z') {
                    letters++;
                    lowercase++;
                } else {
                    symbols++;
                    nonletter++;
                }
            }
            if (DevicePolicyManager.PASSWORD_QUALITY_NUMERIC == mRequestedQuality
                    && (letters > 0 || symbols > 0)) {
                // This shouldn't be possible unless user finds some way to bring up
                // soft keyboard
                return getString(R.string.lockpassword_pin_contains_non_digits);
            } else if (DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == mRequestedQuality) {
                if (letters < mPasswordMinLetters) {
                    return String.format(getResources().getQuantityString(
                            R.plurals.lockpassword_password_requires_letters, mPasswordMinLetters),
                            mPasswordMinLetters);
                } else if (numbers < mPasswordMinNumeric) {
                    return String.format(getResources().getQuantityString(
                            R.plurals.lockpassword_password_requires_numeric, mPasswordMinNumeric),
                            mPasswordMinNumeric);
                } else if (lowercase < mPasswordMinLowerCase) {
                    return String.format(getResources().getQuantityString(
                            R.plurals.lockpassword_password_requires_lowercase, mPasswordMinLowerCase),
                            mPasswordMinLowerCase);
                } else if (uppercase < mPasswordMinUpperCase) {
                    return String.format(getResources().getQuantityString(
                            R.plurals.lockpassword_password_requires_uppercase, mPasswordMinUpperCase),
                            mPasswordMinUpperCase);
                } else if (symbols < mPasswordMinSymbols) {
                    return String.format(getResources().getQuantityString(
                            R.plurals.lockpassword_password_requires_symbols, mPasswordMinSymbols),
                            mPasswordMinSymbols);
                } else if (nonletter < mPasswordMinNonLetter) {
                    return String.format(getResources().getQuantityString(
                            R.plurals.lockpassword_password_requires_nonletter, mPasswordMinNonLetter),
                            mPasswordMinNonLetter);
                }
            } else {
                final boolean alphabetic = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
                        == mRequestedQuality;
                final boolean alphanumeric = DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC
                        == mRequestedQuality;
                if ((alphabetic || alphanumeric) && letters == 0) {
                    return getString(R.string.lockpassword_password_requires_alpha);
                }
                if (alphanumeric && numbers == 0) {
                    return getString(R.string.lockpassword_password_requires_digit);
                }
            }
            if(mLockPatternUtils.checkPasswordHistory(password)) {
                return getString(mIsAlphaMode ? R.string.lockpassword_password_recently_used
                        : R.string.lockpassword_pin_recently_used);
            }
            return null;
        }

        private void handleNext() {
            final String pin = mPasswordEntry.getText().toString();
            if (TextUtils.isEmpty(pin)) {
                return;
            }
            String errorMsg = null;
            if (mUiStage == Stage.Introduction) {
                errorMsg = validatePassword(pin);
                if (errorMsg == null) {
                    mFirstPin = pin;
                    mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                }
            } else if (mUiStage == Stage.NeedToConfirm) {
                if (mFirstPin.equals(pin)) {
                    final boolean isFallback = getActivity().getIntent().getBooleanExtra(
                            LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, false);
                    mLockPatternUtils.clearLock(isFallback);
                    mLockPatternUtils.saveLockPassword(pin, mRequestedQuality, isFallback);
                    getActivity().setResult(RESULT_FINISHED);
                    getActivity().finish();
                } else {
                    CharSequence tmp = mPasswordEntry.getText();
                    if (tmp != null) {
                        Selection.setSelection((Spannable) tmp, 0, tmp.length());
                    }
                    updateStage(Stage.ConfirmWrong);
                }
            }
            if (errorMsg != null) {
                showError(errorMsg, mUiStage);
            }
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.next_button:
                    handleNext();
                    break;

                case R.id.cancel_button:
                    getActivity().finish();
                    break;
            }
        }

        private void showError(String msg, final Stage next) {
            mHeaderText.setText(msg);
            mHeaderText.announceForAccessibility(mHeaderText.getText());
            Message mesg = mHandler.obtainMessage(MSG_SHOW_ERROR, next);
            mHandler.removeMessages(MSG_SHOW_ERROR);
            mHandler.sendMessageDelayed(mesg, ERROR_MESSAGE_TIMEOUT);
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
         * Update the hint based on current Stage and length of password entry
         */
        private void updateUi() {
            String password = mPasswordEntry.getText().toString();
            final int length = password.length();
            if (mUiStage == Stage.Introduction && length > 0) {
                if (length < mPasswordMinLength) {
                    String msg = getString(mIsAlphaMode ? R.string.lockpassword_password_too_short
                            : R.string.lockpassword_pin_too_short, mPasswordMinLength);
                    mHeaderText.setText(msg);
                    mNextButton.setEnabled(false);
                } else {
                    String error = validatePassword(password);
                    if (error != null) {
                        mHeaderText.setText(error);
                        mNextButton.setEnabled(false);
                    } else {
                        mHeaderText.setText(R.string.lockpassword_press_continue);
                        mNextButton.setEnabled(true);
                    }
                }
            } else {
                mHeaderText.setText(mIsAlphaMode ? mUiStage.alphaHint : mUiStage.numericHint);
                mNextButton.setEnabled(length > 0);
            }
            mNextButton.setText(mUiStage.buttonText);
        }

        public void afterTextChanged(Editable s) {
            // Changing the text while error displayed resets to NeedToConfirm state
            if (mUiStage == Stage.ConfirmWrong) {
                mUiStage = Stage.NeedToConfirm;
            }
            updateUi();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
    }
}
