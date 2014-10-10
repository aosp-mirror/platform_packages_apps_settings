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

package com.android.settings;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.pm.UserInfo;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

public class HomeSettings extends SettingsPreferenceFragment implements Indexable {
    static final String TAG = "HomeSettings";

    // Boolean extra, indicates only launchers that support managed profiles should be shown.
    // Note: must match the constant defined in ManagedProvisioning
    private static final String EXTRA_SUPPORT_MANAGED_PROFILES = "support_managed_profiles";

    static final int REQUESTING_UNINSTALL = 10;

    public static final String HOME_PREFS = "home_prefs";
    public static final String HOME_PREFS_DO_SHOW = "do_show";

    public static final String HOME_SHOW_NOTICE = "show";

    private class HomePackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            buildHomeActivitiesList();
            Index.getInstance(context).updateFromClassNameResource(
                    HomeSettings.class.getName(), true, true);
        }
    }

    private PreferenceGroup mPrefGroup;
    private PackageManager mPm;
    private ComponentName[] mHomeComponentSet;
    private ArrayList<HomeAppPreference> mPrefs;
    private HomeAppPreference mCurrentHome = null;
    private final IntentFilter mHomeFilter;
    private boolean mShowNotice;
    private HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver();

    public HomeSettings() {
        mHomeFilter = new IntentFilter(Intent.ACTION_MAIN);
        mHomeFilter.addCategory(Intent.CATEGORY_HOME);
        mHomeFilter.addCategory(Intent.CATEGORY_DEFAULT);
    }

    OnClickListener mHomeClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer)v.getTag();
            HomeAppPreference pref = mPrefs.get(index);
            if (!pref.isChecked) {
                makeCurrentHome(pref);
            }
        }
    };

    OnClickListener mDeleteClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer)v.getTag();
            uninstallApp(mPrefs.get(index));
        }
    };

    void makeCurrentHome(HomeAppPreference newHome) {
        if (mCurrentHome != null) {
            mCurrentHome.setChecked(false);
        }
        newHome.setChecked(true);
        mCurrentHome = newHome;

        mPm.replacePreferredActivity(mHomeFilter, IntentFilter.MATCH_CATEGORY_EMPTY,
                mHomeComponentSet, newHome.activityName);

        getActivity().setResult(Activity.RESULT_OK);
    }

    void uninstallApp(HomeAppPreference pref) {
        // Uninstallation is done by asking the OS to do it
       Uri packageURI = Uri.parse("package:" + pref.uninstallTarget);
       Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
       uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, false);
       int requestCode = REQUESTING_UNINSTALL + (pref.isChecked ? 1 : 0);
       startActivityForResult(uninstallIntent, requestCode);
   }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Rebuild the list now that we might have nuked something
        buildHomeActivitiesList();

        // if the previous home app is now gone, fall back to the system one
        if (requestCode > REQUESTING_UNINSTALL) {
            // if mCurrentHome has gone null, it means we didn't find the previously-
            // default home app when rebuilding the list, i.e. it was the one we
            // just uninstalled.  When that happens we make the system-bundled
            // home app the active default.
            if (mCurrentHome == null) {
                for (int i = 0; i < mPrefs.size(); i++) {
                    HomeAppPreference pref = mPrefs.get(i);
                    if (pref.isSystem) {
                        makeCurrentHome(pref);
                        break;
                    }
                }
            }
        }

        // If we're down to just one possible home app, back out of this settings
        // fragment and show a dialog explaining to the user that they won't see
        // 'Home' settings now until such time as there are multiple available.
        if (mPrefs.size() < 2) {
            if (mShowNotice) {
                mShowNotice = false;
                SettingsActivity.requestHomeNotice();
            }
            finishFragment();
        }
    }

    private void buildHomeActivitiesList() {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        ComponentName currentDefaultHome  = mPm.getHomeActivities(homeActivities);

        Context context = getActivity();
        mCurrentHome = null;
        mPrefGroup.removeAll();
        mPrefs = new ArrayList<HomeAppPreference>();
        mHomeComponentSet = new ComponentName[homeActivities.size()];
        int prefIndex = 0;
        boolean supportManagedProfilesExtra =
                getActivity().getIntent().getBooleanExtra(EXTRA_SUPPORT_MANAGED_PROFILES, false);
        boolean mustSupportManagedProfile = hasManagedProfile()
                || supportManagedProfilesExtra;
        for (int i = 0; i < homeActivities.size(); i++) {
            final ResolveInfo candidate = homeActivities.get(i);
            final ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            mHomeComponentSet[i] = activityName;
            try {
                Drawable icon = info.loadIcon(mPm);
                CharSequence name = info.loadLabel(mPm);
                HomeAppPreference pref;

                if (mustSupportManagedProfile && !launcherHasManagedProfilesFeature(candidate)) {
                    pref = new HomeAppPreference(context, activityName, prefIndex,
                            icon, name, this, info, false /* not enabled */,
                            getResources().getString(R.string.home_work_profile_not_supported));
                } else  {
                    pref = new HomeAppPreference(context, activityName, prefIndex,
                            icon, name, this, info, true /* enabled */, null);
                }

                mPrefs.add(pref);
                mPrefGroup.addPreference(pref);
                if (activityName.equals(currentDefaultHome)) {
                    mCurrentHome = pref;
                }
                prefIndex++;
            } catch (Exception e) {
                Log.v(TAG, "Problem dealing with activity " + activityName, e);
            }
        }

        if (mCurrentHome != null) {
            if (mCurrentHome.isEnabled()) {
                getActivity().setResult(Activity.RESULT_OK);
            }

            new Handler().post(new Runnable() {
               public void run() {
                   mCurrentHome.setChecked(true);
               }
            });
        }
    }

    private boolean hasManagedProfile() {
        Context context = getActivity();
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        List<UserInfo> profiles = userManager.getProfiles(context.getUserId());
        for (UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) return true;
        }
        return false;
    }

    private boolean launcherHasManagedProfilesFeature(ResolveInfo resolveInfo) {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
                    resolveInfo.activityInfo.packageName, 0 /* default flags */);
            return versionNumberAtLeastL(appInfo.targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int versionNumber) {
        return versionNumber >= Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.home_selection);

        mPm = getPackageManager();
        mPrefGroup = (PreferenceGroup) findPreference("home");

        Bundle args = getArguments();
        mShowNotice = (args != null) && args.getBoolean(HOME_SHOW_NOTICE, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mHomePackageReceiver, filter);

        buildHomeActivitiesList();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mHomePackageReceiver);
    }

    private class HomeAppPreference extends Preference {
        ComponentName activityName;
        int index;
        HomeSettings fragment;
        final ColorFilter grayscaleFilter;
        boolean isChecked;

        boolean isSystem;
        String uninstallTarget;

        public HomeAppPreference(Context context, ComponentName activity,
                int i, Drawable icon, CharSequence title, HomeSettings parent, ActivityInfo info,
                boolean enabled, CharSequence summary) {
            super(context);
            setLayoutResource(R.layout.preference_home_app);
            setIcon(icon);
            setTitle(title);
            setEnabled(enabled);
            setSummary(summary);
            activityName = activity;
            fragment = parent;
            index = i;

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            float[] matrix = colorMatrix.getArray();
            matrix[18] = 0.5f;
            grayscaleFilter = new ColorMatrixColorFilter(colorMatrix);

            determineTargets(info);
        }

        // Check whether this activity is bundled on the system, with awareness
        // of the META_HOME_ALTERNATE mechanism.
        private void determineTargets(ActivityInfo info) {
            final Bundle meta = info.metaData;
            if (meta != null) {
                final String altHomePackage = meta.getString(ActivityManager.META_HOME_ALTERNATE);
                if (altHomePackage != null) {
                    try {
                        final int match = mPm.checkSignatures(info.packageName, altHomePackage);
                        if (match >= PackageManager.SIGNATURE_MATCH) {
                            PackageInfo altInfo = mPm.getPackageInfo(altHomePackage, 0);
                            final int altFlags = altInfo.applicationInfo.flags;
                            isSystem = (altFlags & ApplicationInfo.FLAG_SYSTEM) != 0;
                            uninstallTarget = altInfo.packageName;
                            return;
                        }
                    } catch (Exception e) {
                        // e.g. named alternate package not found during lookup
                        Log.w(TAG, "Unable to compare/resolve alternate", e);
                    }
                }
            }
            // No suitable metadata redirect, so use the package's own info
            isSystem = (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            uninstallTarget = info.packageName;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            RadioButton radio = (RadioButton) view.findViewById(R.id.home_radio);
            radio.setChecked(isChecked);

            Integer indexObj = new Integer(index);

            ImageView icon = (ImageView) view.findViewById(R.id.home_app_uninstall);
            if (isSystem) {
                icon.setEnabled(false);
                icon.setColorFilter(grayscaleFilter);
            } else {
                icon.setEnabled(true);
                icon.setOnClickListener(mDeleteClickListener);
                icon.setTag(indexObj);
            }

            View v = view.findViewById(R.id.home_app_pref);
            v.setTag(indexObj);

            v.setOnClickListener(mHomeClickListener);
        }

        void setChecked(boolean state) {
            if (state != isChecked) {
                isChecked = state;
                notifyChanged();
            }
        }
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                final PackageManager pm = context.getPackageManager();
                final ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
                pm.getHomeActivities(homeActivities);

                final SharedPreferences sp = context.getSharedPreferences(
                        HomeSettings.HOME_PREFS, Context.MODE_PRIVATE);
                final boolean doShowHome = sp.getBoolean(HomeSettings.HOME_PREFS_DO_SHOW, false);

                // We index Home Launchers only if there are more than one or if we are showing the
                // Home tile into the Dashboard
                if (homeActivities.size() > 1 || doShowHome) {
                    final Resources res = context.getResources();

                    // Add fragment title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.home_settings);
                    data.screenTitle = res.getString(R.string.home_settings);
                    data.keywords = res.getString(R.string.keywords_home);
                    result.add(data);

                    for (int i = 0; i < homeActivities.size(); i++) {
                        final ResolveInfo resolveInfo = homeActivities.get(i);
                        final ActivityInfo activityInfo = resolveInfo.activityInfo;

                        CharSequence name;
                        try {
                            name = activityInfo.loadLabel(pm);
                            if (TextUtils.isEmpty(name)) {
                                continue;
                            }
                        } catch (Exception e) {
                            Log.v(TAG, "Problem dealing with Home " + activityInfo.name, e);
                            continue;
                        }

                        data = new SearchIndexableRaw(context);
                        data.title = name.toString();
                        data.screenTitle = res.getString(R.string.home_settings);
                        result.add(data);
                    }
                }

                return result;
            }
        };
}
