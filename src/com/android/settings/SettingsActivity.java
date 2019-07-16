/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toolbar;

import com.android.internal.util.ArrayUtils;
import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.backup.BackupSettingsActivity;
import com.android.settings.core.FeatureFlags;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.gateway.SettingsGateway;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DeviceIndexFeatureProvider;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.SharedPreferencesLogger;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends SettingsDrawerActivity
        implements PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        ButtonBarHandler, FragmentManager.OnBackStackChangedListener {

    private static final String LOG_TAG = "SettingsActivity";

    // Constants for state save/restore
    private static final String SAVE_KEY_CATEGORIES = ":settings:categories";

    /**
     * When starting this activity, the invoking Intent can contain this extra
     * string to specify which fragment should be initially displayed.
     * <p/>Starting from Key Lime Pie, when this argument is passed in, the activity
     * will call isValidFragment() to confirm that the fragment class name is valid for this
     * activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * this extra can also be specified to supply a Bundle of arguments to pass
     * to that fragment when it is instantiated during the initial creation
     * of the activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";

    /**
     * Fragment "key" argument passed thru {@link #EXTRA_SHOW_FRAGMENT_ARGUMENTS}
     */
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";

    public static final String BACK_STACK_PREFS = ":settings:prefs";

    // extras that allow any preference activity to be launched as part of a wizard

    // show Back and Next buttons? takes boolean parameter
    // Back will then return RESULT_CANCELED and Next RESULT_OK
    protected static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";

    // add a Skip button?
    private static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";

    // specify custom text for the Back or Next buttons, or cause a button to not appear
    // at all by setting it to null
    protected static final String EXTRA_PREFS_SET_NEXT_TEXT = "extra_prefs_set_next_text";
    protected static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * those extra can also be specify to supply the title or title res id to be shown for
     * that fragment.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";
    /**
     * The package name used to resolve the title resource id.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME =
            ":settings:show_fragment_title_res_package_name";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RESID =
            ":settings:show_fragment_title_resid";
    public static final String EXTRA_SHOW_FRAGMENT_AS_SHORTCUT =
            ":settings:show_fragment_as_shortcut";

    public static final String EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting";

    @Deprecated
    public static final String EXTRA_HIDE_DRAWER = ":settings:hide_drawer";

    public static final String META_DATA_KEY_FRAGMENT_CLASS =
            "com.android.settings.FRAGMENT_CLASS";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private String mFragmentClass;

    private CharSequence mInitialTitle;
    private int mInitialTitleResId;

    private BroadcastReceiver mDevelopmentSettingsListener;

    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean batteryPresent = Utils.isBatteryPresent(intent);

                if (mBatteryPresent != batteryPresent) {
                    mBatteryPresent = batteryPresent;
                    updateTilesList();
                }
            }
        }
    };

    private SwitchBar mSwitchBar;

    private Button mNextButton;

    private boolean mIsShowingDashboard;

    private ViewGroup mContent;

    // Categories
    private ArrayList<DashboardCategory> mCategories = new ArrayList<>();

    private DashboardFeatureProvider mDashboardFeatureProvider;

    public SwitchBar getSwitchBar() {
        return mSwitchBar;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        new SubSettingLauncher(this)
                .setDestination(pref.getFragment())
                .setArguments(pref.getExtras())
                .setSourceMetricsCategory(caller instanceof Instrumentable
                        ? ((Instrumentable) caller).getMetricsCategory()
                        : Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setTitle(-1)
                .launch();
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (name.equals(getPackageName() + "_preferences")) {
            return new SharedPreferencesLogger(this, getMetricsTag(),
                    FeatureFactory.getFactory(this).getMetricsFeatureProvider());
        }
        return super.getSharedPreferences(name, mode);
    }

    private String getMetricsTag() {
        String tag = getClass().getName();
        if (getIntent() != null && getIntent().hasExtra(EXTRA_SHOW_FRAGMENT)) {
            tag = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        }
        if (tag.startsWith("com.android.settings.")) {
            tag = tag.replace("com.android.settings.", "");
        }
        return tag;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Log.d(LOG_TAG, "Starting onCreate");

        if (isLockTaskModePinned() && !isSettingsRunOnTop()) {
            Log.w(LOG_TAG, "Devices lock task mode pinned.");
            finish();
        }

        long startTime = System.currentTimeMillis();

        final FeatureFactory factory = FeatureFactory.getFactory(this);

        mDashboardFeatureProvider = factory.getDashboardFeatureProvider(this);

        // Should happen before any call to getIntent()
        getMetaData();

        final Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(intent.getIntExtra(EXTRA_UI_OPTIONS, 0));
        }

        // Getting Intent properties can only be done after the super.onCreate(...)
        final String initialFragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);

        final ComponentName cn = intent.getComponent();
        final String className = cn.getClassName();

        mIsShowingDashboard = className.equals(Settings.class.getName());

        // This is a "Sub Settings" when:
        // - this is a real SubSettings
        // - or :settings:show_fragment_as_subsetting is passed to the Intent
        final boolean isSubSettings = this instanceof SubSettings ||
                intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, false);

        // If this is a sub settings, then apply the SubSettings Theme for the ActionBar content
        // insets
        if (isSubSettings) {
            setTheme(R.style.Theme_SubSettings);
        }

        setContentView(mIsShowingDashboard ?
                R.layout.settings_main_dashboard : R.layout.settings_main_prefs);

        mContent = findViewById(R.id.main_content);

        getFragmentManager().addOnBackStackChangedListener(this);

        if (savedState != null) {
            // We are restarting from a previous saved state; used that to initialize, instead
            // of starting fresh.
            setTitleFromIntent(intent);

            ArrayList<DashboardCategory> categories =
                    savedState.getParcelableArrayList(SAVE_KEY_CATEGORIES);
            if (categories != null) {
                mCategories.clear();
                mCategories.addAll(categories);
                setTitleFromBackStack();
            }
        } else {
            launchSettingFragment(initialFragmentName, isSubSettings, intent);
        }

        final boolean deviceProvisioned = Utils.isDeviceProvisioned(this);
        if (mIsShowingDashboard) {
            findViewById(R.id.search_bar).setVisibility(
                    deviceProvisioned ? View.VISIBLE : View.INVISIBLE);
            findViewById(R.id.action_bar).setVisibility(View.GONE);
            final Toolbar toolbar = findViewById(R.id.search_action_bar);
            FeatureFactory.getFactory(this).getSearchFeatureProvider()
                    .initSearchToolbar(this, toolbar);
            setActionBar(toolbar);

            // Please forgive me for what I am about to do.
            //
            // Need to make the navigation icon non-clickable so that the entire card is clickable
            // and goes to the search UI. Also set the background to null so there's no ripple.
            View navView = toolbar.getNavigationView();
            navView.setClickable(false);
            navView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            navView.setBackground(null);
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(deviceProvisioned);
            actionBar.setHomeButtonEnabled(deviceProvisioned);
            actionBar.setDisplayShowTitleEnabled(!mIsShowingDashboard);
        }
        mSwitchBar = findViewById(R.id.switch_bar);
        if (mSwitchBar != null) {
            mSwitchBar.setMetricsTag(getMetricsTag());
        }

        // see if we should show Back/Next buttons
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {

            View buttonBar = findViewById(R.id.button_bar);
            if (buttonBar != null) {
                buttonBar.setVisibility(View.VISIBLE);

                Button backButton = (Button) findViewById(R.id.back_button);
                backButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED, null);
                        finish();
                    }
                });
                Button skipButton = (Button) findViewById(R.id.skip_button);
                skipButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK, null);
                        finish();
                    }
                });
                mNextButton = (Button) findViewById(R.id.next_button);
                mNextButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK, null);
                        finish();
                    }
                });

                // set our various button parameters
                if (intent.hasExtra(EXTRA_PREFS_SET_NEXT_TEXT)) {
                    String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_NEXT_TEXT);
                    if (TextUtils.isEmpty(buttonText)) {
                        mNextButton.setVisibility(View.GONE);
                    } else {
                        mNextButton.setText(buttonText);
                    }
                }
                if (intent.hasExtra(EXTRA_PREFS_SET_BACK_TEXT)) {
                    String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_BACK_TEXT);
                    if (TextUtils.isEmpty(buttonText)) {
                        backButton.setVisibility(View.GONE);
                    } else {
                        backButton.setText(buttonText);
                    }
                }
                if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_SKIP, false)) {
                    skipButton.setVisibility(View.VISIBLE);
                }
            }
        }

        if (DEBUG_TIMING) {
            Log.d(LOG_TAG, "onCreate took " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    @VisibleForTesting
    void launchSettingFragment(String initialFragmentName, boolean isSubSettings, Intent intent) {
        if (!mIsShowingDashboard && initialFragmentName != null) {
            setTitleFromIntent(intent);

            Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            switchToFragment(initialFragmentName, initialArguments, true, false,
                    mInitialTitleResId, mInitialTitle, false);
        } else {
            // Show search icon as up affordance if we are displaying the main Dashboard
            mInitialTitleResId = R.string.dashboard_title;

            switchToFragment(DashboardSummary.class.getName(), null /* args */, false, false,
                    mInitialTitleResId, mInitialTitle, false);
        }
    }

    private void setTitleFromIntent(Intent intent) {
        Log.d(LOG_TAG, "Starting to set activity title");
        final int initialTitleResId = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
        if (initialTitleResId > 0) {
            mInitialTitle = null;
            mInitialTitleResId = initialTitleResId;

            final String initialTitleResPackageName = intent.getStringExtra(
                    EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME);
            if (initialTitleResPackageName != null) {
                try {
                    Context authContext = createPackageContextAsUser(initialTitleResPackageName,
                            0 /* flags */, new UserHandle(UserHandle.myUserId()));
                    mInitialTitle = authContext.getResources().getText(mInitialTitleResId);
                    setTitle(mInitialTitle);
                    mInitialTitleResId = -1;
                    return;
                } catch (NameNotFoundException e) {
                    Log.w(LOG_TAG, "Could not find package" + initialTitleResPackageName);
                }
            } else {
                setTitle(mInitialTitleResId);
            }
        } else {
            mInitialTitleResId = -1;
            final String initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
            mInitialTitle = (initialTitle != null) ? initialTitle : getTitle();
            setTitle(mInitialTitle);
        }
        Log.d(LOG_TAG, "Done setting title");
    }

    @Override
    public void onBackStackChanged() {
        setTitleFromBackStack();
    }

    private void setTitleFromBackStack() {
        final int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            if (mInitialTitleResId > 0) {
                setTitle(mInitialTitleResId);
            } else {
                setTitle(mInitialTitle);
            }
            return;
        }

        FragmentManager.BackStackEntry bse = getFragmentManager().getBackStackEntryAt(count - 1);
        setTitleFromBackStackEntry(bse);
    }

    private void setTitleFromBackStackEntry(FragmentManager.BackStackEntry bse) {
        final CharSequence title;
        final int titleRes = bse.getBreadCrumbTitleRes();
        if (titleRes > 0) {
            title = getText(titleRes);
        } else {
            title = bse.getBreadCrumbTitle();
        }
        if (title != null) {
            setTitle(title);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    /**
     * For testing purposes to avoid crashes from final variables in Activity's onSaveInstantState.
     */
    @VisibleForTesting
    void saveState(Bundle outState) {
        if (mCategories.size() > 0) {
            outState.putParcelableArrayList(SAVE_KEY_CATEGORIES, mCategories);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDevelopmentSettingsListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateTilesList();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mDevelopmentSettingsListener,
                new IntentFilter(DevelopmentSettingsEnabler.DEVELOPMENT_SETTINGS_CHANGED_ACTION));

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        updateTilesList();
        updateDeviceIndex();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDevelopmentSettingsListener);
        mDevelopmentSettingsListener = null;
        unregisterReceiver(mBatteryInfoReceiver);
    }

    @Override
    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        final Bitmap icon = getBitmapFromXmlResource(R.drawable.ic_launcher_settings);
        taskDescription.setIcon(icon);
        super.setTaskDescription(taskDescription);
    }

    protected boolean isValidFragment(String fragmentName) {
        // Almost all fragments are wrapped in this,
        // except for a few that have their own activities.
        for (int i = 0; i < SettingsGateway.ENTRY_FRAGMENTS.length; i++) {
            if (SettingsGateway.ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
        }
        return false;
    }

    @Override
    public Intent getIntent() {
        Intent superIntent = super.getIntent();
        String startingFragment = getStartingFragmentClass(superIntent);
        // This is called from super.onCreate, isMultiPane() is not yet reliable
        // Do not use onIsHidingHeaders either, which relies itself on this method
        if (startingFragment != null) {
            Intent modIntent = new Intent(superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, startingFragment);
            Bundle args = superIntent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            if (args != null) {
                args = new Bundle(args);
            } else {
                args = new Bundle();
            }
            args.putParcelable("intent", superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
            return modIntent;
        }
        return superIntent;
    }

    /**
     * Checks if the component name in the intent is different from the Settings class and
     * returns the class name to load as a fragment.
     */
    private String getStartingFragmentClass(Intent intent) {
        if (mFragmentClass != null) return mFragmentClass;

        String intentClass = intent.getComponent().getClassName();
        if (intentClass.equals(getClass().getName())) return null;

        if ("com.android.settings.RunningServices".equals(intentClass)
                || "com.android.settings.applications.StorageUse".equals(intentClass)) {
            // Old names of manage apps.
            intentClass = ManageApplications.class.getName();
        }

        return intentClass;
    }

    /**
     * Called by a preference panel fragment to finish itself.
     *
     * @param resultCode Optional result code to send back to the original
     *                   launching fragment.
     * @param resultData Optional result data to send back to the original
     *                   launching fragment.
     */
    public void finishPreferencePanel(int resultCode, Intent resultData) {
        setResult(resultCode, resultData);
        finish();
    }

    /**
     * Switch to a specific Fragment with taking care of validation, Title and BackStack
     */
    private Fragment switchToFragment(String fragmentName, Bundle args, boolean validate,
            boolean addToBackStack, int titleResId, CharSequence title, boolean withTransition) {
        Log.d(LOG_TAG, "Switching to fragment " + fragmentName);
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, f);
        if (withTransition) {
            TransitionManager.beginDelayedTransition(mContent);
        }
        if (addToBackStack) {
            transaction.addToBackStack(SettingsActivity.BACK_STACK_PREFS);
        }
        if (titleResId > 0) {
            transaction.setBreadCrumbTitle(titleResId);
        } else if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        Log.d(LOG_TAG, "Executed frag manager pendingTransactions");
        return f;
    }

    private void updateTilesList() {
        // Generally the items that are will be changing from these updates will
        // not be in the top list of tiles, so run it in the background and the
        // SettingsDrawerActivity will pick up on the updates automatically.
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                doUpdateTilesList();
            }
        });
    }

    private void updateDeviceIndex() {
        DeviceIndexFeatureProvider indexProvider = FeatureFactory.getFactory(
                this).getDeviceIndexFeatureProvider();

        ThreadUtils.postOnBackgroundThread(
                () -> indexProvider.updateIndex(SettingsActivity.this, false /* force */));
    }

    private void doUpdateTilesList() {
        PackageManager pm = getPackageManager();
        final UserManager um = UserManager.get(this);
        final boolean isAdmin = um.isAdminUser();
        final FeatureFactory featureFactory = FeatureFactory.getFactory(this);
        boolean somethingChanged = false;
        final String packageName = getPackageName();
        final StringBuilder changedList = new StringBuilder();
        somethingChanged = setTileEnabled(changedList,
                new ComponentName(packageName, WifiSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_WIFI), isAdmin) || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.BluetoothSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH), isAdmin)
                || somethingChanged;


        // Enable DataUsageSummaryActivity if the data plan feature flag is turned on otherwise
        // enable DataPlanUsageSummaryActivity.
        somethingChanged = setTileEnabled(changedList,
                new ComponentName(packageName, Settings.DataUsageSummaryActivity.class.getName()),
                Utils.isBandwidthControlEnabled() /* enabled */,
                isAdmin) || somethingChanged;

        somethingChanged = setTileEnabled(changedList,
                new ComponentName(packageName,
                        Settings.ConnectedDeviceDashboardActivity.class.getName()),
                !UserManager.isDeviceInDemoMode(this) /* enabled */,
                isAdmin) || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.SimSettingsActivity.class.getName()),
                Utils.showSimCardTile(this), isAdmin)
                || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.PowerUsageSummaryActivity.class.getName()),
                mBatteryPresent, isAdmin) || somethingChanged;

        final boolean isDataUsageSettingsV2Enabled =
                FeatureFlagUtils.isEnabled(this, FeatureFlags.DATA_USAGE_SETTINGS_V2);
        // Enable new data usage page if v2 enabled
        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.DataUsageSummaryActivity.class.getName()),
                Utils.isBandwidthControlEnabled() && isDataUsageSettingsV2Enabled, isAdmin)
                || somethingChanged;
        // Enable legacy data usage page if v2 disabled
        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.DataUsageSummaryLegacyActivity.class.getName()),
                Utils.isBandwidthControlEnabled() && !isDataUsageSettingsV2Enabled, isAdmin)
                || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.UserSettingsActivity.class.getName()),
                UserHandle.MU_ENABLED && UserManager.supportsMultipleUsers()
                        && !Utils.isMonkeyRunning(), isAdmin)
                || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.NetworkDashboardActivity.class.getName()),
                !UserManager.isDeviceInDemoMode(this), isAdmin)
                || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.DateTimeSettingsActivity.class.getName()),
                !UserManager.isDeviceInDemoMode(this), isAdmin)
                || somethingChanged;

        final boolean showDev = DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this)
                && !Utils.isMonkeyRunning();
        final boolean isAdminOrDemo = um.isAdminUser() || um.isDemoUser();
        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.DevelopmentSettingsDashboardActivity.class.getName()),
                showDev, isAdminOrDemo)
                || somethingChanged;

        // Enable/disable backup settings depending on whether the user is admin.
        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                BackupSettingsActivity.class.getName()), true, isAdmin)
                || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.WifiDisplaySettingsActivity.class.getName()),
                WifiDisplaySettings.isAvailable(this), isAdmin)
                || somethingChanged;

        // Enable/disable the Me Card page.
        final boolean aboutPhoneV2Enabled = featureFactory
                .getAccountFeatureProvider()
                .isAboutPhoneV2Enabled(this);
        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.MyDeviceInfoActivity.class.getName()),
                aboutPhoneV2Enabled, isAdmin)
                || somethingChanged;
        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.DeviceInfoSettingsActivity.class.getName()),
                !aboutPhoneV2Enabled, isAdmin)
                || somethingChanged;

        if (UserHandle.MU_ENABLED && !isAdmin) {

            // When on restricted users, disable all extra categories (but only the settings ones).
            final List<DashboardCategory> categories = mDashboardFeatureProvider.getAllCategories();
            synchronized (categories) {
                for (DashboardCategory category : categories) {
                    final int tileCount = category.getTilesCount();
                    for (int i = 0; i < tileCount; i++) {
                        final ComponentName component = category.getTile(i).intent.getComponent();
                        final String name = component.getClassName();
                        final boolean isEnabledForRestricted = ArrayUtils.contains(
                                SettingsGateway.SETTINGS_FOR_RESTRICTED, name) || (isAdminOrDemo
                                && Settings.DevelopmentSettingsDashboardActivity.class.getName()
                                .equals(name));
                        if (packageName.equals(component.getPackageName())
                                && !isEnabledForRestricted) {
                            somethingChanged =
                                    setTileEnabled(changedList, component, false, isAdmin)
                                            || somethingChanged;
                        }
                    }
                }
            }
        }

        // Final step, refresh categories.
        if (somethingChanged) {
            Log.d(LOG_TAG, "Enabled state changed for some tiles, reloading all categories "
                    + changedList.toString());
            updateCategories();
        } else {
            Log.d(LOG_TAG, "No enabled state changed, skipping updateCategory call");
        }
    }

    /**
     * @return whether or not the enabled state actually changed.
     */
    private boolean setTileEnabled(StringBuilder changedList, ComponentName component,
            boolean enabled, boolean isAdmin) {
        if (UserHandle.MU_ENABLED && !isAdmin && getPackageName().equals(component.getPackageName())
                && !ArrayUtils.contains(SettingsGateway.SETTINGS_FOR_RESTRICTED,
                component.getClassName())) {
            enabled = false;
        }
        boolean changed = setTileEnabled(component, enabled);
        if (changed) {
            changedList.append(component.toShortString()).append(",");
        }
        return changed;
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mFragmentClass = ai.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);
        } catch (NameNotFoundException nnfe) {
            // No recovery
            Log.d(LOG_TAG, "Cannot get Metadata for: " + getComponentName().toString());
        }
    }

    // give subclasses access to the Next button
    public boolean hasNextButton() {
        return mNextButton != null;
    }

    public Button getNextButton() {
        return mNextButton;
    }

    @VisibleForTesting
    Bitmap getBitmapFromXmlResource(int drawableRes) {
        Drawable drawable = getResources().getDrawable(drawableRes, getTheme());
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private boolean isLockTaskModePinned() {
        final ActivityManager activityManager =
            getApplicationContext().getSystemService(ActivityManager.class);
        return activityManager.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_PINNED;
    }

    private boolean isSettingsRunOnTop() {
        final ActivityManager activityManager =
            getApplicationContext().getSystemService(ActivityManager.class);
        final String taskPkgName = activityManager.getRunningTasks(1 /* maxNum */)
            .get(0 /* index */).baseActivity.getPackageName();
        return TextUtils.equals(getPackageName(), taskPkgName);
    }
}
