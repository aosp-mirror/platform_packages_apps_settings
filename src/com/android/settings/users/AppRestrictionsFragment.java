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
import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.users.AppRestrictionsHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class AppRestrictionsFragment extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnClickListener, OnPreferenceClickListener,
        AppRestrictionsHelper.OnDisableUiForPackageListener {

    private static final String TAG = AppRestrictionsFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    private static final String PKG_PREFIX = "pkg_";

    protected PackageManager mPackageManager;
    protected UserManager mUserManager;
    protected IPackageManager mIPm;
    protected UserHandle mUser;
    private PackageInfo mSysPackageInfo;

    private AppRestrictionsHelper mHelper;

    private PreferenceGroup mAppList;

    private static final int MAX_APP_RESTRICTIONS = 100;

    private static final String DELIMITER = ";";

    /** Key for extra passed in from calling fragment for the userId of the user being edited */
    public static final String EXTRA_USER_ID = "user_id";

    /** Key for extra passed in from calling fragment to indicate if this is a newly created user */
    public static final String EXTRA_NEW_USER = "new_user";

    private boolean mFirstTime = true;
    private boolean mNewUser;
    private boolean mAppListChanged;
    protected boolean mRestrictedProfile;

    private static final int CUSTOM_REQUEST_CODE_START = 1000;
    private int mCustomRequestCode = CUSTOM_REQUEST_CODE_START;

    private HashMap<Integer, AppRestrictionsPreference> mCustomRequestMap = new HashMap<>();

    private AsyncTask mAppLoadingTask;

    private BroadcastReceiver mUserBackgrounding = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update the user's app selection right away without waiting for a pause
            // onPause() might come in too late, causing apps to disappear after broadcasts
            // have been scheduled during user startup.
            if (mAppListChanged) {
                if (DEBUG) Log.d(TAG, "User backgrounding, update app list");
                mHelper.applyUserAppsStates(AppRestrictionsFragment.this);
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

    static class AppRestrictionsPreference extends SwitchPreferenceCompat {
        private boolean hasSettings;
        private OnClickListener listener;
        private ArrayList<RestrictionEntry> restrictions;
        private boolean panelOpen;
        private boolean immutable;
        private List<Preference> mChildren = new ArrayList<>();

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

        ArrayList<RestrictionEntry> getRestrictions() {
            return restrictions;
        }

        boolean isPanelOpen() {
            return panelOpen;
        }

        void setPanelOpen(boolean open) {
            panelOpen = open;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);

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
                final CompoundButton toggle = (CompoundButton) widget.getChildAt(0);
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

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        init(icicle);
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

        mHelper = new AppRestrictionsHelper(getContext(), mUser);
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
        mAppList.setOrderingAsAdded(false);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USERS_APP_RESTRICTIONS;
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
            mAppLoadingTask = new AppLoadingTask().execute();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mNewUser = false;
        getActivity().unregisterReceiver(mUserBackgrounding);
        getActivity().unregisterReceiver(mPackageObserver);
        if (mAppListChanged) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    mHelper.applyUserAppsStates(AppRestrictionsFragment.this);
                    return null;
                }
            }.execute();
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

    @Override
    public void onDisableUiForPackage(String packageName) {
        AppRestrictionsPreference pref = (AppRestrictionsPreference) findPreference(
                getKeyForPackage(packageName));
        if (pref != null) {
            pref.setEnabled(false);
        }
    }

    private class AppLoadingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mHelper.fetchAndMergeApps();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            populateApps();
        }
    }

    private boolean isPlatformSigned(PackageInfo pi) {
        return (pi != null && pi.signatures != null &&
                    mSysPackageInfo.signatures[0].equals(pi.signatures[0]));
    }

    private boolean isAppEnabledForUser(PackageInfo pi) {
        if (pi == null) return false;
        final int flags = pi.applicationInfo.flags;
        final int privateFlags = pi.applicationInfo.privateFlags;
        // Return true if it is installed and not hidden
        return ((flags&ApplicationInfo.FLAG_INSTALLED) != 0
                && (privateFlags&ApplicationInfo.PRIVATE_FLAG_HIDDEN) == 0);
    }

    private void populateApps() {
        final Context context = getActivity();
        if (context == null) return;
        final PackageManager pm = mPackageManager;
        final IPackageManager ipm = mIPm;
        final int userId = mUser.getIdentifier();

        // Check if the user was removed in the meantime.
        if (Utils.getExistingUser(mUserManager, mUser) == null) {
            return;
        }
        mAppList.removeAll();
        Intent restrictionsIntent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(restrictionsIntent, 0);
        for (AppRestrictionsHelper.SelectableAppInfo app : mHelper.getVisibleApps()) {
            String packageName = app.packageName;
            if (packageName == null) continue;
            final boolean isSettingsApp = packageName.equals(context.getPackageName());
            AppRestrictionsPreference p = new AppRestrictionsPreference(getPrefContext(), this);
            final boolean hasSettings = resolveInfoListHasPackage(receivers, packageName);
            if (isSettingsApp) {
                addLocationAppRestrictionsPreference(app, p);
                // Settings app should be available to restricted user
                mHelper.setPackageSelected(packageName, true);
                continue;
            }
            PackageInfo pi = null;
            try {
                pi = ipm.getPackageInfo(packageName,
                        PackageManager.MATCH_ANY_USER
                        | PackageManager.GET_SIGNATURES, userId);
            } catch (RemoteException e) {
                // Ignore
            }
            if (pi == null) {
                continue;
            }
            if (mRestrictedProfile && isAppUnsupportedInRestrictedProfile(pi)) {
                continue;
            }
            p.setIcon(app.icon != null ? app.icon.mutate() : null);
            p.setChecked(false);
            p.setTitle(app.activityName);
            p.setKey(getKeyForPackage(packageName));
            p.setSettingsEnabled(hasSettings && app.primaryEntry == null);
            p.setPersistent(false);
            p.setOnPreferenceChangeListener(this);
            p.setOnPreferenceClickListener(this);
            p.setSummary(getPackageSummary(pi, app));
            if (pi.requiredForAllUsers || isPlatformSigned(pi)) {
                p.setChecked(true);
                p.setImmutable(true);
                // If the app is required and has no restrictions, skip showing it
                if (!hasSettings) continue;
                // Get and populate the defaults, since the user is not going to be
                // able to toggle this app ON (it's ON by default and immutable).
                // Only do this for restricted profiles, not single-user restrictions
                // Also don't do this for secondary icons
                if (app.primaryEntry == null) {
                    requestRestrictionsForApp(packageName, p, false);
                }
            } else if (!mNewUser && isAppEnabledForUser(pi)) {
                p.setChecked(true);
            }
            if (app.primaryEntry != null) {
                p.setImmutable(true);
                p.setChecked(mHelper.isPackageSelected(packageName));
            }
            p.setOrder(MAX_APP_RESTRICTIONS * (mAppList.getPreferenceCount() + 2));
            mHelper.setPackageSelected(packageName, p.isChecked());
            mAppList.addPreference(p);
        }
        mAppListChanged = true;
        // If this is the first time for a new profile, install/uninstall default apps for profile
        // to avoid taking the hit in onPause(), which can cause race conditions on user switch.
        if (mNewUser && mFirstTime) {
            mFirstTime = false;
            mHelper.applyUserAppsStates(this);
        }
    }

    private String getPackageSummary(PackageInfo pi, AppRestrictionsHelper.SelectableAppInfo app) {
        // Check for 3 cases:
        // - Secondary entry that can see primary user accounts
        // - Secondary entry that cannot see primary user accounts
        // - Primary entry that can see primary user accounts
        // Otherwise no summary is returned
        if (app.primaryEntry != null) {
            if (mRestrictedProfile && pi.restrictedAccountType != null) {
                return getString(R.string.app_sees_restricted_accounts_and_controlled_by,
                        app.primaryEntry.activityName);
            }
            return getString(R.string.user_restrictions_controlled_by,
                    app.primaryEntry.activityName);
        } else if (pi.restrictedAccountType != null) {
            return getString(R.string.app_sees_restricted_accounts);
        }
        return null;
    }

    private static boolean isAppUnsupportedInRestrictedProfile(PackageInfo pi) {
        return pi.requiredAccountType != null && pi.restrictedAccountType == null;
    }

    private void addLocationAppRestrictionsPreference(AppRestrictionsHelper.SelectableAppInfo app,
            AppRestrictionsPreference p) {
        String packageName = app.packageName;
        p.setIcon(R.drawable.ic_preference_location);
        p.setKey(getKeyForPackage(packageName));
        ArrayList<RestrictionEntry> restrictions = RestrictionUtils.getRestrictions(
                getActivity(), mUser);
        RestrictionEntry locationRestriction = restrictions.get(0);
        p.setTitle(locationRestriction.getTitle());
        p.setRestrictions(restrictions);
        p.setSummary(locationRestriction.getDescription());
        p.setChecked(locationRestriction.getSelectedState());
        p.setPersistent(false);
        p.setOnPreferenceClickListener(this);
        p.setOrder(MAX_APP_RESTRICTIONS);
        mAppList.addPreference(p);
    }

    private String getKeyForPackage(String packageName) {
        return PKG_PREFIX + packageName;
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
                // Settings/Location is handled as a top-level entry
                if (packageName.equals(getActivity().getPackageName())) {
                    pref.restrictions.get(0).setSelectedState(pref.isChecked());
                    RestrictionUtils.setRestrictions(getActivity(), pref.restrictions, mUser);
                    return;
                }
                mHelper.setPackageSelected(packageName, pref.isChecked());
                if (pref.isChecked() && pref.hasSettings
                        && pref.restrictions == null) {
                    // The restrictions have not been initialized, get and save them
                    requestRestrictionsForApp(packageName, pref, false);
                }
                mAppListChanged = true;
                // If it's not a restricted profile, apply the changes immediately
                if (!mRestrictedProfile) {
                    mHelper.applyUserAppState(packageName, pref.isChecked(), this);
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
                        mUserManager.setApplicationRestrictions(packageName,
                                RestrictionsManager.convertRestrictionsToBundle(restrictions),
                                mUser);
                        break;
                    }
                }
            }
            return true;
        }
        return false;
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
                requestRestrictionsForApp(packageName, preference, true /*invoke if custom*/);
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
            Intent restrictionsIntent = results.getParcelable(CUSTOM_RESTRICTIONS_INTENT);
            if (restrictions != null && restrictionsIntent == null) {
                onRestrictionsReceived(preference, restrictions);
                if (mRestrictedProfile) {
                    mUserManager.setApplicationRestrictions(packageName,
                            RestrictionsManager.convertRestrictionsToBundle(restrictions), mUser);
                }
            } else if (restrictionsIntent != null) {
                preference.setRestrictions(restrictions);
                if (invokeIfCustom && AppRestrictionsFragment.this.isResumed()) {
                    try {
                        assertSafeToStartCustomActivity(restrictionsIntent);
                    } catch (ActivityNotFoundException | SecurityException e) {
                        // return without startActivity
                        Log.e(TAG, "Cannot start restrictionsIntent " + e);
                        EventLog.writeEvent(0x534e4554, "200688991", -1 /* UID */, "");
                        return;
                    }

                    int requestCode = generateCustomActivityRequestCode(
                            RestrictionsResultReceiver.this.preference);
                    AppRestrictionsFragment.this.startActivityForResult(
                            new Intent(restrictionsIntent), requestCode);
                }
            }
        }

        private void assertSafeToStartCustomActivity(Intent intent) {
            EventLog.writeEvent(0x534e4554, "223578534", -1 /* UID */, "");
            ResolveInfo resolveInfo = mPackageManager.resolveActivity(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo == null) {
                throw new ActivityNotFoundException("No result for resolving " + intent);
            }
            // Prevent potential privilege escalation
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (!packageName.equals(activityInfo.packageName)) {
                throw new SecurityException("Application " + packageName
                        + " is not allowed to start activity " + intent);
            }
        }
    }

    private void onRestrictionsReceived(AppRestrictionsPreference preference,
            ArrayList<RestrictionEntry> restrictions) {
        // Remove any earlier restrictions
        removeRestrictionsForApp(preference);
        // Non-custom-activity case - expand the restrictions in-place
        int count = 1;
        for (RestrictionEntry entry : restrictions) {
            Preference p = null;
            switch (entry.getType()) {
            case RestrictionEntry.TYPE_BOOLEAN:
                p = new SwitchPreferenceCompat(getPrefContext());
                p.setTitle(entry.getTitle());
                p.setSummary(entry.getDescription());
                ((TwoStatePreference) p).setChecked(entry.getSelectedState());
                break;
            case RestrictionEntry.TYPE_CHOICE:
            case RestrictionEntry.TYPE_CHOICE_LEVEL:
                p = new ListPreference(getPrefContext());
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
                p = new MultiSelectListPreference(getPrefContext());
                p.setTitle(entry.getTitle());
                ((MultiSelectListPreference)p).setEntryValues(entry.getChoiceValues());
                ((MultiSelectListPreference)p).setEntries(entry.getChoiceEntries());
                HashSet<String> set = new HashSet<>();
                Collections.addAll(set, entry.getAllSelectedStrings());
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
                        RestrictionsManager.convertRestrictionsToBundle(list), mUser);
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
                mHelper.setPackageSelected(packageName, newEnabledState);
                updateAllEntries(arp.getKey(), newEnabledState);
                mAppListChanged = true;
                mHelper.applyUserAppState(packageName, newEnabledState, this);
            }
            return true;
        }
        return false;
    }

}
