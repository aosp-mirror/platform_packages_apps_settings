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

package com.android.settings.mahdi.quicksettings;

import static com.android.internal.util.mahdi.QSConstants.TILE_CUSTOM;
import static com.android.internal.util.mahdi.QSConstants.TILE_CUSTOM_DELIMITER;
import static com.android.internal.util.mahdi.QSConstants.TILE_CONTACT;
import static com.android.internal.util.mahdi.QSConstants.TILE_CUSTOM_KEY;
import static com.android.internal.util.mahdi.QSConstants.TILE_DELIMITER;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.mahdi.AppHelper;
import com.android.internal.util.mahdi.DeviceUtils;
import com.android.internal.util.mahdi.LockscreenTargetUtils;
import com.android.internal.util.mahdi.QSConstants;
import com.android.settings.mahdi.quicksettings.DraggableGridView;
import com.android.settings.mahdi.quicksettings.QuickSettingsUtil.TileInfo;
import com.android.settings.mahdi.util.IconPicker;
import com.android.settings.mahdi.util.IconPicker.OnIconPickListener;
import com.android.settings.mahdi.util.ShortcutPickerHelper;
import com.android.settings.Utils;
import com.android.settings.R;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class QuickSettingsTiles extends Fragment implements View.OnClickListener,
        View.OnLongClickListener, ShortcutPickerHelper.OnPickListener, OnIconPickListener {

    private static final String TAG = "QuickSettingsTiles";

    private static final int MENU_RESET         = Menu.FIRST;

    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";

    private static final int DLG_SHOW_LIST     = 0;
    private static final int DLG_MUSIC         = 1;
    private static final int DLG_CUSTOM_TILE = 2;
    private static final int DLG_CUSTOM_TILE_EXTRAS = 3;

    private static final int NUMBER_ACTIONS = 5;
    private static final int PICK_CONTACT = 145;

    private DraggableGridView mDragView;
    private ViewGroup mContainer;
    private LayoutInflater mInflater;
    private Resources mSystemUiResources;
    private TileAdapter mTileAdapter;
    private ShortcutPickerHelper mPicker;
    private IconPicker mIconPicker;
    private File mTemporaryImage;

    private ImageButton[] mDialogIcon = new ImageButton[NUMBER_ACTIONS];
    private Button[] mDialogLabel = new Button[NUMBER_ACTIONS];
    private ImageButton mExtraImageButton;

    private Drawable mEmptyIcon;
    private String mEmptyLabel;

    private String mCurrentCustomTile = null;

    private boolean mShortPress = true;

    private int mCurrentAction = -1;

    private int mTileTextSize;
    private int mTileTextPadding;

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        mDragView = new DraggableGridView(getActivity());
        mContainer = container;
        mContainer.setClipChildren(false);
        mContainer.setClipToPadding(false);
        mInflater = inflater;

        mIconPicker = new IconPicker(getActivity(), this);
        mPicker = new ShortcutPickerHelper(getActivity(), this);
        mEmptyLabel = getResources().getString(R.string.lockscreen_target_empty);
        mEmptyIcon = getResources().getDrawable(R.drawable.ic_empty);
        mTemporaryImage = new File(getActivity().getCacheDir() + "/custom_tile.tmp");

        PackageManager pm = getActivity().getPackageManager();
        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
            }
        }
        int panelWidth = getItemFromSystemUi("notification_panel_width", "dimen");
        if (panelWidth > 0) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(panelWidth,
                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
            mDragView.setLayoutParams(params);
        }
        int cellGap = getItemFromSystemUi("quick_settings_cell_gap", "dimen");
        if (cellGap != 0) {
            mDragView.setCellGap(cellGap);
        }
        int columnCount = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW, 3,
                UserHandle.USER_CURRENT);
        // do not allow duplication on tablets or any device which do not have
        // flipsettings
        boolean duplicateOnLandScape = Settings.System.getIntForUser(
                getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE,
                1, UserHandle.USER_CURRENT) == 1
                        && mSystemUiResources.getBoolean(mSystemUiResources.getIdentifier(
                        "com.android.systemui:bool/config_hasFlipSettingsPanel", null, null))
                        && isLandscape();

        if (columnCount != 0) {
            mDragView.setColumnCount(duplicateOnLandScape ? (columnCount * 2) : columnCount);
            mTileTextSize = mDragView.getTileTextSize(columnCount);
            mTileTextPadding = mDragView.getTileTextPadding(columnCount);
        }
        mTileAdapter = new TileAdapter(getActivity());
        return mDragView;
    }

    private int getItemFromSystemUi(String name, String type) {
        if (mSystemUiResources != null) {
            int resId = (int) mSystemUiResources.getIdentifier(name, type, "com.android.systemui");
            if (resId > 0) {
                try {
                    if (type.equals("dimen")) {
                        return (int) mSystemUiResources.getDimension(resId);
                    } else {
                        return mSystemUiResources.getInteger(resId);
                    }
                } catch (NotFoundException e) {
                }
            }
        }
        return 0;
    }

    void genTiles() {
        mDragView.removeAllViews();
        ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                QuickSettingsUtil.getCurrentTiles(getActivity()));
        for (String tileindex : tiles) {
            QuickSettingsUtil.TileInfo tile = null;
            if (tileindex.contains(TILE_CUSTOM)) {
                tile = QuickSettingsUtil.TILES.get(TILE_CUSTOM);
            } else if (tileindex.contains(TILE_CONTACT)) {
                tile = QuickSettingsUtil.TILES.get(TILE_CONTACT);
            } else {
                tile = QuickSettingsUtil.TILES.get(tileindex);
            }
            if (tile != null) {
                addTile(tile.getTitleResId(), tile.getIcon(), 0, false);
            }
        }
        addTile(R.string.add, null, R.drawable.ic_menu_add_dark, false);
    }

    /**
     * Adds a tile to the dragview
     * @param titleId - string id for tile text in systemui
     * @param iconSysId - resource id for icon in systemui
     * @param iconRegId - resource id for icon in local package
     * @param newTile - whether a new tile is being added by user
     */
    void addTile(int titleId, String iconSysId, int iconRegId, boolean newTile) {
        View tileView = null;
        if (iconRegId != 0) {
            tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_generic, null, false);
            final TextView name = (TextView) tileView.findViewById(R.id.text);
            final ImageView iv = (ImageView) tileView.findViewById(R.id.image);
            name.setText(titleId);
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
            name.setPadding(0, mTileTextPadding, 0, 0);
            iv.setImageDrawable(getResources().getDrawable(iconRegId));
        } else {
            final boolean isUserTile =
                    titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_USER).getTitleResId()
                            ||  titleId == QuickSettingsUtil.TILES.get(
                            TILE_CONTACT).getTitleResId();
            if (mSystemUiResources != null && iconSysId != null) {
                int resId = mSystemUiResources.getIdentifier(iconSysId, null, null);
                if (resId > 0) {
                    try {
                        Drawable d = mSystemUiResources.getDrawable(resId);
                        tileView = null;
                        if (isUserTile) {
                            tileView = (View) mInflater.inflate(
                                    R.layout.quick_settings_tile_user, null, false);
                            ImageView iv = (ImageView) tileView.findViewById(R.id.user_imageview);
                            TextView tv = (TextView) tileView.findViewById(R.id.tile_textview);
                            tv.setText(titleId);
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
                            iv.setImageDrawable(d);
                        } else {
                            tileView = (View) mInflater.inflate(
                                    R.layout.quick_settings_tile_generic, null, false);
                            final TextView name = (TextView) tileView.findViewById(R.id.text);
                            final ImageView iv = (ImageView) tileView.findViewById(R.id.image);
                            name.setText(titleId);
                            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
                            name.setPadding(0, mTileTextPadding, 0, 0);
                            iv.setImageDrawable(d);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (tileView != null) {
            if (titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_MUSIC).getTitleResId()
                || titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_CUSTOM).getTitleResId()
                || titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_CONTACT).getTitleResId()) {

                ImageView settings =  (ImageView) tileView.findViewById(R.id.settings);
                if (settings != null) {
                    settings.setVisibility(View.VISIBLE);
                }
            }
            mDragView.addView(tileView, newTile
                    ? mDragView.getChildCount() - 1 : mDragView.getChildCount());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        genTiles();
        mDragView.setOnRearrangeListener(new DraggableGridView.OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity()));
                String oldTile = tiles.get(oldIndex);
                tiles.remove(oldIndex);
                tiles.add(newIndex, oldTile);
                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                        QuickSettingsUtil.getTileStringFromList(tiles));
            }
            @Override
            public void onDelete(int index) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity()));
                String tileIndex = tiles.get(index);
                if (tileIndex.contains(TILE_CUSTOM)) {
                    QuickSettingsUtil.deleteCustomTile(
                            getActivity(), findCustomKey(tileIndex));
                } else if (tileIndex.contains(TILE_CONTACT)) {
                    QuickSettingsUtil.deleteActions(getActivity(),
                            Settings.System.TILE_CONTACT_ACTIONS, findCustomKey(tileIndex));
                }
                tiles.remove(index);
                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                        mDragView.getChildCount() == 1 ?
                        "" : QuickSettingsUtil.getTileStringFromList(tiles));
            }
        });
        mDragView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity()));
                if (arg2 != mDragView.getChildCount() - 1) {
                    if (arg2 == -1) {
                        return;
                    }
                    if (tiles.get(arg2).equals(QSConstants.TILE_MUSIC)) {
                        showDialogInner(DLG_MUSIC);
                    }
                    if (tiles.get(arg2).contains(TILE_CUSTOM)) {
                        mCurrentCustomTile = findCustomKey(tiles.get(arg2));
                        showDialogInner(DLG_CUSTOM_TILE);
                    }
                    if (tiles.get(arg2).contains(TILE_CONTACT)) {
                        mCurrentCustomTile = findCustomKey(tiles.get(arg2));
                        Intent intent = new Intent(
                                Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                        startActivityForResult(intent, PICK_CONTACT);
                    }
                    return;
                }
                showDialogInner(DLG_SHOW_LIST);
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.isPhone(getActivity())) {
            mContainer.setPadding(20, 0, 20, 0);
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
                resetTiles();
                return true;
            default:
                return false;
        }
    }

    private void resetTiles() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.tiles_reset_title);
        alert.setMessage(R.string.tiles_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                QuickSettingsUtil.resetTiles(getActivity());
                genTiles();
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.create().show();
    }

    private boolean isLandscape() {
        final boolean isLandscape =
            Resources.getSystem().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
        return isLandscape;
    }

    private String findCustomKey(String tile) {
        String[] split = tile.split(TILE_CUSTOM_KEY);
        return split[1];
    }

    private View customTileDialogView(String tileKey) {
        View view = View.inflate(getActivity(), R.layout.custom_tile_dialog, null);

        mCurrentCustomTile = tileKey;

        mDialogIcon[0] = (ImageButton) view.findViewById(R.id.icon);
        mDialogLabel[0] = (Button) view.findViewById(R.id.action);
        mDialogIcon[1] = (ImageButton) view.findViewById(R.id.icon_two);
        mDialogLabel[1] = (Button) view.findViewById(R.id.action_two);
        mDialogIcon[2] = (ImageButton) view.findViewById(R.id.icon_three);
        mDialogLabel[2] = (Button) view.findViewById(R.id.action_three);
        mDialogIcon[3] = (ImageButton) view.findViewById(R.id.icon_four);
        mDialogLabel[3] = (Button) view.findViewById(R.id.action_four);
        mDialogIcon[4] = (ImageButton) view.findViewById(R.id.icon_five);
        mDialogLabel[4] = (Button) view.findViewById(R.id.action_five);
        ImageButton reset = (ImageButton) view.findViewById(R.id.reset);
        ImageButton resetTwo = (ImageButton) view.findViewById(R.id.reset_two);
        ImageButton resetThree = (ImageButton) view.findViewById(R.id.reset_three);
        ImageButton resetFour = (ImageButton) view.findViewById(R.id.reset_four);
        ImageButton resetFive = (ImageButton) view.findViewById(R.id.reset_five);

        for (int i = 0; i < NUMBER_ACTIONS; i++) {
            setDialogIconsAndText(i);
            mDialogIcon[i].setOnClickListener(QuickSettingsTiles.this);
            mDialogLabel[i].setOnClickListener(QuickSettingsTiles.this);
            mDialogLabel[i].setOnLongClickListener(QuickSettingsTiles.this);
        }

        reset.setOnClickListener(QuickSettingsTiles.this);
        resetTwo.setOnClickListener(QuickSettingsTiles.this);
        resetThree.setOnClickListener(QuickSettingsTiles.this);
        resetFour.setOnClickListener(QuickSettingsTiles.this);
        resetFive.setOnClickListener(QuickSettingsTiles.this);

        return view;
    }

    private View customTileExtrasView(String tileKey) {
        View view = View.inflate(getActivity(), R.layout.custom_tile_extras_dialog, null);
        mCurrentCustomTile = tileKey;
        mExtraImageButton = (ImageButton) view.findViewById(R.id.icon_extra);
        mExtraImageButton.setOnClickListener(QuickSettingsTiles.this);
        setResolvedImage();
        return view;
    }

    private void setDialogIconsAndText(int index) {
        mDialogLabel[index].setText(returnFriendlyName(index));
        mDialogIcon[index].setImageDrawable(returnPackageDrawable(index));
    }

    private String returnFriendlyName(int index) {
        String uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 0, mCurrentCustomTile);
        String longpressUri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 1, mCurrentCustomTile);

        if (uri == null && longpressUri == null) {
            return mEmptyLabel;
        } else if (uri == null && longpressUri != null) {
            return getResources().getString(R.string.custom_tile_long_press)
                    + " " + AppHelper.getShortcutPreferred(
                    getActivity(), getActivity().getPackageManager(), longpressUri);
        } else if (uri != null && longpressUri == null) {
            return AppHelper.getShortcutPreferred(
                    getActivity(), getActivity().getPackageManager(), uri);
        } else {
            return AppHelper.getShortcutPreferred(
                    getActivity(), getActivity().getPackageManager(), uri)
                    + "\n" + getResources().getString(R.string.custom_tile_long_press)
                    + " "+ AppHelper.getShortcutPreferred(
                    getActivity(), getActivity().getPackageManager(), longpressUri);
        }
    }

    private void setResolvedImage() {
        String settings = QuickSettingsUtil.getCustomExtras(getActivity(),
                Settings.System.CUSTOM_TOGGLE_EXTRAS, mCurrentCustomTile);
        String[] settingSplit = settings.split(TILE_CUSTOM_DELIMITER);
        if (settingSplit.length < 2) {
            mExtraImageButton.setImageDrawable(mEmptyIcon);
            return;
        }
        Drawable icon = null;
        if (settingSplit[1] != null && !settingSplit.equals(" ")
                && settingSplit[1].length() > 0) {
            File f = new File(Uri.parse(settingSplit[1]).getPath());
            if (f.exists()) {
                icon = new BitmapDrawable(
                        getResources(), f.getAbsolutePath());
            }
        }
        mExtraImageButton.setImageDrawable(icon != null ? icon : mEmptyIcon);
    }

    private Drawable returnPackageDrawable(int index) {
        String uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 0, mCurrentCustomTile);

        if (uri == null ) {
            // Check if long action exists, and use it instead
            uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 1, mCurrentCustomTile);
        }

        Drawable icon = null;
        if (uri != null) {
            String iconUri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 2, mCurrentCustomTile);
            if (iconUri != null && iconUri.length() > 0) {
                File f = new File(Uri.parse(iconUri).getPath());
                if (f.exists()) {
                    icon = new BitmapDrawable(
                            getResources(), f.getAbsolutePath());
                    }
                } else {
                try {
                    Intent intent = Intent.parseUri(uri, 0);
                    icon = LockscreenTargetUtils.getDrawableFromIntent(getActivity(), intent);
                } catch (URISyntaxException e) {
                    Log.wtf(TAG, "Invalid uri: " + uri);
                }
            }
        }

        if (icon == null) {
            return mEmptyIcon;
        } else {
            return icon;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);
            } else if (requestCode == IconPicker.REQUEST_PICK_SYSTEM
                    || requestCode == IconPicker.REQUEST_PICK_GALLERY
                    || requestCode == IconPicker.REQUEST_PICK_ICON_PACK) {
                mIconPicker.onActivityResult(requestCode, resultCode, data);
            } else if (requestCode == PICK_CONTACT) {
                Uri contactData = data.getData();
                String[] projection = new String[]{
                        ContactsContract.Contacts.LOOKUP_KEY
                };
                String selection = ContactsContract.Contacts.DISPLAY_NAME + " IS NOT NULL";
                CursorLoader cursorLoader = new CursorLoader(getActivity().getBaseContext(),
                        contactData, projection, selection, null, null);
                Cursor cursor = cursorLoader.loadInBackground();
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            String lookupKey = cursor.getString(cursor
                                    .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                            QuickSettingsUtil.saveExtras(getActivity(), lookupKey,
                                    mCurrentCustomTile, Settings.System.TILE_CONTACT_ACTIONS);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
    }

    @Override
    public void iconPicked(int requestCode, int resultCode, Intent intent) {
        if (requestCode == IconPicker.REQUEST_PICK_GALLERY) {
            if (resultCode == Activity.RESULT_OK && mCurrentAction != -1) {
                if (mTemporaryImage.length() == 0 || !mTemporaryImage.exists()) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.shortcut_image_not_valid),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                File imageFile = new File(getActivity().getFilesDir(),
                        "/custom_tile_" + System.currentTimeMillis() + ".png");
                String path = imageFile.getAbsolutePath();
                mTemporaryImage.renameTo(imageFile);
                imageFile.setReadable(true, false);

                if (mCurrentAction == 5) {
                    QuickSettingsUtil.saveCustomExtras(getActivity(), mCurrentCustomTile,
                            null, path, null, null, null, null);
                    setResolvedImage();
                } else {
                    deleteCustomIcon();  // Delete current icon if it exists before saving new.
                    QuickSettingsUtil.saveCustomActions(getActivity(), mCurrentAction, 2,
                            path, mCurrentCustomTile);

                    setDialogIconsAndText(mCurrentAction);
                }
            } else {
                if (mTemporaryImage.exists()) {
                    mTemporaryImage.delete();
                }
            }
        }
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        if (uri == null || mCurrentAction == -1) {
            return;
        }

        Drawable icon = null;

        boolean changeIcon = false;
        int setting = 0;

        if (mShortPress) {
            changeIcon = true;
            QuickSettingsUtil.saveCustomExtras(getActivity(), mCurrentCustomTile,
                    null, null, null, null, " ", null);
        } else {
            changeIcon = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                    mCurrentAction, 0, mCurrentCustomTile) == null;
            setting = 1;
        }

        if (changeIcon) {
            deleteCustomIcon();
        }

        QuickSettingsUtil.saveCustomActions(getActivity(),
                mCurrentAction, setting, uri, mCurrentCustomTile);

        setDialogIconsAndText(mCurrentAction);
    }

    private void deleteCustomIcon() {
        String path = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                mCurrentAction, 2, mCurrentCustomTile);

        if (path != null) {
            File f = new File(path);
            if (f != null && f.exists()) {
                f.delete();
            }
        }
        QuickSettingsUtil.saveCustomActions(getActivity(),
                mCurrentAction, 2, " ", mCurrentCustomTile);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.icon:
                prepareCustomIcon(0);
                break;
            case R.id.icon_two:
                prepareCustomIcon(1);
                break;
            case R.id.icon_three:
                prepareCustomIcon(2);
                break;
            case R.id.icon_four:
                prepareCustomIcon(3);
                break;
            case R.id.icon_five:
                prepareCustomIcon(4);
                break;
            case R.id.action:
                prepareCustomAction(0, true);
                break;
            case R.id.action_two:
                prepareCustomAction(1, true);
                break;
            case R.id.action_three:
                prepareCustomAction(2, true);
                break;
            case R.id.action_four:
                prepareCustomAction(3, true);
                break;
            case R.id.action_five:
                prepareCustomAction(4, true);
                break;
            case R.id.reset:
                deleteAction(0);
                break;
            case R.id.reset_two:
                deleteAction(1);
                break;
            case R.id.reset_three:
                deleteAction(2);
                break;
            case R.id.reset_four:
                deleteAction(3);
                break;
            case R.id.reset_five:
                deleteAction(4);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch(v.getId()) {
            case R.id.action:
                prepareCustomAction(0, false);
                break;
            case R.id.action_two:
                prepareCustomAction(1, false);
                break;
            case R.id.action_three:
                prepareCustomAction(2, false);
                break;
            case R.id.action_four:
                prepareCustomAction(3, false);
                break;
            case R.id.action_five:
                prepareCustomAction(4, false);
                break;
        }
        return true;
    }

    private void prepareCustomIcon(int action) {
        mCurrentAction = action;
        String uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                mCurrentAction, 0, mCurrentCustomTile);

        if (uri == null ) {
            // Check if long action exists, and use it instead
            uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                    mCurrentAction, 1, mCurrentCustomTile);
        }

        if (uri != null) {
            try {
                mTemporaryImage.createNewFile();
                mTemporaryImage.setWritable(true, false);
                // Layout will scale down for us
                mIconPicker.pickGalleryWithSize(
                    getId(), mTemporaryImage, 360);
            } catch (IOException e) {
                Log.d(TAG, "Could not create temporary icon", e);
            }
        } else {
            Toast.makeText(getActivity(), R.string.custom_tile_null_warning,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void prepareCustomAction(int action, boolean shortpress) {
        mCurrentAction = action;
        mShortPress = shortpress;
        mPicker.pickShortcut(getId());
    }

    private void deleteAction(int action) {
        mCurrentAction = action;
        QuickSettingsUtil.deleteCustomActions(
                getActivity(), mCurrentAction, mCurrentCustomTile);
        setDialogIconsAndText(mCurrentAction);
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, mCurrentCustomTile);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id, String tileKey) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("tileKey", tileKey);
            frag.setArguments(args);
            return frag;
        }

        QuickSettingsTiles getOwner() {
            return (QuickSettingsTiles) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String tileKey = getArguments().getString("tileKey");
            switch (id) {
                case DLG_SHOW_LIST:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.tile_choose_title)
                    .setNegativeButton(R.string.cancel, null)
                    .setAdapter(getOwner().mTileAdapter, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int position) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String tiles = QuickSettingsUtil.getCurrentTiles(getActivity());
                                    String picked = getOwner().mTileAdapter.getTileId(position);
                                    ArrayList<String> curr = new ArrayList<String>();
                                    // A blank string indicates tiles are currently disabled
                                    // Avoid index being off by one when we add a new tile
                                    if (!tiles.equals("")) {
                                        curr = QuickSettingsUtil.getTileListFromString(tiles);
                                    }
                                    if (picked.contains(TILE_CUSTOM)
                                            || picked.contains(TILE_CONTACT)) {
                                        curr.add(picked
                                                + TILE_CUSTOM_KEY + System.currentTimeMillis());
                                    } else {
                                        curr.add(picked);
                                    }
                                    QuickSettingsUtil.saveCurrentTiles(getActivity(),
                                        QuickSettingsUtil.getTileStringFromList(curr));
                                }
                            }).start();
                            TileInfo info = QuickSettingsUtil.TILES.get(
                                    getOwner().mTileAdapter.getTileId(position));
                            getOwner().addTile(info.getTitleResId(), info.getIcon(), 0, true);
                        }
                    })
                    .create();
                case DLG_MUSIC:
                    int storedMode = Settings.System.getIntForUser(
                            getActivity().getContentResolver(),
                            Settings.System.MUSIC_TILE_MODE, 3,
                            UserHandle.USER_CURRENT);
                    final boolean[] actualMode = new boolean[2];
                    actualMode[0] = storedMode == 1 || storedMode == 3;
                    actualMode[1] = storedMode > 1;

                    final String[] entries =  {
                            getResources().getString(R.string.music_tile_mode_background),
                            getResources().getString(R.string.music_tile_mode_tracktitle)
                    };
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_music_mode_title)
                    .setNegativeButton(R.string.cancel, null)
                    .setMultiChoiceItems(entries, actualMode,
                        new  DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int indexSelected,
                                boolean isChecked) {
                            actualMode[indexSelected] = isChecked;
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int mode = 0;
                            if (actualMode[0]) {
                                if (actualMode[1]) {
                                    mode = 3;
                                } else {
                                    mode = 1;
                                }
                            } else {
                                if (actualMode[1]) {
                                    mode = 2;
                                }
                            }
                            Settings.System.putInt(
                                getActivity().getContentResolver(),
                                Settings.System.MUSIC_TILE_MODE,
                                mode);
                        }
                    })
                    .create();
                case DLG_CUSTOM_TILE:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.custom_tile_dialog_title)
                    .setView(getOwner().customTileDialogView(tileKey))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String extras = QuickSettingsUtil.getCustomExtras(getActivity(),
                                    Settings.System.CUSTOM_TOGGLE_EXTRAS,
                                    tileKey);
                            if (extras == null) {
                                QuickSettingsUtil.saveCustomExtras(getActivity(), tileKey,
                                        Integer.toString(0), null, null, null, null, null);
                            } else {
                                String[] collapseMatch = extras.split(TILE_CUSTOM_DELIMITER);
                                if (collapseMatch[0].equals(" ")) {
                                    QuickSettingsUtil.saveCustomExtras(getActivity(), tileKey,
                                            Integer.toString(0), null, null, null, null, null);
                                }
                            }
                            getOwner().showDialogInner(DLG_CUSTOM_TILE_EXTRAS);
                        }
                    })
                    .create();
                case DLG_CUSTOM_TILE_EXTRAS:
                    int actions = 0;
                    int advActions = 0;
                    int type = 0;
                    boolean matchIncluded = false;
                    String advSetting = null;
                    String checkClick = null;
                    String className = null;
                    Intent intent = null;
                    String values = "";
                    for (int i = 0; i < getOwner().NUMBER_ACTIONS; i++) {
                        className = null;
                        intent = null;
                        checkClick = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                                i, 0, tileKey);
                        if (checkClick != null) {
                            actions++;
                            try {
                                intent = Intent.parseUri(checkClick, 0);
                                String intentClass = intent.getComponent().getClassName();
                                className = intentClass.substring(intentClass.lastIndexOf(".") + 1);
                            } catch (URISyntaxException e) {
                                // Tricky user
                            }
                            if (className != null
                                    && className.equals("ChamberOfSecrets")
                                    && intent != null) {
                                type = intent.getIntExtra("type", 0);
                                String settingHolder = intent.getStringExtra("setting");
                                if (advSetting == null) {
                                    advSetting = settingHolder;
                                }
                                String array = intent.getStringExtra("array");
                                if (array != null && advSetting != null && settingHolder != null) {
                                    String[] strArray = array.split(",");
                                    if (strArray.length == 1 && advSetting.equals(settingHolder)) {
                                        advActions++;
                                        values = values + strArray[0] + ",";
                                    } else {
                                        values = " ";
                                        advActions = 0;
                                        advSetting = " ";
                                    }
                                }
                            } else {
                                values = " ";
                                advActions = 0;
                                advSetting = " ";
                            }
                        }
                    }
                    // User selected multiple click actions
                    // Tile isn't advanced
                    // Only now is this preference relevant
                    if (actions > 1 && advActions == 0) {
                        matchIncluded = true;
                    }

                    final String typeSaved = Integer.toString(type);
                    // Location mode is google's evil step child.
                    // Use location_tile's work-a-round.
                    if (advSetting == null) {
                        advSetting = " ";
                    }
                    final String resolverSaved = !advSetting.equals("location_mode")
                            ? advSetting : "location_last_mode";
                    final String valuesSaved = values;

                    String[] checks = QuickSettingsUtil.getCustomExtras(getActivity(),
                            Settings.System.CUSTOM_TOGGLE_EXTRAS,
                            tileKey).split(TILE_CUSTOM_DELIMITER);
                    int checksSaved = checks[0] == null ? 0 : Integer.parseInt(checks[0]);
                    // No longer allow match state to be selected
                    // if tile is advanced
                    if (advActions > 0) {
                        if (checksSaved == 2) {
                            checksSaved = 0;
                        } else if (checksSaved == 3) {
                            checksSaved = 1;
                        }
                        QuickSettingsUtil.saveCustomExtras(getActivity(), tileKey,
                                Integer.toString(checksSaved), null, null, null, null, null);
                    }

                    final boolean[] checkBox = new boolean[matchIncluded ? 2 : 1];
                    switch (checksSaved) {
                        case 0:
                            checkBox[0] = false;
                            if (matchIncluded) {
                                checkBox[1] = false;
                            }
                            break;
                        case 1:
                            checkBox[0] = true;
                            if (matchIncluded) {
                                checkBox[1] = false;
                            }
                            break;
                        case 2:
                            checkBox[0] = false;
                            if (matchIncluded) {
                                checkBox[1] = true;
                            }
                            break;
                        case 3:
                            checkBox[0] = true;
                            if (matchIncluded) {
                                checkBox[1] = true;
                            }
                            break;
                    }

                    final String[] entry = new String[matchIncluded ? 2 : 1];

                    entry[0] = getResources().getString(
                            R.string.custom_toggle_collapse_check);
                    if (matchIncluded) {
                        entry[1] = getResources().getString(
                                R.string.custom_toggle_match_state_check);
                    }
                    AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                    View view = getOwner().customTileExtrasView(tileKey);
                    final EditText name = (EditText) view.findViewById(R.id.action_label);
                    final String defaultText =
                            getActivity().getResources().getString(
                            R.string.custom_tile_resolve_name);
                    name.append(checks.length > 1 && !checks[2].equals(" ")
                            ? checks[2] : defaultText);
                    // User is using the Chamber of Secrets with one setting and
                    // one value per state with multiple state. We'll implement
                    // a listener so the tile can change states by itself.
                    if (advActions > 0) {
                        alert.setView(view);
                    }

                    return alert.setTitle(R.string.custom_toggle_extras)
                    .setNegativeButton(R.string.cancel, null)
                    .setMultiChoiceItems(entry, checkBox,
                        new  DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int indexSelected,
                                boolean isChecked) {
                            checkBox[indexSelected] = isChecked;
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int userValue = 0;
                            for (int i = 0; i < checkBox.length; i++) {
                                boolean addIt = checkBox[i];
                                switch (i) {
                                    case 0:
                                        if (addIt) {
                                            userValue += 1;
                                        }
                                        break;
                                    case 1:
                                        if (addIt) {
                                            userValue += 2;
                                        }
                                        break;
                                }
                            }

                            QuickSettingsUtil.saveCustomExtras(getActivity(), tileKey,
                                    Integer.toString(userValue), null, name.getText().toString(),
                                    valuesSaved, resolverSaved, typeSaved);
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

    private static class TileAdapter extends ArrayAdapter<String> {
        private static class Entry {
            public final TileInfo tile;
            public final String tileTitle;
            public Entry(TileInfo tile, String tileTitle) {
                this.tile = tile;
                this.tileTitle = tileTitle;
            }
        }

        private Entry[] mTiles;

        public TileAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
            mTiles = new Entry[getCount()];
            loadItems(context.getResources());
            sortItems();
        }

        private void loadItems(Resources resources) {
            int index = 0;
            for (TileInfo t : QuickSettingsUtil.TILES.values()) {
                mTiles[index++] = new Entry(t, resources.getString(t.getTitleResId()));
            }
        }

        private void sortItems() {
            final Collator collator = Collator.getInstance();
            collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            collator.setStrength(Collator.PRIMARY);
            Arrays.sort(mTiles, new Comparator<Entry>() {
                @Override
                public int compare(Entry e1, Entry e2) {
                    return collator.compare(e1.tileTitle, e2.tileTitle);
                }
            });
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setEnabled(isEnabled(position));
            return v;
        }

        @Override
        public int getCount() {
            return QuickSettingsUtil.TILES.size();
        }

        @Override
        public String getItem(int position) {
            return mTiles[position].tileTitle;
        }

        public String getTileId(int position) {
            return mTiles[position].tile.getId();
        }

        @Override
        public boolean isEnabled(int position) {
            String usedTiles = QuickSettingsUtil.getCurrentTiles(
                    getContext());
            String tile = mTiles[position].tile.getId();
            if (TILE_CUSTOM.equals(tile) || TILE_CONTACT.equals(tile)) {
                return true;
            }
            return !usedTiles.contains(tile);
        }
    }
}
