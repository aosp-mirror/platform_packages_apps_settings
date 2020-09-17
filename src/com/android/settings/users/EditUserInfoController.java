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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settingslib.drawable.CircleFramedDrawable;

import java.io.File;

/**
 * This class encapsulates a Dialog for editing the user nickname and photo.
 */
public class EditUserInfoController {

    private static final String KEY_AWAITING_RESULT = "awaiting_result";
    private static final String KEY_SAVED_PHOTO = "pending_photo";

    private Dialog mEditUserInfoDialog;
    private Bitmap mSavedPhoto;
    private EditUserPhotoController mEditUserPhotoController;
    private UserHandle mUser;
    private UserManager mUserManager;
    private boolean mWaitingForActivityResult = false;

    /**
     * Callback made when either the username text or photo choice changes.
     */
    public interface OnContentChangedCallback {
        /** Photo updated. */
        void onPhotoChanged(UserHandle user, Drawable photo);
        /** Username updated. */
        void onLabelChanged(UserHandle user, CharSequence label);
    }

    /**
     * Callback made when the dialog finishes.
     */
    public interface OnDialogCompleteCallback {
        /** Dialog closed with positive button. */
        void onPositive();
        /** Dialog closed with negative button or cancelled. */
        void onNegativeOrCancel();
    }

    public void clear() {
        if (mEditUserPhotoController != null) {
            mEditUserPhotoController.removeNewUserPhotoBitmapFile();
        }
        mEditUserInfoDialog = null;
        mSavedPhoto = null;
    }

    public Dialog getDialog() {
        return mEditUserInfoDialog;
    }

    public void onRestoreInstanceState(Bundle icicle) {
        String pendingPhoto = icicle.getString(KEY_SAVED_PHOTO);
        if (pendingPhoto != null) {
            mSavedPhoto = EditUserPhotoController.loadNewUserPhotoBitmap(new File(pendingPhoto));
        }
        mWaitingForActivityResult = icicle.getBoolean(KEY_AWAITING_RESULT, false);
    }

    public void onSaveInstanceState(Bundle outState) {
        if (mEditUserInfoDialog != null && mEditUserPhotoController != null) {
            // Bitmap cannot be stored into bundle because it may exceed parcel limit
            // Store it in a temporary file instead
            File file = mEditUserPhotoController.saveNewUserPhotoBitmap();
            if (file != null) {
                outState.putString(KEY_SAVED_PHOTO, file.getPath());
            }
        }
        if (mWaitingForActivityResult) {
            outState.putBoolean(KEY_AWAITING_RESULT, mWaitingForActivityResult);
        }
    }

    public void startingActivityForResult() {
        mWaitingForActivityResult = true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mWaitingForActivityResult = false;

        if (mEditUserPhotoController != null && mEditUserInfoDialog != null) {
            mEditUserPhotoController.onActivityResult(requestCode, resultCode, data);
        }
    }

    public Dialog createDialog(final Fragment fragment, final Drawable currentUserIcon,
            final CharSequence currentUserName,
            String title, final OnContentChangedCallback callback, UserHandle user,
            OnDialogCompleteCallback completeCallback) {
        Activity activity = fragment.getActivity();
        mUser = user;
        if (mUserManager == null) {
            mUserManager = activity.getSystemService(UserManager.class);
        }
        LayoutInflater inflater = activity.getLayoutInflater();
        View content = inflater.inflate(R.layout.edit_user_info_dialog_content, null);

        final EditText userNameView = (EditText) content.findViewById(R.id.user_name);
        userNameView.setText(currentUserName);

        final ImageView userPhotoView = (ImageView) content.findViewById(R.id.user_photo);

        boolean canChangePhoto = mUserManager != null &&
                canChangePhoto(activity, mUserManager.getUserInfo(user.getIdentifier()));
        if (!canChangePhoto) {
            // some users can't change their photos so we need to remove suggestive
            // background from the photoView
            userPhotoView.setBackground(null);
        }
        Drawable drawable = null;
        if (mSavedPhoto != null) {
            drawable = CircleFramedDrawable.getInstance(activity, mSavedPhoto);
        } else {
            drawable = currentUserIcon;
        }
        userPhotoView.setImageDrawable(drawable);
        if (canChangePhoto) {
            mEditUserPhotoController =
                    createEditUserPhotoController(fragment, userPhotoView, drawable);
        }
        mEditUserInfoDialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(content)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // Update the name if changed.
                            CharSequence userName = userNameView.getText();
                            if (!TextUtils.isEmpty(userName)) {
                                if (currentUserName == null
                                        || !userName.toString().equals(
                                        currentUserName.toString())) {
                                    if (callback != null) {
                                        callback.onLabelChanged(mUser, userName.toString());
                                    }
                                }
                            }
                            // Update the photo if changed.
                            if (mEditUserPhotoController != null) {
                                Drawable drawable =
                                        mEditUserPhotoController.getNewUserPhotoDrawable();
                                if (drawable != null && !drawable.equals(currentUserIcon)) {
                                    if (callback != null) {
                                        callback.onPhotoChanged(mUser, drawable);
                                    }
                                }
                            }
                        }
                        clear();
                        if (completeCallback != null) {
                            completeCallback.onPositive();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clear();
                        if (completeCallback != null) {
                            completeCallback.onNegativeOrCancel();
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        clear();
                        if (completeCallback != null) {
                            completeCallback.onNegativeOrCancel();
                        }
                    }
                })
                .create();

        // Make sure the IME is up.
        mEditUserInfoDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return mEditUserInfoDialog;
    }

    @VisibleForTesting
    boolean canChangePhoto(Context context, UserInfo user) {
        return PhotoCapabilityUtils.canCropPhoto(context) &&
                (PhotoCapabilityUtils.canChoosePhoto(context)
                        || PhotoCapabilityUtils.canTakePhoto(context));
    }

    @VisibleForTesting
    EditUserPhotoController createEditUserPhotoController(Fragment fragment,
            ImageView userPhotoView, Drawable drawable) {
        return new EditUserPhotoController(fragment, userPhotoView,
                mSavedPhoto, drawable, mWaitingForActivityResult);
    }
}
