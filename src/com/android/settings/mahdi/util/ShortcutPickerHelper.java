/*
 * Copyright (C) 2011 The CyanogenMod Project
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

package com.android.settings.mahdi.util;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.android.settings.R;
public class ShortcutPickerHelper {

    private Fragment mParent;
    private OnPickListener mListener;

    public static final int REQUEST_PICK_SHORTCUT = 100;
    public static final int REQUEST_PICK_APPLICATION = 101;
    public static final int REQUEST_CREATE_SHORTCUT = 102;

    public interface OnPickListener {
        /**
         * Callback after a shortcut is picked
         *
         * @param uri Intent for the shortcut
         * @param friendlyName Title
         * @param icon Icon for the shortcut, or null
         * @param isApplication true for standard app, false for "shortcut"
         */
        void shortcutPicked(String uri, String friendlyName, Bitmap icon, boolean isApplication);
    }

    public ShortcutPickerHelper(Fragment parent, OnPickListener listener) {
        mParent = parent;
        mListener = listener;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_APPLICATION:
                    completeSetCustomApp(data);
                    break;
                case REQUEST_CREATE_SHORTCUT:
                    completeSetCustomShortcut(data);
                    break;
                case REQUEST_PICK_SHORTCUT:
                    processShortcut(data, REQUEST_PICK_APPLICATION, REQUEST_CREATE_SHORTCUT);
                    break;
            }
        }
    }

    public void pickShortcut() {
        Bundle bundle = new Bundle();

        ArrayList<String> shortcutNames = new ArrayList<String>();
        shortcutNames.add(mParent.getString(R.string.group_applications));
        bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

        ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
        shortcutIcons.add(ShortcutIconResource.fromContext(mParent.getActivity(),
                R.drawable.ic_launcher));
        bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

        Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
        pickIntent.putExtra(Intent.EXTRA_TITLE, mParent.getText(R.string.select_custom_app_title));
        pickIntent.putExtras(bundle);

        mParent.startActivityForResult(pickIntent, REQUEST_PICK_SHORTCUT);
    }

    private void processShortcut(Intent intent,
        int requestCodeApplication, int requestCodeShortcut) {
        // Handle case where user selected "Applications"
        String applicationName = mParent.getResources().getString(R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            mParent.startActivityForResult(pickIntent, requestCodeApplication);
        } else {
            mParent.startActivityForResult(intent, requestCodeShortcut);
        }
    }

    private void completeSetCustomApp(Intent data) {
        mListener.shortcutPicked(data.toUri(0), getFriendlyActivityName(data, false), null, true);
    }

    private void completeSetCustomShortcut(Intent data) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        /* preserve shortcut name, we want to restore it later */
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
            data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
        String appUri = intent.toUri(0);
        appUri = appUri.replaceAll("com.android.contacts.action.QUICK_CONTACT",
                "android.intent.action.VIEW");
        /* Try to get the icon (if any) */
        Bitmap bmp = null;
        Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (extra != null && extra instanceof Bitmap)
            bmp = (Bitmap) extra;
        if (bmp == null) {
            extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof Intent.ShortcutIconResource) {
                try {
                    Intent.ShortcutIconResource iconResource = (ShortcutIconResource) extra;
                    final PackageManager packageManager = mParent.getActivity().getPackageManager();
                    Resources resources =
                        packageManager.getResourcesForApplication(iconResource.packageName);
                    final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    bmp = BitmapFactory.decodeResource(resources, id);
                } catch (Exception e) {
                    Log.w("ROMControl.ShortcutPicker", "Could not load shortcut icon: " + extra);
                }
            }
        }
        mListener.shortcutPicked(appUri, getFriendlyShortcutName(intent), bmp, false);
    }

    private String getFriendlyActivityName(Intent intent, boolean labelOnly) {
        PackageManager pm = mParent.getActivity().getPackageManager();
        ActivityInfo ai = intent.resolveActivityInfo(pm, PackageManager.GET_ACTIVITIES);
        String friendlyName = null;

        if (ai != null) {
            friendlyName = ai.loadLabel(pm).toString();
            if (friendlyName == null && !labelOnly) {
                friendlyName = ai.name;
            }
        }

        return friendlyName != null || labelOnly ? friendlyName : intent.toUri(0);
    }

    private String getFriendlyShortcutName(Intent intent) {
        String activityName = getFriendlyActivityName(intent, true);
        String name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (activityName != null && name != null) {
            return activityName + ": " + name;
        }
        return name != null ? name : intent.toUri(0);
    }

    public String getFriendlyNameForUri(String uri) {
        if (uri == null) {
            return null;
        }

        try {
            Intent intent = Intent.parseUri(uri, 0);
            if (Intent.ACTION_MAIN.equals(intent.getAction())) {
                return getFriendlyActivityName(intent, false);
            }
            return getFriendlyShortcutName(intent);
        } catch (URISyntaxException e) {
        }

        return uri;
    }
}
