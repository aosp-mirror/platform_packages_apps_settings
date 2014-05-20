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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.ActionBar;
import android.app.Activity;
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
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import android.widget.SearchView;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.CaptionPropertiesFragment;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.AuthenticatorHelper;
import com.android.settings.accounts.ManageAccountsSettings;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.dashboard.DashboardCategory;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.dashboard.DashboardTile;
import com.android.settings.dashboard.NoHomeDialogFragment;
import com.android.settings.dashboard.SearchResultsSummary;
import com.android.settings.deviceinfo.Memory;
import com.android.settings.deviceinfo.UsbSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.search.DynamicIndexableContentMonitor;
import com.android.settings.search.Index;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.location.LocationSettings;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.ConditionProviderSettings;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.notification.NotificationSettings;
import com.android.settings.notification.NotificationStation;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.android.settings.dashboard.DashboardTile.TILE_ID_UNDEFINED;

public class SettingsActivity extends Activity
        implements PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        ButtonBarHandler, OnAccountsUpdateListener, FragmentManager.OnBackStackChangedListener,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener,
        MenuItem.OnActionExpandListener {

    private static final String LOG_TAG = "Settings";

    // Constants for state save/restore
    private static final String SAVE_KEY_CATEGORIES = ":settings:categories";
    private static final String SAVE_KEY_SEARCH_MENU_EXPANDED = ":settings:search_menu_expanded";
    private static final String SAVE_KEY_SEARCH_QUERY = ":settings:search_query";
    private static final String SAVE_KEY_SHOW_HOME_AS_UP = ":settings:show_home_as_up";

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
     * this extra can also be specify to supply the title to be shown for
     * that fragment.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";

    private static final String META_DATA_KEY_FRAGMENT_CLASS =
        "com.android.settings.FRAGMENT_CLASS";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private static final String EMPTY_QUERY = "";

    private static boolean sShowNoHomeNotice = false;

    private String mFragmentClass;

    private CharSequence mInitialTitle;

    // Show only these settings for restricted users
    private int[] SETTINGS_FOR_RESTRICTED = {
            R.id.wireless_section,
            R.id.wifi_settings,
            R.id.bluetooth_settings,
            R.id.data_usage_settings,
            R.id.wireless_settings,
            R.id.device_section,
            R.id.sound_settings,
            R.id.display_settings,
            R.id.storage_settings,
            R.id.application_settings,
            R.id.battery_settings,
            R.id.personal_section,
            R.id.location_settings,
            R.id.security_settings,
            R.id.language_settings,
            R.id.user_settings,
            R.id.account_settings,
            R.id.account_add,
            R.id.system_section,
            R.id.date_time_settings,
            R.id.about_settings,
            R.id.accessibility_settings,
            R.id.print_settings,
            R.id.nfc_payment_settings,
            R.id.home_settings,
            R.id.dashboard
    };

    private static final String[] ENTRY_FRAGMENTS = {
            WirelessSettings.class.getName(),
            WifiSettings.class.getName(),
            AdvancedWifiSettings.class.getName(),
            BluetoothSettings.class.getName(),
            TetherSettings.class.getName(),
            WifiP2pSettings.class.getName(),
            VpnSettings.class.getName(),
            DateTimeSettings.class.getName(),
            LocalePicker.class.getName(),
            InputMethodAndLanguageSettings.class.getName(),
            SpellCheckersSettings.class.getName(),
            UserDictionaryList.class.getName(),
            UserDictionarySettings.class.getName(),
            SoundSettings.class.getName(),
            DisplaySettings.class.getName(),
            DeviceInfoSettings.class.getName(),
            ManageApplications.class.getName(),
            ProcessStatsUi.class.getName(),
            NotificationStation.class.getName(),
            LocationSettings.class.getName(),
            SecuritySettings.class.getName(),
            PrivacySettings.class.getName(),
            DeviceAdminSettings.class.getName(),
            AccessibilitySettings.class.getName(),
            CaptionPropertiesFragment.class.getName(),
            com.android.settings.accessibility.ToggleInversionPreferenceFragment.class.getName(),
            com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment.class.getName(),
            TextToSpeechSettings.class.getName(),
            Memory.class.getName(),
            DevelopmentSettings.class.getName(),
            UsbSettings.class.getName(),
            AndroidBeam.class.getName(),
            WifiDisplaySettings.class.getName(),
            PowerUsageSummary.class.getName(),
            AccountSyncSettings.class.getName(),
            CryptKeeperSettings.class.getName(),
            DataUsageSummary.class.getName(),
            DreamSettings.class.getName(),
            UserSettings.class.getName(),
            NotificationAccessSettings.class.getName(),
            ConditionProviderSettings.class.getName(),
            ManageAccountsSettings.class.getName(),
            PrintSettingsFragment.class.getName(),
            PrintJobSettingsFragment.class.getName(),
            TrustedCredentialsSettings.class.getName(),
            PaymentSettings.class.getName(),
            KeyboardLayoutPickerFragment.class.getName(),
            ZenModeSettings.class.getName(),
            NotificationSettings.class.getName(),
            ChooseLockPassword.ChooseLockPasswordFragment.class.getName(),
            ChooseLockPattern.ChooseLockPatternFragment.class.getName(),
            InstalledAppDetails.class.getName()
    };

    private SharedPreferences mDevelopmentPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;

    private AuthenticatorHelper mAuthenticatorHelper;
    private boolean mListeningToAccountUpdates;

    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean batteryPresent = Utils.isBatteryPresent(intent);

                if (mBatteryPresent != batteryPresent) {
                    mBatteryPresent = batteryPresent;
                    invalidateCategories();
                }
            }
        }
    };

    private final DynamicIndexableContentMonitor mDynamicIndexableContentMonitor =
            new DynamicIndexableContentMonitor();

    private ActionBar mActionBar;
    private SwitchBar mSwitchBar;

    private Button mNextButton;
    private boolean mDisplayHomeAsUpEnabled;

    private boolean mIsShowingDashboard;

    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private boolean mSearchMenuItemExpanded = false;
    private SearchResultsSummary mSearchResultsFragment;
    private String mSearchQuery;

    // Categories
    private ArrayList<DashboardCategory> mCategories = new ArrayList<DashboardCategory>();
    private boolean mNeedToRebuildCategories;

    private static final int MSG_BUILD_CATEGORIES = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BUILD_CATEGORIES: {
                    buildDashboardCategories(mCategories);
                } break;
            }
        }
    };

    private boolean mNeedToRevertToInitialFragment = false;

    public SwitchBar getSwitchBar() {
        return mSwitchBar;
    }

    public AuthenticatorHelper getAuthenticatorHelper() {
        return mAuthenticatorHelper;
    }

    public List<DashboardCategory> getDashboardCategories() {
        if (mNeedToRebuildCategories) {
            buildDashboardCategories(mCategories);
            mNeedToRebuildCategories = false;
        }
        return mCategories;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        // Override the fragment title for Wallpaper settings
        int titleRes = pref.getTitleRes();
        if (pref.getFragment().equals(WallpaperTypeSettings.class.getName())) {
            titleRes = R.string.wallpaper_settings_fragment_title;
        } else if (pref.getFragment().equals(OwnerInfoSettings.class.getName())
                && UserHandle.myUserId() != UserHandle.USER_OWNER) {
            if (UserManager.get(this).isLinkedUser()) {
                titleRes = R.string.profile_info_settings_title;
            } else {
                titleRes = R.string.user_info_settings_title;
            }
        }
        startPreferencePanel(pref.getFragment(), pref.getExtras(), titleRes, pref.getTitle(),
                null, 0);
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    private void invalidateCategories() {
        if (!mHandler.hasMessages(MSG_BUILD_CATEGORIES)) {
            mHandler.sendEmptyMessage(MSG_BUILD_CATEGORIES);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Index.getInstance(this).update();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mNeedToRevertToInitialFragment) {
            revertToInitialFragment();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
    protected void onCreate(Bundle savedState) {
        if (getIntent().hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(getIntent().getIntExtra(EXTRA_UI_OPTIONS, 0));
        }

        mAuthenticatorHelper = new AuthenticatorHelper();
        mAuthenticatorHelper.updateAuthDescriptions(this);
        mAuthenticatorHelper.onAccountsUpdated(this, null);

        mDevelopmentPreferences = getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);

        getMetaData();

        super.onCreate(savedState);

        setContentView(R.layout.settings_main);

        getFragmentManager().addOnBackStackChangedListener(this);

        mDisplayHomeAsUpEnabled = true;

        // Getting Intent properties can only be done after the super.onCreate(...)
        final String initialFragmentName = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);

        mIsShowingDashboard = (initialFragmentName == null);

        if (mIsShowingDashboard) {
            Index.getInstance(this).update();
        }

        if (savedState != null) {
            // We are restarting from a previous saved state; used that to initialize, instead
            // of starting fresh.
            mSearchMenuItemExpanded = savedState.getBoolean(SAVE_KEY_SEARCH_MENU_EXPANDED);
            mSearchQuery = savedState.getString(SAVE_KEY_SEARCH_QUERY);

            final String initialTitle = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
            mInitialTitle = (initialTitle != null) ? initialTitle : getTitle();
            setTitle(mInitialTitle);

            ArrayList<DashboardCategory> categories =
                    savedState.getParcelableArrayList(SAVE_KEY_CATEGORIES);
            if (categories != null) {
                mCategories.addAll(categories);
                setTitleFromBackStack();
            }

            mDisplayHomeAsUpEnabled = savedState.getBoolean(SAVE_KEY_SHOW_HOME_AS_UP);
        } else {
            // We need to build the Categories in all cases
            buildDashboardCategories(mCategories);

            if (!mIsShowingDashboard) {
                final ComponentName cn = getIntent().getComponent();
                // No UP is we are launched thru a Settings shortcut
                if (!cn.getClassName().equals(SubSettings.class.getName())) {
                    mDisplayHomeAsUpEnabled = false;
                }
                final String initialTitle = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
                mInitialTitle = (initialTitle != null) ? initialTitle : getTitle();
                setTitle(mInitialTitle);

                Bundle initialArguments = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                switchToFragment( initialFragmentName, initialArguments, true, false,
                        mInitialTitle, false);
            } else {
                // No UP if we are displaying the main Dashboard
                mDisplayHomeAsUpEnabled = false;
                if (mCategories.size() > 0) {
                    mInitialTitle = getText(R.string.dashboard_title);
                    switchToFragment(DashboardSummary.class.getName(), null, false, false,
                            mInitialTitle, false);
                }
            }
        }

        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(mDisplayHomeAsUpEnabled);
        mActionBar.setHomeButtonEnabled(mDisplayHomeAsUpEnabled);

        mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);

        // see if we should show Back/Next buttons
        Intent intent = getIntent();
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {

            View buttonBar = findViewById(R.id.button_bar);
            if (buttonBar != null) {
                buttonBar.setVisibility(View.VISIBLE);

                Button backButton = (Button)findViewById(R.id.back_button);
                backButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                Button skipButton = (Button)findViewById(R.id.skip_button);
                skipButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK);
                        finish();
                    }
                });
                mNextButton = (Button)findViewById(R.id.next_button);
                mNextButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK);
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
    }

    @Override
    public void onBackStackChanged() {
        setTitleFromBackStack();
    }

    private int setTitleFromBackStack() {
        final int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            setTitle(mInitialTitle);
            return 0;
        }

        FragmentManager.BackStackEntry bse = getFragmentManager().getBackStackEntryAt(count - 1);
        setTitleFromBackStackEntry(bse);

        return count;
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

    @Override
    public void onResume() {
        super.onResume();

        mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                invalidateCategories();
            }
        };
        mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);

        invalidateCategories();

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        mDynamicIndexableContentMonitor.register(this);

        if(!TextUtils.isEmpty(mSearchQuery)) {
            onQueryTextSubmit(mSearchQuery);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(mBatteryInfoReceiver);

        mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);

        mDevelopmentPreferencesListener = null;

        mDynamicIndexableContentMonitor.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mListeningToAccountUpdates) {
            AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        }
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
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, superIntent.getExtras());
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
     * request code to be received with the resut.
     */
    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
            CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        String title;
        if (titleRes > 0) {
            title = getString(titleRes);
        } else if (titleText != null) {
            title = titleText.toString();
        } else {
            // There not much we can do in that case
            title = "";
        }
        Utils.startWithFragment(this, fragmentClass, args, resultTo, resultRequestCode, title);
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
        transaction.replace(R.id.prefs, fragment);
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
            boolean addToBackStack, CharSequence title, boolean withTransition) {
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.prefs, f);
        if (withTransition) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        if (addToBackStack) {
            transaction.addToBackStack(SettingsActivity.BACK_STACK_PREFS);
        }
        if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        return f;
    }

    public void setNeedToRebuildCategories(boolean need) {
        mNeedToRebuildCategories = need;
    }

    /**
     * Called when the activity needs its list of categories/tiles built.
     *
     * @param categories The list in which to place the tiles categories.
     */
    private void buildDashboardCategories(List<DashboardCategory> categories) {
        mCategories.clear();
        loadCategoriesFromResource(R.xml.dashboard_categories, categories);
        updateTilesList(categories);
    }

    /**
     * Parse the given XML file as a categories description, adding each
     * parsed categories and tiles into the target list.
     *
     * @param resid The XML resource to load and parse.
     * @param target The list in which the parsed categories and tiles should be placed.
     */
    private void loadCategoriesFromResource(int resid, List<DashboardCategory> target) {
        XmlResourceParser parser = null;
        try {
            parser = getResources().getXml(resid);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!"dashboard-categories".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <preference-categories> tag; found"
                                + nodeName + " at " + parser.getPositionDescription());
            }

            Bundle curBundle = null;

            final int outerDepth = parser.getDepth();
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("dashboard-category".equals(nodeName)) {
                    DashboardCategory category = new DashboardCategory();

                    TypedArray sa = obtainStyledAttributes(
                            attrs, com.android.internal.R.styleable.PreferenceHeader);
                    category.id = sa.getResourceId(
                            com.android.internal.R.styleable.PreferenceHeader_id,
                            (int)DashboardCategory.CAT_ID_UNDEFINED);

                    TypedValue tv = sa.peekValue(
                            com.android.internal.R.styleable.PreferenceHeader_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            category.titleRes = tv.resourceId;
                        } else {
                            category.title = tv.string;
                        }
                    }
                    sa.recycle();

                    final int innerDepth = parser.getDepth();
                    while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                            && (type != XmlPullParser.END_TAG || parser.getDepth() > innerDepth)) {
                        if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                            continue;
                        }

                        String innerNodeName = parser.getName();
                        if (innerNodeName.equals("dashboard-tile")) {
                            DashboardTile tile = new DashboardTile();

                            sa = obtainStyledAttributes(
                                    attrs, com.android.internal.R.styleable.PreferenceHeader);
                            tile.id = sa.getResourceId(
                                    com.android.internal.R.styleable.PreferenceHeader_id,
                                    (int)TILE_ID_UNDEFINED);
                            tv = sa.peekValue(
                                    com.android.internal.R.styleable.PreferenceHeader_title);
                            if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                                if (tv.resourceId != 0) {
                                    tile.titleRes = tv.resourceId;
                                } else {
                                    tile.title = tv.string;
                                }
                            }
                            tv = sa.peekValue(
                                    com.android.internal.R.styleable.PreferenceHeader_summary);
                            if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                                if (tv.resourceId != 0) {
                                    tile.summaryRes = tv.resourceId;
                                } else {
                                    tile.summary = tv.string;
                                }
                            }
                            tile.iconRes = sa.getResourceId(
                                    com.android.internal.R.styleable.PreferenceHeader_icon, 0);
                            tile.fragment = sa.getString(
                                    com.android.internal.R.styleable.PreferenceHeader_fragment);
                            sa.recycle();

                            if (curBundle == null) {
                                curBundle = new Bundle();
                            }

                            final int innerDepth2 = parser.getDepth();
                            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                                    && (type != XmlPullParser.END_TAG || parser.getDepth() > innerDepth2)) {
                                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                                    continue;
                                }

                                String innerNodeName2 = parser.getName();
                                if (innerNodeName2.equals("extra")) {
                                    getResources().parseBundleExtra("extra", attrs, curBundle);
                                    XmlUtils.skipCurrentTag(parser);

                                } else if (innerNodeName2.equals("intent")) {
                                    tile.intent = Intent.parseIntent(getResources(), parser, attrs);

                                } else {
                                    XmlUtils.skipCurrentTag(parser);
                                }
                            }

                            if (curBundle.size() > 0) {
                                tile.fragmentArguments = curBundle;
                                curBundle = null;
                            }

                            category.addTile(tile);

                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }

                    target.add(category);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }

        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing categories", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing categories", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    private void updateTilesList(List<DashboardCategory> target) {
        final boolean showDev = mDevelopmentPreferences.getBoolean(
                DevelopmentSettings.PREF_SHOW,
                android.os.Build.TYPE.equals("eng"));

        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);

        final int size = target.size();
        for (int i = 0; i < size; i++) {

            DashboardCategory category = target.get(i);

            // Ids are integers, so downcasting is ok
            int id = (int) category.id;
            if (id == R.id.account_settings) {
                insertAccountsTiles(category);
                continue;
            }
            int n = category.getTilesCount() - 1;
            while (n >= 0) {

                DashboardTile tile = category.getTile(n);

                id = (int) tile.id;
                if (id == R.id.operator_settings || id == R.id.manufacturer_settings) {
                    Utils.updateTileToSpecificActivityFromMetaDataOrRemove(this, category, tile);
                } else if (id == R.id.wifi_settings) {
                    // Remove WiFi Settings if WiFi service is not available.
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                        category.removeTile(n);
                    }
                } else if (id == R.id.bluetooth_settings) {
                    // Remove Bluetooth Settings if Bluetooth service is not available.
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                        category.removeTile(n);
                    }
                } else if (id == R.id.data_usage_settings) {
                    // Remove data usage when kernel module not enabled
                    final INetworkManagementService netManager = INetworkManagementService.Stub
                            .asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
                    try {
                        if (!netManager.isBandwidthControlEnabled()) {
                            category.removeTile(n);
                        }
                    } catch (RemoteException e) {
                        // ignored
                    }
                } else if (id == R.id.battery_settings) {
                    // Remove battery settings when battery is not available. (e.g. TV)

                    if (!mBatteryPresent) {
                        category.removeTile(n);
                    }
                } else if (id == R.id.home_settings) {
                    if (!updateHomeSettingTiles(tile)) {
                        category.removeTile(n);
                    }
                } else if (id == R.id.user_settings) {
                    if (!UserHandle.MU_ENABLED
                            || !UserManager.supportsMultipleUsers()
                            || Utils.isMonkeyRunning()) {
                        category.removeTile(n);
                    }
                } else if (id == R.id.nfc_payment_settings) {
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
                        category.removeTile(n);
                    } else {
                        // Only show if NFC is on and we have the HCE feature
                        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
                        if (!adapter.isEnabled() || !getPackageManager().hasSystemFeature(
                                PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
                            category.removeTile(n);
                        }
                    }
                } else if (id == R.id.print_settings) {
                    boolean hasPrintingSupport = getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_PRINTING);
                    if (!hasPrintingSupport) {
                        category.removeTile(n);
                    }
                } else if (id == R.id.development_settings) {
                    if (!showDev || um.hasUserRestriction(
                            UserManager.DISALLOW_DEBUGGING_FEATURES)) {
                        category.removeTile(n);
                    }
                } else if (id == R.id.account_add) {
                    if (um.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS)) {
                        category.removeTile(n);
                    }
                }

                if (UserHandle.MU_ENABLED && UserHandle.myUserId() != 0
                        && !ArrayUtils.contains(SETTINGS_FOR_RESTRICTED, id)
                        && n < category.getTilesCount()) {
                    category.removeTile(n);
                }

                n--;
            }
        }
    }

    private boolean updateHomeSettingTiles(DashboardTile tile) {
        // Once we decide to show Home settings, keep showing it forever
        SharedPreferences sp = getSharedPreferences(HomeSettings.HOME_PREFS, Context.MODE_PRIVATE);
        if (sp.getBoolean(HomeSettings.HOME_PREFS_DO_SHOW, false)) {
            return true;
        }

        try {
            final ArrayList<ResolveInfo> homeApps = new ArrayList<ResolveInfo>();
            getPackageManager().getHomeActivities(homeApps);
            if (homeApps.size() < 2) {
                // When there's only one available home app, omit this settings
                // category entirely at the top level UI.  If the user just
                // uninstalled the penultimate home app candidiate, we also
                // now tell them about why they aren't seeing 'Home' in the list.
                if (sShowNoHomeNotice) {
                    sShowNoHomeNotice = false;
                    NoHomeDialogFragment.show(this);
                }
                return false;
            } else {
                // Okay, we're allowing the Home settings category.  Tell it, when
                // invoked via this front door, that we'll need to be told about the
                // case when the user uninstalls all but one home app.
                if (tile.fragmentArguments == null) {
                    tile.fragmentArguments = new Bundle();
                }
                tile.fragmentArguments.putBoolean(HomeSettings.HOME_SHOW_NOTICE, true);
            }
        } catch (Exception e) {
            // Can't look up the home activity; bail on configuring the icon
            Log.w(LOG_TAG, "Problem looking up home activity!", e);
        }

        sp.edit().putBoolean(HomeSettings.HOME_PREFS_DO_SHOW, true).apply();
        return true;
    }

    private void insertAccountsTiles(DashboardCategory target) {
        String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();
        List<DashboardTile> dashboardTiles = new ArrayList<DashboardTile>(accountTypes.length);
        for (String accountType : accountTypes) {
            CharSequence label = mAuthenticatorHelper.getLabelForType(this, accountType);
            if (label == null) {
                continue;
            }

            Account[] accounts = AccountManager.get(this).getAccountsByType(accountType);
            boolean skipToAccount = accounts.length == 1
                    && !mAuthenticatorHelper.hasAccountPreferences(accountType);
            DashboardTile accountTile = new DashboardTile();
            accountTile.title = label;
            if (accountTile.extras == null) {
                accountTile.extras = new Bundle();
            }
            if (skipToAccount) {
                accountTile.fragment = AccountSyncSettings.class.getName();
                accountTile.fragmentArguments = new Bundle();
                // Need this for the icon
                accountTile.extras.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accountTile.extras.putParcelable(AccountSyncSettings.ACCOUNT_KEY, accounts[0]);
                accountTile.fragmentArguments.putParcelable(AccountSyncSettings.ACCOUNT_KEY,
                        accounts[0]);
            } else {
                accountTile.fragment = ManageAccountsSettings.class.getName();
                accountTile.fragmentArguments = new Bundle();
                accountTile.extras.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accountTile.fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE,
                        accountType);
                accountTile.fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_LABEL,
                        label.toString());
            }
            dashboardTiles.add(accountTile);
            mAuthenticatorHelper.preloadDrawableForType(this, accountType);
        }

        // Sort by label
        Collections.sort(dashboardTiles, new Comparator<DashboardTile>() {
            @Override
            public int compare(DashboardTile t1, DashboardTile t2) {
                return t1.title.toString().compareTo(t2.title.toString());
            }
        });
        int index = 0;
        for (DashboardTile tile : dashboardTiles) {
            target.addTile(index, tile);
            index++;
        }
        if (!mListeningToAccountUpdates) {
            AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);
            mListeningToAccountUpdates = true;
        }
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
    public void onAccountsUpdated(Account[] accounts) {
        // TODO: watch for package upgrades to invalidate cache; see 7206643
        mAuthenticatorHelper.updateAuthDescriptions(this);
        mAuthenticatorHelper.onAccountsUpdated(this, accounts);
        invalidateCategories();
    }

    public static void requestHomeNotice() {
        sShowNoHomeNotice = true;
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
        if (TextUtils.isEmpty(newText) || mSearchResultsFragment == null) {
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

    private void switchToSearchResultsFragmentIfNeeded() {
        if (mSearchResultsFragment != null) {
            return;
        }
        Fragment current = getFragmentManager().findFragmentById(R.id.prefs);
        if (current != null && current instanceof SearchResultsSummary) {
            mSearchResultsFragment = (SearchResultsSummary) current;
        } else {
            final boolean isShowingSwitchBar = mSwitchBar.isShowing();
            String title = getString(R.string.search_results_title);
            mSearchResultsFragment = (SearchResultsSummary) switchToFragment(
                    SearchResultsSummary.class.getName(), null, false, true, title,
                    !isShowingSwitchBar);
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
}
