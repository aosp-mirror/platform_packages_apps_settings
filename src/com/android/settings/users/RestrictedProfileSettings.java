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

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.UserIcons;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.utils.ThreadUtils;

public class RestrictedProfileSettings extends AppRestrictionsFragment
        implements EditUserInfoController.OnContentChangedCallback {

    private static final String TAG = RestrictedProfileSettings.class.getSimpleName();
    public static final String FILE_PROVIDER_AUTHORITY = "com.android.settings.files";
    static final int DIALOG_ID_EDIT_USER_INFO = 1;
    private static final int DIALOG_CONFIRM_REMOVE = 2;

    private View mHeaderView;
    private ImageView mUserIconView;
    private TextView mUserNameView;
    private ImageView mDeleteButton;
    private View mSwitchUserView;
    private TextView mSwitchTitle;

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

        mSwitchTitle = mHeaderView.findViewById(R.id.switchTitle);
        mSwitchUserView = mHeaderView.findViewById(R.id.switch_pref);
        mSwitchUserView.setOnClickListener(v -> switchUser());

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

            boolean canSwitchUser =
                    mUserManager.getUserSwitchability() == UserManager.SWITCHABILITY_STATUS_OK;
            if (mShowSwitchUser && canSwitchUser) {
                mSwitchUserView.setVisibility(View.VISIBLE);
                mSwitchTitle.setText(getString(com.android.settingslib.R.string.user_switch_to_user,
                        info.name));
            } else {
                mSwitchUserView.setVisibility(View.GONE);
            }
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
                    mUserNameView.getText(), getString(R.string.profile_info_settings_title),
                    this, mUser, null);
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

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_ID_EDIT_USER_INFO:
                return SettingsEnums.DIALOG_USER_EDIT;
            case DIALOG_CONFIRM_REMOVE:
                return SettingsEnums.DIALOG_USER_REMOVE;
            default:
                return 0;
        }
    }

    private void removeUser() {
        getView().post(new Runnable() {
            public void run() {
                mUserManager.removeUser(mUser.getIdentifier());
                finishFragment();
            }
        });
    }

    private void switchUser() {
        try {
            ActivityManager.getService().switchUser(mUser.getIdentifier());
        } catch (RemoteException re) {
            Log.e(TAG, "Error while switching to other user.");
        } finally {
            finishFragment();
        }
    }

    @Override
    public void onPhotoChanged(UserHandle user, Drawable photo) {
        mUserIconView.setImageDrawable(photo);
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                mUserManager.setUserIcon(user.getIdentifier(), UserIcons.convertToBitmap(photo));
            }
        });
    }

    @Override
    public void onLabelChanged(UserHandle user, CharSequence label) {
        mUserNameView.setText(label);
        mUserManager.setUserName(user.getIdentifier(), label.toString());
    }
}
