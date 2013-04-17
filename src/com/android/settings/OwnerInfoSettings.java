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

import android.app.Fragment;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.widget.LockPatternUtils;

public class OwnerInfoSettings extends Fragment {
    private View mView;
    private CheckBox mCheckbox;
    private EditText mEditText;
    private int mUserId;
    private LockPatternUtils mLockPatternUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.ownerinfo, container, false);
        mUserId = UserHandle.myUserId();
        mLockPatternUtils = new LockPatternUtils(getActivity());
        initView(mView);
        return mView;
    }

    private void initView(View view) {
        final ContentResolver res = getActivity().getContentResolver();
        String info = mLockPatternUtils.getOwnerInfo(mUserId);
        boolean enabled = mLockPatternUtils.isOwnerInfoEnabled();
        mCheckbox = (CheckBox) mView.findViewById(R.id.show_owner_info_on_lockscreen_checkbox);
        mEditText = (EditText) mView.findViewById(R.id.owner_info_edit_text);
        mEditText.setText(info);
        mEditText.setEnabled(enabled);
        mCheckbox.setChecked(enabled);
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            mCheckbox.setText(R.string.show_user_info_on_lockscreen_label);
        }
        mCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mLockPatternUtils.setOwnerInfoEnabled(isChecked);
                mEditText.setEnabled(isChecked); // disable text field if not enabled
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        saveToDb();
    }

    void saveToDb() {
        String info = mEditText.getText().toString();
        mLockPatternUtils.setOwnerInfo(info, mUserId);
    }

}
