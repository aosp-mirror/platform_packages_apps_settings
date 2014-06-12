/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.DreamBackend.DreamInfo;
import com.android.settings.widget.SwitchBar;

import java.util.List;

public class DreamSettings extends SettingsPreferenceFragment implements
        SwitchBar.OnSwitchChangeListener {
    private static final String TAG = DreamSettings.class.getSimpleName();
    static final boolean DEBUG = false;
    private static final int DIALOG_WHEN_TO_DREAM = 1;
    private static final String PACKAGE_SCHEME = "package";

    private final PackageReceiver mPackageReceiver = new PackageReceiver();

    private Context mContext;
    private DreamBackend mBackend;
    private DreamInfoAdapter mAdapter;
    private SwitchBar mSwitchBar;
    private MenuItem[] mMenuItemsWhenEnabled;
    private boolean mRefreshing;

    @Override
    public int getHelpResource() {
        return R.string.help_url_dreams;
    }

    @Override
    public void onAttach(Activity activity) {
        logd("onAttach(%s)", activity.getClass().getSimpleName());
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onCreate(Bundle icicle) {
        logd("onCreate(%s)", icicle);
        super.onCreate(icicle);

        mBackend = new DreamBackend(getActivity());

        setHasOptionsMenu(true);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (!mRefreshing) {
            mBackend.setEnabled(isChecked);
            refreshFromBackend();
        }
    }

    @Override
    public void onStart() {
        logd("onStart()");
        super.onStart();
    }

    @Override
    public void onDestroyView() {
        logd("onDestroyView()");
        super.onDestroyView();

        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logd("onActivityCreated(%s)", savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        ListView listView = getListView();
        listView.setItemsCanFocus(true);

        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(R.string.screensaver_settings_disabled_prompt);
        listView.setEmptyView(emptyView);

        mAdapter = new DreamInfoAdapter(mContext);
        listView.setAdapter(mAdapter);

        final SettingsActivity sa = (SettingsActivity) getActivity();
        mSwitchBar = sa.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        logd("onCreateOptionsMenu()");

        boolean isEnabled = mBackend.isEnabled();

        // create "start" action
        MenuItem start = createMenuItem(menu, R.string.screensaver_settings_dream_start,
                MenuItem.SHOW_AS_ACTION_NEVER,
                isEnabled, new Runnable(){
                    @Override
                    public void run() {
                        mBackend.startDreaming();
                    }});

        // create "when to dream" overflow menu item
        MenuItem whenToDream = createMenuItem(menu,
                R.string.screensaver_settings_when_to_dream,
                MenuItem.SHOW_AS_ACTION_NEVER,
                isEnabled,
                new Runnable() {
                    @Override
                    public void run() {
                        showDialog(DIALOG_WHEN_TO_DREAM);
                    }});

        // create "help" overflow menu item (make sure it appears last)
        super.onCreateOptionsMenu(menu, inflater);

        mMenuItemsWhenEnabled = new MenuItem[] { start, whenToDream };
    }

    private MenuItem createMenuItem(Menu menu,
            int titleRes, int actionEnum, boolean isEnabled, final Runnable onClick) {
        MenuItem item = menu.add(titleRes);
        item.setShowAsAction(actionEnum);
        item.setEnabled(isEnabled);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onClick.run();
                return true;
            }
        });
        return item;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        logd("onCreateDialog(%s)", dialogId);
        if (dialogId == DIALOG_WHEN_TO_DREAM)
            return createWhenToDreamDialog();
        return super.onCreateDialog(dialogId);
    }

    private Dialog createWhenToDreamDialog() {
        final CharSequence[] items = {
                mContext.getString(R.string.screensaver_settings_summary_dock),
                mContext.getString(R.string.screensaver_settings_summary_sleep),
                mContext.getString(R.string.screensaver_settings_summary_either_short)
        };

        int initialSelection = mBackend.isActivatedOnDock() && mBackend.isActivatedOnSleep() ? 2
                : mBackend.isActivatedOnDock() ? 0
                : mBackend.isActivatedOnSleep() ? 1
                : -1;

        return new AlertDialog.Builder(mContext)
                .setTitle(R.string.screensaver_settings_when_to_dream)
                .setSingleChoiceItems(items, initialSelection, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        mBackend.setActivatedOnDock(item == 0 || item == 2);
                        mBackend.setActivatedOnSleep(item == 1 || item == 2);
                        dialog.dismiss();
                    }
                })
                .create();
    }

    @Override
    public void onPause() {
        logd("onPause()");
        super.onPause();

        mContext.unregisterReceiver(mPackageReceiver);
    }

    @Override
    public void onResume() {
        logd("onResume()");
        super.onResume();
        refreshFromBackend();

        // listen for package changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme(PACKAGE_SCHEME);
        mContext.registerReceiver(mPackageReceiver , filter);
    }

    public static int getSummaryResource(Context context) {
        DreamBackend backend = new DreamBackend(context);
        boolean isEnabled = backend.isEnabled();
        boolean activatedOnSleep = backend.isActivatedOnSleep();
        boolean activatedOnDock = backend.isActivatedOnDock();
        boolean activatedOnEither = activatedOnSleep && activatedOnDock;
        return !isEnabled ? R.string.screensaver_settings_summary_off
                : activatedOnEither ? R.string.screensaver_settings_summary_either_long
                : activatedOnSleep ? R.string.screensaver_settings_summary_sleep
                : activatedOnDock ? R.string.screensaver_settings_summary_dock
                : 0;
    }

    public static CharSequence getSummaryTextWithDreamName(Context context) {
        DreamBackend backend = new DreamBackend(context);
        boolean isEnabled = backend.isEnabled();
        if (!isEnabled) {
            return context.getString(R.string.screensaver_settings_summary_off);
        } else {
            return backend.getActiveDreamName();
        }
    }

    private void refreshFromBackend() {
        logd("refreshFromBackend()");
        mRefreshing = true;
        boolean dreamsEnabled = mBackend.isEnabled();
        if (mSwitchBar.isChecked() != dreamsEnabled)
            mSwitchBar.setChecked(dreamsEnabled);

        mAdapter.clear();
        if (dreamsEnabled) {
            List<DreamInfo> dreamInfos = mBackend.getDreamInfos();
            mAdapter.addAll(dreamInfos);
        }
        if (mMenuItemsWhenEnabled != null)
            for (MenuItem menuItem : mMenuItemsWhenEnabled)
                menuItem.setEnabled(dreamsEnabled);
        mRefreshing = false;
    }

    private static void logd(String msg, Object... args) {
        if (DEBUG)
            Log.d(TAG, args == null || args.length == 0 ? msg : String.format(msg, args));
    }

    private class DreamInfoAdapter extends ArrayAdapter<DreamInfo> {
        private final LayoutInflater mInflater;

        public DreamInfoAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DreamInfo dreamInfo = getItem(position);
            logd("getView(%s)", dreamInfo.caption);
            final View row = convertView != null ? convertView : createDreamInfoRow(parent);
            row.setTag(dreamInfo);

            // bind icon
            ((ImageView) row.findViewById(android.R.id.icon)).setImageDrawable(dreamInfo.icon);

            // bind caption
            ((TextView) row.findViewById(android.R.id.title)).setText(dreamInfo.caption);

            // bind radio button
            RadioButton radioButton = (RadioButton) row.findViewById(android.R.id.button1);
            radioButton.setChecked(dreamInfo.isActive);
            radioButton.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    row.onTouchEvent(event);
                    return false;
                }});

            // bind settings button + divider
            boolean showSettings = dreamInfo.settingsComponentName != null;
            View settingsDivider = row.findViewById(R.id.divider);
            settingsDivider.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);

            ImageView settingsButton = (ImageView) row.findViewById(android.R.id.button2);
            settingsButton.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);
            settingsButton.setAlpha(dreamInfo.isActive ? 1f : Utils.DISABLED_ALPHA);
            settingsButton.setEnabled(dreamInfo.isActive);
            settingsButton.setFocusable(dreamInfo.isActive);
            settingsButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    mBackend.launchSettings((DreamInfo) row.getTag());
                }});

            return row;
        }

        private View createDreamInfoRow(ViewGroup parent) {
            final View row =  mInflater.inflate(R.layout.dream_info_row, parent, false);
            final View header = row.findViewById(android.R.id.widget_frame);
            header.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    activate((DreamInfo) row.getTag());
                }});
            return row;
        }

        private DreamInfo getCurrentSelection() {
            for (int i = 0; i < getCount(); i++) {
                DreamInfo dreamInfo = getItem(i);
                if (dreamInfo.isActive)
                    return dreamInfo;
            }
            return null;
        }
        private void activate(DreamInfo dreamInfo) {
            if (dreamInfo.equals(getCurrentSelection()))
                return;
            for (int i = 0; i < getCount(); i++) {
                getItem(i).isActive = false;
            }
            dreamInfo.isActive = true;
            mBackend.setActiveDream(dreamInfo.componentName);
            notifyDataSetChanged();
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            logd("PackageReceiver.onReceive");
            refreshFromBackend();
        }
    }
}
