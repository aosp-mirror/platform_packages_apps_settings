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
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.Dialog;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import android.provider.ContactsContract.DisplayPhoto;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.File;
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
    private static final String KEY_USER_INFO = "user_info";

    private static final int DIALOG_ID_EDIT_USER_INFO = 1;

    private PackageManager mPackageManager;
    private UserManager mUserManager;
    private UserHandle mUser;

    private PreferenceGroup mAppList;

    private static final int MAX_APP_RESTRICTIONS = 100;

    private static final String DELIMITER = ";";

    /** Key for extra passed in from calling fragment for the userId of the user being edited */
    public static final String EXTRA_USER_ID = "user_id";

    /** Key for extra passed in from calling fragment to indicate if this is a newly created user */
    public static final String EXTRA_NEW_USER = "new_user";

    private static final String KEY_SAVED_PHOTO = "pending_photo";

    HashMap<String,Boolean> mSelectedPackages = new HashMap<String,Boolean>();
    private boolean mFirstTime = true;
    private boolean mNewUser;
    private boolean mAppListChanged;

    private int mCustomRequestCode;
    private HashMap<Integer, AppRestrictionsPreference> mCustomRequestMap =
            new HashMap<Integer,AppRestrictionsPreference>();
    private View mHeaderView;
    private ImageView mUserIconView;
    private TextView mUserNameView;

    private List<SelectableAppInfo> mVisibleApps;
    private List<ApplicationInfo> mUserApps;

    private Dialog mEditUserInfoDialog;

    private EditUserPhotoController mEditUserPhotoController;
    private Bitmap mSavedPhoto;

    private BroadcastReceiver mUserBackgrounding = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update the user's app selection right away without waiting for a pause
            // onPause() might come in too late, causing apps to disappear after broadcasts
            // have been scheduled during user startup.
            if (mAppListChanged) {
                if (DEBUG) Log.d(TAG, "User backgrounding, update app list");
                updateUserAppList();
                if (DEBUG) Log.d(TAG, "User backgrounding, done updating app list");
            }
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
        boolean panelOpen;
        private boolean immutable;
        List<Preference> childPreferences = new ArrayList<Preference>();
        private SelectableAppInfo appInfo;
        private final ColorFilter grayscaleFilter;

        AppRestrictionsPreference(Context context, OnClickListener listener) {
            super(context);
            setLayoutResource(R.layout.preference_app_restrictions);
            this.listener = listener;

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            float[] matrix = colorMatrix.getArray();
            matrix[18] = 0.5f;
            grayscaleFilter = new ColorMatrixColorFilter(colorMatrix);
        }

        private void setSettingsEnabled(boolean enable) {
            hasSettings = enable;
        }

        @Override
        public void setChecked(boolean checked) {
            if (checked) {
                getIcon().setColorFilter(null);
            } else {
                getIcon().setColorFilter(grayscaleFilter);
            }
            super.setChecked(checked);
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

        void setSelectableAppInfo(SelectableAppInfo appInfo) {
            this.appInfo = appInfo;
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
                final Switch switchView = (Switch) widget.getChildAt(0);
                switchView.setEnabled(!isImmutable());
                switchView.setTag(this);
                switchView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        listener.onClick(switchView);
                    }
                });
            }
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mUser = new UserHandle(icicle.getInt(EXTRA_USER_ID));
            mSavedPhoto = (Bitmap) icicle.getParcelable(KEY_SAVED_PHOTO);
        } else {
            Bundle args = getArguments();

            if (args.containsKey(EXTRA_USER_ID)) {
                mUser = new UserHandle(args.getInt(EXTRA_USER_ID));
            }
            mNewUser = args.getBoolean(EXTRA_NEW_USER, false);
        }
        mPackageManager = getActivity().getPackageManager();
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        addPreferencesFromResource(R.xml.app_restrictions);
        mAppList = getPreferenceScreen();
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (mHeaderView == null) {
            mHeaderView = LayoutInflater.from(getActivity()).inflate(
                    R.layout.user_info_header, null);
            ((ViewGroup) getListView().getParent()).addView(mHeaderView, 0);
            mHeaderView.setOnClickListener(this);
            mUserIconView = (ImageView) mHeaderView.findViewById(android.R.id.icon);
            mUserNameView = (TextView) mHeaderView.findViewById(android.R.id.title);
            getListView().setFastScrollEnabled(true);
        }
        // This is going to bind the preferences.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_USER_ID, mUser.getIdentifier());
        if (mEditUserInfoDialog != null && mEditUserInfoDialog.isShowing()
                && mEditUserPhotoController != null) {
            outState.putParcelable(KEY_SAVED_PHOTO,
                    mEditUserPhotoController.getNewUserPhotoBitmap());
        }
    }

    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mUserBackgrounding,
                new IntentFilter(Intent.ACTION_USER_BACKGROUND));
        mAppListChanged = false;
        new AppLoadingTask().execute((Void[]) null);

        UserInfo info = mUserManager.getUserInfo(mUser.getIdentifier());
        ((TextView) mHeaderView.findViewById(android.R.id.title)).setText(info.name);
        ((ImageView) mHeaderView.findViewById(android.R.id.icon)).setImageDrawable(
                getCircularUserIcon());
    }

    public void onPause() {
        super.onPause();
        mNewUser = false;
        getActivity().unregisterReceiver(mUserBackgrounding);
        if (mAppListChanged) {
            new Thread() {
                public void run() {
                    updateUserAppList();
                }
            }.start();
        }
    }

    private Drawable getCircularUserIcon() {
        Bitmap userIcon = mUserManager.getUserIcon(mUser.getIdentifier());
        CircleFramedDrawable circularIcon =
                CircleFramedDrawable.getInstance(this.getActivity(), userIcon);
        return circularIcon;
    }

    private void updateUserAppList() {
        IPackageManager ipm = IPackageManager.Stub.asInterface(
                ServiceManager.getService("package"));
        final int userId = mUser.getIdentifier();
        if (!mUserManager.getUserInfo(userId).isRestricted()) {
            Log.e(TAG, "Cannot apply application restrictions on a regular user!");
            return;
        }
        for (Map.Entry<String,Boolean> entry : mSelectedPackages.entrySet()) {
            String packageName = entry.getKey();
            if (entry.getValue()) {
                // Enable selected apps
                try {
                    ApplicationInfo info = ipm.getApplicationInfo(packageName, 0, userId);
                    if (info == null || info.enabled == false) {
                        ipm.installExistingPackageAsUser(packageName, mUser.getIdentifier());
                        if (DEBUG) {
                            Log.d(TAG, "Installing " + packageName);
                        }
                    }
                } catch (RemoteException re) {
                }
            } else {
                // Blacklist all other apps, system or downloaded
                try {
                    ApplicationInfo info = ipm.getApplicationInfo(packageName, 0, userId);
                    if (info != null) {
                        ipm.deletePackageAsUser(entry.getKey(), null, mUser.getIdentifier(),
                                PackageManager.DELETE_SYSTEM_APP);
                        if (DEBUG) {
                            Log.d(TAG, "Uninstalling " + packageName);
                        }
                    }
                } catch (RemoteException re) {
                }
            }
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
                PackageManager.GET_DISABLED_COMPONENTS);
        for (ResolveInfo app : launchableApps) {
            if (app.activityInfo != null && app.activityInfo.applicationInfo != null) {
                int flags = app.activityInfo.applicationInfo.flags;
                if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    // System app
                    // Skip excluded packages
                    if (excludePackages.contains(app.activityInfo.packageName)) continue;

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
        IPackageManager ipm = AppGlobals.getPackageManager();

        final HashSet<String> excludePackages = new HashSet<String>();
        addSystemImes(excludePackages);

        // Add launchers
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        addSystemApps(mVisibleApps, launcherIntent, excludePackages);

        // Add widgets
        Intent widgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        addSystemApps(mVisibleApps, widgetIntent, excludePackages);

        List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);
        for (ApplicationInfo app : installedApps) {
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
                    if (pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                        mSelectedPackages.put(app.packageName, false);
                    }
                } catch (NameNotFoundException re) {
                }
            }
        }

        mUserApps = null;
        try {
            mUserApps = ipm.getInstalledApplications(
                    0, mUser.getIdentifier()).getList();
        } catch (RemoteException re) {
        }

        if (mUserApps != null) {
            for (ApplicationInfo app : mUserApps) {
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

    private void populateApps() {
        final Context context = getActivity();
        if (context == null) return;
        final PackageManager pm = mPackageManager;
        IPackageManager ipm = AppGlobals.getPackageManager();
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
                p.setKey(PKG_PREFIX + packageName);
                p.setSettingsEnabled(hasSettings || isSettingsApp);
                p.setPersistent(false);
                p.setOnPreferenceChangeListener(this);
                p.setOnPreferenceClickListener(this);
                PackageInfo pi = null;
                try {
                    pi = pm.getPackageInfo(packageName, 0);
                } catch (NameNotFoundException re) {
                    try {
                        pi = ipm.getPackageInfo(packageName, 0, mUser.getIdentifier());
                    } catch (RemoteException e) {
                    }
                }
                if (pi != null && pi.requiredForAllUsers) {
                    p.setChecked(true);
                    p.setImmutable(true);
                    // If the app is required and has no restrictions, skip showing it
                    if (!hasSettings && !isSettingsApp) continue;
                    // Get and populate the defaults, since the user is not going to be
                    // able to toggle this app ON (it's ON by default and immutable).
                    if (hasSettings) {
                        requestRestrictionsForApp(packageName, p);
                    }
                } else if (!mNewUser && appInfoListHasPackage(mUserApps, packageName)) {
                    p.setChecked(true);
                }
                if (pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                    p.setChecked(false);
                    p.setImmutable(true);
                    p.setSummary(R.string.app_not_supported_in_limited);
                }
                if (pi.restrictedAccountType != null) {
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
                p.setSelectableAppInfo(app);
                mSelectedPackages.put(packageName, p.isChecked());
                mAppListChanged = true;
                i++;
            }
        }
        // If this is the first time for a new profile, install/uninstall default apps for profile
        // to avoid taking the hit in onPause(), which can cause race conditions on user switch.
        if (mNewUser && mFirstTime) {
            mFirstTime = false;
            updateUserAppList();
        }
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

    private boolean appInfoListHasPackage(List<ApplicationInfo> apps, String packageName) {
        for (ApplicationInfo info : apps) {
            if (info.packageName.equals(packageName)) {
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
        if (v == mHeaderView) {
            showDialog(DIALOG_ID_EDIT_USER_INFO);
        } else if (v.getTag() instanceof AppRestrictionsPreference) {
            AppRestrictionsPreference pref = (AppRestrictionsPreference) v.getTag();
            if (v.getId() == R.id.app_restrictions_settings) {
                toggleAppPanel(pref);
            } else if (!pref.isImmutable()) {
                pref.setChecked(!pref.isChecked());
                final String packageName = pref.getKey().substring(PKG_PREFIX.length());
                mSelectedPackages.put(packageName, pref.isChecked());
                if (pref.isChecked() && pref.hasSettings
                        && pref.restrictions == null) {
                    // The restrictions have not been initialized, get and save them
                    requestRestrictionsForApp(packageName, pref);
                }
                mAppListChanged = true;
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
                            MultiSelectListPreference msListPref =
                                    (MultiSelectListPreference) preference;
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

    private void toggleAppPanel(AppRestrictionsPreference preference) {
        if (preference.getKey().startsWith(PKG_PREFIX)) {
            if (preference.panelOpen) {
                for (Preference p : preference.childPreferences) {
                    mAppList.removePreference(p);
                }
                preference.childPreferences.clear();
            } else {
                String packageName = preference.getKey().substring(PKG_PREFIX.length());
                if (packageName.equals(getActivity().getPackageName())) {
                    // Settings, fake it by using user restrictions
                    ArrayList<RestrictionEntry> restrictions = RestrictionUtils.getRestrictions(
                            getActivity(), mUser);
                    onRestrictionsReceived(preference, packageName, restrictions);
                } else {
                    requestRestrictionsForApp(packageName, preference);
                }
            }
            preference.panelOpen = !preference.panelOpen;
        }
    }

    private void requestRestrictionsForApp(String packageName,
            AppRestrictionsPreference preference) {
        Bundle oldEntries =
                mUserManager.getApplicationRestrictions(packageName, mUser);
        Intent intent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
        intent.setPackage(packageName);
        intent.putExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE, oldEntries);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        getActivity().sendOrderedBroadcast(intent, null,
                new RestrictionsResultReceiver(packageName, preference),
                null, Activity.RESULT_OK, null, null);
    }

    class RestrictionsResultReceiver extends BroadcastReceiver {

        private static final String CUSTOM_RESTRICTIONS_INTENT = Intent.EXTRA_RESTRICTIONS_INTENT;
        String packageName;
        AppRestrictionsPreference preference;

        RestrictionsResultReceiver(String packageName, AppRestrictionsPreference preference) {
            super();
            this.packageName = packageName;
            this.preference = preference;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle results = getResultExtras(true);
            final ArrayList<RestrictionEntry> restrictions = results.getParcelableArrayList(
                    Intent.EXTRA_RESTRICTIONS_LIST);
            Intent restrictionsIntent = (Intent) results.getParcelable(CUSTOM_RESTRICTIONS_INTENT);
            if (restrictions != null && restrictionsIntent == null) {
                onRestrictionsReceived(preference, packageName, restrictions);
                mUserManager.setApplicationRestrictions(packageName,
                        RestrictionUtils.restrictionsToBundle(restrictions), mUser);
            } else if (restrictionsIntent != null) {
                final Intent customIntent = restrictionsIntent;
                if (restrictions != null) {
                    customIntent.putExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE,
                            RestrictionUtils.restrictionsToBundle(restrictions));
                }
                Preference p = new Preference(context);
                p.setTitle(R.string.app_restrictions_custom_label);
                p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        int requestCode = generateCustomActivityRequestCode(
                                RestrictionsResultReceiver.this.preference);
                        AppRestrictionsFragment.this.startActivityForResult(
                                customIntent, requestCode);
                        return false;
                    }
                });
                p.setPersistent(false);
                p.setOrder(preference.getOrder() + 1);
                preference.childPreferences.add(p);
                mAppList.addPreference(p);
                preference.setRestrictions(restrictions);
            }
        }
    }

    private void onRestrictionsReceived(AppRestrictionsPreference preference, String packageName,
            ArrayList<RestrictionEntry> restrictions) {
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
                preference.childPreferences.add(p);
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

        if (mEditUserInfoDialog != null && mEditUserInfoDialog.isShowing()
                && mEditUserPhotoController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

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
            toggleAppPanel(pref);
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
                arp.setChecked(!arp.isChecked());
                mSelectedPackages.put(arp.getKey().substring(PKG_PREFIX.length()), arp.isChecked());
                updateAllEntries(arp.getKey(), arp.isChecked());
                mAppListChanged = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_USER_INFO) {
            if (mEditUserInfoDialog != null) {
                return mEditUserInfoDialog;
            }

            LayoutInflater inflater = getActivity().getLayoutInflater();
            View content = inflater.inflate(R.layout.edit_user_info_dialog_content, null);

            UserInfo info = mUserManager.getUserInfo(mUser.getIdentifier());

            final EditText userNameView = (EditText) content.findViewById(R.id.user_name);
            userNameView.setText(info.name);

            final ImageView userPhotoView = (ImageView) content.findViewById(R.id.user_photo);
            Drawable drawable = null;
            if (mSavedPhoto != null) {
                drawable = CircleFramedDrawable.getInstance(getActivity(), mSavedPhoto);
            } else {
                drawable = mUserIconView.getDrawable();
                if (drawable == null) {
                    drawable = getCircularUserIcon();
                }
            }
            userPhotoView.setImageDrawable(drawable);

            mEditUserPhotoController = new EditUserPhotoController(this, userPhotoView,
                    mSavedPhoto, drawable);

            mEditUserInfoDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.profile_info_settings_title)
                .setIconAttribute(R.drawable.ic_settings_multiuser)
                .setView(content)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // Update the name if changed.
                            CharSequence userName = userNameView.getText();
                            if (!TextUtils.isEmpty(userName)) {
                                CharSequence oldUserName = mUserNameView.getText();
                                if (oldUserName == null
                                        || !userName.toString().equals(oldUserName.toString())) {
                                    ((TextView) mHeaderView.findViewById(android.R.id.title))
                                            .setText(userName.toString());
                                    mUserManager.setUserName(mUser.getIdentifier(),
                                            userName.toString());
                                }
                            }
                            // Update the photo if changed.
                            Drawable drawable = mEditUserPhotoController.getNewUserPhotoDrawable();
                            Bitmap bitmap = mEditUserPhotoController.getNewUserPhotoBitmap();
                            if (drawable != null && bitmap != null
                                    && !drawable.equals(mUserIconView.getDrawable())) {
                                mUserIconView.setImageDrawable(drawable);
                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        mUserManager.setUserIcon(mUser.getIdentifier(),
                                                mEditUserPhotoController.getNewUserPhotoBitmap());
                                        return null;
                                    }
                                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                            }
                            removeDialog(DIALOG_ID_EDIT_USER_INFO);
                        }
                        clearEditUserInfoDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel,  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearEditUserInfoDialog();
                    }
                 })
                .create();

            // Make sure the IME is up.
            mEditUserInfoDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            return mEditUserInfoDialog;
        }

        return null;
    }

    private void clearEditUserInfoDialog() {
        mEditUserInfoDialog = null;
        mSavedPhoto = null;
    }

    private static class EditUserPhotoController {
        private static final int POPUP_LIST_ITEM_ID_CHOOSE_PHOTO = 1;
        private static final int POPUP_LIST_ITEM_ID_TAKE_PHOTO = 2;

        // It seems that this class generates custom request codes and they may
        // collide with ours, these values are very unlikely to have a conflict.
        private static final int REQUEST_CODE_CHOOSE_PHOTO = Integer.MAX_VALUE;
        private static final int REQUEST_CODE_TAKE_PHOTO = Integer.MAX_VALUE - 1;
        private static final int REQUEST_CODE_CROP_PHOTO = Integer.MAX_VALUE - 2;

        private static final String CROP_PICTURE_FILE_NAME = "CropEditUserPhoto.jpg";
        private static final String TAKE_PICTURE_FILE_NAME = "TakeEditUserPhoto2.jpg";

        private final int mPhotoSize;

        private final Context mContext;
        private final Fragment mFragment;
        private final ImageView mImageView;

        private final Uri mCropPictureUri;
        private final Uri mTakePictureUri;

        private Bitmap mNewUserPhotoBitmap;
        private Drawable mNewUserPhotoDrawable;

        public EditUserPhotoController(Fragment fragment, ImageView view,
                Bitmap bitmap, Drawable drawable) {
            mContext = view.getContext();
            mFragment = fragment;
            mImageView = view;
            mCropPictureUri = createTempImageUri(mContext, CROP_PICTURE_FILE_NAME);
            mTakePictureUri = createTempImageUri(mContext, TAKE_PICTURE_FILE_NAME);
            mPhotoSize = getPhotoSize(mContext);
            mImageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUpdatePhotoPopup();
                }
            });
            mNewUserPhotoBitmap = bitmap;
            mNewUserPhotoDrawable = drawable;
        }

        public boolean onActivityResult(int requestCode, int resultCode, final Intent data) {
            if (resultCode != Activity.RESULT_OK) {
                return false;
            }
            switch (requestCode) {
                case REQUEST_CODE_CHOOSE_PHOTO:
                case REQUEST_CODE_CROP_PHOTO: {
                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            return BitmapFactory.decodeFile(mCropPictureUri.getPath());
                        }
                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            mNewUserPhotoBitmap = bitmap;
                            mNewUserPhotoDrawable = CircleFramedDrawable
                                    .getInstance(mImageView.getContext(), mNewUserPhotoBitmap);
                            mImageView.setImageDrawable(mNewUserPhotoDrawable);
                            // Delete the files - not needed anymore.
                            new File(mCropPictureUri.getPath()).delete();
                            new File(mTakePictureUri.getPath()).delete();
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                } return true;
                case REQUEST_CODE_TAKE_PHOTO: {
                    cropPhoto();
                } break;
            }
            return false;
        }

        public Bitmap getNewUserPhotoBitmap() {
            return mNewUserPhotoBitmap;
        }

        public Drawable getNewUserPhotoDrawable() {
            return mNewUserPhotoDrawable;
        }

        private void showUpdatePhotoPopup() {
            final boolean canTakePhoto = canTakePhoto();
            final boolean canChoosePhoto = canChoosePhoto();

            if (!canTakePhoto && !canChoosePhoto) {
                return;
            }

            Context context = mImageView.getContext();
            final List<AdapterItem> items = new ArrayList<AdapterItem>();

            if (canTakePhoto()) {
                String title = mImageView.getContext().getString( R.string.user_image_take_photo);
                AdapterItem item = new AdapterItem(title, POPUP_LIST_ITEM_ID_TAKE_PHOTO);
                items.add(item);
            }

            if (canChoosePhoto) {
                String title = context.getString(R.string.user_image_choose_photo);
                AdapterItem item = new AdapterItem(title, POPUP_LIST_ITEM_ID_CHOOSE_PHOTO);
                items.add(item);
            }

            final ListPopupWindow listPopupWindow = new ListPopupWindow(context);

            listPopupWindow.setAnchorView(mImageView);
            listPopupWindow.setModal(true);
            listPopupWindow.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);

            ListAdapter adapter = new ArrayAdapter<AdapterItem>(context,
                    R.layout.edit_user_photo_popup_item, items);
            listPopupWindow.setAdapter(adapter);

            final int width = Math.max(mImageView.getWidth(), context.getResources()
                    .getDimensionPixelSize(R.dimen.update_user_photo_popup_min_width));
            listPopupWindow.setWidth(width);

            listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AdapterItem item = items.get(position);
                    switch (item.id) {
                        case POPUP_LIST_ITEM_ID_CHOOSE_PHOTO: {
                            choosePhoto();
                            listPopupWindow.dismiss();
                        } break;
                        case POPUP_LIST_ITEM_ID_TAKE_PHOTO: {
                            takePhoto();
                            listPopupWindow.dismiss();
                        } break;
                    }
                }
            });

            listPopupWindow.show();
        }

        private boolean canTakePhoto() {
            return mImageView.getContext().getPackageManager().queryIntentActivities(
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
        }

        private boolean canChoosePhoto() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            return mImageView.getContext().getPackageManager().queryIntentActivities(
                    intent, 0).size() > 0;
        }

        private void takePhoto() {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mTakePictureUri);
            mFragment.startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
        }

        private void choosePhoto() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mCropPictureUri);
            appendCropExtras(intent);
            mFragment.startActivityForResult(intent, REQUEST_CODE_CHOOSE_PHOTO);
        }

        private void cropPhoto() {
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(mTakePictureUri, "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mCropPictureUri);
            appendCropExtras(intent);
            mFragment.startActivityForResult(intent, REQUEST_CODE_CROP_PHOTO);
        }

        private void appendCropExtras(Intent intent) {
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", mPhotoSize);
            intent.putExtra("outputY", mPhotoSize);
        }

        private static int getPhotoSize(Context context) {
            Cursor cursor = context.getContentResolver().query(
                    DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
                    new String[]{DisplayPhoto.DISPLAY_MAX_DIM}, null, null, null);
            try {
                cursor.moveToFirst();
                return cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }

        private static Uri createTempImageUri(Context context, String fileName) {
            File folder = context.getExternalCacheDir();
            folder.mkdirs();
            File fullPath = new File(folder, fileName);
            fullPath.delete();
            return Uri.fromFile(fullPath.getAbsoluteFile());
        }

        private static final class AdapterItem {
            final String title;
            final int id;

            public AdapterItem(String title, int id) {
                this.title = title;
                this.id = id;
            }

            @Override
            public String toString() {
                return title;
            }
        }
    }
}
