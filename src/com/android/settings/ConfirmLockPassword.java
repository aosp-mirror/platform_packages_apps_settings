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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ConfirmLockPassword extends Activity implements OnClickListener {
    private static final long ERROR_MESSAGE_TIMEOUT = 3000;
    private final int digitIds[] = new int[] { R.id.zero, R.id.one, R.id.two, R.id.three,
            R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine };
    private TextView mPasswordTextView;
    private LockPatternUtils mLockPatternUtils;
    private TextView mHeaderText;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockPatternUtils = new LockPatternUtils(getContentResolver());
        initViews();
    }

    private void initViews() {
        int mode = mLockPatternUtils.getPasswordMode();
        if (LockPatternUtils.MODE_PIN == mode || LockPatternUtils.MODE_PASSWORD == mode) {
            setContentView(R.layout.confirm_lock_pin);
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
        mHeaderText.setText(R.string.lockpassword_confirm_your_password_header);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok:
                {
                    final String pin = mPasswordTextView.getText().toString();
                    if (mLockPatternUtils.checkPassword(pin)) {
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        showError(R.string.lockpattern_need_to_unlock_wrong);
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
                setResult(RESULT_CANCELED);
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

    private void showError(int msg) {
        mHeaderText.setText(msg);
        mPasswordTextView.setText(null);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mHeaderText.setText(R.string.lockpassword_confirm_your_password_header);
            }
        }, ERROR_MESSAGE_TIMEOUT);
    }
}
