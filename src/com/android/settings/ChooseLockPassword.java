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
import com.android.settings.ChooseLockPattern.LeftButtonMode;
import com.android.settings.ChooseLockPattern.RightButtonMode;
import com.android.settings.ChooseLockPattern.Stage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class ChooseLockPassword extends Activity implements OnClickListener {
    private final int digitIds[] = new int[] { R.id.zero, R.id.one, R.id.two, R.id.three,
            R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine };
    private TextView mPasswordTextView;
    private int mPasswordMinLength = 4;
    private int mPasswordMaxLength = 8;
    private LockPatternUtils mLockPatternUtils;
    private int mRequestedMode = LockPatternUtils.MODE_PIN;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private com.android.settings.ChooseLockPassword.Stage mUiStage = Stage.Introduction;
    private TextView mHeaderText;
    private String mFirstPin;
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
        mLockPatternUtils = new LockPatternUtils(getContentResolver());
        mRequestedMode = getIntent().getIntExtra("password_mode", mRequestedMode);
        mPasswordMinLength = getIntent().getIntExtra("password_min_length", mPasswordMinLength);
        mPasswordMaxLength = getIntent().getIntExtra("password_max_length", mPasswordMaxLength);
        initViews();
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this);
        if (savedInstanceState == null) {
            updateStage(Stage.Introduction);
            mChooseLockSettingsHelper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST);
        }
    }

    private void initViews() {
        if (LockPatternUtils.MODE_PIN == mRequestedMode
                || LockPatternUtils.MODE_PASSWORD == mRequestedMode) {
            setContentView(R.layout.choose_lock_pin);
            // TODO: alphanumeric layout
            // setContentView(R.layout.choose_lock_password);
            for (int i = 0; i < digitIds.length; i++) {
                Button button = (Button) findViewById(digitIds[i]);
                button.setOnClickListener(this);
                button.setText(Integer.toString(i));
            }
            findViewById(R.id.ok).setOnClickListener(this);
            findViewById(R.id.cancel).setOnClickListener(this);
        }
        findViewById(R.id.backspace).setOnClickListener(this);
        mPasswordTextView = (TextView) findViewById(R.id.pinDisplay);
        mHeaderText = (TextView) findViewById(R.id.headerText);
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
        mPasswordTextView.setText("");
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
            case R.id.ok:
                {
                    final String pin = mPasswordTextView.getText().toString();
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


                            mLockPatternUtils.saveLockPassword(pin);
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

            case R.id.backspace:
                {
                    final Editable digits = mPasswordTextView.getEditableText();
                    final int len = digits.length();
                    if (len > 0) {
                        digits.delete(len-1, len);
                    }
                }
                break;

            case R.id.cancel:
                finish();
                break;

            default:
                // Digits
                for (int i = 0; i < digitIds.length; i++) {
                    if (v.getId() == digitIds[i]) {
                        mPasswordTextView.append(Integer.toString(i));
                        return;
                    }
                }
                break;
        }
    }

    private void showError(String msg, final Stage next) {
        mHeaderText.setText(msg);
        mPasswordTextView.setText("");
        mHandler.postDelayed(new Runnable() {
            public void run() {
                updateStage(next);
            }
        }, ERROR_MESSAGE_TIMEOUT);
    }
}
