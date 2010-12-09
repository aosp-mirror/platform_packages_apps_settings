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

import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ConfirmLockPassword extends PreferenceActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmLockPasswordFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Disable IME on our window since we provide our own keyboard
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                //WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);
    }

    public static class ConfirmLockPasswordFragment extends Fragment implements OnClickListener,
            OnEditorActionListener {
        private static final long ERROR_MESSAGE_TIMEOUT = 3000;
        private TextView mPasswordEntry;
        private LockPatternUtils mLockPatternUtils;
        private TextView mHeaderText;
        private Handler mHandler = new Handler();
        private PasswordEntryKeyboardHelper mKeyboardHelper;
        private PasswordEntryKeyboardView mKeyboardView;


        // required constructor for fragments
        public ConfirmLockPasswordFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality();
            View view = inflater.inflate(R.layout.confirm_lock_password, null);
            // Disable IME on our window since we provide our own keyboard

            view.findViewById(R.id.cancel_button).setOnClickListener(this);
            view.findViewById(R.id.next_button).setOnClickListener(this);
            mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mKeyboardView = (PasswordEntryKeyboardView) view.findViewById(R.id.keyboard);
            mHeaderText = (TextView) view.findViewById(R.id.headerText);
            final boolean isAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality;
            mHeaderText.setText(isAlpha ? R.string.lockpassword_confirm_your_password_header
                    : R.string.lockpassword_confirm_your_pin_header);
            mKeyboardHelper = new PasswordEntryKeyboardHelper(getActivity(),
                        mKeyboardView, mPasswordEntry);
            mKeyboardHelper.setKeyboardMode(isAlpha ? PasswordEntryKeyboardHelper.KEYBOARD_MODE_ALPHA
                    : PasswordEntryKeyboardHelper.KEYBOARD_MODE_NUMERIC);
            mKeyboardView.requestFocus();
            return view;
        }

        @Override
        public void onPause() {
            super.onPause();
            mKeyboardView.requestFocus();
        }

        @Override
        public void onResume() {
            // TODO Auto-generated method stub
            super.onResume();
            mKeyboardView.requestFocus();
        }

        private void handleNext() {
            final String pin = mPasswordEntry.getText().toString();
            if (mLockPatternUtils.checkPassword(pin)) {
                getActivity().setResult(RESULT_OK);
                getActivity().finish();
            } else {
                showError(R.string.lockpattern_need_to_unlock_wrong);
            }
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
            mHeaderText.setText(msg);
            mPasswordEntry.setText(null);
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mHeaderText.setText(R.string.lockpassword_confirm_your_password_header);
                }
            }, ERROR_MESSAGE_TIMEOUT);
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Check if this was the result of hitting the enter key
            if (actionId == EditorInfo.IME_NULL) {
                handleNext();
                return true;
            }
            return false;
        }
    }
}
