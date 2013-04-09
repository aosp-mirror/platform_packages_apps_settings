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

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import libcore.util.CollectionUtils;

public class AppRestrictionsFragment extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnClickListener, OnPreferenceClickListener {

    private static final String TAG = AppRestrictionsFragment.class.getSimpleName();

    private static final String PKG_PREFIX = "pkg_";
    private static final String KEY_USER_INFO = "user_info";

    private UserManager mUserManager;
    private UserHandle mUser;

    private EditTextPreference mUserPreference;
    private PreferenceGroup mAppList;

    private static final int MAX_APP_RESTRICTIONS = 100;

    private static final String DELIMITER = ";";
    HashMap<String,Boolean> mSelectedPackages = new HashMap<String,Boolean>();
    private boolean mFirstTime = true;
    private boolean mNewUser;

    private int mCustomRequestCode;
    private HashMap<Integer, AppRestrictionsPreference> mCustomRequestMap =
            new HashMap<Integer,AppRestrictionsPreference>();

    public static class Activity extends PreferenceActivity {
        @Override
        public Intent getIntent() {
            Intent modIntent = new Intent(super.getIntent());
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, AppRestrictionsFragment.class.getName());
            modIntent.putExtra(EXTRA_NO_HEADERS, true);
            return modIntent;
        }
    }

    static class AppRestrictionsPreference extends SwitchPreference {
        private boolean hasSettings;
        private OnClickListener listener;
        private ArrayList<RestrictionEntry> restrictions;
        boolean panelOpen;
        private boolean required;
        List<Preference> childPreferences = new ArrayList<Preference>();

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

        void setRequired(boolean required) {
            this.required = required;
        }

        boolean isRequired() {
            return required;
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
            widget.setEnabled(!isRequired());
            if (widget.getChildCount() > 0) {
                final Switch switchView = (Switch) widget.getChildAt(0);
                switchView.setEnabled(!isRequired());
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

        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        addPreferencesFromResource(R.xml.app_restrictions);
        mAppList = getPreferenceScreen();
        mUserPreference = (EditTextPreference) findPreference(KEY_USER_INFO);
        mUserPreference.setOnPreferenceChangeListener(this);
        setHasOptionsMenu(true);
    }

    void setUser(UserHandle user, boolean newUser) {
        mUser = user;
        mNewUser = newUser;
    }

    public void onResume() {
        super.onResume();
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

    private void addSystemApps(List<ApplicationInfo> visibleApps, Intent intent) {
        final PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> launchableApps = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo app : launchableApps) {
            if (app.activityInfo != null && app.activityInfo.applicationInfo != null) {
                int flags = app.activityInfo.applicationInfo.flags;
                if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    // System app
                    visibleApps.add(app.activityInfo.applicationInfo);
                }
            }
        }
    }

    private void populateApps() {
        mAppList.setOrderingAsAdded(false);
        List<ApplicationInfo> visibleApps = new ArrayList<ApplicationInfo>();
        // TODO: Do this asynchronously since it can be a long operation
        final Context context = getActivity();
        PackageManager pm = context.getPackageManager();

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
                visibleApps.add(app);
            }
        }
        Collections.sort(visibleApps, new AppLabelComparator(pm));

        for (int i = visibleApps.size() - 1; i > 1; i--) {
            ApplicationInfo appInfo = visibleApps.get(i);
            if (appInfo.packageName.equals(visibleApps.get(i-1).packageName)) {
                visibleApps.remove(i);
            }
        }
        Intent restrictionsIntent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(restrictionsIntent, 0);
        final List<ResolveInfo> existingApps = pm.queryIntentActivitiesAsUser(launcherIntent,
                0, mUser.getIdentifier());
        int i = 0;
        if (visibleApps.size() > 0) {
            for (ApplicationInfo app : visibleApps) {
                if (app.packageName == null) continue;
                String packageName = app.packageName;
                Drawable icon = app.loadIcon(pm);
                CharSequence label = app.loadLabel(pm);
                AppRestrictionsPreference p = new AppRestrictionsPreference(context, this);
                p.setIcon(icon);
                p.setTitle(label);
                p.setKey(PKG_PREFIX + packageName);
                p.setSettingsEnabled(hasPackage(receivers, packageName)
                        || packageName.equals(getActivity().getPackageName()));
                p.setPersistent(false);
                p.setOnPreferenceChangeListener(this);
                p.setOnPreferenceClickListener(this);
                try {
                    PackageInfo pi = pm.getPackageInfo(packageName, 0);
                    if (pi.requiredForAllUsers) {
                        p.setChecked(true);
                        p.setRequired(true);
                    } else if (!mNewUser && hasPackage(existingApps, packageName)) {
                        p.setChecked(true);
                    }
                } catch (NameNotFoundException re) {
                    // This would be bad
                }

                mAppList.addPreference(p);
                if (packageName.equals(getActivity().getPackageName())) {
                    p.setOrder(MAX_APP_RESTRICTIONS * 1);
                } else {
                    p.setOrder(MAX_APP_RESTRICTIONS * (i + 2));
                }
                mSelectedPackages.put(packageName, p.isChecked());
                i++;
            }
        }
    }

    private class AppLabelComparator implements Comparator<ApplicationInfo> {

        PackageManager pm;

        private AppLabelComparator(PackageManager pm) {
            this.pm = pm;
        }

        private CharSequence getLabel(ApplicationInfo info) {
            // TODO: Optimize this with a cache
            return info.loadLabel(pm);
        }

        @Override
        public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
            String lhsLabel = getLabel(lhs).toString();
            String rhsLabel = getLabel(rhs).toString();
            return lhsLabel.compareTo(rhsLabel);
        }
    }

    private boolean hasPackage(List<ResolveInfo> receivers, String packageName) {
        for (ResolveInfo info : receivers) {
            if (info.activityInfo.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof AppRestrictionsPreference) {
            AppRestrictionsPreference pref = (AppRestrictionsPreference) v.getTag();
            if (v.getId() == R.id.app_restrictions_settings) {
                toggleAppPanel(pref);
            } else if (!pref.isRequired()) {
                pref.setChecked(!pref.isChecked());
                mSelectedPackages.put(pref.getKey().substring(PKG_PREFIX.length()),
                        pref.isChecked());
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
            if (!arp.isRequired()) {
                arp.setChecked(!arp.isChecked());
                mSelectedPackages.put(arp.getKey().substring(PKG_PREFIX.length()), arp.isChecked());
            }
            return true;
        }
        return false;
    }
}
