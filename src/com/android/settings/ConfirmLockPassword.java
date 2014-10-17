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

import android.text.TextUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;

import android.app.Activity;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ConfirmLockPassword extends SettingsActivity {

    public static final String PACKAGE = "com.android.settings";
    public static final String HEADER_TEXT = PACKAGE + ".ConfirmLockPattern.header";

    public static class InternalActivity extends ConfirmLockPassword {
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmLockPasswordFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmLockPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Disable IME on our window since we provide our own keyboard
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                //WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_confirm_your_password_header);
        setTitle(msg);
    }

    public static class ConfirmLockPasswordFragment extends Fragment implements OnClickListener,
            OnEditorActionListener, TextWatcher {
        private static final String KEY_NUM_WRONG_CONFIRM_ATTEMPTS
                = "confirm_lock_password_fragment.key_num_wrong_confirm_attempts";
        private static final long ERROR_MESSAGE_TIMEOUT = 3000;
        private TextView mPasswordEntry;
        private LockPatternUtils mLockPatternUtils;
        private TextView mHeaderText;
        private Handler mHandler = new Handler();
        private PasswordEntryKeyboardHelper mKeyboardHelper;
        private PasswordEntryKeyboardView mKeyboardView;
        private Button mContinueButton;
        private int mNumWrongConfirmAttempts;
        private CountDownTimer mCountdownTimer;
        private boolean mIsAlpha;

        // required constructor for fragments
        public ConfirmLockPasswordFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
            if (savedInstanceState != null) {
                mNumWrongConfirmAttempts = savedInstanceState.getInt(
                        KEY_NUM_WRONG_CONFIRM_ATTEMPTS, 0);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality();
            View view = inflater.inflate(R.layout.confirm_lock_password, null);
            // Disable IME on our window since we provide our own keyboard

            view.findViewById(R.id.cancel_button).setOnClickListener(this);
            mContinueButton = (Button) view.findViewById(R.id.next_button);
            mContinueButton.setOnClickListener(this);
            mContinueButton.setEnabled(false); // disable until the user enters at least one char

            mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);

            mKeyboardView = (PasswordEntryKeyboardView) view.findViewById(R.id.keyboard);
            mHeaderText = (TextView) view.findViewById(R.id.headerText);
            mIsAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality;

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                CharSequence headerMessage = intent.getCharSequenceExtra(HEADER_TEXT);
                if (TextUtils.isEmpty(headerMessage)) {
                    headerMessage = getString(getDefaultHeader());
                }
                mHeaderText.setText(headerMessage);
            }

            final Activity activity = getActivity();
            mKeyboardHelper = new PasswordEntryKeyboardHelper(activity,
                    mKeyboardView, mPasswordEntry);
            mKeyboardHelper.setKeyboardMode(mIsAlpha ?
                    PasswordEntryKeyboardHelper.KEYBOARD_MODE_ALPHA
                    : PasswordEntryKeyboardHelper.KEYBOARD_MODE_NUMERIC);
            mKeyboardView.requestFocus();

            int currentType = mPasswordEntry.getInputType();
            mPasswordEntry.setInputType(mIsAlpha ? currentType
                    : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));

            if (activity instanceof SettingsActivity) {
                final SettingsActivity sa = (SettingsActivity) activity;
                int id = getDefaultHeader();
                CharSequence title = getText(id);
                sa.setTitle(title);
            }

            return view;
        }

        private int getDefaultHeader() {
            return mIsAlpha ? R.string.lockpassword_confirm_your_password_header
                    : R.string.lockpassword_confirm_your_pin_header;
        }

        @Override
        public void onPause() {
            super.onPause();
            mKeyboardView.requestFocus();
            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
                mCountdownTimer = null;
            }
        }

        @Override
        public void onResume() {
            // TODO Auto-generated method stub
            super.onResume();
            mKeyboardView.requestFocus();
            long deadline = mLockPatternUtils.getLockoutAttemptDeadline();
            if (deadline != 0) {
                handleAttemptLockout(deadline);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(KEY_NUM_WRONG_CONFIRM_ATTEMPTS, mNumWrongConfirmAttempts);
        }

        private void handleNext() {
            final String pin = mPasswordEntry.getText().toString();
            if (mLockPatternUtils.checkPassword(pin)) {

                Intent intent = new Intent();
                if (getActivity() instanceof ConfirmLockPassword.InternalActivity) {
                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_TYPE,
                                    mIsAlpha ? StorageManager.CRYPT_TYPE_PASSWORD
                                             : StorageManager.CRYPT_TYPE_PIN);
                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, pin);
                }

                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            } else {
                if (++mNumWrongConfirmAttempts >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline();
                    handleAttemptLockout(deadline);
                } else {
                    showError(R.string.lockpattern_need_to_unlock_wrong);
                }
            }
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            showError(R.string.lockpattern_too_many_failed_confirmation_attempts_header, 0);
            mPasswordEntry.setEnabled(false);
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    mHeaderText.setText(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts_footer,
                            secondsCountdown));
                }

                @Override
                public void onFinish() {
                    mPasswordEntry.setEnabled(true);
                    mHeaderText.setText(getDefaultHeader());
                    mNumWrongConfirmAttempts = 0;
                }
            }.start();
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.next_button:
                    handleNext();
                    break;

                case R.id.cancel_button:
                    getActivity().setResult(RESULT_CANCELED);
                    getActivity().finish();
                    break;
            }
        }

        private void showError(int msg) {
            showError(msg, ERROR_MESSAGE_TIMEOUT);
        }

        private final Runnable mResetErrorRunnable = new Runnable() {
            public void run() {
                mHeaderText.setText(getDefaultHeader());
            }
        };

        private void showError(int msg, long timeout) {
            mHeaderText.setText(msg);
            mHeaderText.announceForAccessibility(mHeaderText.getText());
            mPasswordEntry.setText(null);
            mHandler.removeCallbacks(mResetErrorRunnable);
            if (timeout != 0) {
                mHandler.postDelayed(mResetErrorRunnable, timeout);
            }
        }

        // {@link OnEditorActionListener} methods.
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

        // {@link TextWatcher} methods.
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            mContinueButton.setEnabled(mPasswordEntry.getText().length() > 0);
        }
    }
}
