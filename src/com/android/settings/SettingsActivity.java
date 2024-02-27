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

import static com.android.settings.activityembedding.EmbeddedDeepLinkUtils.tryStartMultiPaneDeepLink;
import static com.android.settings.applications.appinfo.AppButtonsPreferenceController.KEY_REMOVE_TASK_WHEN_FINISHING;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.internal.util.ArrayUtils;
import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.OnActivityResultListener;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.gateway.SettingsGateway;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.homepage.TopLevelSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.PasswordUtils;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.SharedPreferencesLogger;
import com.android.settingslib.drawer.DashboardCategory;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;


public class SettingsActivity extends SettingsBaseActivity
        implements PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
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

    public static final String EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting";
    public static final String EXTRA_IS_SECOND_LAYER_PAGE = ":settings:is_second_layer_page";

    /**
     * Additional extra of Settings#ACTION_SETTINGS_LARGE_SCREEN_DEEP_LINK.
     * Set true when the deep link intent is from a slice
     */
    public static final String EXTRA_IS_FROM_SLICE = "is_from_slice";

    public static final String EXTRA_USER_HANDLE = "user_handle";
    public static final String EXTRA_INITIAL_CALLING_PACKAGE = "initial_calling_package";

    /**
     * Personal or Work profile tab of {@link ProfileSelectFragment}
     * <p>0: Personal tab.
     * <p>1: Work profile tab.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TAB =
            ":settings:show_fragment_tab";

    public static final String META_DATA_KEY_FRAGMENT_CLASS =
            "com.android.settings.FRAGMENT_CLASS";

    public static final String META_DATA_KEY_HIGHLIGHT_MENU_KEY =
            "com.android.settings.HIGHLIGHT_MENU_KEY";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private String mFragmentClass;
    private String mHighlightMenuKey;

    private CharSequence mInitialTitle;
    private int mInitialTitleResId;

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

    private SettingsMainSwitchBar mMainSwitch;

    private Button mNextButton;

    // Categories
    private ArrayList<DashboardCategory> mCategories = new ArrayList<>();

    private DashboardFeatureProvider mDashboardFeatureProvider;

    public SettingsMainSwitchBar getSwitchBar() {
        return mMainSwitch;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        new SubSettingLauncher(this)
                .setDestination(pref.getFragment())
                .setArguments(pref.getExtras())
                .setSourceMetricsCategory(caller instanceof Instrumentable
                        ? ((Instrumentable) caller).getMetricsCategory()
                        : Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setTitleRes(-1)
                .launch();
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (!TextUtils.equals(name, getPackageName() + "_preferences")) {
            return super.getSharedPreferences(name, mode);
        }

        String tag = getMetricsTag();

        return new SharedPreferencesLogger(this, tag,
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider(),
                lookupMetricsCategory());
    }

    private int lookupMetricsCategory() {
        int category = SettingsEnums.PAGE_UNKNOWN;
        Bundle args = null;
        if (getIntent() != null) {
            args = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        }

        Fragment fragment = Utils.getTargetFragment(this, getMetricsTag(), args);

        if (fragment instanceof Instrumentable) {
            category = ((Instrumentable) fragment).getMetricsCategory();
        }
        Log.d(LOG_TAG, "MetricsCategory is " + category);

        return category;
    }

    private String getMetricsTag() {
        String tag = null;
        if (getIntent() != null && getIntent().hasExtra(EXTRA_SHOW_FRAGMENT)) {
            tag = getInitialFragmentName(getIntent());
        }

        if (TextUtils.isEmpty(tag)) {
            Log.w(LOG_TAG, "MetricsTag is invalid " + tag);
            tag = getClass().getName();
        }
        return tag;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        // Should happen before any call to getIntent()
        getMetaData();
        final Intent intent = getIntent();

        if (shouldShowMultiPaneDeepLink(intent)
                && tryStartMultiPaneDeepLink(this, intent, mHighlightMenuKey)) {
            finish();
            super.onCreate(savedState);
            return;
        }

        super.onCreate(savedState);
        Log.d(LOG_TAG, "Starting onCreate");
        createUiFromIntent(savedState, intent);
    }

    protected void createUiFromIntent(Bundle savedState, Intent intent) {
        long startTime = System.currentTimeMillis();

        final FeatureFactory factory = FeatureFactory.getFeatureFactory();
        mDashboardFeatureProvider = factory.getDashboardFeatureProvider();

        if (intent.hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(intent.getIntExtra(EXTRA_UI_OPTIONS, 0));
        }

        // Getting Intent properties can only be done after the super.onCreate(...)
        final String initialFragmentName = getInitialFragmentName(intent);

        // If this is a sub settings, then apply the SubSettings Theme for the ActionBar content
        // insets.
        // If this is in setup flow, don't apply theme. Because light theme needs to be applied
        // in SettingsBaseActivity#onCreate().
        if (isSubSettings(intent) && !WizardManagerHelper.isAnySetupWizard(getIntent())) {
            setTheme(R.style.Theme_SubSettings);
        }

        setContentView(R.layout.settings_main_prefs);

        getSupportFragmentManager().addOnBackStackChangedListener(this);

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
            launchSettingFragment(initialFragmentName, intent);
        }

        mMainSwitch = findViewById(R.id.switch_bar);
        if (mMainSwitch != null) {
            mMainSwitch.setMetricsCategory(lookupMetricsCategory());
            mMainSwitch.setTranslationZ(findViewById(R.id.main_content).getTranslationZ() + 1);
        }

        // see if we should show Back/Next buttons
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {

            View buttonBar = findViewById(R.id.button_bar);
            if (buttonBar != null) {
                buttonBar.setVisibility(View.VISIBLE);

                Button backButton = findViewById(R.id.back_button);
                backButton.setOnClickListener(v -> {
                    setResult(RESULT_CANCELED, null);
                    finish();
                });
                Button skipButton = findViewById(R.id.skip_button);
                skipButton.setOnClickListener(v -> {
                    setResult(RESULT_OK, null);
                    finish();
                });
                mNextButton = findViewById(R.id.next_button);
                mNextButton.setOnClickListener(v -> {
                    setResult(RESULT_OK, null);
                    finish();
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

    private void setActionBarStatus() {
        final boolean isActionBarButtonEnabled = isActionBarButtonEnabled(getIntent());

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(isActionBarButtonEnabled);
            actionBar.setHomeButtonEnabled(isActionBarButtonEnabled);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    private boolean isActionBarButtonEnabled(Intent intent) {
        if (WizardManagerHelper.isAnySetupWizard(intent)) {
            return false;
        }
        final boolean isSecondLayerPage =
                intent.getBooleanExtra(EXTRA_IS_SECOND_LAYER_PAGE, false);

        // TODO: move Settings's ActivityEmbeddingUtils to SettingsLib.
        return !com.android.settingslib.activityembedding.ActivityEmbeddingUtils
                        .shouldHideNavigateUpButton(this, isSecondLayerPage);
    }

    private boolean isSubSettings(Intent intent) {
        return this instanceof SubSettings ||
            intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, false);
    }

    private boolean shouldShowMultiPaneDeepLink(Intent intent) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this)) {
            return false;
        }

        // If the activity is task root, starting trampoline is needed in order to show two-pane UI.
        // If FLAG_ACTIVITY_NEW_TASK is set, the activity will become the start of a new task on
        // this history stack, so starting trampoline is needed in order to notify the homepage that
        // the highlight key is changed.
        if (!isTaskRoot() && (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
            return false;
        }

        // Only starts trampoline for deep links. Should return false for all the cases that
        // Settings app starts SettingsActivity or SubSetting by itself.
        if (intent.getAction() == null) {
            // Other apps should send deep link intent which matches intent filter of the Activity.
            return false;
        }

        // If the activity's launch mode is "singleInstance", it can't be embedded in Settings since
        // it will be created in a new task.
        ActivityInfo info = intent.resolveActivityInfo(getPackageManager(),
                PackageManager.MATCH_DEFAULT_ONLY);
        if (info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            Log.w(LOG_TAG, "launchMode: singleInstance");
            return false;
        }

        if (intent.getBooleanExtra(EXTRA_IS_FROM_SLICE, false)) {
            // Slice deep link starts the Intent using SubSettingLauncher. Returns true to show
            // 2-pane deep link.
            return true;
        }

        if (isSubSettings(intent)) {
            return false;
        }

        if (intent.getBooleanExtra(SettingsHomepageActivity.EXTRA_IS_FROM_SETTINGS_HOMEPAGE,
                /* defaultValue */ false)) {
            return false;
        }

        if (TextUtils.equals(intent.getAction(), Intent.ACTION_CREATE_SHORTCUT)) {
            // Returns false to show full screen for Intent.ACTION_CREATE_SHORTCUT because
            // - Launcher startActivityForResult for Intent.ACTION_CREATE_SHORTCUT and activity
            //   stack starts from launcher, CreateShortcutActivity will not follows SplitPaitRule
            //   registered by Settings.
            // - There is no CreateShortcutActivity entry point from Settings app UI.
            return false;
        }

        return true;
    }

    /** Returns the initial calling package name that launches the activity. */
    public String getInitialCallingPackage() {
        String callingPackage = PasswordUtils.getCallingAppPackageName(getActivityToken());
        if (!TextUtils.equals(callingPackage, getPackageName())) {
            return callingPackage;
        }

        String initialCallingPackage = getIntent().getStringExtra(EXTRA_INITIAL_CALLING_PACKAGE);
        return TextUtils.isEmpty(initialCallingPackage) ? callingPackage : initialCallingPackage;
    }

    /** Returns the initial fragment name that the activity will launch. */
    @VisibleForTesting
    public String getInitialFragmentName(Intent intent) {
        return intent.getStringExtra(EXTRA_SHOW_FRAGMENT);
    }

    @Override
    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof OnActivityResultListener) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    @VisibleForTesting
    void launchSettingFragment(String initialFragmentName, Intent intent) {
        if (initialFragmentName != null) {
            if (SettingsActivityUtil.launchSpaActivity(this, initialFragmentName, intent)) {
                finish();
                return;
            }

            setTitleFromIntent(intent);

            Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            switchToFragment(initialFragmentName, initialArguments, true,
                    mInitialTitleResId, mInitialTitle);
        } else {
            // Show search icon as up affordance if we are displaying the main Dashboard
            mInitialTitleResId = R.string.dashboard_title;
            switchToFragment(TopLevelSettings.class.getName(), null /* args */, false,
                    mInitialTitleResId, mInitialTitle);
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
                } catch (Resources.NotFoundException resourceNotFound) {
                    Log.w(LOG_TAG,
                            "Could not find title resource in " + initialTitleResPackageName);
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
        final int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            if (mInitialTitleResId > 0) {
                setTitle(mInitialTitleResId);
            } else {
                setTitle(mInitialTitle);
            }
            return;
        }

        FragmentManager.BackStackEntry bse = getSupportFragmentManager().
                getBackStackEntryAt(count - 1);
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
        setActionBarStatus();

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        updateTilesList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBatteryInfoReceiver);
    }

    @Override
    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        taskDescription.setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_settings));
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
        if (resultData != null &&
                resultData.getBooleanExtra(KEY_REMOVE_TASK_WHEN_FINISHING, false)) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    /**
     * Switch to a specific Fragment with taking care of validation, Title and BackStack
     */
    private void switchToFragment(String fragmentName, Bundle args, boolean validate,
            int titleResId, CharSequence title) {
        Log.d(LOG_TAG, "Switching to fragment " + fragmentName);
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }
        Fragment f = Utils.getTargetFragment(this, fragmentName, args);
        if (f == null) {
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, f);
        if (titleResId > 0) {
            transaction.setBreadCrumbTitle(titleResId);
        } else if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
        Log.d(LOG_TAG, "Executed frag manager pendingTransactions");
    }

    private void updateTilesList() {
        // Generally the items that are will be changing from these updates will
        // not be in the top list of tiles, so run it in the background and the
        // SettingsBaseActivity will pick up on the updates automatically.
        AsyncTask.execute(() -> doUpdateTilesList());
    }

    private void doUpdateTilesList() {
        PackageManager pm = getPackageManager();
        final UserManager um = UserManager.get(this);
        final boolean isAdmin = um.isAdminUser();
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
                        Settings.PowerUsageSummaryActivity.class.getName()),
                mBatteryPresent, isAdmin) || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.DataUsageSummaryActivity.class.getName()),
                Utils.isBandwidthControlEnabled(), isAdmin)
                || somethingChanged;

        somethingChanged = setTileEnabled(changedList, new ComponentName(packageName,
                        Settings.WifiDisplaySettingsActivity.class.getName()),
                WifiDisplaySettings.isAvailable(this), isAdmin)
                || somethingChanged;

        if (UserHandle.MU_ENABLED && !isAdmin) {
            // When on restricted users, disable all extra categories (but only the settings ones).
            final List<DashboardCategory> categories = mDashboardFeatureProvider.getAllCategories();
            synchronized (categories) {
                for (DashboardCategory category : categories) {
                    final int tileCount = category.getTilesCount();
                    for (int i = 0; i < tileCount; i++) {
                        final ComponentName component = category.getTile(i)
                                .getIntent().getComponent();
                        final String name = component.getClassName();
                        final boolean isEnabledForRestricted = ArrayUtils.contains(
                                SettingsGateway.SETTINGS_FOR_RESTRICTED, name);
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
            mCategoryMixin.updateCategories();
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
            mHighlightMenuKey = ai.metaData.getString(META_DATA_KEY_HIGHLIGHT_MENU_KEY);
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
}
