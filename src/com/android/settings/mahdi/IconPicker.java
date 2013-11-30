/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.mahdi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.util.mahdi.LockscreenTargetUtils;
import com.android.settings.R;

import java.io.File;

public class IconPicker {
    private static final String ICON_ACTION = "com.mahdi.ACTION_PICK_ICON";
    public static final String RESOURCE_NAME = "resource_name";
    public static final String PACKAGE_NAME = "package_name";

    public static final int REQUEST_PICK_SYSTEM = 0;
    public static final int REQUEST_PICK_GALLERY = 1;
    public static final int REQUEST_PICK_ICON_PACK = 2;

    private Activity mParent;
    private Resources mResources;
    private OnIconPickListener mIconListener;

    public interface OnIconPickListener {
        void iconPicked(int requestCode, int resultCode, Intent in);
    }

    public IconPicker(Activity parent, OnIconPickListener listener) {
        mParent = parent;
        mResources = parent.getResources();
        mIconListener = listener;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIconListener.iconPicked(requestCode, resultCode, data);
    }

    public void pickIcon(final int fragmentId, final File image) {
        Intent iconPackIntent = new Intent(ICON_ACTION);
        ComponentName component = iconPackIntent.resolveActivity(mParent.getPackageManager());

        String[] items = new String[component != null ? 2 : 1];
        items[0] = mResources.getString(R.string.icon_picker_system_icons_title);
        //items[1] = mResources.getString(R.string.icon_picker_gallery_title);
        if (component != null) {
            items[1] = mResources.getString(R.string.icon_picker_pack_title);
        }

        new AlertDialog.Builder(mParent)
                .setTitle(R.string.icon_picker_title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        showChosen(item, image, fragmentId);
                    }
                })
                .show();
    }

    private void startFragmentOrActivityForResult(Intent pickIntent,
            int requestCode, int fragmentId) {
        if (fragmentId == 0) {
            mParent.startActivityForResult(pickIntent, requestCode);
        } else {
            Fragment fragment = mParent.getFragmentManager().findFragmentById(fragmentId);
            if (fragment != null) {
                mParent.startActivityFromFragment(fragment, pickIntent, requestCode);
            }
        }
    }

    private void showChosen(final int type, File image, int fragmentId) {
        if (type == REQUEST_PICK_SYSTEM) {
            ListView list = new ListView(mParent);
            final IconAdapter adapter = new IconAdapter();

            final Dialog dialog = new Dialog(mParent);
            dialog.setTitle(R.string.icon_picker_choose_icon_title);
            dialog.setContentView(list);

            list.setAdapter(adapter);
            list.setOnItemClickListener(new OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent();
                    intent.putExtra(RESOURCE_NAME, adapter.getItemReference(position));
                    mIconListener.iconPicked(type, Activity.RESULT_OK, intent);
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else if (type == REQUEST_PICK_GALLERY) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", 162);
            intent.putExtra("outputY", 162);
            try {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                intent.putExtra("return-data", false);
                startFragmentOrActivityForResult(intent, type, fragmentId);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        } else if (type == REQUEST_PICK_ICON_PACK) {
            Intent iconPackIntent = new Intent(ICON_ACTION);
            startFragmentOrActivityForResult(iconPackIntent, type, fragmentId);
        }
    }

    class IconAdapter extends BaseAdapter {
        String[] labels;
        String[] icons;

        public IconAdapter() {
            labels = mResources.getStringArray(R.array.lockscreen_icon_picker_labels);
            icons = mResources.getStringArray(R.array.lockscreen_icon_picker_icons);
        }

        @Override
        public int getCount() {
            return labels.length;
        }

        @Override
        public Object getItem(int position) {
            return LockscreenTargetUtils.getDrawableFromResources(
                    mParent, null, icons[position], false);
        }

        public String getItemReference(int position) {
            String name = icons[position];
            return name;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (convertView == null) {
                view = View.inflate(mParent, android.R.layout.simple_list_item_1, null);
            }

            TextView label = (TextView) view.findViewById(android.R.id.text1);
            label.setText(labels[position]);

            Drawable icon = ((Drawable) getItem(position)).mutate();
            int bound = mParent.getResources().getDimensionPixelSize(
                    R.dimen.shortcut_picker_left_padding);

            icon.setBounds(0,  0, bound, bound);
            label.setCompoundDrawables(icon, null, null, null);

            return view;
        }
    }
}
