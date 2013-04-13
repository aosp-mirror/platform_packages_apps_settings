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
import android.app.AppGlobals;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SelectableEditTextPreference;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import libcore.util.CollectionUtils;

public class AppRestrictionsFragment extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnClickListener, OnPreferenceClickListener {

    private static final String TAG = AppRestrictionsFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    private static final String PKG_PREFIX = "pkg_";
    private static final String KEY_USER_INFO = "user_info";

    private UserManager mUserManager;
    private UserHandle mUser;

    private SelectableEditTextPreference mUserPreference;
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

    private int mCustomRequestCode;
    private HashMap<Integer, AppRestrictionsPreference> mCustomRequestMap =
            new HashMap<Integer,AppRestrictionsPreference>();

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
            mNewUser = icicle.getBoolean(EXTRA_NEW_USER, false);
            mUser = new UserHandle(icicle.getInt(EXTRA_USER_ID));
        } else {
            Bundle args = getArguments();

            if (args.containsKey(EXTRA_USER_ID)) {
                mUser = new UserHandle(args.getInt(EXTRA_USER_ID));
            }
            mNewUser = args.getBoolean(EXTRA_NEW_USER, false);
        }
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        addPreferencesFromResource(R.xml.app_restrictions);
        mAppList = getPreferenceScreen();
        mUserPreference = (SelectableEditTextPreference) findPreference(KEY_USER_INFO);
        mUserPreference.setOnPreferenceChangeListener(this);
        mUserPreference.getEditText().setInputType(
                InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mUserPreference.setInitialSelectionMode(
                SelectableEditTextPreference.SELECTION_SELECT_ALL);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_NEW_USER, mNewUser);
        outState.putInt(EXTRA_USER_ID, mUser.getIdentifier());
    }

    public void onResume() {
        super.onResume();
        mAppListChanged = false;
        if (mFirstTime) {
            mFirstTime = false;
            populateApps();
        }
        UserInfo info = mUserManager.getUserInfo(mUser.getIdentifier());
        mUserPreference.setTitle(info.name);
        Bitmap userIcon = mUserManager.getUserIcon(mUser.getIdentifier());
        CircleFramedDrawable circularIcon =
                CircleFramedDrawable.getInstance(this.getActivity(), userIcon);
        mUserPreference.setIcon(circularIcon);
        mUserPreference.setText(info.name);
    }

    public void onPause() {
        super.onPause();
        if (mAppListChanged) {
            new Thread() {
                public void run() {
                    updateUserAppList();
                }
            }.start();
        }
    }

    private void updateUserAppList() {
        IPackageManager ipm = IPackageManager.Stub.asInterface(
                ServiceManager.getService("package"));
        for (Map.Entry<String,Boolean> entry : mSelectedPackages.entrySet()) {
            if (entry.getValue()) {
                // Enable selected apps
                try {
                    ipm.installExistingPackageAsUser(entry.getKey(), mUser.getIdentifier());
                } catch (RemoteException re) {
                }
            } else {
                // Blacklist all other apps, system or downloaded
                try {
                    ipm.deletePackageAsUser(entry.getKey(), null, mUser.getIdentifier(),
                            PackageManager.DELETE_SYSTEM_APP);
                } catch (RemoteException re) {
                }
            }
        }
    }

    private void addSystemApps(List<SelectableAppInfo> visibleApps, Intent intent) {
        final PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> launchableApps = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo app : launchableApps) {
            if (app.activityInfo != null && app.activityInfo.applicationInfo != null) {
                int flags = app.activityInfo.applicationInfo.flags;
                if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    // System app
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

    private void populateApps() {
        mAppList.setOrderingAsAdded(false);
        List<SelectableAppInfo> visibleApps = new ArrayList<SelectableAppInfo>();
        // TODO: Do this asynchronously since it can be a long operation
        final Context context = getActivity();
        PackageManager pm = context.getPackageManager();
        IPackageManager ipm = AppGlobals.getPackageManager();

        // Add launchers
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        addSystemApps(visibleApps, launcherIntent);

        // Add widgets
        Intent widgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        addSystemApps(visibleApps, widgetIntent);

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
                visibleApps.add(info);
            }
        }

        // Now check apps that are installed on target user
        List<ApplicationInfo> userApps = null;
        try {
            userApps = ipm.getInstalledApplications(
                    0, mUser.getIdentifier()).getList();
        } catch (RemoteException re) {
        }

        if (userApps != null) {
            for (ApplicationInfo app : userApps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                        && (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    // Downloaded app
                    SelectableAppInfo info = new SelectableAppInfo();
                    info.packageName = app.packageName;
                    info.appName = app.loadLabel(pm);
                    info.activityName = info.appName;
                    info.icon = app.loadIcon(pm);
                    visibleApps.add(info);
                }
            }
        }
        Collections.sort(visibleApps, new AppLabelComparator());

        // Remove dupes
        for (int i = visibleApps.size() - 1; i > 1; i--) {
            SelectableAppInfo info = visibleApps.get(i);
            if (DEBUG) Log.i(TAG, info.toString());
            if (info.packageName.equals(visibleApps.get(i-1).packageName)
                    && info.activityName.equals(visibleApps.get(i-1).activityName)) {
                visibleApps.remove(i);
            }
        }

        // Establish master/slave relationship for entries that share a package name
        HashMap<String,SelectableAppInfo> packageMap = new HashMap<String,SelectableAppInfo>();
        for (SelectableAppInfo info : visibleApps) {
            if (packageMap.containsKey(info.packageName)) {
                info.masterEntry = packageMap.get(info.packageName);
            } else {
                packageMap.put(info.packageName, info);
            }
        }

        Intent restrictionsIntent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(restrictionsIntent, 0);
        int i = 0;
        if (visibleApps.size() > 0) {
            for (SelectableAppInfo app : visibleApps) {
                String packageName = app.packageName;
                if (packageName == null) continue;
                final boolean isSettingsApp = packageName.equals(getActivity().getPackageName());
                AppRestrictionsPreference p = new AppRestrictionsPreference(context, this);
                final boolean hasSettings = resolveInfoListHasPackage(receivers, packageName);
                p.setIcon(app.icon);
                p.setChecked(false);
                p.setTitle(app.activityName);
                if (app.masterEntry != null) {
                    p.setSummary(getActivity().getString(R.string.user_restrictions_controlled_by,
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
                } else if (!mNewUser && appInfoListHasPackage(userApps, packageName)) {
                    p.setChecked(true);
                }
                if (pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                    p.setChecked(false);
                    p.setImmutable(true);
                    p.setSummary(R.string.app_not_supported_in_limited);
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
        if (v.getTag() instanceof AppRestrictionsPreference) {
            AppRestrictionsPreference pref = (AppRestrictionsPreference) v.getTag();
            if (v.getId() == R.id.app_restrictions_settings) {
                toggleAppPanel(pref);
            } else if (!pref.isImmutable()) {
                pref.setChecked(!pref.isChecked());
                mSelectedPackages.put(pref.getKey().substring(PKG_PREFIX.length()),
                        pref.isChecked());
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
                            mUserManager.setApplicationRestrictions(packageName, restrictions,
                                    mUser);
                        }
                        break;
                    }
                }
            }
        } else if (preference == mUserPreference) {
            String userName = ((CharSequence) newValue).toString();
            if (!TextUtils.isEmpty(userName)) {
                mUserManager.setUserName(mUser.getIdentifier(), userName);
                mUserPreference.setTitle(userName);
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
                    List<RestrictionEntry> oldEntries =
                            mUserManager.getApplicationRestrictions(packageName, mUser);
                    Intent intent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
                    intent.setPackage(packageName);
                    intent.putParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS,
                            new ArrayList<RestrictionEntry>(oldEntries));
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    getActivity().sendOrderedBroadcast(intent, null,
                            new RestrictionsResultReceiver(packageName, preference),
                            null, Activity.RESULT_OK, null, null);
                }
            }
            preference.panelOpen = !preference.panelOpen;
        }
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
                    Intent.EXTRA_RESTRICTIONS);
            Intent restrictionsIntent = (Intent) results.getParcelable(CUSTOM_RESTRICTIONS_INTENT);
            if (restrictions != null && restrictionsIntent == null) {
                onRestrictionsReceived(preference, packageName, restrictions);
                mUserManager.setApplicationRestrictions(packageName, restrictions, mUser);
            } else if (restrictionsIntent != null) {
                final Intent customIntent = restrictionsIntent;
                customIntent.putParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS, restrictions);
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

        Log.i(TAG, "Got activity resultCode=" + resultCode + ", requestCode="
                + requestCode + ", data=" + data);

        AppRestrictionsPreference pref = mCustomRequestMap.get(requestCode);
        if (pref == null) {
            Log.w(TAG, "Unknown requestCode " + requestCode);
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            ArrayList<RestrictionEntry> list =
                    data.getParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS);
            if (list != null) {
                // If there's a valid result, persist it to the user manager.
                String packageName = pref.getKey().substring(PKG_PREFIX.length());
                pref.setRestrictions(list);
                mUserManager.setApplicationRestrictions(packageName, list, mUser);
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
}
