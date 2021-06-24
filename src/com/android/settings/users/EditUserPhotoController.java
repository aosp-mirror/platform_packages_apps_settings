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
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract.DisplayPhoto;
import android.provider.MediaStore;
import android.util.EventLog;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.drawable.CircleFramedDrawable;

import libcore.io.Streams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class EditUserPhotoController {
    private static final String TAG = "EditUserPhotoController";

    // It seems that this class generates custom request codes and they may
    // collide with ours, these values are very unlikely to have a conflict.
    private static final int REQUEST_CODE_CHOOSE_PHOTO = 1001;
    private static final int REQUEST_CODE_TAKE_PHOTO   = 1002;
    private static final int REQUEST_CODE_CROP_PHOTO   = 1003;

    private static final String CROP_PICTURE_FILE_NAME = "CropEditUserPhoto.jpg";
    private static final String TAKE_PICTURE_FILE_NAME = "TakeEditUserPhoto2.jpg";
    private static final String NEW_USER_PHOTO_FILE_NAME = "NewUserPhoto.png";

    private final int mPhotoSize;

    private final Context mContext;
    private final Fragment mFragment;
    private final ImageView mImageView;

    private final Uri mCropPictureUri;
    private final Uri mTakePictureUri;

    private Bitmap mNewUserPhotoBitmap;
    private Drawable mNewUserPhotoDrawable;

    public EditUserPhotoController(Fragment fragment, ImageView view,
            Bitmap bitmap, Drawable drawable, boolean waiting) {
        mContext = view.getContext();
        mFragment = fragment;
        mImageView = view;
        mCropPictureUri = createTempImageUri(mContext, CROP_PICTURE_FILE_NAME, !waiting);
        mTakePictureUri = createTempImageUri(mContext, TAKE_PICTURE_FILE_NAME, !waiting);
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

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return false;
        }
        final Uri pictureUri = data != null && data.getData() != null
                ? data.getData() : mTakePictureUri;

        // Check if the result is a content uri
        if (!ContentResolver.SCHEME_CONTENT.equals(pictureUri.getScheme())) {
            Log.e(TAG, "Invalid pictureUri scheme: " + pictureUri.getScheme());
            EventLog.writeEvent(0x534e4554, "172939189", -1, pictureUri.getPath());
            return false;
        }

        switch (requestCode) {
            case REQUEST_CODE_CROP_PHOTO:
                onPhotoCropped(pictureUri, true);
                return true;
            case REQUEST_CODE_TAKE_PHOTO:
            case REQUEST_CODE_CHOOSE_PHOTO:
                if (mTakePictureUri.equals(pictureUri)) {
                    cropPhoto();
                } else {
                    copyAndCropPhoto(pictureUri);
                }
                return true;
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
        final Context context = mImageView.getContext();
        final boolean canTakePhoto = PhotoCapabilityUtils.canTakePhoto(context);
        final boolean canChoosePhoto = PhotoCapabilityUtils.canChoosePhoto(context);

        if (!canTakePhoto && !canChoosePhoto) {
            return;
        }

        final List<EditUserPhotoController.RestrictedMenuItem> items = new ArrayList<>();

        if (canTakePhoto) {
            final String title = context.getString(R.string.user_image_take_photo);
            final Runnable action = new Runnable() {
                @Override
                public void run() {
                    takePhoto();
                }
            };
            items.add(new RestrictedMenuItem(context, title, UserManager.DISALLOW_SET_USER_ICON,
                    action));
        }

        if (canChoosePhoto) {
            final String title = context.getString(R.string.user_image_choose_photo);
            final Runnable action = new Runnable() {
                @Override
                public void run() {
                    choosePhoto();
                }
            };
            items.add(new RestrictedMenuItem(context, title, UserManager.DISALLOW_SET_USER_ICON,
                    action));
        }

        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);

        listPopupWindow.setAnchorView(mImageView);
        listPopupWindow.setModal(true);
        listPopupWindow.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        listPopupWindow.setAdapter(new RestrictedPopupMenuAdapter(context, items));

        final int width = Math.max(mImageView.getWidth(), context.getResources()
                .getDimensionPixelSize(R.dimen.update_user_photo_popup_min_width));
        listPopupWindow.setWidth(width);
        listPopupWindow.setDropDownGravity(Gravity.START);

        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listPopupWindow.dismiss();
                final RestrictedMenuItem item =
                        (RestrictedMenuItem) parent.getAdapter().getItem(position);
                item.doAction();
            }
        });

        listPopupWindow.show();
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        appendOutputExtra(intent, mTakePictureUri);
        mFragment.startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        appendOutputExtra(intent, mTakePictureUri);
        mFragment.startActivityForResult(intent, REQUEST_CODE_CHOOSE_PHOTO);
    }

    private void copyAndCropPhoto(final Uri pictureUri) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final ContentResolver cr = mContext.getContentResolver();
                try (InputStream in = cr.openInputStream(pictureUri);
                        OutputStream out = cr.openOutputStream(mTakePictureUri)) {
                    Streams.copy(in, out);
                } catch (IOException e) {
                    Log.w(TAG, "Failed to copy photo", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!mFragment.isAdded()) return;
                cropPhoto();
            }
        }.execute();
    }

    private void cropPhoto() {
        // TODO: Use a public intent, when there is one.
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(mTakePictureUri, "image/*");
        appendOutputExtra(intent, mCropPictureUri);
        appendCropExtras(intent);
        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            try {
                StrictMode.disableDeathOnFileUriExposure();
                mFragment.startActivityForResult(intent, REQUEST_CODE_CROP_PHOTO);
            } finally {
                StrictMode.enableDeathOnFileUriExposure();
            }
        } else {
            onPhotoCropped(mTakePictureUri, false);
        }
    }

    private void appendOutputExtra(Intent intent, Uri pictureUri) {
        intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri(MediaStore.EXTRA_OUTPUT, pictureUri));
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

    private void onPhotoCropped(final Uri data, final boolean cropped) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                if (cropped) {
                    InputStream imageStream = null;
                    try {
                        imageStream = mContext.getContentResolver()
                                .openInputStream(data);
                        return BitmapFactory.decodeStream(imageStream);
                    } catch (FileNotFoundException fe) {
                        Log.w(TAG, "Cannot find image file", fe);
                        return null;
                    } finally {
                        if (imageStream != null) {
                            try {
                                imageStream.close();
                            } catch (IOException ioe) {
                                Log.w(TAG, "Cannot close image stream", ioe);
                            }
                        }
                    }
                } else {
                    // Scale and crop to a square aspect ratio
                    Bitmap croppedImage = Bitmap.createBitmap(mPhotoSize, mPhotoSize,
                            Config.ARGB_8888);
                    Canvas canvas = new Canvas(croppedImage);
                    Bitmap fullImage = null;
                    try {
                        InputStream imageStream = mContext.getContentResolver()
                                .openInputStream(data);
                        fullImage = BitmapFactory.decodeStream(imageStream);
                    } catch (FileNotFoundException fe) {
                        return null;
                    }
                    if (fullImage != null) {
                        final int squareSize = Math.min(fullImage.getWidth(),
                                fullImage.getHeight());
                        final int left = (fullImage.getWidth() - squareSize) / 2;
                        final int top = (fullImage.getHeight() - squareSize) / 2;
                        Rect rectSource = new Rect(left, top,
                                left + squareSize, top + squareSize);
                        Rect rectDest = new Rect(0, 0, mPhotoSize, mPhotoSize);
                        Paint paint = new Paint();
                        canvas.drawBitmap(fullImage, rectSource, rectDest, paint);
                        return croppedImage;
                    } else {
                        // Bah! Got nothin.
                        return null;
                    }
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    mNewUserPhotoBitmap = bitmap;
                    mNewUserPhotoDrawable = CircleFramedDrawable
                            .getInstance(mImageView.getContext(), mNewUserPhotoBitmap);
                    mImageView.setImageDrawable(mNewUserPhotoDrawable);
                }
                new File(mContext.getCacheDir(), TAKE_PICTURE_FILE_NAME).delete();
                new File(mContext.getCacheDir(), CROP_PICTURE_FILE_NAME).delete();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
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

    private Uri createTempImageUri(Context context, String fileName, boolean purge) {
        final File folder = context.getCacheDir();
        folder.mkdirs();
        final File fullPath = new File(folder, fileName);
        if (purge) {
            fullPath.delete();
        }
        return FileProvider.getUriForFile(context, Utils.FILE_PROVIDER_AUTHORITY, fullPath);
    }

    File saveNewUserPhotoBitmap() {
        if (mNewUserPhotoBitmap == null) {
            return null;
        }
        try {
            File file = new File(mContext.getCacheDir(), NEW_USER_PHOTO_FILE_NAME);
            OutputStream os = new FileOutputStream(file);
            mNewUserPhotoBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
            return file;
        } catch (IOException e) {
            Log.e(TAG, "Cannot create temp file", e);
        }
        return null;
    }

    static Bitmap loadNewUserPhotoBitmap(File file) {
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    void removeNewUserPhotoBitmapFile() {
        new File(mContext.getCacheDir(), NEW_USER_PHOTO_FILE_NAME).delete();
    }

    private static final class RestrictedMenuItem {
        private final Context mContext;
        private final String mTitle;
        private final Runnable mAction;
        private final RestrictedLockUtils.EnforcedAdmin mAdmin;
        // Restriction may be set by system or something else via UserManager.setUserRestriction().
        private final boolean mIsRestrictedByBase;

        /**
         * The menu item, used for popup menu. Any element of such a menu can be disabled by admin.
         * @param context A context.
         * @param title The title of the menu item.
         * @param restriction The restriction, that if is set, blocks the menu item.
         * @param action The action on menu item click.
         */
        public RestrictedMenuItem(Context context, String title, String restriction,
                Runnable action) {
            mContext = context;
            mTitle = title;
            mAction = action;

            final int myUserId = UserHandle.myUserId();
            mAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(context,
                    restriction, myUserId);
            mIsRestrictedByBase = RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                    restriction, myUserId);
        }

        @Override
        public String toString() {
            return mTitle;
        }

        final void doAction() {
            if (isRestrictedByBase()) {
                return;
            }

            if (isRestrictedByAdmin()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext, mAdmin);
                return;
            }

            mAction.run();
        }

        final boolean isRestrictedByAdmin() {
            return mAdmin != null;
        }

        final boolean isRestrictedByBase() {
            return mIsRestrictedByBase;
        }
    }

    /**
     * Provide this adapter to ListPopupWindow.setAdapter() to have a popup window menu, where
     * any element can be restricted by admin (profile owner or device owner).
     */
    private static final class RestrictedPopupMenuAdapter extends ArrayAdapter<RestrictedMenuItem> {
        public RestrictedPopupMenuAdapter(Context context, List<RestrictedMenuItem> items) {
            super(context, R.layout.restricted_popup_menu_item, R.id.text, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            final RestrictedMenuItem item = getItem(position);
            final TextView text = (TextView) view.findViewById(R.id.text);
            final ImageView image = (ImageView) view.findViewById(R.id.restricted_icon);

            text.setEnabled(!item.isRestrictedByAdmin() && !item.isRestrictedByBase());
            image.setVisibility(item.isRestrictedByAdmin() && !item.isRestrictedByBase() ?
                    ImageView.VISIBLE : ImageView.GONE);

            return view;
        }
    }
}
