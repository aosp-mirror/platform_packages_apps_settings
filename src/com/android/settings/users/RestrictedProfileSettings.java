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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;

public class RestrictedProfileSettings extends AppRestrictionsFragment
        implements EditUserInfoController.OnContentChangedCallback {

    public static final String FILE_PROVIDER_AUTHORITY = "com.android.settings.files";
    static final int DIALOG_ID_EDIT_USER_INFO = 1;
    private static final int DIALOG_CONFIRM_REMOVE = 2;

    private View mHeaderView;
    private ImageView mUserIconView;
    private TextView mUserNameView;
    private ImageView mDeleteButton;

    private EditUserInfoController mEditUserInfoController =
            new EditUserInfoController();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mEditUserInfoController.onRestoreInstanceState(icicle);
        }

        init(icicle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mHeaderView = setPinnedHeaderView(R.layout.user_info_header);
        mHeaderView.setOnClickListener(this);
        mUserIconView = (ImageView) mHeaderView.findViewById(android.R.id.icon);
        mUserNameView = (TextView) mHeaderView.findViewById(android.R.id.title);
        mDeleteButton = (ImageView) mHeaderView.findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        getListView().setFastScrollEnabled(true);
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
        UserInfo info = Utils.getExistingUser(mUserManager, mUser);
        if (info == null) {
            finishFragment();
        } else {
            ((TextView) mHeaderView.findViewById(android.R.id.title)).setText(info.name);
            ((ImageView) mHeaderView.findViewById(android.R.id.icon)).setImageDrawable(
                    com.android.settingslib.Utils.getUserIcon(getActivity(), mUserManager, info));
        }
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
        } else if (view == mDeleteButton) {
            showDialog(DIALOG_CONFIRM_REMOVE);
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
        } else if (dialogId == DIALOG_CONFIRM_REMOVE) {
            Dialog dlg =
                    UserDialogs.createRemoveDialog(getActivity(), mUser.getIdentifier(),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    removeUser();
                                }
                            }
                    );
            return dlg;
        }

        return null;
    }

    private void removeUser() {
        getView().post(new Runnable() {
            public void run() {
                mUserManager.removeUser(mUser.getIdentifier());
                finishFragment();
            }
        });
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
