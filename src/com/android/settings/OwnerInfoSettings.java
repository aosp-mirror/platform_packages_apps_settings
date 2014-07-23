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
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.widget.LockPatternUtils;

public class OwnerInfoSettings extends Fragment {

    public static final String EXTRA_SHOW_NICKNAME = "show_nickname";

    private View mView;
    private CheckBox mCheckbox;
    private int mUserId;
    private LockPatternUtils mLockPatternUtils;
    private EditText mOwnerInfo;
    private EditText mNickname;
    private boolean mShowNickname;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.containsKey(EXTRA_SHOW_NICKNAME)) {
            mShowNickname = args.getBoolean(EXTRA_SHOW_NICKNAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.ownerinfo, container, false);
        mUserId = UserHandle.myUserId();
        mLockPatternUtils = new LockPatternUtils(getActivity());
        initView();
        return mView;
    }

    private void initView() {
        mNickname = (EditText) mView.findViewById(R.id.owner_info_nickname);
        if (!mShowNickname) {
            mNickname.setVisibility(View.GONE);
        } else {
            mNickname.setText(UserManager.get(getActivity()).getUserName());
            mNickname.setSelected(true);
        }

        final boolean enabled = mLockPatternUtils.isOwnerInfoEnabled();

        mCheckbox = (CheckBox) mView.findViewById(R.id.show_owner_info_on_lockscreen_checkbox);
        mCheckbox.setChecked(enabled);
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            if (UserManager.get(getActivity()).isLinkedUser()) {
                mCheckbox.setText(R.string.show_profile_info_on_lockscreen_label);
            } else {
                mCheckbox.setText(R.string.show_user_info_on_lockscreen_label);
            }
        }
        mCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mLockPatternUtils.setOwnerInfoEnabled(isChecked);
                mOwnerInfo.setEnabled(isChecked); // disable text field if not enabled
            }
        });

        String info = mLockPatternUtils.getOwnerInfo(mUserId);

        mOwnerInfo = (EditText) mView.findViewById(R.id.owner_info_edit_text);
        mOwnerInfo.setEnabled(enabled);
        if (!TextUtils.isEmpty(info)) {
            mOwnerInfo.setText(info);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChanges();
    }

    void saveChanges() {
        String info = mOwnerInfo.getText().toString();
        mLockPatternUtils.setOwnerInfo(info, mUserId);
        if (mShowNickname) {
            String oldName = UserManager.get(getActivity()).getUserName();
            CharSequence newName = mNickname.getText();
            if (!TextUtils.isEmpty(newName) && !newName.equals(oldName)) {
                UserManager.get(getActivity()).setUserName(UserHandle.myUserId(),
                        newName.toString());
            }
        }
    }
}
