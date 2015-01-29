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
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.drawable.CircleFramedDrawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class AppRestrictionsFragment extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnClickListener, OnPreferenceClickListener {

    private static final String TAG = AppRestrictionsFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    private static final String PKG_PREFIX = "pkg_";

    protected PackageManager mPackageManager;
    protected UserManager mUserManager;
    protected IPackageManager mIPm;
    protected UserHandle mUser;
    private PackageInfo mSysPackageInfo;

    private PreferenceGroup mAppList;

    private static final int MAX_APP_RESTRICTIONS = 100;

    private static final String DELIMITER = ";";

    /** Key for extra passed in from calling fragment for the userId of the user being edited */
    public static final String EXTRA_USER_ID = "user_id";

    /** Key for extra passed in from calling fragment to indicate if this is a newly created user */
    public static final String EXTRA_NEW_USER = "new_user";

    HashMap<String,Boolean> mSelectedPackages = new HashMap<String,Boolean>();
    private boolean mFirstTime = true;
    private boolean mNewUser;
    private boolean mAppListChanged;
    protected boolean mRestrictedProfile;

    private static final int CUSTOM_REQUEST_CODE_START = 1000;
    private int mCustomRequestCode = CUSTOM_REQUEST_CODE_START;

    private HashMap<Integer, AppRestrictionsPreference> mCustomRequestMap =
            new HashMap<Integer,AppRestrictionsPreference>();

    private List<SelectableAppInfo> mVisibleApps;
    private List<ApplicationInfo> mUserApps;
    private AsyncTask mAppLoadingTask;

    private BroadcastReceiver mUserBackgrounding = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update the user's app selection right away without waiting for a pause
            // onPause() might come in too late, causing apps to disappear after broadcasts
            // have been scheduled during user startup.
            if (mAppListChanged) {
                if (DEBUG) Log.d(TAG, "User backgrounding, update app list");
                applyUserAppsStates();
                if (DEBUG) Log.d(TAG, "User backgrounding, done updating app list");
            }
        }
    };

    private BroadcastReceiver mPackageObserver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onPackageChanged(intent);
        }
    };

    static class SelectableAppInfo {
        String packageName;
        CharSequence appName;
        CharSequence activityName;
        Drawable icon;
        SelectableAppInfo masterEntry;

        @Override
        public String toString() {
            return packageName + ": appName=" + appName + "; activityName=" + activityName
                    + "; icon=" + icon + "; masterEntry=" + masterEntry;
        }
    }

    static class AppRestrictionsPreference extends SwitchPreference {
        private boolean hasSettings;
        private OnClickListener listener;
        private ArrayList<RestrictionEntry> restrictions;
        private boolean panelOpen;
        private boolean immutable;
        private List<Preference> mChildren = new ArrayList<Preference>();

        AppRestrictionsPreference(Context context, OnClickListener listener) {
            super(context);
            setLayoutResource(R.layout.preference_app_restrictions);
            this.listener = listener;
        }

        private void setSettingsEnabled(boolean enable) {
            hasSettings = enable;
        }

        void setRestrictions(ArrayList<RestrictionEntry> restrictions) {
            this.restrictions = restrictions;
        }

        void setImmutable(boolean immutable) {
            this.immutable = immutable;
        }

        boolean isImmutable() {
            return immutable;
        }

        RestrictionEntry getRestriction(String key) {
            if (restrictions == null) return null;
            for (RestrictionEntry entry : restrictions) {
                if (entry.getKey().equals(key)) {
                    return entry;
                }
            }
            return null;
        }

        ArrayList<RestrictionEntry> getRestrictions() {
            return restrictions;
        }

        boolean isPanelOpen() {
            return panelOpen;
        }

        void setPanelOpen(boolean open) {
            panelOpen = open;
        }

        List<Preference> getChildren() {
            return mChildren;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            View appRestrictionsSettings = view.findViewById(R.id.app_restrictions_settings);
            appRestrictionsSettings.setVisibility(hasSettings ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.settings_divider).setVisibility(
                    hasSettings ? View.VISIBLE : View.GONE);
            appRestrictionsSettings.setOnClickListener(listener);
            appRestrictionsSettings.setTag(this);

            View appRestrictionsPref = view.findViewById(R.id.app_restrictions_pref);
            appRestrictionsPref.setOnClickListener(listener);
            appRestrictionsPref.setTag(this);

            ViewGroup widget = (ViewGroup) view.findViewById(android.R.id.widget_frame);
            widget.setEnabled(!isImmutable());
            if (widget.getChildCount() > 0) {
                final Switch toggle = (Switch) widget.getChildAt(0);
                toggle.setEnabled(!isImmutable());
                toggle.setTag(this);
                toggle.setClickable(true);
                toggle.setFocusable(true);
                toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        listener.onClick(toggle);
                    }
                });
            }
        }
    }

    protected void init(Bundle icicle) {
        if (icicle != null) {
            mUser = new UserHandle(icicle.getInt(EXTRA_USER_ID));
        } else {
            Bundle args = getArguments();
            if (args != null) {
                if (args.containsKey(EXTRA_USER_ID)) {
                    mUser = new UserHandle(args.getInt(EXTRA_USER_ID));
                }
                mNewUser = args.getBoolean(EXTRA_NEW_USER, false);
            }
        }

        if (mUser == null) {
            mUser = android.os.Process.myUserHandle();
        }

        mPackageManager = getActivity().getPackageManager();
        mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        mRestrictedProfile = mUserManager.getUserInfo(mUser.getIdentifier()).isRestricted();
        try {
            mSysPackageInfo = mPackageManager.getPackageInfo("android",
                PackageManager.GET_SIGNATURES);
        } catch (NameNotFoundException nnfe) {
            // ?
        }
        addPreferencesFromResource(R.xml.app_restrictions);
        mAppList = getAppPreferenceGroup();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_USER_ID, mUser.getIdentifier());
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mUserBackgrounding,
                new IntentFilter(Intent.ACTION_USER_BACKGROUND));
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");
        getActivity().registerReceiver(mPackageObserver, packageFilter);

        mAppListChanged = false;
        if (mAppLoadingTask == null || mAppLoadingTask.getStatus() == AsyncTask.Status.FINISHED) {
            mAppLoadingTask = new AppLoadingTask().execute((Void[]) null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mNewUser = false;
        getActivity().unregisterReceiver(mUserBackgrounding);
        getActivity().unregisterReceiver(mPackageObserver);
        if (mAppListChanged) {
            new Thread() {
                public void run() {
                    applyUserAppsStates();
                }
            }.start();
        }
    }

    private void onPackageChanged(Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getData().getSchemeSpecificPart();
        // Package added, check if the preference needs to be enabled
        AppRestrictionsPreference pref = (AppRestrictionsPreference)
                findPreference(getKeyForPackage(packageName));
        if (pref == null) return;

        if ((Intent.ACTION_PACKAGE_ADDED.equals(action) && pref.isChecked())
                || (Intent.ACTION_PACKAGE_REMOVED.equals(action) && !pref.isChecked())) {
            pref.setEnabled(true);
        }
    }

    protected PreferenceGroup getAppPreferenceGroup() {
        return getPreferenceScreen();
    }

    Drawable getCircularUserIcon() {
        Bitmap userIcon = mUserManager.getUserIcon(mUser.getIdentifier());
        if (userIcon == null) {
            return null;
        }
        CircleFramedDrawable circularIcon =
                CircleFramedDrawable.getInstance(this.getActivity(), userIcon);
        return circularIcon;
    }

    protected void clearSelectedApps() {
        mSelectedPackages.clear();
    }

    private void applyUserAppsStates() {
        final int userId = mUser.getIdentifier();
        if (!mUserManager.getUserInfo(userId).isRestricted() && userId != UserHandle.myUserId()) {
            Log.e(TAG, "Cannot apply application restrictions on another user!");
            return;
        }
        for (Map.Entry<String,Boolean> entry : mSelectedPackages.entrySet()) {
            String packageName = entry.getKey();
            boolean enabled = entry.getValue();
            applyUserAppState(packageName, enabled);
        }
    }

    private void applyUserAppState(String packageName, boolean enabled) {
        final int userId = mUser.getIdentifier();
        if (enabled) {
            // Enable selected apps
            try {
                ApplicationInfo info = mIPm.getApplicationInfo(packageName,
                        PackageManager.GET_UNINSTALLED_PACKAGES, userId);
                if (info == null || info.enabled == false
                        || (info.flags&ApplicationInfo.FLAG_INSTALLED) == 0) {
                    mIPm.installExistingPackageAsUser(packageName, mUser.getIdentifier());
                    if (DEBUG) {
                        Log.d(TAG, "Installing " + packageName);
                    }
                }
                if (info != null && (info.flags&ApplicationInfo.FLAG_HIDDEN) != 0
                        && (info.flags&ApplicationInfo.FLAG_INSTALLED) != 0) {
                    disableUiForPackage(packageName);
                    mIPm.setApplicationHiddenSettingAsUser(packageName, false, userId);
                    if (DEBUG) {
                        Log.d(TAG, "Unhiding " + packageName);
                    }
                }
            } catch (RemoteException re) {
            }
        } else {
            // Blacklist all other apps, system or downloaded
            try {
                ApplicationInfo info = mIPm.getApplicationInfo(packageName, 0, userId);
                if (info != null) {
                    if (mRestrictedProfile) {
                        mIPm.deletePackageAsUser(packageName, null, mUser.getIdentifier(),
                                PackageManager.DELETE_SYSTEM_APP);
                        if (DEBUG) {
                            Log.d(TAG, "Uninstalling " + packageName);
                        }
                    } else {
                        disableUiForPackage(packageName);
                        mIPm.setApplicationHiddenSettingAsUser(packageName, true, userId);
                        if (DEBUG) {
                            Log.d(TAG, "Hiding " + packageName);
                        }
                    }
                }
            } catch (RemoteException re) {
            }
        }
    }

    private void disableUiForPackage(String packageName) {
        AppRestrictionsPreference pref = (AppRestrictionsPreference) findPreference(
                getKeyForPackage(packageName));
        if (pref != null) {
            pref.setEnabled(false);
        }
    }

    private boolean isSystemPackage(String packageName) {
        try {
            final PackageInfo pi = mPackageManager.getPackageInfo(packageName, 0);
            if (pi.applicationInfo == null) return false;
            final int flags = pi.applicationInfo.flags;
            if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                return true;
            }
        } catch (NameNotFoundException nnfe) {
            // Missing package?
        }
        return false;
    }

    /**
     * Find all pre-installed input methods that are marked as default
     * and add them to an exclusion list so that they aren't
     * presented to the user for toggling.
     * Don't add non-default ones, as they may include other stuff that we
     * don't need to auto-include.
     * @param excludePackages the set of package names to append to
     */
    private void addSystemImes(Set<String> excludePackages) {
        final Context context = getActivity();
        if (context == null) return;
        InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> imis = imm.getInputMethodList();
        for (InputMethodInfo imi : imis) {
            try {
                if (imi.isDefault(context) && isSystemPackage(imi.getPackageName())) {
                    excludePackages.add(imi.getPackageName());
                }
            } catch (Resources.NotFoundException rnfe) {
                // Not default
            }
        }
    }

    /**
     * Add system apps that match an intent to the list, excluding any packages in the exclude list.
     * @param visibleApps list of apps to append the new list to
     * @param intent the intent to match
     * @param excludePackages the set of package names to be excluded, since they're required
     */
    private void addSystemApps(List<SelectableAppInfo> visibleApps, Intent intent,
            Set<String> excludePackages) {
        if (getActivity() == null) return;
        final PackageManager pm = mPackageManager;
        List<ResolveInfo> launchableApps = pm.queryIntentActivities(intent,
                PackageManager.GET_DISABLED_COMPONENTS | PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ResolveInfo app : launchableApps) {
            if (app.activityInfo != null && app.activityInfo.applicationInfo != null) {
                final String packageName = app.activityInfo.packageName;
                int flags = app.activityInfo.applicationInfo.flags;
                if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    // System app
                    // Skip excluded packages
                    if (excludePackages.contains(packageName)) continue;
                    int enabled = pm.getApplicationEnabledSetting(packageName);
                    if (enabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
                            || enabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        // Check if the app is already enabled for the target user
                        ApplicationInfo targetUserAppInfo = getAppInfoForUser(packageName,
                                0, mUser);
                        if (targetUserAppInfo == null
                                || (targetUserAppInfo.flags&ApplicationInfo.FLAG_INSTALLED) == 0) {
                            continue;
                        }
                    }
                    SelectableAppInfo info = new SelectableAppInfo();
                    info.packageName = app.activityInfo.packageName;
                    info.appName = app.activityInfo.applicationInfo.loadLabel(pm);
                    info.icon = app.activityInfo.loadIcon(pm);
                    info.activityName = app.activityInfo.loadLabel(pm);
                    if (info.activityName == null) info.activityName = info.appName;

                    visibleApps.add(info);
                }
            }
        }
    }

    private ApplicationInfo getAppInfoForUser(String packageName, int flags, UserHandle user) {
        try {
            ApplicationInfo targetUserAppInfo = mIPm.getApplicationInfo(packageName, flags,
                    user.getIdentifier());
            return targetUserAppInfo;
        } catch (RemoteException re) {
            return null;
        }
    }

    private class AppLoadingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            fetchAndMergeApps();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            populateApps();
        }

        @Override
        protected void onPreExecute() {
        }
    }

    private void fetchAndMergeApps() {
        mAppList.setOrderingAsAdded(false);
        mVisibleApps = new ArrayList<SelectableAppInfo>();
        final Context context = getActivity();
        if (context == null) return;
        final PackageManager pm = mPackageManager;
        final IPackageManager ipm = mIPm;

        final HashSet<String> excludePackages = new HashSet<String>();
        addSystemImes(excludePackages);

        // Add launchers
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        addSystemApps(mVisibleApps, launcherIntent, excludePackages);

        // Add widgets
        Intent widgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        addSystemApps(mVisibleApps, widgetIntent, excludePackages);

        List<ApplicationInfo> installedApps = pm.getInstalledApplications(
                PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ApplicationInfo app : installedApps) {
            // If it's not installed, skip
            if ((app.flags & ApplicationInfo.FLAG_INSTALLED) == 0) continue;

            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    && (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                // Downloaded app
                SelectableAppInfo info = new SelectableAppInfo();
                info.packageName = app.packageName;
                info.appName = app.loadLabel(pm);
                info.activityName = info.appName;
                info.icon = app.loadIcon(pm);
                mVisibleApps.add(info);
            } else {
                try {
                    PackageInfo pi = pm.getPackageInfo(app.packageName, 0);
                    // If it's a system app that requires an account and doesn't see restricted
                    // accounts, mark for removal. It might get shown in the UI if it has an icon
                    // but will still be marked as false and immutable.
                    if (mRestrictedProfile
                            && pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                        mSelectedPackages.put(app.packageName, false);
                    }
                } catch (NameNotFoundException re) {
                }
            }
        }

        // Get the list of apps already installed for the user
        mUserApps = null;
        try {
            mUserApps = ipm.getInstalledApplications(
                    PackageManager.GET_UNINSTALLED_PACKAGES, mUser.getIdentifier()).getList();
        } catch (RemoteException re) {
        }

        if (mUserApps != null) {
            for (ApplicationInfo app : mUserApps) {
                if ((app.flags & ApplicationInfo.FLAG_INSTALLED) == 0) continue;

                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                        && (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    // Downloaded app
                    SelectableAppInfo info = new SelectableAppInfo();
                    info.packageName = app.packageName;
                    info.appName = app.loadLabel(pm);
                    info.activityName = info.appName;
                    info.icon = app.loadIcon(pm);
                    mVisibleApps.add(info);
                }
            }
        }

        // Sort the list of visible apps
        Collections.sort(mVisibleApps, new AppLabelComparator());

        // Remove dupes
        Set<String> dedupPackageSet = new HashSet<String>();
        for (int i = mVisibleApps.size() - 1; i >= 0; i--) {
            SelectableAppInfo info = mVisibleApps.get(i);
            if (DEBUG) Log.i(TAG, info.toString());
            String both = info.packageName + "+" + info.activityName;
            if (!TextUtils.isEmpty(info.packageName)
                    && !TextUtils.isEmpty(info.activityName)
                    && dedupPackageSet.contains(both)) {
                mVisibleApps.remove(i);
            } else {
                dedupPackageSet.add(both);
            }
        }

        // Establish master/slave relationship for entries that share a package name
        HashMap<String,SelectableAppInfo> packageMap = new HashMap<String,SelectableAppInfo>();
        for (SelectableAppInfo info : mVisibleApps) {
            if (packageMap.containsKey(info.packageName)) {
                info.masterEntry = packageMap.get(info.packageName);
            } else {
                packageMap.put(info.packageName, info);
            }
        }
    }

    private boolean isPlatformSigned(PackageInfo pi) {
        return (pi != null && pi.signatures != null &&
                    mSysPackageInfo.signatures[0].equals(pi.signatures[0]));
    }

    private boolean isAppEnabledForUser(PackageInfo pi) {
        if (pi == null) return false;
        final int flags = pi.applicationInfo.flags;
        // Return true if it is installed and not hidden
        return ((flags&ApplicationInfo.FLAG_INSTALLED) != 0
                && (flags&ApplicationInfo.FLAG_HIDDEN) == 0);
    }

    private void populateApps() {
        final Context context = getActivity();
        if (context == null) return;
        final PackageManager pm = mPackageManager;
        final IPackageManager ipm = mIPm;

        mAppList.removeAll();
        Intent restrictionsIntent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(restrictionsIntent, 0);
        int i = 0;
        if (mVisibleApps.size() > 0) {
            for (SelectableAppInfo app : mVisibleApps) {
                String packageName = app.packageName;
                if (packageName == null) continue;
                final boolean isSettingsApp = packageName.equals(context.getPackageName());
                AppRestrictionsPreference p = new AppRestrictionsPreference(context, this);
                final boolean hasSettings = resolveInfoListHasPackage(receivers, packageName);
                p.setIcon(app.icon != null ? app.icon.mutate() : null);
                p.setChecked(false);
                p.setTitle(app.activityName);
                if (app.masterEntry != null) {
                    p.setSummary(context.getString(R.string.user_restrictions_controlled_by,
                            app.masterEntry.activityName));
                }
                p.setKey(getKeyForPackage(packageName));
                p.setSettingsEnabled((hasSettings || isSettingsApp) && app.masterEntry == null);
                p.setPersistent(false);
                p.setOnPreferenceChangeListener(this);
                p.setOnPreferenceClickListener(this);
                PackageInfo pi = null;
                try {
                    pi = ipm.getPackageInfo(packageName,
                            PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_SIGNATURES, mUser.getIdentifier());
                } catch (RemoteException e) {
                }
                if (pi != null && (pi.requiredForAllUsers || isPlatformSigned(pi))) {
                    p.setChecked(true);
                    p.setImmutable(true);
                    // If the app is required and has no restrictions, skip showing it
                    if (!hasSettings && !isSettingsApp) continue;
                    // Get and populate the defaults, since the user is not going to be
                    // able to toggle this app ON (it's ON by default and immutable).
                    // Only do this for restricted profiles, not single-user restrictions
                    // Also don't do this for slave icons
                    if (hasSettings && app.masterEntry == null) {
                        requestRestrictionsForApp(packageName, p, false);
                    }
                } else if (!mNewUser && isAppEnabledForUser(pi)) {
                    p.setChecked(true);
                }
                if (mRestrictedProfile
                        && pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                    p.setChecked(false);
                    p.setImmutable(true);
                    p.setSummary(R.string.app_not_supported_in_limited);
                }
                if (mRestrictedProfile && pi.restrictedAccountType != null) {
                    p.setSummary(R.string.app_sees_restricted_accounts);
                }
                if (app.masterEntry != null) {
                    p.setImmutable(true);
                    p.setChecked(mSelectedPackages.get(packageName));
                }
                mAppList.addPreference(p);
                if (isSettingsApp) {
                    p.setOrder(MAX_APP_RESTRICTIONS * 1);
                } else {
                    p.setOrder(MAX_APP_RESTRICTIONS * (i + 2));
                }
                mSelectedPackages.put(packageName, p.isChecked());
                mAppListChanged = true;
                i++;
            }
        }
        // If this is the first time for a new profile, install/uninstall default apps for profile
        // to avoid taking the hit in onPause(), which can cause race conditions on user switch.
        if (mNewUser && mFirstTime) {
            mFirstTime = false;
            applyUserAppsStates();
        }
    }

    private String getKeyForPackage(String packageName) {
        return PKG_PREFIX + packageName;
    }

    private class AppLabelComparator implements Comparator<SelectableAppInfo> {

        @Override
        public int compare(SelectableAppInfo lhs, SelectableAppInfo rhs) {
            String lhsLabel = lhs.activityName.toString();
            String rhsLabel = rhs.activityName.toString();
            return lhsLabel.toLowerCase().compareTo(rhsLabel.toLowerCase());
        }
    }

    private boolean resolveInfoListHasPackage(List<ResolveInfo> receivers, String packageName) {
        for (ResolveInfo info : receivers) {
            if (info.activityInfo.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void updateAllEntries(String prefKey, boolean checked) {
        for (int i = 0; i < mAppList.getPreferenceCount(); i++) {
            Preference pref = mAppList.getPreference(i);
            if (pref instanceof AppRestrictionsPreference) {
                if (prefKey.equals(pref.getKey())) {
                    ((AppRestrictionsPreference) pref).setChecked(checked);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof AppRestrictionsPreference) {
            AppRestrictionsPreference pref = (AppRestrictionsPreference) v.getTag();
            if (v.getId() == R.id.app_restrictions_settings) {
                onAppSettingsIconClicked(pref);
            } else if (!pref.isImmutable()) {
                pref.setChecked(!pref.isChecked());
                final String packageName = pref.getKey().substring(PKG_PREFIX.length());
                mSelectedPackages.put(packageName, pref.isChecked());
                if (pref.isChecked() && pref.hasSettings
                        && pref.restrictions == null) {
                    // The restrictions have not been initialized, get and save them
                    requestRestrictionsForApp(packageName, pref, false);
                }
                mAppListChanged = true;
                // If it's not a restricted profile, apply the changes immediately
                if (!mRestrictedProfile) {
                    applyUserAppState(packageName, pref.isChecked());
                }
                updateAllEntries(pref.getKey(), pref.isChecked());
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key != null && key.contains(DELIMITER)) {
            StringTokenizer st = new StringTokenizer(key, DELIMITER);
            final String packageName = st.nextToken();
            final String restrictionKey = st.nextToken();
            AppRestrictionsPreference appPref = (AppRestrictionsPreference)
                    mAppList.findPreference(PKG_PREFIX+packageName);
            ArrayList<RestrictionEntry> restrictions = appPref.getRestrictions();
            if (restrictions != null) {
                for (RestrictionEntry entry : restrictions) {
                    if (entry.getKey().equals(restrictionKey)) {
                        switch (entry.getType()) {
                        case RestrictionEntry.TYPE_BOOLEAN:
                            entry.setSelectedState((Boolean) newValue);
                            break;
                        case RestrictionEntry.TYPE_CHOICE:
                        case RestrictionEntry.TYPE_CHOICE_LEVEL:
                            ListPreference listPref = (ListPreference) preference;
                            entry.setSelectedString((String) newValue);
                            String readable = findInArray(entry.getChoiceEntries(),
                                    entry.getChoiceValues(), (String) newValue);
                            listPref.setSummary(readable);
                            break;
                        case RestrictionEntry.TYPE_MULTI_SELECT:
                            Set<String> set = (Set<String>) newValue;
                            String [] selectedValues = new String[set.size()];
                            set.toArray(selectedValues);
                            entry.setAllSelectedStrings(selectedValues);
                            break;
                        default:
                            continue;
                        }
                        if (packageName.equals(getActivity().getPackageName())) {
                            RestrictionUtils.setRestrictions(getActivity(), restrictions, mUser);
                        } else {
                            mUserManager.setApplicationRestrictions(packageName,
                                    RestrictionUtils.restrictionsToBundle(restrictions),
                                    mUser);
                        }
                        break;
                    }
                }
            }
        }
        return true;
    }

    private void removeRestrictionsForApp(AppRestrictionsPreference preference) {
        for (Preference p : preference.mChildren) {
            mAppList.removePreference(p);
        }
        preference.mChildren.clear();
    }

    private void onAppSettingsIconClicked(AppRestrictionsPreference preference) {
        if (preference.getKey().startsWith(PKG_PREFIX)) {
            if (preference.isPanelOpen()) {
                removeRestrictionsForApp(preference);
            } else {
                String packageName = preference.getKey().substring(PKG_PREFIX.length());
                if (packageName.equals(getActivity().getPackageName())) {
                    // Settings, fake it by using user restrictions
                    ArrayList<RestrictionEntry> restrictions = RestrictionUtils.getRestrictions(
                            getActivity(), mUser);
                    onRestrictionsReceived(preference, packageName, restrictions);
                } else {
                    requestRestrictionsForApp(packageName, preference, true /*invoke if custom*/);
                }
            }
            preference.setPanelOpen(!preference.isPanelOpen());
        }
    }

    /**
     * Send a broadcast to the app to query its restrictions
     * @param packageName package name of the app with restrictions
     * @param preference the preference item for the app toggle
     * @param invokeIfCustom whether to directly launch any custom activity that is returned
     *        for the app.
     */
    private void requestRestrictionsForApp(String packageName,
            AppRestrictionsPreference preference, boolean invokeIfCustom) {
        Bundle oldEntries =
                mUserManager.getApplicationRestrictions(packageName, mUser);
        Intent intent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
        intent.setPackage(packageName);
        intent.putExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE, oldEntries);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        getActivity().sendOrderedBroadcast(intent, null,
                new RestrictionsResultReceiver(packageName, preference, invokeIfCustom),
                null, Activity.RESULT_OK, null, null);
    }

    class RestrictionsResultReceiver extends BroadcastReceiver {

        private static final String CUSTOM_RESTRICTIONS_INTENT = Intent.EXTRA_RESTRICTIONS_INTENT;
        String packageName;
        AppRestrictionsPreference preference;
        boolean invokeIfCustom;

        RestrictionsResultReceiver(String packageName, AppRestrictionsPreference preference,
                boolean invokeIfCustom) {
            super();
            this.packageName = packageName;
            this.preference = preference;
            this.invokeIfCustom = invokeIfCustom;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle results = getResultExtras(true);
            final ArrayList<RestrictionEntry> restrictions = results.getParcelableArrayList(
                    Intent.EXTRA_RESTRICTIONS_LIST);
            Intent restrictionsIntent = (Intent) results.getParcelable(CUSTOM_RESTRICTIONS_INTENT);
            if (restrictions != null && restrictionsIntent == null) {
                onRestrictionsReceived(preference, packageName, restrictions);
                if (mRestrictedProfile) {
                    mUserManager.setApplicationRestrictions(packageName,
                            RestrictionUtils.restrictionsToBundle(restrictions), mUser);
                }
            } else if (restrictionsIntent != null) {
                preference.setRestrictions(restrictions);
                if (invokeIfCustom && AppRestrictionsFragment.this.isResumed()) {
                    assertSafeToStartCustomActivity(restrictionsIntent);
                    int requestCode = generateCustomActivityRequestCode(
                            RestrictionsResultReceiver.this.preference);
                    AppRestrictionsFragment.this.startActivityForResult(
                            restrictionsIntent, requestCode);
                }
            }
        }

        private void assertSafeToStartCustomActivity(Intent intent) {
            // Activity can be started if it belongs to the same app
            if (intent.getPackage() != null && intent.getPackage().equals(packageName)) {
                return;
            }
            // Activity can be started if intent resolves to multiple activities
            List<ResolveInfo> resolveInfos = AppRestrictionsFragment.this.mPackageManager
                    .queryIntentActivities(intent, 0 /* no flags */);
            if (resolveInfos.size() != 1) {
                return;
            }
            // Prevent potential privilege escalation
            ActivityInfo activityInfo = resolveInfos.get(0).activityInfo;
            if (!packageName.equals(activityInfo.packageName)) {
                throw new SecurityException("Application " + packageName
                        + " is not allowed to start activity " + intent);
            };
        }
    }

    private void onRestrictionsReceived(AppRestrictionsPreference preference, String packageName,
            ArrayList<RestrictionEntry> restrictions) {
        // Remove any earlier restrictions
        removeRestrictionsForApp(preference);
        // Non-custom-activity case - expand the restrictions in-place
        final Context context = preference.getContext();
        int count = 1;
        for (RestrictionEntry entry : restrictions) {
            Preference p = null;
            switch (entry.getType()) {
            case RestrictionEntry.TYPE_BOOLEAN:
                p = new CheckBoxPreference(context);
                p.setTitle(entry.getTitle());
                p.setSummary(entry.getDescription());
                ((CheckBoxPreference)p).setChecked(entry.getSelectedState());
                break;
            case RestrictionEntry.TYPE_CHOICE:
            case RestrictionEntry.TYPE_CHOICE_LEVEL:
                p = new ListPreference(context);
                p.setTitle(entry.getTitle());
                String value = entry.getSelectedString();
                if (value == null) {
                    value = entry.getDescription();
                }
                p.setSummary(findInArray(entry.getChoiceEntries(), entry.getChoiceValues(),
                        value));
                ((ListPreference)p).setEntryValues(entry.getChoiceValues());
                ((ListPreference)p).setEntries(entry.getChoiceEntries());
                ((ListPreference)p).setValue(value);
                ((ListPreference)p).setDialogTitle(entry.getTitle());
                break;
            case RestrictionEntry.TYPE_MULTI_SELECT:
                p = new MultiSelectListPreference(context);
                p.setTitle(entry.getTitle());
                ((MultiSelectListPreference)p).setEntryValues(entry.getChoiceValues());
                ((MultiSelectListPreference)p).setEntries(entry.getChoiceEntries());
                HashSet<String> set = new HashSet<String>();
                for (String s : entry.getAllSelectedStrings()) {
                    set.add(s);
                }
                ((MultiSelectListPreference)p).setValues(set);
                ((MultiSelectListPreference)p).setDialogTitle(entry.getTitle());
                break;
            case RestrictionEntry.TYPE_NULL:
            default:
            }
            if (p != null) {
                p.setPersistent(false);
                p.setOrder(preference.getOrder() + count);
                // Store the restrictions key string as a key for the preference
                p.setKey(preference.getKey().substring(PKG_PREFIX.length()) + DELIMITER
                        + entry.getKey());
                mAppList.addPreference(p);
                p.setOnPreferenceChangeListener(AppRestrictionsFragment.this);
                p.setIcon(R.drawable.empty_icon);
                preference.mChildren.add(p);
                count++;
            }
        }
        preference.setRestrictions(restrictions);
        if (count == 1 // No visible restrictions
                && preference.isImmutable()
                && preference.isChecked()) {
            // Special case of required app with no visible restrictions. Remove it
            mAppList.removePreference(preference);
        }
    }

    /**
     * Generates a request code that is stored in a map to retrieve the associated
     * AppRestrictionsPreference.
     * @param preference
     * @return
     */
    private int generateCustomActivityRequestCode(AppRestrictionsPreference preference) {
        mCustomRequestCode++;
        mCustomRequestMap.put(mCustomRequestCode, preference);
        return mCustomRequestCode;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AppRestrictionsPreference pref = mCustomRequestMap.get(requestCode);
        if (pref == null) {
            Log.w(TAG, "Unknown requestCode " + requestCode);
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            String packageName = pref.getKey().substring(PKG_PREFIX.length());
            ArrayList<RestrictionEntry> list =
                    data.getParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS_LIST);
            Bundle bundle = data.getBundleExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE);
            if (list != null) {
                // If there's a valid result, persist it to the user manager.
                pref.setRestrictions(list);
                mUserManager.setApplicationRestrictions(packageName,
                        RestrictionUtils.restrictionsToBundle(list), mUser);
            } else if (bundle != null) {
                // If there's a valid result, persist it to the user manager.
                mUserManager.setApplicationRestrictions(packageName, bundle, mUser);
            }
        }
        // Remove request from the map
        mCustomRequestMap.remove(requestCode);
    }

    private String findInArray(String[] choiceEntries, String[] choiceValues,
            String selectedString) {
        for (int i = 0; i < choiceValues.length; i++) {
            if (choiceValues[i].equals(selectedString)) {
                return choiceEntries[i];
            }
        }
        return selectedString;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().startsWith(PKG_PREFIX)) {
            AppRestrictionsPreference arp = (AppRestrictionsPreference) preference;
            if (!arp.isImmutable()) {
                final String packageName = arp.getKey().substring(PKG_PREFIX.length());
                final boolean newEnabledState = !arp.isChecked();
                arp.setChecked(newEnabledState);
                mSelectedPackages.put(packageName, newEnabledState);
                updateAllEntries(arp.getKey(), newEnabledState);
                mAppListChanged = true;
                applyUserAppState(packageName, newEnabledState);
            }
            return true;
        }
        return false;
    }

}
