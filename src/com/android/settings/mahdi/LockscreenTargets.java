/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.internal.util.mahdi.LockscreenTargetUtils;
import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.internal.widget.multiwaveview.TargetDrawable;
import com.android.settings.R;
import com.android.settings.mahdi.IconPicker.OnIconPickListener;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class LockscreenTargets extends Fragment implements
        ShortcutPickHelper.OnPickListener, GlowPadView.OnTriggerListener, OnIconPickListener {
    private static final String TAG = "LockscreenTargets";

    private Activity mActivity;
    private Resources mResources;
    private ShortcutPickHelper mPicker;
    private IconPicker mIconPicker;

    private GlowPadView mWaveView;
    private ViewGroup mContainer;

    private ImageButton mDialogIcon;
    private Button mDialogLabel;

    private ArrayList<TargetInfo> mTargetStore = new ArrayList<TargetInfo>();
    private int mTargetOffset;
    private int mMaxTargets;

    private File mTemporaryImage;
    private int mTargetIndex = 0;
    private static String mEmptyLabel;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;

    private static class TargetInfo {
        String uri;
        String packageName;
        StateListDrawable icon;
        Drawable defaultIcon;
        String iconType;
        String iconSource;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;

        setHasOptionsMenu(true);

        mActivity = getActivity();
        mResources = getResources();

        mTargetOffset = LockscreenTargetUtils.getTargetOffset(mActivity);
        mMaxTargets = LockscreenTargetUtils.getMaxTargets(mActivity);

        mIconPicker = new IconPicker(mActivity, this);
        mPicker = new ShortcutPickHelper(mActivity, this);

        mTemporaryImage = new File(mActivity.getCacheDir() + "/target.tmp");
        mEmptyLabel = mResources.getString(R.string.lockscreen_target_empty);

        return inflater.inflate(R.layout.lockscreen_targets, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWaveView = (GlowPadView) mActivity.findViewById(R.id.lock_target);
        Drawable handle = LockscreenTargetUtils.getDrawableFromResources(mActivity, null, "ic_lockscreen_handle", false);
        mWaveView.setHandleDrawable(handle);
        mWaveView.setOnTriggerListener(this);

        initializeView(Settings.System.getString(mActivity.getContentResolver(),
                Settings.System.LOCKSCREEN_TARGETS));
    }

    @Override
    public void onResume() {
        super.onResume();
        // If running on a phone, remove padding around container
        if (!LockscreenTargetUtils.isScreenLarge(mActivity)) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
            .setIcon(R.drawable.ic_settings_backup) // use the backup icon
            .setAlphabeticShortcut('r')
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetAll();
                return true;
            default:
                return false;
        }
    }

    private void initializeView(String input) {
        if (input == null) {
            input = LockscreenTargetUtils.EMPTY_TARGET;
        }

        mTargetStore.clear();

        final Drawable activeBack = LockscreenTargetUtils.getDrawableFromResources(
                mActivity, null, "ic_lockscreen_target_activated" ,false);
        final String[] targetStore = input.split("\\|");

        for (int i = 0; i < mTargetOffset; i++) {
            mTargetStore.add(new TargetInfo());
        }

        //Add the unlock icon
        Drawable unlockFront = LockscreenTargetUtils.getDrawableFromResources(
                mActivity, null, "ic_lockscreen_unlock_normal" ,false);
        Drawable unlockBack = LockscreenTargetUtils.getDrawableFromResources(
                mActivity, null, "ic_lockscreen_unlock_activated" ,false);
        TargetInfo unlockTarget = new TargetInfo();
        unlockTarget.icon = LockscreenTargetUtils.getLayeredDrawable(
                mActivity, unlockBack, unlockFront, 0, true);
        mTargetStore.add(unlockTarget);

        for (int i = 0; i < 8 - mTargetOffset - 1; i++) {
            if (i >= mMaxTargets) {
                mTargetStore.add(new TargetInfo());
                continue;
            }

            Drawable front = null;
            Drawable back = activeBack;
            boolean frontBlank = false;
            TargetInfo info = new TargetInfo();
            info.uri = i < targetStore.length ? targetStore[i] : LockscreenTargetUtils.EMPTY_TARGET;

             if (!info.uri.equals(LockscreenTargetUtils.EMPTY_TARGET)) {
                try {
                    Intent intent = Intent.parseUri(info.uri, 0);
                    if (intent.hasExtra(LockscreenTargetUtils.ICON_FILE)) {
                        info.iconType = LockscreenTargetUtils.ICON_FILE;
                        info.iconSource = intent.getStringExtra(LockscreenTargetUtils.ICON_FILE);
                        front = LockscreenTargetUtils.getDrawableFromFile(mActivity,
                                info.iconSource);
                    } else if (intent.hasExtra(LockscreenTargetUtils.ICON_RESOURCE)) {
                        info.iconType = LockscreenTargetUtils.ICON_RESOURCE;
                        info.iconSource = intent.getStringExtra(LockscreenTargetUtils.ICON_RESOURCE);
                        info.packageName = intent.getStringExtra(LockscreenTargetUtils.ICON_PACKAGE);

                        if (info.iconSource != null) {
                            front = LockscreenTargetUtils.getDrawableFromResources(mActivity,
                                    null, info.iconSource, false);
                            back = LockscreenTargetUtils.getDrawableFromResources(mActivity,
                                    null, info.iconSource, true);
                            frontBlank = true;
                        }
                    }
                    if (front == null) {
                        info.iconType = null;
                        front = LockscreenTargetUtils.getDrawableFromIntent(mActivity, intent);
                    }
                } catch (URISyntaxException e) {
                    Log.w(TAG, "Invalid lockscreen target " + info.uri);
                }
            }

            if (front == null) {
                front = mResources.getDrawable(R.drawable.ic_empty);
            }
            if (back == null) {
                back = activeBack;
            }

            int inset = LockscreenTargetUtils.getInsetForIconType(mActivity, info.iconType);
            info.icon = LockscreenTargetUtils.getLayeredDrawable(mActivity,
                    back, front, inset, frontBlank);
            info.defaultIcon = front;

            mTargetStore.add(info);
        }

        ArrayList<TargetDrawable> targetDrawables = new ArrayList<TargetDrawable>();
        for (TargetInfo i : mTargetStore) {
            targetDrawables.add(new TargetDrawable(mResources, i != null ? i.icon : null));
        }
        mWaveView.setTargetResources(targetDrawables);
    }

    /**
     * Resets the target layout to stock
     */
    private void resetAll() {
        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.lockscreen_target_reset_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.lockscreen_target_reset_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        initializeView(null);
                        Settings.System.putString(mActivity.getContentResolver(),
                                Settings.System.LOCKSCREEN_TARGETS, null);
                        Toast.makeText(mActivity, R.string.lockscreen_target_reset,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Save targets to settings provider
     */
    private void saveAll() {
        StringBuilder targetLayout = new StringBuilder();
        ArrayList<String> existingImages = new ArrayList<String>();
        boolean hasValidTargets = false;

        for (int i = mTargetOffset + 1; i <= mTargetOffset + mMaxTargets; i++) {
            TargetInfo info = mTargetStore.get(i);
            String uri = info.uri;

            if (info.iconSource != null) {
                existingImages.add(info.iconSource);
            }

            if (!TextUtils.equals(uri, LockscreenTargetUtils.EMPTY_TARGET)) {
                try {
                    Intent intent = Intent.parseUri(info.uri, 0);
                    // make sure to remove any outdated icon references
                    intent.removeExtra(LockscreenTargetUtils.ICON_RESOURCE);
                    intent.removeExtra(LockscreenTargetUtils.ICON_FILE);
                    if (info.iconType != null) {
                        intent.putExtra(info.iconType, info.iconSource);
                    }
                    if (LockscreenTargetUtils.ICON_RESOURCE.equals(info.iconType)
                            && info.packageName != null) {
                        intent.putExtra(LockscreenTargetUtils.ICON_PACKAGE, info.packageName);
                    } else {
                        intent.removeExtra(LockscreenTargetUtils.ICON_PACKAGE);
                    }

                    uri = intent.toUri(0);
                    hasValidTargets = true;
                } catch (URISyntaxException e) {
                    Log.w(TAG, "Invalid uri " + info.uri + " on save, ignoring");
                    uri = LockscreenTargetUtils.EMPTY_TARGET;
                }
            }

            if (targetLayout.length() > 0) {
                targetLayout.append("|");
            }
            targetLayout.append(uri);
        }

        final String targets = hasValidTargets ? targetLayout.toString() : null;
        Settings.System.putString(mActivity.getContentResolver(),
                Settings.System.LOCKSCREEN_TARGETS, targets);

        for (File image : mActivity.getFilesDir().listFiles()) {
            if (image.getName().startsWith("lockscreen_")
                    && !existingImages.contains(image.getAbsolutePath())) {
                image.delete();
            }
        }
    }

    /**
     * Updates a target in the GlowPadView
     */
    private void setTarget(int position, String uri, Drawable drawable,
            String iconType, String iconSource, String packageName) {
        TargetInfo item = mTargetStore.get(position);
        StateListDrawable state = (StateListDrawable) item.icon;
        LayerDrawable inactiveLayer = (LayerDrawable) state.getStateDrawable(0);
        LayerDrawable activeLayer = (LayerDrawable) state.getStateDrawable(1);
        boolean hasBackground = false;

        inactiveLayer.setDrawableByLayerId(1, drawable);

        if (LockscreenTargetUtils.ICON_RESOURCE.equals(iconType) && iconSource != null) {
            InsetDrawable empty = new InsetDrawable(
                    mResources.getDrawable(android.R.color.transparent), 0, 0, 0, 0);
            activeLayer.setDrawableByLayerId(1, empty);
            Drawable back = LockscreenTargetUtils.getDrawableFromResources(mActivity,
                    packageName, iconSource, true);
            if (back != null) {
                activeLayer.setDrawableByLayerId(0, back);
                hasBackground = true;
            }
        } else {
            activeLayer.setDrawableByLayerId(1, drawable);
        }

        if (!hasBackground) {
            final Drawable activeBack = LockscreenTargetUtils.getDrawableFromResources(
                    mActivity, null, "ic_lockscreen_target_activated", false);
            activeLayer.setDrawableByLayerId(0, new InsetDrawable(activeBack, 0, 0, 0, 0));
        }

        item.defaultIcon = getPickedIconFromDialog();
        item.uri = uri;
        item.iconType = iconType;
        item.iconSource = iconSource;
        item.packageName = packageName;

        saveAll();
    }

    private Drawable getPickedIconFromDialog() {
        return mDialogIcon.getDrawable().mutate();
    }

    private void setIconForDialog(Drawable icon) {
        // need to mutate the drawable here to not share drawable state with GlowPadView
        mDialogIcon.setImageDrawable(icon.getConstantState().newDrawable().mutate());
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        if (uri == null) {
            return;
        }

        try {
            Intent intent = Intent.parseUri(uri, 0);
            Drawable icon = LockscreenTargetUtils.getDrawableFromIntent(mActivity, intent);

            mDialogLabel.setText(friendlyName);
            mDialogLabel.setTag(uri);
            // this is a fresh drawable, so we can assign it directly
            mDialogIcon.setImageDrawable(icon);
            mDialogIcon.setTag(null);
        } catch (URISyntaxException e) {
            Log.wtf(TAG, "Invalid uri " + uri + " on pick");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String shortcutName = null;
        if (data != null) {
            shortcutName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        }

        if (TextUtils.equals(shortcutName, mEmptyLabel)) {
            mDialogLabel.setText(mEmptyLabel);
            mDialogLabel.setTag(LockscreenTargetUtils.EMPTY_TARGET);
            mDialogIcon.setImageResource(R.drawable.ic_empty);
            mDialogIcon.setTag(null);
        } else if (requestCode == IconPicker.REQUEST_PICK_SYSTEM
                || requestCode == IconPicker.REQUEST_PICK_GALLERY
                || requestCode == IconPicker.REQUEST_PICK_ICON_PACK) {
            mIconPicker.onActivityResult(requestCode, resultCode, data);
        } else if (requestCode != Activity.RESULT_CANCELED
                && resultCode != Activity.RESULT_CANCELED) {
            mPicker.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onGrabbed(View v, int handle) {
    }

    @Override
    public void onReleased(View v, int handle) {
    }

    @Override
    public void onTrigger(View v, final int target) {
        mTargetIndex = target;

        if (target == mTargetOffset) {
            mWaveView.reset(true);
            return;
        }

        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.lockscreen_target_edit_title)
                .setView(createShortcutDialogView(target))
                .setPositiveButton(R.string.ok,  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TargetInfo info = (TargetInfo) mDialogIcon.getTag();
                        String type = info != null ? info.iconType : null;
                        String source = info != null ? info.iconSource : null;
                        String packageName = info != null ? info.packageName : null;
                        int inset = LockscreenTargetUtils.getInsetForIconType(mActivity, type);

                        InsetDrawable drawable = new InsetDrawable(getPickedIconFromDialog(),
                                inset, inset, inset, inset);
                        setTarget(mTargetIndex, mDialogLabel.getTag().toString(),
                                drawable, type, source, packageName);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show();
    }

    private View createShortcutDialogView(int target) {
        View view = View.inflate(mActivity, R.layout.lockscreen_shortcut_dialog, null);
        view.findViewById(R.id.icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mDialogLabel.getText().equals(mEmptyLabel)) {
                    try {
                        mTemporaryImage.createNewFile();
                        mTemporaryImage.setWritable(true, false);
                        mIconPicker.pickIcon(getId(), mTemporaryImage);
                    } catch (IOException e) {
                        Log.d(TAG, "Could not create temporary icon", e);
                    }
                }
            }
        });
        view.findViewById(R.id.label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] names = new String[] { mEmptyLabel };
                ShortcutIconResource[] icons = new ShortcutIconResource[] {
                    ShortcutIconResource.fromContext(mActivity, android.R.drawable.ic_delete)
                };
                mPicker.pickShortcut(names, icons, getId());
            }
        });

        mDialogIcon = (ImageButton) view.findViewById(R.id.icon);
        mDialogLabel = (Button) view.findViewById(R.id.label);

        TargetInfo item = mTargetStore.get(target);
        setIconForDialog(item.defaultIcon);

        TargetInfo icon = new TargetInfo();
        icon.iconType = item.iconType;
        icon.iconSource = item.iconSource;
        icon.packageName = item.packageName;
        mDialogIcon.setTag(icon);

        if (TextUtils.equals(item.uri, LockscreenTargetUtils.EMPTY_TARGET)) {
            mDialogLabel.setText(mEmptyLabel);
        } else {
            mDialogLabel.setText(mPicker.getFriendlyNameForUri(item.uri));
        }
        mDialogLabel.setTag(item.uri);

        return view;
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {
    }

    @Override
    public void iconPicked(int requestCode, int resultCode, Intent intent) {
        TargetInfo icon = new TargetInfo();
        Drawable iconDrawable = null;

        if (requestCode == IconPicker.REQUEST_PICK_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                File imageFile = new File(mActivity.getFilesDir(),
                        "/lockscreen_" + System.currentTimeMillis() + ".png");
                if (mTemporaryImage.exists()) {
                    mTemporaryImage.renameTo(imageFile);
                }
                imageFile.setReadOnly();

                icon.iconType = LockscreenTargetUtils.ICON_FILE;
                icon.iconSource = imageFile.getAbsolutePath();
                iconDrawable = LockscreenTargetUtils.getDrawableFromFile(
                        mActivity, icon.iconSource);
            } else {
                if (mTemporaryImage.exists()) {
                    mTemporaryImage.delete();
                }
                return;
            }
        } else if (requestCode == IconPicker.REQUEST_PICK_SYSTEM) {
            icon.iconType = LockscreenTargetUtils.ICON_RESOURCE;
            icon.iconSource = intent.getStringExtra(IconPicker.RESOURCE_NAME);
            iconDrawable = LockscreenTargetUtils.getDrawableFromResources(mActivity,
                    null, icon.iconSource, false);
        } else if (requestCode == IconPicker.REQUEST_PICK_ICON_PACK
                && resultCode == Activity.RESULT_OK) {
            icon.packageName = intent.getStringExtra(IconPicker.PACKAGE_NAME);
            icon.iconType = LockscreenTargetUtils.ICON_RESOURCE;
            icon.iconSource = intent.getStringExtra(IconPicker.RESOURCE_NAME);
            iconDrawable = LockscreenTargetUtils.getDrawableFromResources(mActivity,
                    icon.packageName, icon.iconSource, false);
        } else {
            return;
        }

        if (iconDrawable != null) {
            mDialogIcon.setTag(icon);
            setIconForDialog(iconDrawable);
        } else {
            Log.w(TAG, "Could not fetch icon, keeping old one (type=" + icon.iconType
                    + ", source=" + icon.iconSource + ", package= " + icon.packageName + ")");
        }
    }

    public void onTargetChange(View v, int target) {
    }

    @Override
    public void onFinishFinalAnimation() {
    }
}
