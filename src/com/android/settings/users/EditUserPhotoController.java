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
import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.ContactsContract.DisplayPhoto;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;

import com.android.settings.R;
import com.android.settingslib.drawable.CircleFramedDrawable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditUserPhotoController {
    private static final String TAG = "EditUserPhotoController";

    private static final int POPUP_LIST_ITEM_ID_CHOOSE_PHOTO = 1;
    private static final int POPUP_LIST_ITEM_ID_TAKE_PHOTO = 2;

    // It seems that this class generates custom request codes and they may
    // collide with ours, these values are very unlikely to have a conflict.
    private static final int REQUEST_CODE_CHOOSE_PHOTO = 1001;
    private static final int REQUEST_CODE_TAKE_PHOTO   = 1002;
    private static final int REQUEST_CODE_CROP_PHOTO   = 1003;

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
        switch (requestCode) {
            case REQUEST_CODE_CROP_PHOTO:
                onPhotoCropped(pictureUri, true);
                return true;
            case REQUEST_CODE_TAKE_PHOTO:
            case REQUEST_CODE_CHOOSE_PHOTO:
                cropPhoto(pictureUri);
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
        final boolean canTakePhoto = canTakePhoto();
        final boolean canChoosePhoto = canChoosePhoto();

        if (!canTakePhoto && !canChoosePhoto) {
            return;
        }

        Context context = mImageView.getContext();
        final List<EditUserPhotoController.AdapterItem> items = new ArrayList<EditUserPhotoController.AdapterItem>();

        if (canTakePhoto()) {
            String title = mImageView.getContext().getString( R.string.user_image_take_photo);
            EditUserPhotoController.AdapterItem item = new AdapterItem(title, POPUP_LIST_ITEM_ID_TAKE_PHOTO);
            items.add(item);
        }

        if (canChoosePhoto) {
            String title = context.getString(R.string.user_image_choose_photo);
            EditUserPhotoController.AdapterItem item = new AdapterItem(title, POPUP_LIST_ITEM_ID_CHOOSE_PHOTO);
            items.add(item);
        }

        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);

        listPopupWindow.setAnchorView(mImageView);
        listPopupWindow.setModal(true);
        listPopupWindow.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);

        ListAdapter adapter = new ArrayAdapter<EditUserPhotoController.AdapterItem>(context,
                R.layout.edit_user_photo_popup_item, items);
        listPopupWindow.setAdapter(adapter);

        final int width = Math.max(mImageView.getWidth(), context.getResources()
                .getDimensionPixelSize(R.dimen.update_user_photo_popup_min_width));
        listPopupWindow.setWidth(width);
        listPopupWindow.setDropDownGravity(Gravity.START);

        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EditUserPhotoController.AdapterItem item = items.get(position);
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
        appendOutputExtra(intent, mTakePictureUri);
        mFragment.startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        appendOutputExtra(intent, mTakePictureUri);
        mFragment.startActivityForResult(intent, REQUEST_CODE_CHOOSE_PHOTO);
    }

    private void cropPhoto(Uri pictureUri) {
        // TODO: Use a public intent, when there is one.
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(pictureUri, "image/*");
        appendOutputExtra(intent, mCropPictureUri);
        appendCropExtras(intent);
        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            mFragment.startActivityForResult(intent, REQUEST_CODE_CROP_PHOTO);
        } else {
            onPhotoCropped(pictureUri, false);
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
        final Uri fileUri =
                FileProvider.getUriForFile(context, RestrictedProfileSettings.FILE_PROVIDER_AUTHORITY, fullPath);
        return fileUri;
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
