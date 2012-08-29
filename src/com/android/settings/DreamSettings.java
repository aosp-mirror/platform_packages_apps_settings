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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Gravity;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.DreamBackend.DreamInfo;

import java.util.List;

public class DreamSettings extends SettingsPreferenceFragment {
    private static final int DIALOG_WHEN_TO_DREAM = 1;
    private static final int DIVIDER_HEIGHT = 15;

    private Context mContext;
    private DreamBackend mBackend;
    private DreamInfoAdapter mAdapter;
    private Switch mSwitch;
    private MenuItem[] mDependentMenuItems;
    private boolean mRefreshing;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Activity activity = getActivity();

        mBackend = new DreamBackend(activity);
        mSwitch = new Switch(activity);
        mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!mRefreshing) {
                    mBackend.setEnabled(isChecked);
                    refreshFromBackend();
                }
            }
        });
        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                mSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(mSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
            }
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView listView = getListView();

        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(R.string.screensaver_settings_disabled_prompt);
        listView.setEmptyView(emptyView);

        mAdapter = new DreamInfoAdapter(mContext);
        listView.setAdapter(mAdapter);
        listView.setDivider(null);
        listView.setDividerHeight(DIVIDER_HEIGHT);
        listView.setBackgroundColor(Color.BLACK);
        listView.setPadding(0, DIVIDER_HEIGHT, 0, DIVIDER_HEIGHT);
        listView.setHeaderDividersEnabled(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean isEnabled = mBackend.isEnabled();

        MenuItem whenToDream = createMenuItem(menu,
                R.string.screensaver_settings_when_to_dream,
                MenuItem.SHOW_AS_ACTION_NEVER,
                isEnabled,
                new Runnable() {
                    @Override
                    public void run() {
                        showDialog(DIALOG_WHEN_TO_DREAM);
                    }});

        createMenuItem(menu,
                R.string.help_label,
                MenuItem.SHOW_AS_ACTION_NEVER,
                true,
                new Runnable() {
                    @Override
                    public void run() {
                       // TODO show help url
                    }});

        MenuItem startDreaming = createMenuItem(menu,
                R.string.screensaver_settings_start_dreaming,
                MenuItem.SHOW_AS_ACTION_ALWAYS,
                isEnabled,
                new Runnable(){
                    @Override
                    public void run() {
                        mBackend.startDreamingNow();
                    }});

        mDependentMenuItems = new MenuItem[] { whenToDream, startDreaming };
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
                    }
                })
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFromBackend();
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

    private void refreshFromBackend() {
        mRefreshing = true;
        boolean dreamsEnabled = mBackend.isEnabled();
        if (mSwitch.isChecked() != dreamsEnabled)
            mSwitch.setChecked(dreamsEnabled);

        mAdapter.clear();
        if (dreamsEnabled) {
            List<DreamInfo> dreamInfos = mBackend.getDreamInfos();
            mAdapter.addAll(dreamInfos);
        }
        if (mDependentMenuItems != null)
            for (MenuItem item : mDependentMenuItems)
                item.setEnabled(dreamsEnabled);
        mRefreshing = false;
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
            final View row = convertView != null ? convertView : createDreamInfoRow(parent);
            row.setTag(dreamInfo);

            // bind icon
            ((ImageView) row.findViewById(android.R.id.icon)).setImageDrawable(dreamInfo.icon);

            // bind caption
            ((TextView) row.findViewById(android.R.id.text1)).setText(dreamInfo.caption);

            // bind radio button
            ((RadioButton) row.findViewById(android.R.id.button1)).setChecked(dreamInfo.isActive);
            ((RadioButton) row.findViewById(android.R.id.button1)).setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    row.onTouchEvent(event);
                    return false;
                }});

            // bind button container
            row.findViewById(android.R.id.widget_frame).setVisibility(dreamInfo.isActive ? View.VISIBLE : View.GONE);

            // bind settings button
            ((Button) row.findViewById(android.R.id.button2)).setEnabled(dreamInfo.settingsComponentName != null);
            ((Button) row.findViewById(android.R.id.button2)).setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    mBackend.launchSettings((DreamInfo)row.getTag());
                }});

            // bind preview button
            ((Button) row.findViewById(android.R.id.button3)).setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    mBackend.preview((DreamInfo)row.getTag());
                }});

            return row;
        }

        private View createDreamInfoRow(ViewGroup parent) {
            final View row =  mInflater.inflate(R.layout.dream_info_row, parent, false);
            row.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    activate((DreamInfo) row.getTag());
                }});
            return row;
        }

        private void activate(DreamInfo dreamInfo) {
            for (int i = 0; i < getCount(); i++) {
                getItem(i).isActive = false;
            }
            dreamInfo.isActive = true;
            mBackend.setActiveDream(dreamInfo.componentName);
            notifyDataSetChanged();
        }
    }
}
