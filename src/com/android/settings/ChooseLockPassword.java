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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;
import com.android.settings.ChooseLockPattern.LeftButtonMode;
import com.android.settings.ChooseLockPattern.RightButtonMode;
import com.android.settings.ChooseLockPattern.Stage;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class ChooseLockPassword extends Activity implements OnClickListener {
    private TextView mPasswordEntry;
    private int mPasswordMinLength = 4;
    private int mPasswordMaxLength = 8;
    private LockPatternUtils mLockPatternUtils;
    private int mRequestedMode = LockPatternUtils.MODE_PIN;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private com.android.settings.ChooseLockPassword.Stage mUiStage = Stage.Introduction;
    private TextView mHeaderText;
    private String mFirstPin;
    private KeyboardView mKeyboardView;
    private PasswordEntryKeyboardHelper mKeyboardHelper;
    public static final String PASSWORD_MIN_KEY = "lockscreen.password_min";
    public static final String PASSWORD_MAX_KEY = "lockscreen.password_max";
    private static Handler mHandler = new Handler();
    private static final int CONFIRM_EXISTING_REQUEST = 58;
    static final int RESULT_FINISHED = RESULT_FIRST_USER;
    private static final long ERROR_MESSAGE_TIMEOUT = 3000;

    /**
     * Keep track internally of where the user is in choosing a pattern.
     */
    protected enum Stage {

        Introduction(R.string.lockpassword_choose_your_password_header),
        NeedToConfirm(R.string.lockpassword_confirm_your_password_header),
        ConfirmWrong(R.string.lockpassword_confirm_passwords_dont_match),
        ChoiceConfirmed(R.string.lockpassword_password_confirmed_header);

        /**
         * @param headerMessage The message displayed at the top.
         */
        Stage(int headerMessage) {
            this.headerMessage = headerMessage;
        }

        final int headerMessage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockPatternUtils = new LockPatternUtils(this);
        mRequestedMode = getIntent().getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, mRequestedMode);
        mPasswordMinLength = getIntent().getIntExtra("password_min_length", mPasswordMinLength);
        mPasswordMaxLength = getIntent().getIntExtra("password_max_length", mPasswordMaxLength);
        int minMode = mLockPatternUtils.getRequestedPasswordMode();
        if (mRequestedMode < minMode) {
            mRequestedMode = minMode;
        }
        int minLength = mLockPatternUtils.getRequestedMinimumPasswordLength();
        if (mPasswordMinLength < minLength) {
            mPasswordMinLength = minLength;
        }
        initViews();
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this);
        if (savedInstanceState == null) {
            updateStage(Stage.Introduction);
            mChooseLockSettingsHelper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST);
        }
    }

    private void initViews() {
        setContentView(R.layout.choose_lock_password);
        // Disable IME on our window since we provide our own keyboard
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        findViewById(R.id.cancel_button).setOnClickListener(this);
        findViewById(R.id.next_button).setOnClickListener(this);

        mKeyboardView = (PasswordEntryKeyboardView) findViewById(R.id.keyboard);
        mPasswordEntry = (TextView) findViewById(R.id.password_entry);

        final boolean isAlpha = LockPatternUtils.MODE_PASSWORD == mRequestedMode;
        mKeyboardHelper = new PasswordEntryKeyboardHelper(this, mKeyboardView, mPasswordEntry);
        mKeyboardHelper.setKeyboardMode(isAlpha ? PasswordEntryKeyboardHelper.KEYBOARD_MODE_ALPHA
                : PasswordEntryKeyboardHelper.KEYBOARD_MODE_NUMERIC);

        mHeaderText = (TextView) findViewById(R.id.headerText);
        mKeyboardView.requestFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mKeyboardView.requestFocus();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        updateStage(mUiStage);
        mKeyboardView.requestFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CONFIRM_EXISTING_REQUEST:
                if (resultCode != Activity.RESULT_OK) {
                    setResult(RESULT_FINISHED);
                    finish();
                }
                break;
        }
    }

    protected void updateStage(Stage stage) {
        mHeaderText.setText(stage.headerMessage);
        mPasswordEntry.setText("");
        mUiStage = stage;
    }

    /**
     * Validates PIN and returns a message to display if PIN fails test.
     * @param pin
     * @return message id to display to user
     */
    private String validatePassword(String pin) {
        if (pin.length() < mPasswordMinLength) {
            return getString(R.string.pin_password_too_short, mPasswordMinLength);
        }
        if (pin.length() > mPasswordMaxLength) {
            return getString(R.string.pin_password_too_long, mPasswordMaxLength);
        }
        if (LockPatternUtils.MODE_PIN == mRequestedMode) {
            Pattern p = Pattern.compile("[0-9]+");
            Matcher m = p.matcher(pin);
            if (!m.find()) {
                return getString(R.string.pin_password_contains_non_digits);
            }
        } else if (LockPatternUtils.MODE_PASSWORD == mRequestedMode) {
            // allow Latin-1 characters only
            for (int i = 0; i < pin.length(); i++) {
                char c = pin.charAt(i);
                if (c <= 32 || c > 127) {
                    return getString(R.string.pin_password_illegal_character);
                }
            }
        }
        return null;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next_button:
                {
                    final String pin = mPasswordEntry.getText().toString();
                    if (TextUtils.isEmpty(pin)) {
                        break;
                    }
                    String errorMsg = null;
                    if (mUiStage == Stage.Introduction) {
                        errorMsg = validatePassword(pin);
                        if (errorMsg == null) {
                            mFirstPin = pin;
                            updateStage(Stage.NeedToConfirm);
                        }
                    } else if (mUiStage == Stage.NeedToConfirm) {
                        if (mFirstPin.equals(pin)) {
                            // TODO: move these to LockPatternUtils
                            mLockPatternUtils.setLockPatternEnabled(false);
                            mLockPatternUtils.saveLockPattern(null);
                            mLockPatternUtils.saveLockPassword(pin, mRequestedMode);
                            finish();
                        } else {
                            int msg = R.string.lockpassword_confirm_passwords_dont_match;
                            errorMsg = getString(msg);
                        }
                    }
                    if (errorMsg != null) {
                        showError(errorMsg, Stage.Introduction);
                    }
                }
                break;

            case R.id.cancel_button:
                finish();
                break;
        }
    }

    private void showError(String msg, final Stage next) {
        mHeaderText.setText(msg);
        mPasswordEntry.setText("");
        mHandler.postDelayed(new Runnable() {
            public void run() {
                updateStage(next);
            }
        }, ERROR_MESSAGE_TIMEOUT);
    }
}
