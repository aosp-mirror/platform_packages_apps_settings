/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.AppItem;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.net.NetworkCycleDataForUid;
import com.android.settingslib.net.NetworkCycleDataForUidLoader;
import com.android.settingslib.net.UidDetail;
import com.android.settingslib.net.UidDetailProvider;

import java.util.ArrayList;
import java.util.List;

public class AppDataUsage extends DataUsageBaseFragment implements OnPreferenceChangeListener,
        DataSaverBackend.Listener {

    private static final String TAG = "AppDataUsage";

    static final String ARG_APP_ITEM = "app_item";
    static final String ARG_NETWORK_TEMPLATE = "network_template";
    static final String ARG_NETWORK_CYCLES = "network_cycles";
    static final String ARG_SELECTED_CYCLE = "selected_cycle";

    private static final String KEY_TOTAL_USAGE = "total_usage";
    private static final String KEY_FOREGROUND_USAGE = "foreground_usage";
    private static final String KEY_BACKGROUND_USAGE = "background_usage";
    private static final String KEY_APP_SETTINGS = "app_settings";
    private static final String KEY_RESTRICT_BACKGROUND = "restrict_background";
    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_CYCLE = "cycle";
    private static final String KEY_UNRESTRICTED_DATA = "unrestricted_data_saver";

    private static final int LOADER_APP_USAGE_DATA = 2;
    private static final int LOADER_APP_PREF = 3;

    private PackageManager mPackageManager;
    private final ArraySet<String> mPackages = new ArraySet<>();
    private Preference mTotalUsage;
    private Preference mForegroundUsage;
    private Preference mBackgroundUsage;
    private Preference mAppSettings;
    private RestrictedSwitchPreference mRestrictBackground;
    private PreferenceCategory mAppList;

    private Drawable mIcon;
    @VisibleForTesting
    CharSequence mLabel;
    @VisibleForTesting
    String mPackageName;
    private CycleAdapter mCycleAdapter;

    private List<NetworkCycleDataForUid> mUsageData;
    @VisibleForTesting
    NetworkTemplate mTemplate;
    private AppItem mAppItem;
    private Intent mAppSettingsIntent;
    private SpinnerPreference mCycle;
    private RestrictedSwitchPreference mUnrestrictedData;
    private DataSaverBackend mDataSaverBackend;
    private Context mContext;
    private ArrayList<Long> mCycles;
    private long mSelectedCycle;
    private boolean mIsLoading;

    public boolean isSimHardwareVisible(Context context) {
        return SubscriptionUtil.isSimHardwareVisible(context);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getContext();
        mPackageManager = getPackageManager();
        final Bundle args = getArguments();

        mAppItem = (args != null) ? (AppItem) args.getParcelable(ARG_APP_ITEM) : null;
        mTemplate = (args != null) ? (NetworkTemplate) args.getParcelable(ARG_NETWORK_TEMPLATE)
                : null;
        mCycles = (args != null) ? (ArrayList) args.getSerializable(ARG_NETWORK_CYCLES)
            : null;
        mSelectedCycle = (args != null) ? args.getLong(ARG_SELECTED_CYCLE) : 0L;

        if (mTemplate == null) {
            mTemplate = DataUsageUtils.getDefaultTemplate(mContext,
                    SubscriptionManager.getDefaultDataSubscriptionId());
        }
        if (mAppItem == null) {
            int uid = (args != null) ? args.getInt(AppInfoBase.ARG_PACKAGE_UID, -1)
                    : getActivity().getIntent().getIntExtra(AppInfoBase.ARG_PACKAGE_UID, -1);
            if (uid == -1) {
                // TODO: Log error.
                getActivity().finish();
            } else {
                addUid(uid);
                mAppItem = new AppItem(uid);
                mAppItem.addUid(uid);
            }
        } else {
            for (int i = 0; i < mAppItem.uids.size(); i++) {
                addUid(mAppItem.uids.keyAt(i));
            }
        }

        if (mAppItem.key > 0 && UserHandle.isApp(mAppItem.key)) {
            // In case we've been asked data usage for an app, automatically
            // include data usage of the corresponding SDK sandbox
            final int appSandboxUid = Process.toSdkSandboxUid(mAppItem.key);
            if (!mAppItem.uids.get(appSandboxUid)) {
                mAppItem.addUid(appSandboxUid);
            }
        }
        mTotalUsage = findPreference(KEY_TOTAL_USAGE);
        mForegroundUsage = findPreference(KEY_FOREGROUND_USAGE);
        mBackgroundUsage = findPreference(KEY_BACKGROUND_USAGE);

        initCycle();

        final UidDetailProvider uidDetailProvider = getUidDetailProvider();

        if (mAppItem.key > 0) {
            if ((!isSimHardwareVisible(mContext)) || !UserHandle.isApp(mAppItem.key)) {
                final UidDetail uidDetail = uidDetailProvider.getUidDetail(mAppItem.key, true);
                mIcon = uidDetail.icon;
                mLabel = uidDetail.label;
                removePreference(KEY_UNRESTRICTED_DATA);
                removePreference(KEY_RESTRICT_BACKGROUND);
            } else {
                if (mPackages.size() != 0) {
                    try {
                        final ApplicationInfo info = mPackageManager.getApplicationInfoAsUser(
                            mPackages.valueAt(0), 0, UserHandle.getUserId(mAppItem.key));
                        mIcon = IconDrawableFactory.newInstance(getActivity()).getBadgedIcon(info);
                        mLabel = info.loadLabel(mPackageManager);
                        mPackageName = info.packageName;
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                mRestrictBackground = findPreference(KEY_RESTRICT_BACKGROUND);
                mRestrictBackground.setOnPreferenceChangeListener(this);
                mUnrestrictedData = findPreference(KEY_UNRESTRICTED_DATA);
                mUnrestrictedData.setOnPreferenceChangeListener(this);
            }
            mDataSaverBackend = new DataSaverBackend(mContext);
            mAppSettings = findPreference(KEY_APP_SETTINGS);

            mAppSettingsIntent = new Intent(Intent.ACTION_MANAGE_NETWORK_USAGE);
            mAppSettingsIntent.addCategory(Intent.CATEGORY_DEFAULT);

            final PackageManager pm = getPackageManager();
            boolean matchFound = false;
            for (String packageName : mPackages) {
                mAppSettingsIntent.setPackage(packageName);
                if (pm.resolveActivity(mAppSettingsIntent, 0) != null) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                removePreference(KEY_APP_SETTINGS);
                mAppSettings = null;
            }

            if (mPackages.size() > 1) {
                mAppList = findPreference(KEY_APP_LIST);
                LoaderManager.getInstance(this).restartLoader(LOADER_APP_PREF, Bundle.EMPTY,
                        mAppPrefCallbacks);
            } else {
                removePreference(KEY_APP_LIST);
            }
        } else {
            final Context context = getActivity();
            final UidDetail uidDetail = uidDetailProvider.getUidDetail(mAppItem.key, true);
            mIcon = uidDetail.icon;
            mLabel = uidDetail.label;
            mPackageName = context.getPackageName();

            removePreference(KEY_UNRESTRICTED_DATA);
            removePreference(KEY_APP_SETTINGS);
            removePreference(KEY_RESTRICT_BACKGROUND);
            removePreference(KEY_APP_LIST);
        }

        addEntityHeader();
    }

    @Override
    public void onResume() {
        super.onResume();
        // No animations will occur before:
        //  - LOADER_APP_USAGE_DATA initially updates the cycle
        //  - updatePrefs() initially updates the preference visibility
        // This is mainly for the cycle spinner, because when the page is entered from the
        // AppInfoDashboardFragment, there is no way to know whether the cycle data is available
        // before finished the async loading.
        // The animator will be set back if any page updates happens after loading, in
        // setBackPreferenceListAnimatorIfLoaded().
        mIsLoading = true;
        getListView().setItemAnimator(null);
        if (mDataSaverBackend != null) {
            mDataSaverBackend.addListener(this);
        }
        LoaderManager.getInstance(this).restartLoader(LOADER_APP_USAGE_DATA, null /* args */,
                mUidDataCallbacks);
        updatePrefs();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mDataSaverBackend != null) {
            mDataSaverBackend.remListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRestrictBackground) {
            mDataSaverBackend.setIsDenylisted(mAppItem.key, mPackageName, !(Boolean) newValue);
            updatePrefs();
            return true;
        } else if (preference == mUnrestrictedData) {
            mDataSaverBackend.setIsAllowlisted(mAppItem.key, mPackageName, (Boolean) newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mAppSettings) {
            // TODO: target towards entire UID instead of just first package
            getActivity().startActivityAsUser(mAppSettingsIntent, new UserHandle(
                    UserHandle.getUserId(mAppItem.key)));
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_data_usage;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @VisibleForTesting
    void updatePrefs() {
        updatePrefs(getAppRestrictBackground(), getUnrestrictData());
    }

    @VisibleForTesting
    UidDetailProvider getUidDetailProvider() {
        return new UidDetailProvider(mContext);
    }

    private void initCycle() {
        mCycle = findPreference(KEY_CYCLE);
        mCycleAdapter = new CycleAdapter(mContext, mCycle, mCycleListener);
        if (mCycles != null) {
            // If coming from a page like DataUsageList where already has a selected cycle, display
            // that before loading to reduce flicker.
            mCycleAdapter.setInitialCycleList(mCycles, mSelectedCycle);
            mCycle.setHasCycles(true);
        }
    }

    /**
     * Sets back the preference list's animator if the loading is finished.
     *
     * The preference list's animator was temporarily removed before loading in onResume().
     * When need to update the preference visibility in this page after the loading, adding the
     * animator back to keeping the usual animations.
     */
    private void setBackPreferenceListAnimatorIfLoaded() {
        if (mIsLoading) {
            return;
        }
        RecyclerView recyclerView = getListView();
        if (recyclerView.getItemAnimator() == null) {
            recyclerView.setItemAnimator(new DefaultItemAnimator());
        }
    }

    private void updatePrefs(boolean restrictBackground, boolean unrestrictData) {
        if (!isSimHardwareVisible(mContext)) {
            return;
        }
        setBackPreferenceListAnimatorIfLoaded();
        final EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfMeteredDataRestricted(
                mContext, mPackageName, UserHandle.getUserId(mAppItem.key));
        if (mRestrictBackground != null) {
            mRestrictBackground.setChecked(!restrictBackground);
            mRestrictBackground.setDisabledByAdmin(admin);
        }
        if (mUnrestrictedData != null) {
            if (restrictBackground) {
                mUnrestrictedData.setVisible(false);
            } else {
                mUnrestrictedData.setVisible(true);
                mUnrestrictedData.setChecked(unrestrictData);
                mUnrestrictedData.setDisabledByAdmin(admin);
            }
        }
    }

    private void addUid(int uid) {
        if (Process.isSdkSandboxUid(uid)) {
            // For a sandbox process, get the associated app UID
            uid = Process.getAppUidForSdkSandboxUid(uid);
        }
        String[] packages = mPackageManager.getPackagesForUid(uid);
        if (packages != null) {
            for (int i = 0; i < packages.length; i++) {
                mPackages.add(packages[i]);
            }
        }
    }

    @VisibleForTesting
    void bindData(int position) {
        final long backgroundBytes, foregroundBytes;
        if (mUsageData == null || position >= mUsageData.size()) {
            backgroundBytes = foregroundBytes = 0;
            mCycle.setHasCycles(false);
        } else {
            mCycle.setHasCycles(true);
            final NetworkCycleDataForUid data = mUsageData.get(position);
            backgroundBytes = data.getBackgroudUsage();
            foregroundBytes = data.getForegroudUsage();
        }
        final long totalBytes = backgroundBytes + foregroundBytes;

        mTotalUsage.setSummary(DataUsageUtils.formatDataUsage(mContext, totalBytes));
        mForegroundUsage.setSummary(DataUsageUtils.formatDataUsage(mContext, foregroundBytes));
        mBackgroundUsage.setSummary(DataUsageUtils.formatDataUsage(mContext, backgroundBytes));
    }

    private boolean getAppRestrictBackground() {
        final int uid = mAppItem.key;
        final int uidPolicy = services.mPolicyManager.getUidPolicy(uid);
        return (uidPolicy & POLICY_REJECT_METERED_BACKGROUND) != 0;
    }

    private boolean getUnrestrictData() {
        if (mDataSaverBackend != null) {
            return mDataSaverBackend.isAllowlisted(mAppItem.key);
        }
        return false;
    }

    @VisibleForTesting
    void addEntityHeader() {
        String pkg = mPackages.size() != 0 ? mPackages.valueAt(0) : null;
        int uid = 0;
        if (pkg != null) {
            try {
                uid = mPackageManager.getPackageUidAsUser(pkg,
                        UserHandle.getUserId(mAppItem.key));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Skipping UID because cannot find package " + pkg);
            }
        }

        final boolean showInfoButton = mAppItem.key > 0;

        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this, null /* header */)
                .setRecyclerView(getListView(), getSettingsLifecycle())
                .setUid(uid)
                .setHasAppInfoLink(showInfoButton)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .setIcon(mIcon)
                .setLabel(mLabel)
                .setPackageName(pkg)
                .done(activity, getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APP_DATA_USAGE;
    }

    private AdapterView.OnItemSelectedListener mCycleListener =
            new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            bindData(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // ignored
        }
    };

    @VisibleForTesting
    final LoaderManager.LoaderCallbacks<List<NetworkCycleDataForUid>> mUidDataCallbacks =
        new LoaderManager.LoaderCallbacks<List<NetworkCycleDataForUid>>() {
            @Override
            public Loader<List<NetworkCycleDataForUid>> onCreateLoader(int id, Bundle args) {
                final NetworkCycleDataForUidLoader.Builder builder
                    = NetworkCycleDataForUidLoader.builder(mContext);
                builder.setRetrieveDetail(true)
                    .setNetworkTemplate(mTemplate);
                for (int i = 0; i < mAppItem.uids.size(); i++) {
                    builder.addUid(mAppItem.uids.keyAt(i));
                }
                if (mCycles != null) {
                    builder.setCycles(mCycles);
                }
                return builder.build();
            }

            @Override
            public void onLoadFinished(Loader<List<NetworkCycleDataForUid>> loader,
                    List<NetworkCycleDataForUid> data) {
                mUsageData = data;
                mCycleAdapter.updateCycleList(data);
                if (mSelectedCycle > 0L) {
                    final int numCycles = data.size();
                    int position = 0;
                    for (int i = 0; i < numCycles; i++) {
                        final NetworkCycleDataForUid cycleData = data.get(i);
                        if (cycleData.getEndTime() == mSelectedCycle) {
                            position = i;
                            break;
                        }
                    }
                    if (position > 0) {
                        mCycle.setSelection(position);
                    }
                    bindData(position);
                } else {
                    bindData(0 /* position */);
                }
                mIsLoading = false;
            }

            @Override
            public void onLoaderReset(Loader<List<NetworkCycleDataForUid>> loader) {
            }
        };

    private final LoaderManager.LoaderCallbacks<ArraySet<Preference>> mAppPrefCallbacks =
        new LoaderManager.LoaderCallbacks<ArraySet<Preference>>() {
            @Override
            public Loader<ArraySet<Preference>> onCreateLoader(int i, Bundle bundle) {
                return new AppPrefLoader(getPrefContext(), mPackages, getPackageManager());
            }

            @Override
            public void onLoadFinished(Loader<ArraySet<Preference>> loader,
                    ArraySet<Preference> preferences) {
                if (preferences != null && mAppList != null) {
                    for (Preference preference : preferences) {
                        mAppList.addPreference(preference);
                    }
                }
            }

            @Override
            public void onLoaderReset(Loader<ArraySet<Preference>> loader) {
            }
        };

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {

    }

    @Override
    public void onAllowlistStatusChanged(int uid, boolean isAllowlisted) {
        if (mAppItem.uids.get(uid, false)) {
            updatePrefs(getAppRestrictBackground(), isAllowlisted);
        }
    }

    @Override
    public void onDenylistStatusChanged(int uid, boolean isDenylisted) {
        if (mAppItem.uids.get(uid, false)) {
            updatePrefs(isDenylisted, getUnrestrictData());
        }
    }
}
