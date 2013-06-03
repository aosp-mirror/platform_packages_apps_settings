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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.ContactsContract.DisplayPhoto;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import com.android.settings.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RestrictedProfileSettings extends AppRestrictionsFragment {

    private static final String KEY_SAVED_PHOTO = "pending_photo";
    private static final int DIALOG_ID_EDIT_USER_INFO = 1;

    private View mHeaderView;
    private ImageView mUserIconView;
    private TextView mUserNameView;

    private Dialog mEditUserInfoDialog;
    private EditUserPhotoController mEditUserPhotoController;
    private Bitmap mSavedPhoto;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mSavedPhoto = (Bitmap) icicle.getParcelable(KEY_SAVED_PHOTO);
        }

        init(icicle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (mHeaderView == null) {
            mHeaderView = LayoutInflater.from(getActivity()).inflate(
                    R.layout.user_info_header, null);
            ((ViewGroup) getListView().getParent()).addView(mHeaderView, 0);
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
        if (mEditUserInfoDialog != null && mEditUserInfoDialog.isShowing()
                && mEditUserPhotoController != null) {
            outState.putParcelable(KEY_SAVED_PHOTO,
                    mEditUserPhotoController.getNewUserPhotoBitmap());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        UserInfo info = mUserManager.getUserInfo(mUser.getIdentifier());
        ((TextView) mHeaderView.findViewById(android.R.id.title)).setText(info.name);
        ((ImageView) mHeaderView.findViewById(android.R.id.icon)).setImageDrawable(
                getCircularUserIcon());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mEditUserInfoDialog != null && mEditUserInfoDialog.isShowing()
                && mEditUserPhotoController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
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
            if (mEditUserInfoDialog != null) {
                return mEditUserInfoDialog;
            }

            LayoutInflater inflater = getActivity().getLayoutInflater();
            View content = inflater.inflate(R.layout.edit_user_info_dialog_content, null);

            UserInfo info = mUserManager.getUserInfo(mUser.getIdentifier());

            final EditText userNameView = (EditText) content.findViewById(R.id.user_name);
            userNameView.setText(info.name);

            final ImageView userPhotoView = (ImageView) content.findViewById(R.id.user_photo);
            Drawable drawable = null;
            if (mSavedPhoto != null) {
                drawable = CircleFramedDrawable.getInstance(getActivity(), mSavedPhoto);
            } else {
                drawable = mUserIconView.getDrawable();
                if (drawable == null) {
                    drawable = getCircularUserIcon();
                }
            }
            userPhotoView.setImageDrawable(drawable);

            mEditUserPhotoController = new EditUserPhotoController(this, userPhotoView,
                    mSavedPhoto, drawable);

            mEditUserInfoDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.profile_info_settings_title)
                .setIconAttribute(R.drawable.ic_settings_multiuser)
                .setView(content)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // Update the name if changed.
                            CharSequence userName = userNameView.getText();
                            if (!TextUtils.isEmpty(userName)) {
                                CharSequence oldUserName = mUserNameView.getText();
                                if (oldUserName == null
                                        || !userName.toString().equals(oldUserName.toString())) {
                                    ((TextView) mHeaderView.findViewById(android.R.id.title))
                                            .setText(userName.toString());
                                    mUserManager.setUserName(mUser.getIdentifier(),
                                            userName.toString());
                                }
                            }
                            // Update the photo if changed.
                            Drawable drawable = mEditUserPhotoController.getNewUserPhotoDrawable();
                            Bitmap bitmap = mEditUserPhotoController.getNewUserPhotoBitmap();
                            if (drawable != null && bitmap != null
                                    && !drawable.equals(mUserIconView.getDrawable())) {
                                mUserIconView.setImageDrawable(drawable);
                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        mUserManager.setUserIcon(mUser.getIdentifier(),
                                                mEditUserPhotoController.getNewUserPhotoBitmap());
                                        return null;
                                    }
                                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                            }
                            removeDialog(DIALOG_ID_EDIT_USER_INFO);
                        }
                        clearEditUserInfoDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel,  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearEditUserInfoDialog();
                    }
                 })
                .create();

            // Make sure the IME is up.
            mEditUserInfoDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            return mEditUserInfoDialog;
        }

        return null;
    }

    private void clearEditUserInfoDialog() {
        mEditUserInfoDialog = null;
        mSavedPhoto = null;
    }

    private static class EditUserPhotoController {
        private static final int POPUP_LIST_ITEM_ID_CHOOSE_PHOTO = 1;
        private static final int POPUP_LIST_ITEM_ID_TAKE_PHOTO = 2;

        // It seems that this class generates custom request codes and they may
        // collide with ours, these values are very unlikely to have a conflict.
        private static final int REQUEST_CODE_CHOOSE_PHOTO = 1;
        private static final int REQUEST_CODE_TAKE_PHOTO   = 2;
        private static final int REQUEST_CODE_CROP_PHOTO   = 3;

        private static final String CROP_PICTURE_FILE_NAME = "CropEditUserPhoto.jpg";
        private static final String TAKE_PICTURE_FILE_NAME = "TakeEditUserPhoto2.jpg";

        private final int mPhotoSize;

        private final Context mContext;
        private final Fragment mFragment;
        private final ImageView mImageView;

        private final Uri mCropPictureUri;
        private final Uri mTakePictureUri;

        private Bitmap mNewUserPhotoBitmap;
        private Drawable mNewUserPhotoDrawable;

        public EditUserPhotoController(Fragment fragment, ImageView view,
                Bitmap bitmap, Drawable drawable) {
            mContext = view.getContext();
            mFragment = fragment;
            mImageView = view;
            mCropPictureUri = createTempImageUri(mContext, CROP_PICTURE_FILE_NAME);
            mTakePictureUri = createTempImageUri(mContext, TAKE_PICTURE_FILE_NAME);
            mPhotoSize = getPhotoSize(mContext);
            mImageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUpdatePhotoPopup();
                }
            });
            mNewUserPhotoBitmap = bitmap;
            mNewUserPhotoDrawable = drawable;
        }

        public boolean onActivityResult(int requestCode, int resultCode, final Intent data) {
            if (resultCode != Activity.RESULT_OK) {
                return false;
            }
            switch (requestCode) {
                case REQUEST_CODE_CHOOSE_PHOTO:
                case REQUEST_CODE_CROP_PHOTO: {
                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            return BitmapFactory.decodeFile(mCropPictureUri.getPath());
                        }
                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            mNewUserPhotoBitmap = bitmap;
                            mNewUserPhotoDrawable = CircleFramedDrawable
                                    .getInstance(mImageView.getContext(), mNewUserPhotoBitmap);
                            mImageView.setImageDrawable(mNewUserPhotoDrawable);
                            // Delete the files - not needed anymore.
                            new File(mCropPictureUri.getPath()).delete();
                            new File(mTakePictureUri.getPath()).delete();
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                } return true;
                case REQUEST_CODE_TAKE_PHOTO: {
                    cropPhoto();
                } break;
            }
            return false;
        }

        public Bitmap getNewUserPhotoBitmap() {
            return mNewUserPhotoBitmap;
        }

        public Drawable getNewUserPhotoDrawable() {
            return mNewUserPhotoDrawable;
        }

        private void showUpdatePhotoPopup() {
            final boolean canTakePhoto = canTakePhoto();
            final boolean canChoosePhoto = canChoosePhoto();

            if (!canTakePhoto && !canChoosePhoto) {
                return;
            }

            Context context = mImageView.getContext();
            final List<AdapterItem> items = new ArrayList<AdapterItem>();

            if (canTakePhoto()) {
                String title = mImageView.getContext().getString( R.string.user_image_take_photo);
                AdapterItem item = new AdapterItem(title, POPUP_LIST_ITEM_ID_TAKE_PHOTO);
                items.add(item);
            }

            if (canChoosePhoto) {
                String title = context.getString(R.string.user_image_choose_photo);
                AdapterItem item = new AdapterItem(title, POPUP_LIST_ITEM_ID_CHOOSE_PHOTO);
                items.add(item);
            }

            final ListPopupWindow listPopupWindow = new ListPopupWindow(context);

            listPopupWindow.setAnchorView(mImageView);
            listPopupWindow.setModal(true);
            listPopupWindow.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);

            ListAdapter adapter = new ArrayAdapter<AdapterItem>(context,
                    R.layout.edit_user_photo_popup_item, items);
            listPopupWindow.setAdapter(adapter);

            final int width = Math.max(mImageView.getWidth(), context.getResources()
                    .getDimensionPixelSize(R.dimen.update_user_photo_popup_min_width));
            listPopupWindow.setWidth(width);

            listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AdapterItem item = items.get(position);
                    switch (item.id) {
                        case POPUP_LIST_ITEM_ID_CHOOSE_PHOTO: {
                            choosePhoto();
                            listPopupWindow.dismiss();
                        } break;
                        case POPUP_LIST_ITEM_ID_TAKE_PHOTO: {
                            takePhoto();
                            listPopupWindow.dismiss();
                        } break;
                    }
                }
            });

            listPopupWindow.show();
        }

        private boolean canTakePhoto() {
            return mImageView.getContext().getPackageManager().queryIntentActivities(
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
        }

        private boolean canChoosePhoto() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            return mImageView.getContext().getPackageManager().queryIntentActivities(
                    intent, 0).size() > 0;
        }

        private void takePhoto() {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mTakePictureUri);
            mFragment.startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
        }

        private void choosePhoto() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mCropPictureUri);
            appendCropExtras(intent);
            mFragment.startActivityForResult(intent, REQUEST_CODE_CHOOSE_PHOTO);
        }

        private void cropPhoto() {
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(mTakePictureUri, "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mCropPictureUri);
            appendCropExtras(intent);
            mFragment.startActivityForResult(intent, REQUEST_CODE_CROP_PHOTO);
        }

        private void appendCropExtras(Intent intent) {
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", mPhotoSize);
            intent.putExtra("outputY", mPhotoSize);
        }

        private static int getPhotoSize(Context context) {
            Cursor cursor = context.getContentResolver().query(
                    DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
                    new String[]{DisplayPhoto.DISPLAY_MAX_DIM}, null, null, null);
            try {
                cursor.moveToFirst();
                return cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }

        private static Uri createTempImageUri(Context context, String fileName) {
            File folder = context.getExternalCacheDir();
            folder.mkdirs();
            File fullPath = new File(folder, fileName);
            fullPath.delete();
            return Uri.fromFile(fullPath.getAbsoluteFile());
        }

        private static final class AdapterItem {
            final String title;
            final int id;

            public AdapterItem(String title, int id) {
                this.title = title;
                this.id = id;
            }

            @Override
            public String toString() {
                return title;
            }
        }
    }

}
