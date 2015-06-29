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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.internal.widget.LockPatternUtils;

public class OwnerInfoSettings extends DialogFragment implements OnClickListener {

    private static final String TAG_OWNER_INFO = "ownerInfo";

    private View mView;
    private int mUserId;
    private LockPatternUtils mLockPatternUtils;
    private EditText mOwnerInfo;

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
