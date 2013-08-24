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

package com.android.settings.profiles;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationGroup;
import android.app.Profile;
import android.app.ProfileManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.HashMap;

public class ProfilesSettings extends SettingsPreferenceFragment {

    private static final String TAG = "ProfilesSettings";
    private static final String PROFILE_SERVICE = "profile";

    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_ADD = Menu.FIRST + 1;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    private ProfileManager mProfileManager;
    private ProfileEnabler mProfileEnabler;

    private Switch mActionBarSwitch;

    private ViewPager mViewPager;
    private TextView mEmptyText;
    private ProfilesPagerAdapter mAdapter;
    private boolean mEnabled;

    ViewGroup mContainer;

    static Bundle mSavedState;

    public ProfilesSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(ProfileManager.PROFILES_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ProfileManager.PROFILES_STATE_CHANGED_ACTION.equals(action)) {
                    updateProfilesEnabledState();
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;

        View view = inflater.inflate(R.layout.profile_tabs, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        mEmptyText = (TextView) view.findViewById(R.id.empty);

        mAdapter = new ProfilesPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mAdapter);

        PagerTabStrip tabs = (PagerTabStrip) view.findViewById(R.id.tabs);
        tabs.setTabIndicatorColorResource(android.R.color.holo_blue_light);

        mProfileManager = (ProfileManager) getActivity().getSystemService(PROFILE_SERVICE);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We don't call super.onActivityCreated() here, since it assumes we already set up
        // Preference (probably in onCreate()), while ProfilesSettings exceptionally set it up in
        // this method.
        // On/off switch
        Activity activity = getActivity();
        //Switch
        mActionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                mActionBarSwitch.setPaddingRelative(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(mActionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
            }
        }

        mProfileEnabler = new ProfileEnabler(activity, mActionBarSwitch);

        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProfileEnabler != null) {
            mProfileEnabler.resume();
        }
        getActivity().registerReceiver(mReceiver, mFilter);

        // check if we are enabled
        updateProfilesEnabledState();

        // If running on a phone, remove padding around tabs
        if (!Utils.isTablet(getActivity())) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mProfileEnabler != null) {
            mProfileEnabler.pause();
        }
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setAlphabeticShortcut('r')
                .setEnabled(mEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, MENU_ADD, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add_dark)
                .setAlphabeticShortcut('a')
                .setEnabled(mEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetAll();
                return true;

            case MENU_ADD:
                // determine dialog to launch
                if (mViewPager.getCurrentItem() == 0) {
                    addProfile();
                } else {
                    addAppGroup();
                }
                return true;

            default:
                return false;
        }
    }

    private void addProfile() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.profile_name_dialog, null);
        final TextView prompt = (TextView) content.findViewById(R.id.prompt);
        final EditText entry = (EditText) content.findViewById(R.id.name);

        prompt.setText(R.string.profile_profile_name_prompt);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.menu_new_profile);
        builder.setView(content);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = entry.getText().toString();
                if (!mProfileManager.profileExists(name)) {
                    Profile profile = new Profile(name);
                    mProfileManager.addProfile(profile);
                    mAdapter.refreshProfiles();
                } else {
                    Toast.makeText(getActivity(),
                            R.string.duplicate_profile_name, Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void resetAll() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.profile_reset_title);
        alert.setIconAttribute(android.R.attr.alertDialogIcon);
        alert.setMessage(R.string.profile_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mProfileManager.resetAll();
                mAdapter.refreshProfiles();
                mAdapter.refreshAppGroups();
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.show();
    }

    private void addAppGroup() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.profile_name_dialog, null);
        final TextView prompt = (TextView) content.findViewById(R.id.prompt);
        final EditText entry = (EditText) content.findViewById(R.id.name);

        prompt.setText(R.string.profile_appgroup_name_prompt);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.profile_new_appgroup);
        builder.setView(content);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = entry.getText().toString();
                if (!mProfileManager.notificationGroupExists(name)) {
                    NotificationGroup newGroup = new NotificationGroup(name);
                    mProfileManager.addNotificationGroup(newGroup);
                    mAdapter.refreshAppGroups();
                } else {
                    Toast.makeText(getActivity(),
                            R.string.duplicate_appgroup_name, Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateProfilesEnabledState() {
        Activity activity = getActivity();

        mEnabled = Settings.System.getInt(activity.getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;
        activity.invalidateOptionsMenu();

        mViewPager.setVisibility(mEnabled ? View.VISIBLE : View.GONE);
        mEmptyText.setVisibility(mEnabled ? View.GONE : View.VISIBLE);
    }

    class ProfilesPagerAdapter extends FragmentStatePagerAdapter {
        Fragment[] frags = { new ProfilesList(), new AppGroupList() };
        String[] titles = { getString(R.string.profile_profiles_manage),
                            getString(R.string.profile_appgroups_manage) };

        ProfilesPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        public void refreshProfiles() {
            ((ProfilesList) frags[0]).refreshList();
        }

        public void refreshAppGroups() {
            ((AppGroupList) frags[1]).refreshList();
        }
    }
}
