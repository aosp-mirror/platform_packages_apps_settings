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

import android.app.ActionBar;
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
import android.content.res.Configuration;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;

import com.android.internal.util.ArrayUtils;
import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilitySettingsForSetupWizard;
import com.android.settings.accessibility.CaptionPropertiesFragment;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.ChooseAccountActivity;
import com.android.settings.accounts.ManagedProfileSettings;
import com.android.settings.applications.AdvancedAppSettings;
import com.android.settings.applications.DrawOverlayDetails;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.ManageAssist;
import com.android.settings.applications.NotificationApps;
import com.android.settings.applications.ProcessStatsSummary;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.applications.UsageAccessDetails;
import com.android.settings.applications.VrListenerSettings;
import com.android.settings.applications.WriteSettingsDetails;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.dashboard.DashboardContainerFragment;
import com.android.settings.dashboard.SearchResultsSummary;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deviceinfo.ImeiInformation;
import com.android.settings.deviceinfo.PrivateVolumeForget;
import com.android.settings.deviceinfo.PrivateVolumeSettings;
import com.android.settings.deviceinfo.PublicVolumeSettings;
import com.android.settings.deviceinfo.SimStatus;
import com.android.settings.deviceinfo.Status;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.NightDisplaySettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageDetail;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment2;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.localepicker.LocaleListEditor;
import com.android.settings.location.LocationSettings;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.notification.NotificationStation;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenAccessSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModeEventRuleSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeScheduleRuleSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.qstile.DevelopmentTiles;
import com.android.settings.search.DynamicIndexableContentMonitor;
import com.android.settings.search.Index;
import com.android.settings.sim.SimSettings;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiAPITest;
import com.android.settings.wifi.WifiInfo;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends SettingsDrawerActivity
        implements PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        ButtonBarHandler, FragmentManager.OnBackStackChangedListener,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener,
        MenuItem.OnActionExpandListener {

    private static final String LOG_TAG = "Settings";

    private static final int LOADER_ID_INDEXABLE_CONTENT_MONITOR = 1;

    // Constants for state save/restore
    private static final String SAVE_KEY_CATEGORIES = ":settings:categories";
    private static final String SAVE_KEY_SEARCH_MENU_EXPANDED = ":settings:search_menu_expanded";
    private static final String SAVE_KEY_SEARCH_QUERY = ":settings:search_query";
    private static final String SAVE_KEY_SHOW_HOME_AS_UP = ":settings:show_home_as_up";
    private static final String SAVE_KEY_SHOW_SEARCH = ":settings:show_search";
    private static final String SAVE_KEY_HOME_ACTIVITIES_COUNT = ":settings:home_activities_count";

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

    public static final String EXTRA_HIDE_DRAWER = ":settings:hide_drawer";

    public static final String META_DATA_KEY_FRAGMENT_CLASS =
        "com.android.settings.FRAGMENT_CLASS";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private static final String EMPTY_QUERY = "";

    private static final int REQUEST_SUGGESTION = 42;

    private String mFragmentClass;

    private CharSequence mInitialTitle;
    private int mInitialTitleResId;

    // Show only these settings for restricted users
    private String[] SETTINGS_FOR_RESTRICTED = {
            //wireless_section
            WifiSettingsActivity.class.getName(),
            Settings.BluetoothSettingsActivity.class.getName(),
            Settings.DataUsageSummaryActivity.class.getName(),
            Settings.SimSettingsActivity.class.getName(),
            Settings.WirelessSettingsActivity.class.getName(),
            //device_section
            Settings.HomeSettingsActivity.class.getName(),
            Settings.SoundSettingsActivity.class.getName(),
            Settings.DisplaySettingsActivity.class.getName(),
            Settings.StorageSettingsActivity.class.getName(),
            Settings.ManageApplicationsActivity.class.getName(),
            Settings.PowerUsageSummaryActivity.class.getName(),
            Settings.GestureSettingsActivity.class.getName(),
            //personal_section
            Settings.LocationSettingsActivity.class.getName(),
            Settings.SecuritySettingsActivity.class.getName(),
            Settings.InputMethodAndLanguageSettingsActivity.class.getName(),
            Settings.UserSettingsActivity.class.getName(),
            Settings.AccountSettingsActivity.class.getName(),
            //system_section
            Settings.DateTimeSettingsActivity.class.getName(),
            Settings.DeviceInfoSettingsActivity.class.getName(),
            Settings.AccessibilitySettingsActivity.class.getName(),
            Settings.PrintSettingsActivity.class.getName(),
            Settings.PaymentSettingsActivity.class.getName(),
    };

    private static final String[] ENTRY_FRAGMENTS = {
            WirelessSettings.class.getName(),
            WifiSettings.class.getName(),
            AdvancedWifiSettings.class.getName(),
            SavedAccessPointsWifiSettings.class.getName(),
            BluetoothSettings.class.getName(),
            SimSettings.class.getName(),
            TetherSettings.class.getName(),
            WifiP2pSettings.class.getName(),
            VpnSettings.class.getName(),
            DateTimeSettings.class.getName(),
            LocaleListEditor.class.getName(),
            InputMethodAndLanguageSettings.class.getName(),
            AvailableVirtualKeyboardFragment.class.getName(),
            SpellCheckersSettings.class.getName(),
            UserDictionaryList.class.getName(),
            UserDictionarySettings.class.getName(),
            HomeSettings.class.getName(),
            DisplaySettings.class.getName(),
            DeviceInfoSettings.class.getName(),
            ManageApplications.class.getName(),
            NotificationApps.class.getName(),
            ManageAssist.class.getName(),
            ProcessStatsUi.class.getName(),
            NotificationStation.class.getName(),
            LocationSettings.class.getName(),
            SecuritySettings.class.getName(),
            UsageAccessDetails.class.getName(),
            PrivacySettings.class.getName(),
            DeviceAdminSettings.class.getName(),
            AccessibilitySettings.class.getName(),
            AccessibilitySettingsForSetupWizard.class.getName(),
            CaptionPropertiesFragment.class.getName(),
            com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment.class.getName(),
            TextToSpeechSettings.class.getName(),
            StorageSettings.class.getName(),
            PrivateVolumeForget.class.getName(),
            PrivateVolumeSettings.class.getName(),
            PublicVolumeSettings.class.getName(),
            DevelopmentSettings.class.getName(),
            AndroidBeam.class.getName(),
            WifiDisplaySettings.class.getName(),
            PowerUsageSummary.class.getName(),
            AccountSyncSettings.class.getName(),
            AccountSettings.class.getName(),
            GestureSettings.class.getName(),
            CryptKeeperSettings.class.getName(),
            DataUsageSummary.class.getName(),
            DreamSettings.class.getName(),
            UserSettings.class.getName(),
            NotificationAccessSettings.class.getName(),
            ZenAccessSettings.class.getName(),
            PrintSettingsFragment.class.getName(),
            PrintJobSettingsFragment.class.getName(),
            TrustedCredentialsSettings.class.getName(),
            PaymentSettings.class.getName(),
            KeyboardLayoutPickerFragment.class.getName(),
            KeyboardLayoutPickerFragment2.class.getName(),
            PhysicalKeyboardFragment.class.getName(),
            ZenModeSettings.class.getName(),
            SoundSettings.class.getName(),
            ConfigureNotificationSettings.class.getName(),
            ChooseLockPassword.ChooseLockPasswordFragment.class.getName(),
            ChooseLockPattern.ChooseLockPatternFragment.class.getName(),
            InstalledAppDetails.class.getName(),
            BatterySaverSettings.class.getName(),
            AppNotificationSettings.class.getName(),
            OtherSoundSettings.class.getName(),
            ApnSettings.class.getName(),
            ApnEditor.class.getName(),
            WifiCallingSettings.class.getName(),
            ZenModePrioritySettings.class.getName(),
            ZenModeAutomationSettings.class.getName(),
            ZenModeScheduleRuleSettings.class.getName(),
            ZenModeEventRuleSettings.class.getName(),
            ZenModeVisualInterruptionSettings.class.getName(),
            ProcessStatsUi.class.getName(),
            PowerUsageDetail.class.getName(),
            ProcessStatsSummary.class.getName(),
            DrawOverlayDetails.class.getName(),
            WriteSettingsDetails.class.getName(),
            AdvancedAppSettings.class.getName(),
            WallpaperTypeSettings.class.getName(),
            VrListenerSettings.class.getName(),
            ManagedProfileSettings.class.getName(),
            ChooseAccountActivity.class.getName(),
            IccLockSettings.class.getName(),
            ImeiInformation.class.getName(),
            SimStatus.class.getName(),
            Status.class.getName(),
            TestingSettings.class.getName(),
            WifiAPITest.class.getName(),
            WifiInfo.class.getName(),
            MasterClear.class.getName(),
            NightDisplaySettings.class.getName(),
    };


    private static final String[] LIKE_SHORTCUT_INTENT_ACTION_ARRAY = {
            "android.settings.APPLICATION_DETAILS_SETTINGS"
    };

    private SharedPreferences mDevelopmentPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;

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

    private final BroadcastReceiver mUserAddRemoveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_ADDED)
                    || action.equals(Intent.ACTION_USER_REMOVED)) {
                Index.getInstance(getApplicationContext()).update();
            }
        }
    };

    private final DynamicIndexableContentMonitor mDynamicIndexableContentMonitor =
            new DynamicIndexableContentMonitor();

    private ActionBar mActionBar;
    private SwitchBar mSwitchBar;

    private Button mNextButton;

    private boolean mDisplayHomeAsUpEnabled;
    private boolean mDisplaySearch;

    private boolean mIsShowingDashboard;
    private boolean mIsShortcut;

    private ViewGroup mContent;

    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private boolean mSearchMenuItemExpanded = false;
    private SearchResultsSummary mSearchResultsFragment;
    private String mSearchQuery;

    // Categories
    private ArrayList<DashboardCategory> mCategories = new ArrayList<DashboardCategory>();

    private static final String MSG_DATA_FORCE_REFRESH = "msg_data_force_refresh";

    private boolean mNeedToRevertToInitialFragment = false;

    private Intent mResultIntentData;
    private ComponentName mCurrentSuggestion;

    public SwitchBar getSwitchBar() {
        return mSwitchBar;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        startPreferencePanel(pref.getFragment(), pref.getExtras(), -1, pref.getTitle(),
                null, 0);
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Index.getInstance(this).update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDisplaySearch) {
            return false;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        // Cache the search query (can be overriden by the OnQueryTextListener)
        final String query = mSearchQuery;

        mSearchMenuItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();

        if (mSearchMenuItem == null || mSearchView == null) {
            return false;
        }

        if (mSearchResultsFragment != null) {
            mSearchResultsFragment.setSearchView(mSearchView);
        }

        mSearchMenuItem.setOnActionExpandListener(this);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);

        if (mSearchMenuItemExpanded) {
            mSearchMenuItem.expandActionView();
        }
        mSearchView.setQuery(query, true /* submit */);

        return true;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (name.equals(getPackageName() + "_preferences")) {
            return new SharedPreferencesLogger(this, getMetricsTag());
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

    private static boolean isShortCutIntent(final Intent intent) {
        Set<String> categories = intent.getCategories();
        return (categories != null) && categories.contains("com.android.settings.SHORTCUT");
    }

    private static boolean isLikeShortCutIntent(final Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return false;
        }
        for (int i = 0; i < LIKE_SHORTCUT_INTENT_ACTION_ARRAY.length; i++) {
            if (LIKE_SHORTCUT_INTENT_ACTION_ARRAY[i].equals(action)) return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        long startTime = System.currentTimeMillis();

        // Should happen before any call to getIntent()
        getMetaData();

        final Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(intent.getIntExtra(EXTRA_UI_OPTIONS, 0));
        }
        if (intent.getBooleanExtra(EXTRA_HIDE_DRAWER, false)) {
            setIsDrawerPresent(false);
        }

        mDevelopmentPreferences = getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);

        // Getting Intent properties can only be done after the super.onCreate(...)
        final String initialFragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);

        mIsShortcut = isShortCutIntent(intent) || isLikeShortCutIntent(intent) ||
                intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SHORTCUT, false);

        final ComponentName cn = intent.getComponent();
        final String className = cn.getClassName();

        mIsShowingDashboard = className.equals(Settings.class.getName())
                || className.equals(Settings.WirelessSettings.class.getName())
                || className.equals(Settings.DeviceSettings.class.getName())
                || className.equals(Settings.PersonalSettings.class.getName())
                || className.equals(Settings.WirelessSettings.class.getName());

        // This is a "Sub Settings" when:
        // - this is a real SubSettings
        // - or :settings:show_fragment_as_subsetting is passed to the Intent
        final boolean isSubSettings = this instanceof SubSettings ||
                intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, false);

        // If this is a sub settings, then apply the SubSettings Theme for the ActionBar content insets
        if (isSubSettings) {
            // Check also that we are not a Theme Dialog as we don't want to override them
            final int themeResId = getThemeResId();
            if (themeResId != R.style.Theme_DialogWhenLarge &&
                    themeResId != R.style.Theme_SubSettingsDialogWhenLarge) {
                setTheme(R.style.Theme_SubSettings);
            }
        }

        setContentView(mIsShowingDashboard ?
                R.layout.settings_main_dashboard : R.layout.settings_main_prefs);

        mContent = (ViewGroup) findViewById(R.id.main_content);

        getFragmentManager().addOnBackStackChangedListener(this);

        if (mIsShowingDashboard) {
            // Run the Index update only if we have some space
            if (!Utils.isLowStorage(this)) {
                long indexStartTime = System.currentTimeMillis();
                Index.getInstance(getApplicationContext()).update();
                if (DEBUG_TIMING) Log.d(LOG_TAG, "Index.update() took "
                        + (System.currentTimeMillis() - indexStartTime) + " ms");
            } else {
                Log.w(LOG_TAG, "Cannot update the Indexer as we are running low on storage space!");
            }
        }

        if (savedState != null) {
            // We are restarting from a previous saved state; used that to initialize, instead
            // of starting fresh.
            mSearchMenuItemExpanded = savedState.getBoolean(SAVE_KEY_SEARCH_MENU_EXPANDED);
            mSearchQuery = savedState.getString(SAVE_KEY_SEARCH_QUERY);

            setTitleFromIntent(intent);

            ArrayList<DashboardCategory> categories =
                    savedState.getParcelableArrayList(SAVE_KEY_CATEGORIES);
            if (categories != null) {
                mCategories.clear();
                mCategories.addAll(categories);
                setTitleFromBackStack();
            }

            mDisplayHomeAsUpEnabled = savedState.getBoolean(SAVE_KEY_SHOW_HOME_AS_UP);
            mDisplaySearch = savedState.getBoolean(SAVE_KEY_SHOW_SEARCH);
        } else {
            if (!mIsShowingDashboard) {
                mDisplaySearch = false;
                // UP will be shown only if it is a sub settings
                if (mIsShortcut) {
                    mDisplayHomeAsUpEnabled = isSubSettings;
                } else if (isSubSettings) {
                    mDisplayHomeAsUpEnabled = true;
                } else {
                    mDisplayHomeAsUpEnabled = false;
                }
                setTitleFromIntent(intent);

                Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                switchToFragment(initialFragmentName, initialArguments, true, false,
                        mInitialTitleResId, mInitialTitle, false);
            } else {
                // No UP affordance if we are displaying the main Dashboard
                mDisplayHomeAsUpEnabled = false;
                // Show Search affordance
                mDisplaySearch = true;
                mInitialTitleResId = R.string.dashboard_title;
                switchToFragment(DashboardContainerFragment.class.getName(), null, false, false,
                        mInitialTitleResId, mInitialTitle, false);
            }
        }

        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(mDisplayHomeAsUpEnabled);
            mActionBar.setHomeButtonEnabled(mDisplayHomeAsUpEnabled);
        }
        mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        if (mSwitchBar != null) {
            mSwitchBar.setMetricsTag(getMetricsTag());
        }

        // see if we should show Back/Next buttons
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {

            View buttonBar = findViewById(R.id.button_bar);
            if (buttonBar != null) {
                buttonBar.setVisibility(View.VISIBLE);

                Button backButton = (Button)findViewById(R.id.back_button);
                backButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED, getResultIntentData());
                        finish();
                    }
                });
                Button skipButton = (Button)findViewById(R.id.skip_button);
                skipButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK, getResultIntentData());
                        finish();
                    }
                });
                mNextButton = (Button)findViewById(R.id.next_button);
                mNextButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK, getResultIntentData());
                        finish();
                    }
                });

                // set our various button parameters
                if (intent.hasExtra(EXTRA_PREFS_SET_NEXT_TEXT)) {
                    String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_NEXT_TEXT);
                    if (TextUtils.isEmpty(buttonText)) {
                        mNextButton.setVisibility(View.GONE);
                    }
                    else {
                        mNextButton.setText(buttonText);
                    }
                }
                if (intent.hasExtra(EXTRA_PREFS_SET_BACK_TEXT)) {
                    String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_BACK_TEXT);
                    if (TextUtils.isEmpty(buttonText)) {
                        backButton.setVisibility(View.GONE);
                    }
                    else {
                        backButton.setText(buttonText);
                    }
                }
                if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_SKIP, false)) {
                    skipButton.setVisibility(View.VISIBLE);
                }
            }
        }

        if (DEBUG_TIMING) Log.d(LOG_TAG, "onCreate took " + (System.currentTimeMillis() - startTime)
                + " ms");
    }

    public void setDisplaySearchMenu(boolean displaySearch) {
        if (displaySearch != mDisplaySearch) {
            mDisplaySearch = displaySearch;
            invalidateOptionsMenu();
        }
    }

    private void setTitleFromIntent(Intent intent) {
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

        if (mCategories.size() > 0) {
            outState.putParcelableArrayList(SAVE_KEY_CATEGORIES, mCategories);
        }

        outState.putBoolean(SAVE_KEY_SHOW_HOME_AS_UP, mDisplayHomeAsUpEnabled);
        outState.putBoolean(SAVE_KEY_SHOW_SEARCH, mDisplaySearch);

        if (mDisplaySearch) {
            // The option menus are created if the ActionBar is visible and they are also created
            // asynchronously. If you launch Settings with an Intent action like
            // android.intent.action.POWER_USAGE_SUMMARY and at the same time your device is locked
            // thru a LockScreen, onCreateOptionsMenu() is not yet called and references to the search
            // menu item and search view are null.
            boolean isExpanded = (mSearchMenuItem != null) && mSearchMenuItem.isActionViewExpanded();
            outState.putBoolean(SAVE_KEY_SEARCH_MENU_EXPANDED, isExpanded);

            String query = (mSearchView != null) ? mSearchView.getQuery().toString() : EMPTY_QUERY;
            outState.putString(SAVE_KEY_SEARCH_QUERY, query);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mNeedToRevertToInitialFragment) {
            revertToInitialFragment();
        }

        mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateTilesList();
            }
        };
        mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(mUserAddRemoveReceiver, new IntentFilter(Intent.ACTION_USER_ADDED));
        registerReceiver(mUserAddRemoveReceiver, new IntentFilter(Intent.ACTION_USER_REMOVED));

        mDynamicIndexableContentMonitor.register(this, LOADER_ID_INDEXABLE_CONTENT_MONITOR);

        if(mDisplaySearch && !TextUtils.isEmpty(mSearchQuery)) {
            onQueryTextSubmit(mSearchQuery);
        }
        updateTilesList();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBatteryInfoReceiver);
        unregisterReceiver(mUserAddRemoveReceiver);
        mDynamicIndexableContentMonitor.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);
        mDevelopmentPreferencesListener = null;
    }

    protected boolean isValidFragment(String fragmentName) {
        // Almost all fragments are wrapped in this,
        // except for a few that have their own activities.
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
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
            Bundle args = superIntent.getExtras();
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

        if ("com.android.settings.ManageApplications".equals(intentClass)
                || "com.android.settings.RunningServices".equals(intentClass)
                || "com.android.settings.applications.StorageUse".equals(intentClass)) {
            // Old names of manage apps.
            intentClass = com.android.settings.applications.ManageApplications.class.getName();
        }

        return intentClass;
    }

    /**
     * Start a new fragment containing a preference panel.  If the preferences
     * are being displayed in multi-pane mode, the given fragment class will
     * be instantiated and placed in the appropriate pane.  If running in
     * single-pane mode, a new activity will be launched in which to show the
     * fragment.
     *
     * @param fragmentClass Full name of the class implementing the fragment.
     * @param args Any desired arguments to supply to the fragment.
     * @param titleRes Optional resource identifier of the title of this
     * fragment.
     * @param titleText Optional text of the title of this fragment.
     * @param resultTo Optional fragment that result data should be sent to.
     * If non-null, resultTo.onActivityResult() will be called when this
     * preference panel is done.  The launched panel must use
     * {@link #finishPreferencePanel(Fragment, int, Intent)} when done.
     * @param resultRequestCode If resultTo is non-null, this is the caller's
     * request code to be received with the result.
     */
    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
            CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        String title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                // There not much we can do in that case
                title = "";
            }
        }
        Utils.startWithFragment(this, fragmentClass, args, resultTo, resultRequestCode,
                titleRes, title, mIsShortcut);
    }

    /**
     * Start a new fragment in a new activity containing a preference panel for a given user. If the
     * preferences are being displayed in multi-pane mode, the given fragment class will be
     * instantiated and placed in the appropriate pane. If running in single-pane mode, a new
     * activity will be launched in which to show the fragment.
     *
     * @param fragmentClass Full name of the class implementing the fragment.
     * @param args Any desired arguments to supply to the fragment.
     * @param titleRes Optional resource identifier of the title of this fragment.
     * @param titleText Optional text of the title of this fragment.
     * @param userHandle The user for which the panel has to be started.
     */
    public void startPreferencePanelAsUser(String fragmentClass, Bundle args, int titleRes,
            CharSequence titleText, UserHandle userHandle) {
        // This is a workaround.
        //
        // Calling startWithFragmentAsUser() without specifying FLAG_ACTIVITY_NEW_TASK to the intent
        // starting the fragment could cause a native stack corruption. See b/17523189. However,
        // adding that flag and start the preference panel with the same UserHandler will make it
        // impossible to use back button to return to the previous screen. See b/20042570.
        //
        // We work around this issue by adding FLAG_ACTIVITY_NEW_TASK to the intent, while doing
        // another check here to call startPreferencePanel() instead of startWithFragmentAsUser()
        // when we're calling it as the same user.
        if (userHandle.getIdentifier() == UserHandle.myUserId()) {
            startPreferencePanel(fragmentClass, args, titleRes, titleText, null, 0);
        } else {
            String title = null;
            if (titleRes < 0) {
                if (titleText != null) {
                    title = titleText.toString();
                } else {
                    // There not much we can do in that case
                    title = "";
                }
            }
            Utils.startWithFragmentAsUser(this, fragmentClass, args,
                    titleRes, title, mIsShortcut, userHandle);
        }
    }

    /**
     * Called by a preference panel fragment to finish itself.
     *
     * @param caller The fragment that is asking to be finished.
     * @param resultCode Optional result code to send back to the original
     * launching fragment.
     * @param resultData Optional result data to send back to the original
     * launching fragment.
     */
    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        setResult(resultCode, resultData);
        finish();
    }

    /**
     * Start a new fragment.
     *
     * @param fragment The fragment to start
     * @param push If true, the current fragment will be pushed onto the back stack.  If false,
     * the current fragment will be replaced.
     */
    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, fragment);
        if (push) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack(BACK_STACK_PREFS);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
        transaction.commitAllowingStateLoss();
    }

    /**
     * Switch to a specific Fragment with taking care of validation, Title and BackStack
     */
    private Fragment switchToFragment(String fragmentName, Bundle args, boolean validate,
            boolean addToBackStack, int titleResId, CharSequence title, boolean withTransition) {
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

    private void doUpdateTilesList() {
        PackageManager pm = getPackageManager();
        final UserManager um = UserManager.get(this);
        final boolean isAdmin = um.isAdminUser();

        String packageName = getPackageName();
        setTileEnabled(new ComponentName(packageName, WifiSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_WIFI), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.BluetoothSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.DataUsageSummaryActivity.class.getName()),
                Utils.isBandwidthControlEnabled(), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.SimSettingsActivity.class.getName()),
                Utils.showSimCardTile(this), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.PowerUsageSummaryActivity.class.getName()),
                mBatteryPresent, isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.UserSettingsActivity.class.getName()),
                UserHandle.MU_ENABLED && UserManager.supportsMultipleUsers()
                && !Utils.isMonkeyRunning(), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                        Settings.WirelessSettingsActivity.class.getName()),
                !UserManager.isDeviceInDemoMode(this), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                        Settings.DateTimeSettingsActivity.class.getName()),
                !UserManager.isDeviceInDemoMode(this), isAdmin, pm);
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        setTileEnabled(new ComponentName(packageName,
                        Settings.PaymentSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_NFC)
                        && pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
                        && adapter != null && adapter.isEnabled(), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.PrintSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_PRINTING), isAdmin, pm);

        final boolean showDev = mDevelopmentPreferences.getBoolean(
                    DevelopmentSettings.PREF_SHOW, android.os.Build.TYPE.equals("eng"))
                && !um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
        setTileEnabled(new ComponentName(packageName,
                        Settings.DevelopmentSettingsActivity.class.getName()),
                showDev, isAdmin, pm);

        // Reveal development-only quick settings tiles
        DevelopmentTiles.setTilesEnabled(this, showDev);

        if (UserHandle.MU_ENABLED && !isAdmin) {
            // When on restricted users, disable all extra categories (but only the settings ones).
            List<DashboardCategory> categories = getDashboardCategories();
            for (DashboardCategory category : categories) {
                for (Tile tile : category.tiles) {
                    ComponentName component = tile.intent.getComponent();
                    if (packageName.equals(component.getPackageName()) && !ArrayUtils.contains(
                            SETTINGS_FOR_RESTRICTED, component.getClassName())) {
                        setTileEnabled(component, false, isAdmin, pm);
                    }
                }
            }
        }

        String backupIntent = getResources().getString(R.string.config_backup_settings_intent);
        boolean useDefaultBackup = TextUtils.isEmpty(backupIntent);
        setTileEnabled(new ComponentName(packageName,
                Settings.PrivacySettingsActivity.class.getName()), useDefaultBackup, isAdmin, pm);
        boolean hasBackupActivity = false;
        if (!useDefaultBackup) {
            try {
                Intent intent = Intent.parseUri(backupIntent, 0);
                hasBackupActivity = !getPackageManager().queryIntentActivities(intent, 0).isEmpty();
            } catch (URISyntaxException e) {
                Log.e(LOG_TAG, "Invalid backup intent URI!", e);
            }
        }
        setTileEnabled(new ComponentName(packageName,
                BackupSettingsActivity.class.getName()), hasBackupActivity, isAdmin, pm);

    }

    private void setTileEnabled(ComponentName component, boolean enabled, boolean isAdmin,
                                PackageManager pm) {
        if (UserHandle.MU_ENABLED && !isAdmin && getPackageName().equals(component.getPackageName())
                && !ArrayUtils.contains(SETTINGS_FOR_RESTRICTED, component.getClassName())) {
            enabled = false;
        }
        setTileEnabled(component, enabled);
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

    @Override
    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return super.shouldUpRecreateTask(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        switchToSearchResultsFragmentIfNeeded();
        mSearchQuery = query;
        return mSearchResultsFragment.onQueryTextSubmit(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchQuery = newText;
        if (mSearchResultsFragment == null) {
            return false;
        }
        return mSearchResultsFragment.onQueryTextChange(newText);
    }

    @Override
    public boolean onClose() {
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == mSearchMenuItem.getItemId()) {
            switchToSearchResultsFragmentIfNeeded();
        }
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (item.getItemId() == mSearchMenuItem.getItemId()) {
            if (mSearchMenuItemExpanded) {
                revertToInitialFragment();
            }
        }
        return true;
    }

    @Override
    protected void onTileClicked(Tile tile) {
        if (mIsShowingDashboard) {
            // If on dashboard, don't finish so the back comes back to here.
            openTile(tile);
        } else {
            super.onTileClicked(tile);
        }
    }

    @Override
    public void onProfileTileOpen() {
        if (!mIsShowingDashboard) {
            finish();
        }
    }

    private void switchToSearchResultsFragmentIfNeeded() {
        if (mSearchResultsFragment != null) {
            return;
        }
        Fragment current = getFragmentManager().findFragmentById(R.id.main_content);
        if (current != null && current instanceof SearchResultsSummary) {
            mSearchResultsFragment = (SearchResultsSummary) current;
        } else {
            setContentHeaderView(null);
            mSearchResultsFragment = (SearchResultsSummary) switchToFragment(
                    SearchResultsSummary.class.getName(), null, false, true,
                    R.string.search_results_title, null, true);
        }
        mSearchResultsFragment.setSearchView(mSearchView);
        mSearchMenuItemExpanded = true;
    }

    public void needToRevertToInitialFragment() {
        mNeedToRevertToInitialFragment = true;
    }

    private void revertToInitialFragment() {
        mNeedToRevertToInitialFragment = false;
        mSearchResultsFragment = null;
        mSearchMenuItemExpanded = false;
        getFragmentManager().popBackStackImmediate(SettingsActivity.BACK_STACK_PREFS,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }
    }

    public Intent getResultIntentData() {
        return mResultIntentData;
    }

    public void setResultIntentData(Intent resultIntentData) {
        mResultIntentData = resultIntentData;
    }

    public void startSuggestion(Intent intent) {
        mCurrentSuggestion = intent.getComponent();
        startActivityForResult(intent, REQUEST_SUGGESTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SUGGESTION && mCurrentSuggestion != null
                && resultCode != RESULT_CANCELED) {
            getPackageManager().setComponentEnabledSetting(mCurrentSuggestion,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
