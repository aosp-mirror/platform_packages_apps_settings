/*
 * Copyright (C) 2013 Slimroms
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

package com.android.settings.mahdi.dslv;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.util.mahdi.ButtonConfig;
import com.android.internal.util.mahdi.ButtonsConstants;
import com.android.internal.util.mahdi.ButtonsHelper;
import com.android.internal.util.mahdi.DeviceUtils;
import com.android.internal.util.mahdi.DeviceUtils.FilteredDeviceFeaturesArray;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.mahdi.dslv.DragSortListView;
import com.android.settings.mahdi.dslv.DragSortController;
import com.android.settings.mahdi.util.ShortcutPickerHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class ButtonsListViewSettings extends ListFragment implements
            ShortcutPickerHelper.OnPickListener {

    private static final int MENU_HELP = Menu.FIRST;
    private static final int MENU_ADD = MENU_HELP + 1;
    private static final int MENU_RESET = MENU_ADD + 1;

    private static final int NAV_BAR               = 0;
    private static final int PIE                   = 1;
    private static final int PIE_SECOND            = 2;
    private static final int NAV_RING              = 3;
    private static final int LOCKSCREEN_SHORTCUT   = 4;
    private static final int NOTIFICATION_SHORTCUT = 5;

    private static final int DEFAULT_MAX_BUTTON_NUMBER = 5;

    public static final int REQUEST_PICK_CUSTOM_ICON = 1000;

    private int mButtonMode;
    private int mMaxAllowedButtons;
    private boolean mUseAppPickerOnly;
    private boolean mDisableLongpress;
    private boolean mDisableIconPicker;
    private boolean mDisableDeleteLastEntry;

    private TextView mDisableMessage;

    private ButtonConfigsAdapter mButtonConfigsAdapter;

    private ArrayList<ButtonConfig> mButtonConfigs;
    private ButtonConfig mButtonConfig;

    private boolean mAdditionalFragmentAttached;
    private String mAdditionalFragment;
    private View mDivider;

    private int mPendingIndex = -1;
    private boolean mPendingLongpress;
    private boolean mPendingNewButton;

    private String[] mActionDialogValues;
    private String[] mActionDialogEntries;
    private String mActionValuesKey;
    private String mActionEntriesKey;

    private Activity mActivity;
    private ShortcutPickerHelper mPicker;

    private File mImageTmp;

    private DragSortListView.DropListener onDrop =
        new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                ButtonConfig item = mButtonConfigsAdapter.getItem(from);

                mButtonConfigsAdapter.remove(item);
                mButtonConfigsAdapter.insert(item, to);

                setConfig(mButtonConfigs, false);
            }
        };

    private DragSortListView.RemoveListener onRemove =
        new DragSortListView.RemoveListener() {
            @Override
            public void remove(int which) {
                ButtonConfig item = mButtonConfigsAdapter.getItem(which);
                mButtonConfigsAdapter.remove(item);
                if (mDisableDeleteLastEntry && mButtonConfigs.size() == 0) {
                    mButtonConfigsAdapter.add(item);
                    showDialogDeletionNotAllowed();
                } else {
                    deleteIconFileIfPresent(item);
                    setConfig(mButtonConfigs, false);
                    if (mButtonConfigs.size() == 0) {
                        showDisableMessage(true);
                    }
                }
            }
        };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.buttons_list_view_main, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Resources res = getResources();

        mButtonMode = getArguments().getInt("buttonMode", NAV_BAR);
        mMaxAllowedButtons = getArguments().getInt("maxAllowedButtons", DEFAULT_MAX_BUTTON_NUMBER);
        mAdditionalFragment = getArguments().getString("fragment", null);
        mActionValuesKey = getArguments().getString("actionValues", "shortcut_action_values");
        mActionEntriesKey = getArguments().getString("actionEntries", "shortcut_action_entries");
        mDisableLongpress = getArguments().getBoolean("disableLongpress", false);
        mUseAppPickerOnly = getArguments().getBoolean("useAppPickerOnly", false);
        mDisableIconPicker = getArguments().getBoolean("disableIconPicker", false);
        mDisableDeleteLastEntry = getArguments().getBoolean("disableDeleteLastEntry", false);

        mDisableMessage = (TextView) view.findViewById(R.id.disable_message);

        FilteredDeviceFeaturesArray finalActionDialogArray = new FilteredDeviceFeaturesArray();
        finalActionDialogArray = DeviceUtils.filterUnsupportedDeviceFeatures(mActivity,
            res.getStringArray(res.getIdentifier(mActionValuesKey, "array", "com.android.settings")),
            res.getStringArray(res.getIdentifier(mActionEntriesKey, "array", "com.android.settings")));
        mActionDialogValues = finalActionDialogArray.values;
        mActionDialogEntries = finalActionDialogArray.entries;

        mPicker = new ShortcutPickerHelper(this, this);

        mImageTmp = new File(mActivity.getCacheDir()
                + File.separator + "shortcut.tmp");

        DragSortListView listView = (DragSortListView) getListView();

        listView.setDropListener(onDrop);
        listView.setRemoveListener(onRemove);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                if (!mUseAppPickerOnly) {
                    showActionDialog(arg2, false, false);
                } else {
                    if (mPicker != null) {
                        mPendingIndex = arg2;
                        mPendingLongpress = false;
                        mPendingNewButton = false;
                        mPicker.pickShortcut();
                    }
                }
            }
        });

        if (!mDisableLongpress) {
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
                        long arg3) {
                    if (!mUseAppPickerOnly) {
                        showActionDialog(arg2, true, false);
                    } else {
                        if (mPicker != null) {
                            mPendingIndex = arg2;
                            mPendingLongpress = true;
                            mPendingNewButton = false;
                            mPicker.pickShortcut();
                        }
                    }
                    return true;
                }
            });
        }

        mButtonConfigs = getConfig();

        if (mButtonConfigs != null) {
            mButtonConfigsAdapter = new ButtonConfigsAdapter(mActivity, mButtonConfigs);
            setListAdapter(mButtonConfigsAdapter);
            showDisableMessage(mButtonConfigs.size() == 0);
        }

        mDivider = (View) view.findViewById(R.id.divider);
        loadAdditionalFragment();

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAdditionalFragmentAttached) {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
            if (fragment != null && !fragmentManager.isDestroyed()) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }
    }

    private void loadAdditionalFragment() {
        if (mAdditionalFragment != null && !mAdditionalFragment.isEmpty()) {
            try {
                Class<?> classAdditionalFragment = Class.forName(mAdditionalFragment);
                Fragment fragment = (Fragment) classAdditionalFragment.newInstance();
                getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment).commit();
                if (mDivider != null) {
                    mDivider.setVisibility(View.VISIBLE);
                }
                mAdditionalFragmentAttached = true;
            } catch (Exception e) {
                mAdditionalFragmentAttached = false;
                e.printStackTrace();
            }
        }
    }

    private void showActionDialog(
        final int which, final boolean longpress, final boolean newButton) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mActivity);
        if (longpress) {
            alertDialog.setTitle(R.string.shortcut_action_select_action_longpress);
        } else if (newButton) {
            alertDialog.setTitle(R.string.shortcut_action_select_action_newbutton);
        } else {
            alertDialog.setTitle(R.string.shortcut_action_select_action);
        }
        alertDialog.setItems(mActionDialogEntries, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (mActionDialogValues[item].equals(ButtonsConstants.ACTION_APP)) {
                    if (mPicker != null) {
                        mPendingIndex = which;
                        mPendingLongpress = longpress;
                        mPendingNewButton = newButton;
                        mPicker.pickShortcut();
                    }
                } else {
                    if (newButton) {
                        addNewButton(mActionDialogValues[item], mActionDialogEntries[item]);
                    } else {
                        updateButton(mActionDialogValues[item],
                            mActionDialogEntries[item], null, which, longpress);
                    }
                }
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap bmp, boolean isApplication) {
        if (mPendingIndex == -1) {
            return;
        }
        if (mPendingNewButton) {
            addNewButton(action, description);
        } else {
            updateButton(action, description, null, mPendingIndex, mPendingLongpress);
        }
        mPendingLongpress = false;
        mPendingNewButton = false;
        mPendingIndex = -1;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            } else if (requestCode == REQUEST_PICK_CUSTOM_ICON && mPendingIndex != -1) {
                if (mImageTmp.length() == 0 || !mImageTmp.exists()) {
                    mPendingIndex = -1;
                    Toast.makeText(mActivity,
                            getResources().getString(R.string.shortcut_image_not_valid),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                File image = new File(mActivity.getFilesDir() + File.separator
                        + "shortcut_" + System.currentTimeMillis() + ".png");
                String path = image.getAbsolutePath();
                mImageTmp.renameTo(image);
                image.setReadable(true, false);
                updateButton(null, null, path, mPendingIndex, false);
                mPendingIndex = -1;
            }
        } else {
            if (mImageTmp.exists()) {
                mImageTmp.delete();
            }
            mPendingLongpress = false;
            mPendingNewButton = false;
            mPendingIndex = -1;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateButton(String action, String description, String icon,
                int which, boolean longpress) {

        if (!longpress && checkForDuplicateMainNavButtons(action)) {
            return;
        }

        ButtonConfig button = mButtonConfigsAdapter.getItem(which);
        mButtonConfigsAdapter.remove(button);

        if (!longpress) {
            deleteIconFileIfPresent(button);
        }

        if (icon != null) {
            button.setIcon(icon);
        } else {
            if (longpress) {
                button.setLongpressAction(action);
                button.setLongpressActionDescription(description);
            } else {
                button.setClickAction(action);
                button.setClickActionDescription(description);
                button.setIcon(ButtonsConstants.ICON_EMPTY);
            }
        }

        mButtonConfigsAdapter.insert(button, which);
        showDisableMessage(false);
        setConfig(mButtonConfigs, false);
    }

    private boolean checkForDuplicateMainNavButtons(String action) {
        // disabled for now till navbar navring and pie is back for 4.4
        ButtonConfig button;
        for (int i = 0; i < mButtonConfigs.size(); i++) {
            button = mButtonConfigsAdapter.getItem(i);
            if (button.getClickAction().equals(action)
                && (action.equals(ButtonsConstants.ACTION_HOME)
                || action.equals(ButtonsConstants.ACTION_BACK)
                || action.equals(ButtonsConstants.ACTION_RECENTS))) {
                Toast.makeText(mActivity,
                        getResources().getString(R.string.shortcut_duplicate_entry),
                        Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }

    private void deleteIconFileIfPresent(ButtonConfig button) {
        File oldImage = new File(button.getIcon());
        if (oldImage.exists()) {
            oldImage.delete();
        }
    }

    private void showDisableMessage(boolean show) {
        if (mDisableMessage == null || mDisableDeleteLastEntry) {
            return;
        }
        if (show) {
            mDisableMessage.setVisibility(View.VISIBLE);
        } else {
            mDisableMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                if (mButtonConfigs.size() == mMaxAllowedButtons) {
                    Toast.makeText(mActivity,
                            getResources().getString(R.string.shortcut_action_max),
                            Toast.LENGTH_LONG).show();
                    break;
                }
                if (!mUseAppPickerOnly) {
                    showActionDialog(0, false, true);
                } else {
                    if (mPicker != null) {
                        mPendingIndex = 0;
                        mPendingLongpress = false;
                        mPendingNewButton = true;
                        mPicker.pickShortcut();
                    }
                }
                break;
            case MENU_RESET:
                resetToDefault();
                break;
            case MENU_HELP:
                showHelpScreen();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.shortcut_action_reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, MENU_ADD, 0, R.string.shortcut_action_add)
                .setIcon(R.drawable.ic_menu_add_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, MENU_HELP, 0, R.string.shortcut_action_help)
                .setIcon(R.drawable.ic_settings_about)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void showHelpScreen() {
        Resources res = getResources();
        String finalHelpMessage;
        String buttonMode;
        String icon = "";
        switch (mButtonMode) {
            // case LOCKSCREEN_SHORTCUT:
            case NOTIFICATION_SHORTCUT:
                buttonMode = res.getString(R.string.shortcut_action_help_shortcut);
                break;
            // case NAV_BAR:
            // case PIE:
            // case PIE_SECOND:
            // case NAV_RING:
            default:
                buttonMode = res.getString(R.string.shortcut_action_help_button);
                break;
        }
        if (!mDisableIconPicker) {
            icon = res.getString(R.string.shortcut_action_help_icon);
        }
        finalHelpMessage = res.getString(R.string.shortcut_action_help_main, buttonMode, icon);
        if (!mDisableDeleteLastEntry) {
            finalHelpMessage += " " + res.getString(
                R.string.shortcut_action_help_delete_last_entry, buttonMode);
        }
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mActivity);
        alertDialog.setTitle(R.string.shortcut_action_help);
        alertDialog.setMessage(finalHelpMessage);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alertDialog.create().show();
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mActivity);
        alertDialog.setTitle(R.string.shortcut_action_reset);
        alertDialog.setMessage(R.string.shortcut_action_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                setConfig(null, true);
                mButtonConfigsAdapter.clear();

                // Add the new default objects fetched from @getConfig()
                ArrayList<ButtonConfig> buttonConfigs = getConfig();
                final int newConfigsSize = buttonConfigs.size();
                for (int i = 0; i < newConfigsSize; i++) {
                    mButtonConfigsAdapter.add(buttonConfigs.get(i));
                }

                // dirty helper if buttonConfigs list has no entries
                // to proper update the content. .notifyDatSetChanged()
                // does not work in this case.
                if (newConfigsSize == 0) {
                    ButtonConfig emptyButton = new ButtonConfig(null, null, null, null, null);
                    mButtonConfigsAdapter.add(emptyButton);
                    mButtonConfigsAdapter.remove(emptyButton);
                }
                showDisableMessage(newConfigsSize == 0);
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void showDialogDeletionNotAllowed() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mActivity);
        alertDialog.setTitle(R.string.shortcut_action_warning);
        alertDialog.setMessage(R.string.shortcut_action_warning_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alertDialog.create().show();
    }

    private void addNewButton(String action, String description) {
        if (checkForDuplicateMainNavButtons(action)) {
            return;
        }
        ButtonConfig button = new ButtonConfig(
            action, description,
            ButtonsConstants.ACTION_NULL, getResources().getString(R.string.shortcut_action_none),
            ButtonsConstants.ICON_EMPTY);

            mButtonConfigsAdapter.add(button);
            showDisableMessage(false);
            setConfig(mButtonConfigs, false);
    }

    private ArrayList<ButtonConfig> getConfig() {
        switch (mButtonMode) {
            case NAV_BAR:
                return ButtonsHelper.getNavBarConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case NAV_RING:
                return ButtonsHelper.getNavRingConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case NOTIFICATION_SHORTCUT:
                return ButtonsHelper.getNotificationsShortcutConfig(mActivity);
        }
        return null;
    }

    private void setConfig(ArrayList<ButtonConfig> buttonConfigs, boolean reset) {
        switch (mButtonMode) {
            case NAV_BAR:
                ButtonsHelper.setNavBarConfig(mActivity, buttonConfigs, reset);
                break;
            case NAV_RING:
                ButtonsHelper.setNavRingConfig(mActivity, buttonConfigs, reset);
                break;
            case NOTIFICATION_SHORTCUT:
                ButtonsHelper.setNotificationShortcutConfig(mActivity, buttonConfigs, reset);
                if (reset) {
                    loadAdditionalFragment();
                }
                break;
        }
    }

    private void pickIcon() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.icon_picker_type)
                .setItems(R.array.icon_types, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which) {
                            case 0: // Default
                                updateButton(null, null,
                                    ButtonsConstants.ICON_EMPTY, mPendingIndex, false);
                                mPendingIndex = -1;
                                break;
                            case 1: // System defaults
                                ListView list = new ListView(mActivity);
                                list.setAdapter(new IconAdapter());
                                final Dialog holoDialog = new Dialog(mActivity);
                                holoDialog.setTitle(R.string.icon_picker_choose_icon_title);
                                holoDialog.setContentView(list);
                                list.setOnItemClickListener(new OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                                        IconAdapter adapter = (IconAdapter) parent.getAdapter();
                                        updateButton(null, null,
                                            adapter.getItemReference(position),
                                            mPendingIndex, false);
                                        mPendingIndex = -1;
                                        holoDialog.cancel();
                                    }
                                });
                                holoDialog.show();
                                break;
                            case 2: // Custom user icon
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                                intent.setType("image/*");
                                intent.putExtra("crop", "true");
                                intent.putExtra("scale", true);
                                intent.putExtra("outputFormat",
                                    Bitmap.CompressFormat.PNG.toString());
                                intent.putExtra("aspectX", 100);
                                intent.putExtra("aspectY", 100);
                                intent.putExtra("outputX", 100);
                                intent.putExtra("outputY", 100);
                                try {
                                    mImageTmp.createNewFile();
                                    mImageTmp.setWritable(true, false);
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                        Uri.fromFile(mImageTmp));
                                    intent.putExtra("return-data", false);
                                    startActivityForResult(intent, REQUEST_PICK_CUSTOM_ICON);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
        );
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class ViewHolder {
        public TextView longpressActionDescriptionView;
        public ImageView iconView;
    }

    private class ButtonConfigsAdapter extends ArrayAdapter<ButtonConfig> {

        public ButtonConfigsAdapter(Context context, List<ButtonConfig> clickActionDescriptions) {
            super(context, R.layout.buttons_list_view_item,
                    R.id.click_action_description, clickActionDescriptions);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            if (v != convertView && v != null) {
                ViewHolder holder = new ViewHolder();

                TextView longpressActionDecription =
                    (TextView) v.findViewById(R.id.longpress_action_description);
                ImageView icon = (ImageView) v.findViewById(R.id.icon);

                if (mDisableLongpress) {
                    longpressActionDecription.setVisibility(View.GONE);
                } else {
                    holder.longpressActionDescriptionView = longpressActionDecription;
                }

                holder.iconView = icon;

                v.setTag(holder);
            }

            ViewHolder holder = (ViewHolder) v.getTag();

            if (!mDisableLongpress) {
                holder.longpressActionDescriptionView.setText(
                    getResources().getString(R.string.shortcut_action_longpress)
                    + " " + getItem(position).getLongpressActionDescription());
            }
            holder.iconView.setImageDrawable(ButtonsHelper.getButtonIconImage(mActivity,
                    getItem(position).getClickAction(), getItem(position).getIcon()));

            if (!mDisableIconPicker) {
                holder.iconView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPendingIndex = position;
                        pickIcon();
                    }
                });
            }

            return v;
        }
    }

    public class IconAdapter extends BaseAdapter {

        TypedArray icons;
        String[] labels;

        public IconAdapter() {
            labels = getResources().getStringArray(R.array.shortcut_icon_picker_labels);
            icons = getResources().obtainTypedArray(R.array.shortcut_icon_picker_icons);
        }

        @Override
        public Object getItem(int position) {
            return icons.getDrawable(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getCount() {
            return labels.length;
        }

        public String getItemReference(int position) {
            String name = icons.getString(position);
            int separatorIndex = name.lastIndexOf(File.separator);
            int periodIndex = name.lastIndexOf('.');
            return ButtonsConstants.SYSTEM_ICON_IDENTIFIER
                + name.substring(separatorIndex + 1, periodIndex);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View iView = convertView;
            if (convertView == null) {
                iView = View.inflate(mActivity, android.R.layout.simple_list_item_1, null);
            }
            TextView tt = (TextView) iView.findViewById(android.R.id.text1);
            tt.setText(labels[position]);
            Drawable ic = ((Drawable) getItem(position)).mutate();
            tt.setCompoundDrawablePadding(15);
            tt.setCompoundDrawablesWithIntrinsicBounds(ic, null, null, null);
            return iView;
        }
    }
}
