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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.CaptionPropertiesFragment;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.AuthenticatorHelper;
import com.android.settings.accounts.ChooseAccountFragment;
import com.android.settings.accounts.ManageAccountsSettings;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.bluetooth.BluetoothEnabler;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.deviceinfo.Memory;
import com.android.settings.deviceinfo.UsbSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.location.LocationSettings;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.WifiEnabler;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SettingsActivity extends Activity
        implements PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        ButtonBarHandler, OnAccountsUpdateListener, FragmentManager.OnBackStackChangedListener {

    private static final String LOG_TAG = "Settings";

    // Constants for state save/restore
    private static final String SAVE_KEY_HEADERS_TAG = ":settings:headers";
    private static final String SAVE_KEY_CURRENT_HEADER_TAG = ":settings:cur_header";

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
     * When starting this activity, the invoking Intent can contain this extra
     * boolean that the header list should not be displayed.  This is most often
     * used in conjunction with {@link #EXTRA_SHOW_FRAGMENT} to launch
     * the activity to display a specific fragment that the user has navigated
     * to.
     */
    public static final String EXTRA_NO_HEADERS = ":settings:no_headers";

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
    protected static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";

    private static final String BACK_STACK_PREFS = ":settings:prefs";

    private static final String META_DATA_KEY_HEADER_ID =
        "com.android.settings.TOP_LEVEL_HEADER_ID";

    private static final String META_DATA_KEY_FRAGMENT_CLASS =
        "com.android.settings.FRAGMENT_CLASS";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private static boolean sShowNoHomeNotice = false;

    private String mFragmentClass;
    private int mTopLevelHeaderId;
    private Header mFirstHeader;
    private Header mSelectedHeader;
    private Header mCurrentHeader;

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
            R.id.home_settings
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
            com.android.settings.accessibility.ToggleContrastPreferenceFragment.class.getName(),
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
            ManageAccountsSettings.class.getName(),
            PrintSettingsFragment.class.getName(),
            PrintJobSettingsFragment.class.getName(),
            TrustedCredentialsSettings.class.getName(),
            PaymentSettings.class.getName(),
            KeyboardLayoutPickerFragment.class.getName(),
            ChooseAccountFragment.class.getName(),
            DashboardSummary.class.getName()
    };

    private SharedPreferences mDevelopmentPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;

    // TODO: Update Call Settings based on airplane mode state.

    protected HashMap<Integer, Integer> mHeaderIndexMap = new HashMap<Integer, Integer>();

    private AuthenticatorHelper mAuthenticatorHelper;
    private boolean mListeningToAccountUpdates;

    private Button mNextButton;

    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean batteryPresent = Utils.isBatteryPresent(intent);

                if (mBatteryPresent != batteryPresent) {
                    mBatteryPresent = batteryPresent;
                    invalidateHeaders();
                }
            }
        }
    };

    private final ArrayList<Header> mHeaders = new ArrayList<Header>();
    private HeaderAdapter mHeaderAdapter;

    private class TitlePair extends Pair<Integer, CharSequence> {

        public TitlePair(Integer first, CharSequence second) {
            super(first, second);
        }
    }

    private final ArrayList<TitlePair> mTitleStack = new ArrayList<TitlePair>();

    private DrawerLayout mDrawerLayout;
    private ListView mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private ActionBar mActionBar;

    private static final int MSG_BUILD_HEADERS = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BUILD_HEADERS: {
                    mHeaders.clear();
                    onBuildHeaders(mHeaders);
                    mHeaderAdapter.notifyDataSetChanged();
                    if (mCurrentHeader != null) {
                        Header mappedHeader = findBestMatchingHeader(mCurrentHeader, mHeaders);
                        if (mappedHeader != null) {
                            setSelectedHeader(mappedHeader);
                        }
                    }
                } break;
            }
        }
    };

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

    private class DrawerListener implements DrawerLayout.DrawerListener {
        @Override
        public void onDrawerOpened(View drawerView) {
            mDrawerToggle.onDrawerOpened(drawerView);
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            mDrawerToggle.onDrawerClosed(drawerView);
            // Cannot process clicks when the App is finishing
            if (isFinishing()) return;
            onHeaderClick(mSelectedHeader);
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            mDrawerToggle.onDrawerStateChanged(newState);
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mDrawerLayout.closeDrawer(mDrawer);
            onListItemClick((ListView)parent, view, position, id);
        }
    }

    private Header findBestMatchingHeader(Header current, ArrayList<Header> from) {
        ArrayList<Header> matches = new ArrayList<Header>();
        for (int j=0; j<from.size(); j++) {
            Header oh = from.get(j);
            if (current == oh || (current.id != HEADER_ID_UNDEFINED && current.id == oh.id)) {
                // Must be this one.
                matches.clear();
                matches.add(oh);
                break;
            }
            if (current.fragment != null) {
                if (current.fragment.equals(oh.fragment)) {
                    matches.add(oh);
                }
            } else if (current.intent != null) {
                if (current.intent.equals(oh.intent)) {
                    matches.add(oh);
                }
            } else if (current.title != null) {
                if (current.title.equals(oh.title)) {
                    matches.add(oh);
                }
            }
        }
        final int NM = matches.size();
        if (NM == 1) {
            return matches.get(0);
        } else if (NM > 1) {
            for (int j=0; j<NM; j++) {
                Header oh = matches.get(j);
                if (current.fragmentArguments != null &&
                        current.fragmentArguments.equals(oh.fragmentArguments)) {
                    return oh;
                }
                if (current.extras != null && current.extras.equals(oh.extras)) {
                    return oh;
                }
                if (current.title != null && current.title.equals(oh.title)) {
                    return oh;
                }
            }
        }
        return null;
    }

    private void invalidateHeaders() {
        if (!mHandler.hasMessages(MSG_BUILD_HEADERS)) {
            mHandler.sendEmptyMessage(MSG_BUILD_HEADERS);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(getIntent().getIntExtra(EXTRA_UI_OPTIONS, 0));
        }

        mAuthenticatorHelper = new AuthenticatorHelper();
        mAuthenticatorHelper.updateAuthDescriptions(this);
        mAuthenticatorHelper.onAccountsUpdated(this, null);

        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mHeaderAdapter= new HeaderAdapter(this, getHeaders(), mAuthenticatorHelper, dpm);

        mDevelopmentPreferences = getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);

        getMetaData();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_main);

        getFragmentManager().addOnBackStackChangedListener(this);

        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);

            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

            mDrawer = (ListView) findViewById(R.id.headers_drawer);
            mDrawer.setAdapter(mHeaderAdapter);
            mDrawer.setOnItemClickListener(new DrawerItemClickListener());
            mDrawer.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                    R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
        }

        String initialFragment = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        Bundle initialArguments = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);

        if (savedInstanceState != null) {
            // We are restarting from a previous saved state; used that to
            // initialize, instead of starting fresh.
            ArrayList<Header> headers =
                    savedInstanceState.getParcelableArrayList(SAVE_KEY_HEADERS_TAG);
            if (headers != null) {
                mHeaders.addAll(headers);
                int curHeader = savedInstanceState.getInt(SAVE_KEY_CURRENT_HEADER_TAG,
                        (int) HEADER_ID_UNDEFINED);
                if (curHeader >= 0 && curHeader < mHeaders.size()) {
                    setSelectedHeader(mHeaders.get(curHeader));
                }
            }

        } else {
            if (initialFragment != null) {
                // If we are just showing a fragment, we want to run in
                // new fragment mode, but don't need to compute and show
                // the headers.
                switchToHeader(initialFragment, initialArguments, true);

                final int initialTitle = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
                if (initialTitle != 0) {
                    setTitle(getText(initialTitle));
                }
            } else {
                // We need to try to build the headers.
                onBuildHeaders(mHeaders);

                // If there are headers, then at this point we need to show
                // them and, depending on the screen, we may also show in-line
                // the currently selected preference fragment.
                if (mHeaders.size() > 0) {
                    if (initialFragment == null) {
                        Header h = onGetInitialHeader();
                        switchToHeader(h, false);
                    } else {
                        switchToHeader(initialFragment, initialArguments, false);
                    }
                }
            }
        }

        // see if we should show Back/Next buttons
        Intent intent = getIntent();
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {

            View buttonBar = findViewById(com.android.internal.R.id.button_bar);
            if (buttonBar != null) {
                buttonBar.setVisibility(View.VISIBLE);

                Button backButton = (Button)findViewById(com.android.internal.R.id.back_button);
                backButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                Button skipButton = (Button)findViewById(com.android.internal.R.id.skip_button);
                skipButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK);
                        finish();
                    }
                });
                mNextButton = (Button)findViewById(com.android.internal.R.id.next_button);
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

        if (!onIsHidingHeaders()) {
            highlightHeader(mTopLevelHeaderId);
        }
    }

    @Override
    public void onBackStackChanged() {
        final int count = getFragmentManager().getBackStackEntryCount() + 1;
        TitlePair pair = null;
        int last;
        while (mTitleStack.size() > count) {
            last = mTitleStack.size() - 1;
            pair = mTitleStack.remove(last);
        }
        // Check if we go back
        if (pair != null) {
            int size = mTitleStack.size();
            if (size > 0) {
                last = mTitleStack.size() - 1;
                pair = mTitleStack.get(last);
                if (pair != null) {
                    final CharSequence title;
                    if (pair.first > 0) {
                        title = getText(pair.first);
                    } else {
                        title = pair.second;
                    }
                    setTitle(title);
                }
            }
        }
    }

    /**
     * Returns the Header list
     */
    private List<Header> getHeaders() {
        return mHeaders;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mHeaders.size() > 0) {
            outState.putParcelableArrayList(SAVE_KEY_HEADERS_TAG, mHeaders);
            if (mCurrentHeader != null) {
                int index = mHeaders.indexOf(mCurrentHeader);
                if (index >= 0) {
                    outState.putInt(SAVE_KEY_CURRENT_HEADER_TAG, index);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                invalidateHeaders();
            }
        };
        mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);

        mHeaderAdapter.resume();
        invalidateHeaders();

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        mDrawerLayout.setDrawerListener(new DrawerListener());
    }

    @Override
    public void onPause() {
        super.onPause();

        mDrawerLayout.setDrawerListener(null);

        unregisterReceiver(mBatteryInfoReceiver);

        mHeaderAdapter.pause();

        mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);

        mDevelopmentPreferencesListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mListeningToAccountUpdates) {
            AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        }
    }

    /**
     * @hide
     */
    protected boolean isValidFragment(String fragmentName) {
        // Almost all fragments are wrapped in this,
        // except for a few that have their own activities.
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
        }
        return false;
    }

    /**
     * When in two-pane mode, switch to the fragment pane to show the given
     * preference fragment.
     *
     * @param header The new header to display.
     * @param validate true means that the fragment's Header needs to be validated
     */
    private void switchToHeader(Header header, boolean validate) {
        if (mCurrentHeader == header) {
            // This is the header we are currently displaying.  Just make sure
            // to pop the stack up to its root state.
            getFragmentManager().popBackStack(BACK_STACK_PREFS,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            mTitleStack.clear();
            if (header.fragment == null) {
                throw new IllegalStateException("can't switch to header that has no fragment");
            }
            switchToHeaderInner(header.fragment, header.fragmentArguments, validate);
            setSelectedHeader(header);
            final CharSequence title;
            if (header.fragment.equals("com.android.settings.dashboard.DashboardSummary")) {
                title = getResources().getString(R.string.settings_label);
            } else {
                title = header.getTitle(getResources());
            }
            final TitlePair pair = new TitlePair(0, title);
            mTitleStack.add(pair);
            setTitle(title);
        }
    }

    private void setSelectedHeader(Header header) {
        mCurrentHeader = header;
        int index = mHeaders.indexOf(header);
        if (mDrawer != null) {
            if (index >= 0) {
                mDrawer.setItemChecked(index, true);
            } else {
                mDrawer.clearChoices();
            }
        }
    }

    public Header onGetInitialHeader() {
        String fragmentClass = getStartingFragmentClass(super.getIntent());
        if (fragmentClass != null) {
            Header header = new Header();
            header.fragment = fragmentClass;
            header.title = getTitle();
            header.fragmentArguments = getIntent().getExtras();
            return header;
        }

        return mFirstHeader;
    }

    /**
     * When in two-pane mode, switch the fragment pane to show the given
     * preference fragment.
     *
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param validate true means that the fragment's Header needs to be validated
     */
    private void switchToHeader(String fragmentName, Bundle args, boolean validate) {
        setSelectedHeader(null);
        switchToHeaderInner(fragmentName, args, validate);
    }

    private void switchToHeaderInner(String fragmentName, Bundle args, boolean validate) {
        getFragmentManager().popBackStack(BACK_STACK_PREFS,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.replace(R.id.prefs, f);
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // If it is not launched from history, then reset to top-level
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            if (mDrawer != null) {
                mDrawer.setSelectionFromTop(0, 0);
            }
        }
    }

    /**
     * Called to determine whether the header list should be hidden.
     * The default implementation returns the
     * value given in {@link #EXTRA_NO_HEADERS} or false if it is not supplied.
     * This is set to false, for example, when the activity is being re-launched
     * to show a particular preference activity.
     */
    public boolean onIsHidingHeaders() {
        return getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false);
    }

    private void highlightHeader(int id) {
        if (id != 0) {
            Integer index = mHeaderIndexMap.get(id);
            if (index != null && mDrawer != null) {
                mDrawer.setItemChecked(index, true);
                if (mDrawer.getVisibility() == View.VISIBLE) {
                    mDrawer.smoothScrollToPosition(index);
                }
            }
        }
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
                                     CharSequence titleText, Fragment resultTo,
                                     int resultRequestCode) {
        startWithFragment(fragmentClass, args, resultTo, resultRequestCode, titleRes, titleText);
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
     * Start a new fragment.
     *
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param resultTo Option fragment that should receive the result of
     *                 the activity launch.
     * @param resultRequestCode If resultTo is non-null, this is the request code in which to
     *                          report the result.
     * @param titleRes Resource ID of string to display for the title of. If the Resource ID is a
     *                 valid one then it will be used to get the title. Otherwise the titleText
     *                 argument will be used as the title.
     * @param titleText string to display for the title of.
     */
    private void startWithFragment(String fragmentName, Bundle args, Fragment resultTo,
                                   int resultRequestCode, int titleRes, CharSequence titleText) {
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        if (resultTo != null) {
            f.setTargetFragment(resultTo, resultRequestCode);
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.prefs, f);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack(BACK_STACK_PREFS);
        transaction.commitAllowingStateLoss();

        final TitlePair pair;
        final CharSequence cs;
        if (titleRes != 0) {
            pair = new TitlePair(titleRes, null);
            cs = getText(titleRes);
        } else {
            pair = new TitlePair(0, titleText);
            cs = titleText;
        }
        setTitle(cs);
        mTitleStack.add(pair);
    }

    /**
     * Called when the activity needs its list of headers build.  By
     * implementing this and adding at least one item to the list, you
     * will cause the activity to run in its modern fragment mode.  Note
     * that this function may not always be called; for example, if the
     * activity has been asked to display a particular fragment without
     * the header list, there is no need to build the headers.
     *
     * <p>Typical implementations will use {@link #loadHeadersFromResource}
     * to fill in the list from a resource.
     *
     * @param headers The list in which to place the headers.
     */
    private void onBuildHeaders(List<Header> headers) {
        loadHeadersFromResource(R.xml.settings_headers, headers);
        updateHeaderList(headers);
    }

    /**
     * Parse the given XML file as a header description, adding each
     * parsed Header into the target list.
     *
     * @param resid The XML resource to load and parse.
     * @param target The list in which the parsed headers should be placed.
     */
    private void loadHeadersFromResource(int resid, List<Header> target) {
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
            if (!"preference-headers".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <preference-headers> tag; found"
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
                if ("header".equals(nodeName)) {
                    Header header = new Header();

                    TypedArray sa = obtainStyledAttributes(
                            attrs, com.android.internal.R.styleable.PreferenceHeader);
                    header.id = sa.getResourceId(
                            com.android.internal.R.styleable.PreferenceHeader_id,
                            (int)HEADER_ID_UNDEFINED);
                    TypedValue tv = sa.peekValue(
                            com.android.internal.R.styleable.PreferenceHeader_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.titleRes = tv.resourceId;
                        } else {
                            header.title = tv.string;
                        }
                    }
                    tv = sa.peekValue(
                            com.android.internal.R.styleable.PreferenceHeader_summary);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.summaryRes = tv.resourceId;
                        } else {
                            header.summary = tv.string;
                        }
                    }
                    header.iconRes = sa.getResourceId(
                            com.android.internal.R.styleable.PreferenceHeader_icon, 0);
                    header.fragment = sa.getString(
                            com.android.internal.R.styleable.PreferenceHeader_fragment);
                    sa.recycle();

                    if (curBundle == null) {
                        curBundle = new Bundle();
                    }

                    final int innerDepth = parser.getDepth();
                    while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                            && (type != XmlPullParser.END_TAG || parser.getDepth() > innerDepth)) {
                        if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                            continue;
                        }

                        String innerNodeName = parser.getName();
                        if (innerNodeName.equals("extra")) {
                            getResources().parseBundleExtra("extra", attrs, curBundle);
                            XmlUtils.skipCurrentTag(parser);

                        } else if (innerNodeName.equals("intent")) {
                            header.intent = Intent.parseIntent(getResources(), parser, attrs);

                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }

                    if (curBundle.size() > 0) {
                        header.fragmentArguments = curBundle;
                        curBundle = null;
                    }

                    target.add(header);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }

        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing headers", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    private void updateHeaderList(List<Header> target) {
        final boolean showDev = mDevelopmentPreferences.getBoolean(
                DevelopmentSettings.PREF_SHOW,
                android.os.Build.TYPE.equals("eng"));
        int i = 0;

        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        mHeaderIndexMap.clear();
        while (i < target.size()) {
            Header header = target.get(i);
            // Ids are integers, so downcasting
            int id = (int) header.id;
            if (id == R.id.operator_settings || id == R.id.manufacturer_settings) {
                Utils.updateHeaderToSpecificActivityFromMetaDataOrRemove(this, target, header);
            } else if (id == R.id.wifi_settings) {
                // Remove WiFi Settings if WiFi service is not available.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                    target.remove(i);
                }
            } else if (id == R.id.bluetooth_settings) {
                // Remove Bluetooth Settings if Bluetooth service is not available.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    target.remove(i);
                }
            } else if (id == R.id.data_usage_settings) {
                // Remove data usage when kernel module not enabled
                final INetworkManagementService netManager = INetworkManagementService.Stub
                        .asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
                try {
                    if (!netManager.isBandwidthControlEnabled()) {
                        target.remove(i);
                    }
                } catch (RemoteException e) {
                    // ignored
                }
            } else if (id == R.id.battery_settings) {
                // Remove battery settings when battery is not available. (e.g. TV)

                if (!mBatteryPresent) {
                    target.remove(i);
                }
            } else if (id == R.id.account_settings) {
                int headerIndex = i + 1;
                i = insertAccountsHeaders(target, headerIndex);
            } else if (id == R.id.home_settings) {
                if (!updateHomeSettingHeaders(header)) {
                    target.remove(i);
                }
            } else if (id == R.id.user_settings) {
                if (!UserHandle.MU_ENABLED
                        || !UserManager.supportsMultipleUsers()
                        || Utils.isMonkeyRunning()) {
                    target.remove(i);
                }
            } else if (id == R.id.nfc_payment_settings) {
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
                    target.remove(i);
                } else {
                    // Only show if NFC is on and we have the HCE feature
                    NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
                    if (!adapter.isEnabled() || !getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
                        target.remove(i);
                    }
                }
            } else if (id == R.id.development_settings) {
                if (!showDev) {
                    target.remove(i);
                }
            } else if (id == R.id.account_add) {
                if (um.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS)) {
                    target.remove(i);
                }
            }

            if (i < target.size() && target.get(i) == header
                    && UserHandle.MU_ENABLED && UserHandle.myUserId() != 0
                    && !ArrayUtils.contains(SETTINGS_FOR_RESTRICTED, id)) {
                target.remove(i);
            }

            // Increment if the current one wasn't removed by the Utils code.
            if (i < target.size() && target.get(i) == header) {
                // Hold on to the first header, when we need to reset to the top-level
                if (mFirstHeader == null &&
                        HeaderAdapter.getHeaderType(header) != HeaderAdapter.HEADER_TYPE_CATEGORY) {
                    mFirstHeader = header;
                }
                mHeaderIndexMap.put(id, i);
                i++;
            }
        }
    }

    private int insertAccountsHeaders(List<Header> target, int headerIndex) {
        String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();
        List<Header> accountHeaders = new ArrayList<Header>(accountTypes.length);
        for (String accountType : accountTypes) {
            CharSequence label = mAuthenticatorHelper.getLabelForType(this, accountType);
            if (label == null) {
                continue;
            }

            Account[] accounts = AccountManager.get(this).getAccountsByType(accountType);
            boolean skipToAccount = accounts.length == 1
                    && !mAuthenticatorHelper.hasAccountPreferences(accountType);
            Header accHeader = new Header();
            accHeader.title = label;
            if (accHeader.extras == null) {
                accHeader.extras = new Bundle();
            }
            if (skipToAccount) {
                accHeader.fragment = AccountSyncSettings.class.getName();
                accHeader.fragmentArguments = new Bundle();
                // Need this for the icon
                accHeader.extras.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accHeader.extras.putParcelable(AccountSyncSettings.ACCOUNT_KEY, accounts[0]);
                accHeader.fragmentArguments.putParcelable(AccountSyncSettings.ACCOUNT_KEY,
                        accounts[0]);
            } else {
                accHeader.fragment = ManageAccountsSettings.class.getName();
                accHeader.fragmentArguments = new Bundle();
                accHeader.extras.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accHeader.fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE,
                        accountType);
                    accHeader.fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_LABEL,
                            label.toString());
            }
            accountHeaders.add(accHeader);
            mAuthenticatorHelper.preloadDrawableForType(this, accountType);
        }

        // Sort by label
        Collections.sort(accountHeaders, new Comparator<Header>() {
            @Override
            public int compare(Header h1, Header h2) {
                return h1.title.toString().compareTo(h2.title.toString());
            }
        });

        for (Header header : accountHeaders) {
            target.add(headerIndex++, header);
        }
        if (!mListeningToAccountUpdates) {
            AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);
            mListeningToAccountUpdates = true;
        }
        return headerIndex;
    }

    private boolean updateHomeSettingHeaders(Header header) {
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
                if (header.fragmentArguments == null) {
                    header.fragmentArguments = new Bundle();
                }
                header.fragmentArguments.putBoolean(HomeSettings.HOME_SHOW_NOTICE, true);
            }
        } catch (Exception e) {
            // Can't look up the home activity; bail on configuring the icon
            Log.w(LOG_TAG, "Problem looking up home activity!", e);
        }

        sp.edit().putBoolean(HomeSettings.HOME_PREFS_DO_SHOW, true).apply();
        return true;
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mTopLevelHeaderId = ai.metaData.getInt(META_DATA_KEY_HEADER_ID);
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

    public static class NoHomeDialogFragment extends DialogFragment {
        public static void show(Activity parent) {
            final NoHomeDialogFragment dialog = new NoHomeDialogFragment();
            dialog.show(parent.getFragmentManager(), null);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.only_one_home_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        static final int HEADER_TYPE_CATEGORY = 0;
        static final int HEADER_TYPE_NORMAL = 1;
        static final int HEADER_TYPE_SWITCH = 2;
        static final int HEADER_TYPE_BUTTON = 3;
        private static final int HEADER_TYPE_COUNT = HEADER_TYPE_BUTTON + 1;

        private final WifiEnabler mWifiEnabler;
        private final BluetoothEnabler mBluetoothEnabler;
        private AuthenticatorHelper mAuthHelper;
        private DevicePolicyManager mDevicePolicyManager;

        private static class HeaderViewHolder {
            ImageView mIcon;
            TextView mTitle;
            TextView mSummary;
            Switch mSwitch;
            ImageButton mButton;
            View mDivider;
        }

        private LayoutInflater mInflater;

        static int getHeaderType(Header header) {
            if (header.fragment == null && header.intent == null) {
                return HEADER_TYPE_CATEGORY;
            } else if (header.id == R.id.wifi_settings || header.id == R.id.bluetooth_settings) {
                return HEADER_TYPE_SWITCH;
            } else if (header.id == R.id.security_settings) {
                return HEADER_TYPE_BUTTON;
            } else {
                return HEADER_TYPE_NORMAL;
            }
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false; // because of categories
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != HEADER_TYPE_CATEGORY;
        }

        @Override
        public int getViewTypeCount() {
            return HEADER_TYPE_COUNT;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public HeaderAdapter(Context context, List<Header> objects,
                AuthenticatorHelper authenticatorHelper, DevicePolicyManager dpm) {
            super(context, 0, objects);

            mAuthHelper = authenticatorHelper;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Temp Switches provided as placeholder until the adapter replaces these with actual
            // Switches inflated from their layouts. Must be done before adapter is set in super
            mWifiEnabler = new WifiEnabler(context, new Switch(context));
            mBluetoothEnabler = new BluetoothEnabler(context, new Switch(context));
            mDevicePolicyManager = dpm;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            Header header = getItem(position);
            int headerType = getHeaderType(header);
            View view = null;

            if (convertView == null) {
                holder = new HeaderViewHolder();
                switch (headerType) {
                    case HEADER_TYPE_CATEGORY:
                        view = new TextView(getContext(), null,
                                android.R.attr.listSeparatorTextViewStyle);
                        holder.mTitle = (TextView) view;
                        break;

                    case HEADER_TYPE_SWITCH:
                        view = mInflater.inflate(R.layout.preference_header_switch_item, parent,
                                false);
                        holder.mIcon = (ImageView) view.findViewById(R.id.icon);
                        holder.mTitle = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.mSummary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        holder.mSwitch = (Switch) view.findViewById(R.id.switchWidget);
                        break;

                    case HEADER_TYPE_BUTTON:
                        view = mInflater.inflate(R.layout.preference_header_button_item, parent,
                                false);
                        holder.mIcon = (ImageView) view.findViewById(R.id.icon);
                        holder.mTitle = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.mSummary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        holder.mButton = (ImageButton) view.findViewById(R.id.buttonWidget);
                        holder.mDivider = view.findViewById(R.id.divider);
                        break;

                    case HEADER_TYPE_NORMAL:
                        view = mInflater.inflate(
                                R.layout.preference_header_item, parent,
                                false);
                        holder.mIcon = (ImageView) view.findViewById(R.id.icon);
                        holder.mTitle = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.mSummary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        break;
                }
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            // All view fields must be updated every time, because the view may be recycled
            switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    holder.mTitle.setText(header.getTitle(getContext().getResources()));
                    break;

                case HEADER_TYPE_SWITCH:
                    // Would need a different treatment if the main menu had more switches
                    if (header.id == R.id.wifi_settings) {
                        mWifiEnabler.setSwitch(holder.mSwitch);
                    } else {
                        mBluetoothEnabler.setSwitch(holder.mSwitch);
                    }
                    updateCommonHeaderView(header, holder);
                    break;

                case HEADER_TYPE_BUTTON:
                    if (header.id == R.id.security_settings) {
                        boolean hasCert = DevicePolicyManager.hasAnyCaCertsInstalled();
                        if (hasCert) {
                            holder.mButton.setVisibility(View.VISIBLE);
                            holder.mDivider.setVisibility(View.VISIBLE);
                            boolean isManaged = mDevicePolicyManager.getDeviceOwner() != null;
                            if (isManaged) {
                                holder.mButton.setImageResource(R.drawable.ic_settings_about);
                            } else {
                                holder.mButton.setImageResource(
                                        android.R.drawable.stat_notify_error);
                            }
                            holder.mButton.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(
                                            android.provider.Settings.ACTION_MONITORING_CERT_INFO);
                                    getContext().startActivity(intent);
                                }
                            });
                        } else {
                            holder.mButton.setVisibility(View.GONE);
                            holder.mDivider.setVisibility(View.GONE);
                        }
                    }
                    updateCommonHeaderView(header, holder);
                    break;

                case HEADER_TYPE_NORMAL:
                    updateCommonHeaderView(header, holder);
                    break;
            }

            return view;
        }

        private void updateCommonHeaderView(Header header, HeaderViewHolder holder) {
                if (header.extras != null
                        && header.extras.containsKey(ManageAccountsSettings.KEY_ACCOUNT_TYPE)) {
                    String accType = header.extras.getString(
                            ManageAccountsSettings.KEY_ACCOUNT_TYPE);
                    Drawable icon = mAuthHelper.getDrawableForType(getContext(), accType);
                    setHeaderIcon(holder, icon);
                } else {
                    holder.mIcon.setImageResource(header.iconRes);
                }
                holder.mTitle.setText(header.getTitle(getContext().getResources()));
                CharSequence summary = header.getSummary(getContext().getResources());
                if (!TextUtils.isEmpty(summary)) {
                    holder.mSummary.setVisibility(View.VISIBLE);
                    holder.mSummary.setText(summary);
                } else {
                    holder.mSummary.setVisibility(View.GONE);
                }
            }

        private void setHeaderIcon(HeaderViewHolder holder, Drawable icon) {
            ViewGroup.LayoutParams lp = holder.mIcon.getLayoutParams();
            lp.width = getContext().getResources().getDimensionPixelSize(
                    R.dimen.header_icon_width);
            lp.height = lp.width;
            holder.mIcon.setLayoutParams(lp);
            holder.mIcon.setImageDrawable(icon);
        }

        public void resume() {
            mWifiEnabler.resume();
            mBluetoothEnabler.resume();
        }

        public void pause() {
            mWifiEnabler.pause();
            mBluetoothEnabler.pause();
        }
    }

    private void onListItemClick(ListView l, View v, int position, long id) {
        if (!isResumed()) {
            return;
        }
        Object item = mHeaderAdapter.getItem(position);
        if (item instanceof Header) {
            mSelectedHeader = (Header) item;
        }
    }

    /**
     * Called when the user selects an item in the header list.  The default
     * implementation will call either
     * {@link #startWithFragment(String, android.os.Bundle, android.app.Fragment, int, int, CharSequence)}
     * or {@link #switchToHeader(com.android.settings.SettingsActivity.Header, boolean)}
     * as appropriate.
     *
     * @param header The header that was selected.
     */
    private void onHeaderClick(Header header) {
        if (header == null) return;
        if (header.fragment != null) {
            switchToHeader(header, false);
        } else if (header.intent != null) {
            startActivity(header.intent);
        }
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
        invalidateHeaders();
    }

    public static void requestHomeNotice() {
        sShowNoHomeNotice = true;
    }

    /**
     * Default value for {@link Header#id Header.id} indicating that no
     * identifier value is set.  All other values (including those below -1)
     * are valid.
     */
    private static final long HEADER_ID_UNDEFINED = -1;

    /**
     * Description of a single Header item that the user can select.
     */
    static final class Header implements Parcelable {
        /**
         * Identifier for this header, to correlate with a new list when
         * it is updated.  The default value is
         * {@link SettingsActivity#HEADER_ID_UNDEFINED}, meaning no id.
         * @attr ref android.R.styleable#PreferenceHeader_id
         */
        public long id = HEADER_ID_UNDEFINED;

        /**
         * Resource ID of title of the header that is shown to the user.
         * @attr ref android.R.styleable#PreferenceHeader_title
         */
        public int titleRes;

        /**
         * Title of the header that is shown to the user.
         * @attr ref android.R.styleable#PreferenceHeader_title
         */
        public CharSequence title;

        /**
         * Resource ID of optional summary describing what this header controls.
         * @attr ref android.R.styleable#PreferenceHeader_summary
         */
        public int summaryRes;

        /**
         * Optional summary describing what this header controls.
         * @attr ref android.R.styleable#PreferenceHeader_summary
         */
        public CharSequence summary;

        /**
         * Optional icon resource to show for this header.
         * @attr ref android.R.styleable#PreferenceHeader_icon
         */
        public int iconRes;

        /**
         * Full class name of the fragment to display when this header is
         * selected.
         * @attr ref android.R.styleable#PreferenceHeader_fragment
         */
        public String fragment;

        /**
         * Optional arguments to supply to the fragment when it is
         * instantiated.
         */
        public Bundle fragmentArguments;

        /**
         * Intent to launch when the preference is selected.
         */
        public Intent intent;

        /**
         * Optional additional data for use by subclasses of the activity
         */
        public Bundle extras;

        public Header() {
            // Empty
        }

        /**
         * Return the currently set title.  If {@link #titleRes} is set,
         * this resource is loaded from <var>res</var> and returned.  Otherwise
         * {@link #title} is returned.
         */
        public CharSequence getTitle(Resources res) {
            if (titleRes != 0) {
                return res.getText(titleRes);
            }
            return title;
        }

        /**
         * Return the currently set summary.  If {@link #summaryRes} is set,
         * this resource is loaded from <var>res</var> and returned.  Otherwise
         * {@link #summary} is returned.
         */
        public CharSequence getSummary(Resources res) {
            if (summaryRes != 0) {
                return res.getText(summaryRes);
            }
            return summary;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id);
            dest.writeInt(titleRes);
            TextUtils.writeToParcel(title, dest, flags);
            dest.writeInt(summaryRes);
            TextUtils.writeToParcel(summary, dest, flags);
            dest.writeInt(iconRes);
            dest.writeString(fragment);
            dest.writeBundle(fragmentArguments);
            if (intent != null) {
                dest.writeInt(1);
                intent.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
            dest.writeBundle(extras);
        }

        public void readFromParcel(Parcel in) {
            id = in.readLong();
            titleRes = in.readInt();
            title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            summaryRes = in.readInt();
            summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            iconRes = in.readInt();
            fragment = in.readString();
            fragmentArguments = in.readBundle();
            if (in.readInt() != 0) {
                intent = Intent.CREATOR.createFromParcel(in);
            }
            extras = in.readBundle();
        }

        Header(Parcel in) {
            readFromParcel(in);
        }

        public static final Creator<Header> CREATOR = new Creator<Header>() {
            public Header createFromParcel(Parcel source) {
                return new Header(source);
            }
            public Header[] newArray(int size) {
                return new Header[size];
            }
        };
    }
}
