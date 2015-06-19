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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;

public class OwnerInfoSettings extends DialogFragment implements OnClickListener {

    private static final String TAG_OWNER_INFO = "ownerInfo";

    private static final int MAX_CHARS = 100;

    private View mView;
    private int mUserId;
    private LockPatternUtils mLockPatternUtils;
    private EditText mOwnerInfo;
    private TextView mOwnerInfoStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = UserHandle.myUserId();
        mLockPatternUtils = new LockPatternUtils(getActivity());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mView = LayoutInflater.from(getActivity()).inflate(R.layout.ownerinfo, null);
        initView();
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.owner_info_settings_title)
                .setView(mView)
                .setPositiveButton(R.string.save, this)
                .setNegativeButton(R.string.cancel, this)
                .show();
    }

    private void initView() {
        String info = mLockPatternUtils.getOwnerInfo(mUserId);

        mOwnerInfo = (EditText) mView.findViewById(R.id.owner_info_edit_text);
        if (!TextUtils.isEmpty(info)) {
            mOwnerInfo.setText(info);
        }
        mOwnerInfoStatus = (TextView) mView.findViewById(R.id.owner_info_status);
        updateOwnerInfoStatus();

        mOwnerInfo.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateOwnerInfoStatus();
            }
        });
    }

    private void updateOwnerInfoStatus() {
        int chars = mOwnerInfo.getText().toString().length();
        String status = getString(R.string.owner_info_settings_status,
                chars, MAX_CHARS);
        String accessibilityStatus = getString(R.string.accessibility_lock_screen_progress,
                chars, MAX_CHARS);
        mOwnerInfoStatus.setText(status);
        mOwnerInfoStatus.setContentDescription(accessibilityStatus);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String info = mOwnerInfo.getText().toString();
            mLockPatternUtils.setOwnerInfoEnabled(!TextUtils.isEmpty(info), mUserId);
            mLockPatternUtils.setOwnerInfo(info, mUserId);

            if (getTargetFragment() instanceof SecuritySettings) {
                ((SecuritySettings) getTargetFragment()).updateOwnerInfo();
            }
        }
    }

    public static void show(Fragment parent) {
        if (!parent.isAdded()) return;

        final OwnerInfoSettings dialog = new OwnerInfoSettings();
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getFragmentManager(), TAG_OWNER_INFO);
    }
}
