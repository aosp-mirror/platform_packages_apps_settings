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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RadioButton;

public class HomeSettings extends SettingsPreferenceFragment {
    static final String TAG = "HomeSettings";

    static final int REQUESTING_UNINSTALL = 10;

    public static final String CURRENT_HOME = "current_home";

    PreferenceGroup mPrefGroup;

    PackageManager mPm;
    ComponentName[] mHomeComponentSet;
    ArrayList<HomeAppPreference> mPrefs;
    HomeAppPreference mCurrentHome = null;
    final IntentFilter mHomeFilter;

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
    }

    void uninstallApp(HomeAppPreference pref) {
        // Uninstallation is done by asking the OS to do it
       Uri packageURI = Uri.parse("package:" + pref.activityName.getPackageName());
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
    }

    void buildHomeActivitiesList() {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        ComponentName currentDefaultHome  = mPm.getHomeActivities(homeActivities);

        Context context = getActivity();
        mCurrentHome = null;
        mPrefGroup.removeAll();
        mPrefs = new ArrayList<HomeAppPreference>();
        mHomeComponentSet = new ComponentName[homeActivities.size()];
        int prefIndex = 0;
        for (int i = 0; i < homeActivities.size(); i++) {
            final ResolveInfo candidate = homeActivities.get(i);
            final ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            mHomeComponentSet[i] = activityName;
            try {
                Drawable icon = info.loadIcon(mPm);
                CharSequence name = info.loadLabel(mPm);
                boolean isSystem = (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                HomeAppPreference pref = new HomeAppPreference(context, activityName, prefIndex,
                        icon, name, this, isSystem);
                mPrefs.add(pref);
                mPrefGroup.addPreference(pref);
                pref.setEnabled(true);
                if (activityName.equals(currentDefaultHome)) {
                    mCurrentHome = pref;
                }
                prefIndex++;
            } catch (Exception e) {
                Log.v(TAG, "Problem dealing with activity " + activityName, e);
            }
        }

        if (mCurrentHome != null) {
            new Handler().post(new Runnable() {
               public void run() {
                   mCurrentHome.setChecked(true);
               }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.home_selection);

        mPm = getPackageManager();
        mPrefGroup = (PreferenceGroup) findPreference("home");
    }

    @Override
    public void onResume() {
        super.onResume();
        buildHomeActivitiesList();
    }

    class HomeAppPreference extends Preference {
        ComponentName activityName;
        int index;
        boolean isSystem;
        HomeSettings fragment;
        final ColorFilter grayscaleFilter;
        boolean isChecked;

        public HomeAppPreference(Context context, ComponentName activity,
                int i, Drawable icon, CharSequence title,
                HomeSettings parent, boolean sys) {
            super(context);
            setLayoutResource(R.layout.preference_home_app);
            setIcon(icon);
            setTitle(title);
            activityName = activity;
            fragment = parent;
            index = i;
            isSystem = sys;

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            float[] matrix = colorMatrix.getArray();
            matrix[18] = 0.5f;
            grayscaleFilter = new ColorMatrixColorFilter(colorMatrix);
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
                icon.setOnClickListener(mDeleteClickListener);
                icon.setTag(indexObj);
            }

            View v = view.findViewById(R.id.home_app_pref);
            v.setOnClickListener(mHomeClickListener);
            v.setTag(indexObj);
        }

        void setChecked(boolean state) {
            if (state != isChecked) {
                isChecked = state;
                notifyChanged();
            }
        }
    }
}
