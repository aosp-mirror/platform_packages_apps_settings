/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.users;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

import java.util.List;

public class RestrictedProfileSettings extends AppRestrictionsFragment
        implements EditUserInfoController.OnContentChangedCallback {

    static final String KEY_SAVED_PHOTO = "pending_photo";
    static final String KEY_AWAITING_RESULT = "awaiting_result";
    static final int DIALOG_ID_EDIT_USER_INFO = 1;
    public static final String FILE_PROVIDER_AUTHORITY = "com.android.settings.files";

    private View mHeaderView;
    private ImageView mUserIconView;
    private TextView mUserNameView;

    private EditUserInfoController mEditUserInfoController =
            new EditUserInfoController();
    private boolean mWaitingForActivityResult;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mEditUserInfoController.onRestoreInstanceState(icicle);
            mWaitingForActivityResult = icicle.getBoolean(KEY_AWAITING_RESULT, false);
        }

        init(icicle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (mHeaderView == null) {
            mHeaderView = LayoutInflater.from(getActivity()).inflate(
                    R.layout.user_info_header, null);
            setPinnedHeaderView(mHeaderView);
            mHeaderView.setOnClickListener(this);
            mUserIconView = (ImageView) mHeaderView.findViewById(android.R.id.icon);
            mUserNameView = (TextView) mHeaderView.findViewById(android.R.id.title);
            getListView().setFastScrollEnabled(true);
        }
        // This is going to bind the preferences.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mEditUserInfoController.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if user still exists
        UserInfo info = getExistingUser(mUser);
        if (info == null) {
            finishFragment();
        } else {
            ((TextView) mHeaderView.findViewById(android.R.id.title)).setText(info.name);
            ((ImageView) mHeaderView.findViewById(android.R.id.icon)).setImageDrawable(
                    getCircularUserIcon());
        }
    }

    private UserInfo getExistingUser(UserHandle thisUser) {
        final List<UserInfo> users = mUserManager.getUsers(true); // Only get non-dying
        for (UserInfo user : users) {
            if (user.id == thisUser.getIdentifier()) {
                return user;
            }
        }
        return null;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        mEditUserInfoController.startingActivityForResult();
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mEditUserInfoController.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        if (view == mHeaderView) {
            showDialog(DIALOG_ID_EDIT_USER_INFO);
        } else {
            super.onClick(view); // in AppRestrictionsFragment
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_USER_INFO) {
            return mEditUserInfoController.createDialog(this, mUserIconView.getDrawable(),
                    mUserNameView.getText(), R.string.profile_info_settings_title,
                    this, mUser);
        }

        return null;
    }

    @Override
    public void onPhotoChanged(Drawable photo) {
        mUserIconView.setImageDrawable(photo);
    }

    @Override
    public void onLabelChanged(CharSequence label) {
        mUserNameView.setText(label);
    }
}
