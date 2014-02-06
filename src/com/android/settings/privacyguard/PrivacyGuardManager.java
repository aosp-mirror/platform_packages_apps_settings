/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.privacyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Settings.AppOpsSummaryActivity;
import com.android.settings.applications.AppOpsDetails;
import com.android.settings.applications.AppOpsState;
import com.android.settings.applications.AppOpsState.OpsTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PrivacyGuardManager extends Fragment
        implements OnItemClickListener, OnItemLongClickListener {

    private static final String TAG = "PrivacyGuardManager";

    private TextView mNoUserAppsInstalled;
    private ListView mAppsList;
    private PrivacyGuardAppListAdapter mAdapter;
    private List<AppInfo> mApps;

    private PackageManager mPm;
    private Activity mActivity;

    private SharedPreferences mPreferences;
    private AppOpsManager mAppOps;

    private int mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
    private int mSavedFirstItemOffset;

    // keys for extras and icicles
    private final static String LAST_LIST_POS = "last_list_pos";
    private final static String LAST_LIST_OFFSET = "last_list_offset";

    // holder for package data passed into the adapter
    public static final class AppInfo {
        String title;
        String packageName;
        boolean enabled;
        boolean privacyGuardEnabled;
        int uid;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mActivity = getActivity();
        mPm = mActivity.getPackageManager();
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);

        return inflater.inflate(R.layout.privacy_guard_manager, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id.privacy_guard_prefs);
        if (f != null && !fm.isDestroyed()) {
            fm.beginTransaction().remove(f).commit();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mNoUserAppsInstalled = (TextView) mActivity.findViewById(R.id.error);

        mAppsList = (ListView) mActivity.findViewById(R.id.apps_list);
        mAppsList.setOnItemClickListener(this);
        mAppsList.setOnItemLongClickListener(this);

        // get shared preference
        mPreferences = mActivity.getSharedPreferences("privacy_guard_manager", Activity.MODE_PRIVATE);
        if (!mPreferences.getBoolean("first_help_shown", false)) {
            showHelp();
        }

        if (savedInstanceState != null) {
            mSavedFirstVisiblePosition = savedInstanceState.getInt(LAST_LIST_POS,
                    AdapterView.INVALID_POSITION);
            mSavedFirstItemOffset = savedInstanceState.getInt(LAST_LIST_OFFSET, 0);
        } else {
            mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
            mSavedFirstItemOffset = 0;
        }

        // load apps and construct the list
        loadApps();

        setHasOptionsMenu(true);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(LAST_LIST_POS, mSavedFirstVisiblePosition);
        outState.putInt(LAST_LIST_OFFSET, mSavedFirstItemOffset);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Remember where the list is scrolled to so we can restore the scroll position
        // when we come back to this activity and *after* we complete querying for the
        // conversations.
        mSavedFirstVisiblePosition = mAppsList.getFirstVisiblePosition();
        View firstChild = mAppsList.getChildAt(0);
        mSavedFirstItemOffset = (firstChild == null) ? 0 : firstChild.getTop();
    }

    @Override
    public void onResume() {
        super.onResume();

        // rebuild the list; the user might have changed settings inbetween
        loadApps();

        if (mSavedFirstVisiblePosition != AdapterView.INVALID_POSITION) {
            mAppsList.setSelectionFromTop(mSavedFirstVisiblePosition, mSavedFirstItemOffset);
            mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
        }
    }

    private void loadApps() {
        mApps = loadInstalledApps();

        // if app list is empty inform the user
        // else go ahead and construct the list
        if (mApps == null || mApps.isEmpty()) {
            mNoUserAppsInstalled.setText(R.string.privacy_guard_no_user_apps);
            mNoUserAppsInstalled.setVisibility(View.VISIBLE);
            mAppsList.setVisibility(View.GONE);
            mAppsList.setAdapter(null);
        } else {
            mNoUserAppsInstalled.setVisibility(View.GONE);
            mAppsList.setVisibility(View.VISIBLE);
            mAdapter = createAdapter();
            mAppsList.setAdapter(mAdapter);
            mAppsList.setFastScrollEnabled(true);
        }
    }

    private PrivacyGuardAppListAdapter createAdapter() {
        String lastSectionIndex = null;
        ArrayList<String> sections = new ArrayList<String>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        int count = mApps.size(), offset = 0;

        for (int i = 0; i < count; i++) {
            AppInfo app = mApps.get(i);
            String sectionIndex;

            if (!app.enabled) {
                sectionIndex = "--"; //XXX
            } else if (app.title.isEmpty()) {
                sectionIndex = "";
            } else {
                sectionIndex = app.title.substring(0, 1).toUpperCase();
            }
            if (lastSectionIndex == null) {
                lastSectionIndex = sectionIndex;
            }

            if (!TextUtils.equals(sectionIndex, lastSectionIndex)) {
                sections.add(sectionIndex);
                positions.add(offset);
                lastSectionIndex = sectionIndex;
            }
            offset++;
        }

        return new PrivacyGuardAppListAdapter(mActivity, mApps, sections, positions);
    }

    private void resetPrivacyGuard() {
        if (mApps == null || mApps.isEmpty()) {
            return;
        }
        showResetDialog();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // on click change the privacy guard status for this item
        final AppInfo app = (AppInfo) parent.getItemAtPosition(position);

        app.privacyGuardEnabled = !app.privacyGuardEnabled;
        mAppOps.setPrivacyGuardSettingForPackage(app.uid, app.packageName, app.privacyGuardEnabled);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // on long click open app details window
        final AppInfo app = (AppInfo) parent.getItemAtPosition(position);

        Bundle args = new Bundle();
        args.putString(AppOpsDetails.ARG_PACKAGE_NAME, app.packageName);

        PreferenceActivity pa = (PreferenceActivity)getActivity();
        pa.startPreferencePanel(AppOpsDetails.class.getName(), args,
                R.string.app_ops_settings, null, this, 2);
        return true;
    }

    /**
    * Uses the package manager to query for all currently installed apps
    * for the list.
    *
    * @return the complete List off installed applications (@code PrivacyGuardAppInfo)
    */
    private List<AppInfo> loadInstalledApps() {
        List<AppInfo> apps = new ArrayList<AppInfo>();
        List<PackageInfo> packages = mPm.getInstalledPackages(
            PackageManager.GET_PERMISSIONS | PackageManager.GET_SIGNATURES);
        boolean showSystemApps = shouldShowSystemApps();
        Signature platformCert;

        try {
            PackageInfo sysInfo = mPm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            platformCert = sysInfo.signatures[0];
        } catch (PackageManager.NameNotFoundException e) {
            platformCert = null;
        }

        for (PackageInfo info : packages) {
            final ApplicationInfo appInfo = info.applicationInfo;

            // hide apps signed with the platform certificate to avoid the user
            // shooting himself in the foot
            if (platformCert != null && info.signatures != null
                    && platformCert.equals(info.signatures[0])) {
                continue;
            }

            // skip all system apps if they shall not be included
            if (!showSystemApps && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }

            AppInfo app = new AppInfo();
            app.title = appInfo.loadLabel(mPm).toString();
            app.packageName = info.packageName;
            app.enabled = appInfo.enabled;
            app.uid = info.applicationInfo.uid;
            app.privacyGuardEnabled = mAppOps.getPrivacyGuardSettingForPackage(
                    app.uid, app.packageName);
            apps.add(app);
        }

        // sort the apps by their enabled state, then by title
        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                if (lhs.enabled != rhs.enabled) {
                    return lhs.enabled ? -1 : 1;
                }
                return lhs.title.compareToIgnoreCase(rhs.title);
            }
        });

        return apps;
    }

    private boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }

    private class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_help_title)
                    .setMessage(R.string.privacy_guard_help_text)
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mPreferences.edit().putBoolean("first_help_shown", true).commit();
        }
    }

    private void showHelp() {
        HelpDialogFragment fragment = new HelpDialogFragment();
        fragment.show(getFragmentManager(), "help_dialog");
    }

    private class ResetDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_reset_title)
                    .setMessage(R.string.privacy_guard_reset_text)
                    .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // turn off privacy guard for all apps shown in the current list
                                for (AppInfo app : mApps) {
                                    app.privacyGuardEnabled = false;
                                }
                                mAppOps.resetAllModes();
                                mAdapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing
                        }
                    })
                    .create();
        }
    }

    private void showResetDialog() {
        ResetDialogFragment dialog = new ResetDialogFragment();
        dialog.show(getFragmentManager(), "reset_dialog");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.privacy_guard_manager, menu);
        menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                showHelp();
                return true;
            case R.id.reset:
                resetPrivacyGuard();
                return true;
            case R.id.show_system_apps:
                final String prefName = "show_system_apps";
                // set the menu checkbox and save it in
                // shared preference and rebuild the list
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefName, item.isChecked()).commit();
                loadApps();
                return true;
            case R.id.advanced:
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setClass(mActivity, AppOpsSummaryActivity.class);
                mActivity.startActivity(i);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
