/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.notificationlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NotificationLightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, View.OnLongClickListener {
    private static final String TAG = "NotificationLightSettings";
    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR = "notification_light_pulse_default_color";
    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON = "notification_light_pulse_default_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF = "notification_light_pulse_default_led_off";
    private static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE = "notification_light_pulse_custom_enable";
    private static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES = "notification_light_pulse_custom_values";
    private static final String NOTIFICATION_LIGHT_PULSE = "notification_light_pulse";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_COLOR = "notification_light_pulse_call_color";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_ON = "notification_light_pulse_call_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF = "notification_light_pulse_call_led_off";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR = "notification_light_pulse_vmail_color";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON = "notification_light_pulse_vmail_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF = "notification_light_pulse_vmail_led_off";
    private static final String PULSE_PREF = "pulse_enabled";
    private static final String DEFAULT_PREF = "default";
    private static final String CUSTOM_PREF = "custom_enabled";
    private static final String MISSED_CALL_PREF = "missed_call";
    private static final String VOICEMAIL_PREF = "voicemail";
    public static final int ACTION_TEST = 0;
    public static final int ACTION_DELETE = 1;
    private static final int MENU_ADD = 0;
    private static final int DIALOG_APPS = 0;
    private int mDefaultColor;
    private int mDefaultLedOn;
    private int mDefaultLedOff;
    private List<ResolveInfo> mInstalledApps;
    private PackageManager mPackageManager;
    private boolean mCustomEnabled;
    private boolean mLightEnabled;
    private boolean mVoiceCapable;
    private ApplicationLightPreference mDefaultPref;
    private ApplicationLightPreference mCallPref;
    private ApplicationLightPreference mVoicemailPref;
    private CheckBoxPreference mCustomEnabledPref;
    private Menu mMenu;
    AppAdapter mAppAdapter;
    private String mApplicationList;
    private Map<String, Application> mApplications;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_light_settings);

        Resources resources = getResources();
        mDefaultColor = resources.getColor(
                com.android.internal.R.color.config_defaultNotificationColor);
        mDefaultLedOn = resources.getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOn);
        mDefaultLedOff = resources.getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOff);

        // Get launch-able applications
        mPackageManager = getPackageManager();
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mInstalledApps = mPackageManager.queryIntentActivities(mainIntent, 0);
        mAppAdapter = new AppAdapter(mInstalledApps);
        mAppAdapter.update();

        mApplications = new HashMap<String, Application>();

        // Determine if the device has voice capabilities
        mVoiceCapable = (((TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE)).getPhoneType()
                != TelephonyManager.PHONE_TYPE_NONE);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDefault();
        refreshCustomApplications();
        setCustomEnabled();
    }

    private void refreshDefault() {
        ContentResolver resolver = getContentResolver();
        int color = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, mDefaultColor);
        int timeOn = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, mDefaultLedOn);
        int timeOff = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, mDefaultLedOff);
        mLightEnabled = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE, 0) == 1;
        mCustomEnabled = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE, 0) == 1;

        // Get Missed call and Voicemail values
        int callColor = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_COLOR, mDefaultColor);
        int callTimeOn = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_LED_ON, mDefaultLedOn);
        int callTimeOff = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF, mDefaultLedOff);
        int vmailColor = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, mDefaultColor);
        int vmailTimeOn = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON, mDefaultLedOn);
        int vmailTimeOff = Settings.System.getInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF, mDefaultLedOff);

        PreferenceScreen prefSet = getPreferenceScreen();
        PreferenceGroup generalPrefs = (PreferenceGroup) prefSet.findPreference("general_section");
        if (generalPrefs != null) {

            // Pulse preference
            CheckBoxPreference cPref = (CheckBoxPreference) prefSet.findPreference(PULSE_PREF);
            cPref.setChecked(mLightEnabled);
            cPref.setOnPreferenceChangeListener(this);

            // Default preference
            mDefaultPref = (ApplicationLightPreference) prefSet.findPreference(DEFAULT_PREF);
            mDefaultPref.setAllValues(color, timeOn, timeOff);
            mDefaultPref.setEnabled(mLightEnabled);
            mDefaultPref.setOnPreferenceChangeListener(this);

            // Custom enabled preference
            mCustomEnabledPref = (CheckBoxPreference) prefSet.findPreference(CUSTOM_PREF);
            mCustomEnabledPref.setChecked(mCustomEnabled);
            mCustomEnabledPref.setEnabled(mLightEnabled);
            mCustomEnabledPref.setOnPreferenceChangeListener(this);
        }

        PreferenceGroup phonePrefs = (PreferenceGroup) prefSet.findPreference("phone_list");
        if (phonePrefs != null) {

            // Missed call and Voicemail preferences
            // Should only show on devices with a voice capabilities
            if (!mVoiceCapable) {
                prefSet.removePreference(phonePrefs);
            } else {
                mCallPref = (ApplicationLightPreference) prefSet.findPreference(MISSED_CALL_PREF);
                mCallPref.setAllValues(callColor, callTimeOn, callTimeOff);
                mCallPref.setEnabled(mCustomEnabled);
                mCallPref.setOnPreferenceChangeListener(this);

                mVoicemailPref = (ApplicationLightPreference) prefSet.findPreference(VOICEMAIL_PREF);
                mVoicemailPref.setAllValues(vmailColor, vmailTimeOn, vmailTimeOff);
                mVoicemailPref.setEnabled(mCustomEnabled);
                mVoicemailPref.setOnPreferenceChangeListener(this);
            }
        }

    }

    private void refreshCustomApplications() {
        Context context = getActivity();

        if (!parseApplicationList()) {
            return;
        }

        // Add the Application Preferences
        final PreferenceScreen prefSet = getPreferenceScreen();
        final PackageManager pm = getPackageManager();
        final PreferenceGroup appList = (PreferenceGroup) prefSet.findPreference("applications_list");

        if (appList != null) {
            final Map<CharSequence, ApplicationLightPreference> prefs =
                    new TreeMap<CharSequence, ApplicationLightPreference>();

            appList.removeAll();

            for (Application i : mApplications.values()) {
                try {
                    PackageInfo info = pm.getPackageInfo(i.name, PackageManager.GET_META_DATA);
                    ApplicationLightPreference pref =
                            new ApplicationLightPreference(context, this, i.color, i.timeon, i.timeoff);
                    final CharSequence label = info.applicationInfo.loadLabel(pm);

                    pref.setKey(i.name);
                    pref.setTitle(label);
                    pref.setIcon(info.applicationInfo.loadIcon(pm));
                    // Does not fit on low res devices, we need it so we hide the view in the preference
                    pref.setSummary(i.name);
                    pref.setPersistent(false);
                    pref.setOnPreferenceChangeListener(this);

                    prefs.put(label, pref);
                } catch (NameNotFoundException e) {
                    // Do nothing
                }
            }

            for (ApplicationLightPreference pref : prefs.values()) {
                appList.addPreference(pref);
            }
        }
    }

    private void setCustomEnabled() {

        Boolean enabled = mCustomEnabled && mLightEnabled;

        // Phone related preferences
        if (mVoiceCapable) {
            mCallPref.setEnabled(enabled);
            mVoicemailPref.setEnabled(enabled);
        }

        // Custom applications
        PreferenceScreen prefSet = getPreferenceScreen();
        PreferenceGroup appList = (PreferenceGroup) prefSet.findPreference("applications_list");
        if (appList != null) {
            appList.setEnabled(enabled);
            setHasOptionsMenu(enabled);
        }
    }

    private void addCustomApplication(String packageName) {
        Application app = mApplications.get(packageName);
        if (app == null) {
            app = new Application(packageName, mDefaultColor, mDefaultLedOn, mDefaultLedOff);
            mApplications.put(packageName, app);
            saveApplicationList(false);
            refreshCustomApplications();
        }
    }

    private void removeCustomApplication(String packageName) {
        if (mApplications.remove(packageName) != null) {
            saveApplicationList(false);
            refreshCustomApplications();
        }
    }

    private boolean parseApplicationList() {
        final String baseString = Settings.System.getString(getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES);

        if (TextUtils.equals(mApplicationList, baseString)) {
            return false;
        }

        mApplicationList = baseString;
        mApplications.clear();

        if (baseString != null) {
            final String[] array = TextUtils.split(baseString, "\\|");
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                Application app = Application.fromString(item);
                if (app != null) {
                    mApplications.put(app.name, app);
                }
            }
        }

        return true;
    }

    private void saveApplicationList(boolean preferencesUpdated) {
        List<String> settings = new ArrayList<String>();
        for (Application app : mApplications.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            mApplicationList = value;
        }
        Settings.System.putString(getContentResolver(),
                                  Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES, value);
    }

    /**
     * Updates the default or application specific notification settings.
     *
     * @param application Package name of application specific settings to update
     * @param color
     * @param timeon
     * @param timeoff
     */
    protected void updateValues(String application, Integer color, Integer timeon, Integer timeoff) {
        ContentResolver resolver = getContentResolver();

        if (application.equals(DEFAULT_PREF)) {
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, color);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, timeon);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, timeoff);
            refreshDefault();
            return;
        } else if (application.equals(MISSED_CALL_PREF)) {
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_COLOR, color);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_LED_ON, timeon);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF, timeoff);
            refreshDefault();
            return;
        } else if (application.equals(VOICEMAIL_PREF)) {
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, color);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON, timeon);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF, timeoff);
            refreshDefault();
            return;
        }

        // Find the custom app and sets its new values
        Application app = mApplications.get(application);
        if (app != null) {
            app.color = color;
            app.timeon = timeon;
            app.timeoff = timeoff;
            saveApplicationList(true);
        }
    }

    public boolean onLongClick(View v) {
        final TextView tView;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if ((v != null) && ((tView = (TextView) v.findViewById(android.R.id.summary)) != null)) {
            builder.setTitle(R.string.dialog_delete_title);
            builder.setMessage(R.string.dialog_delete_message);
            builder.setIconAttribute(android.R.attr.alertDialogIcon);
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeCustomApplication(tView.getText().toString());
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
            return true;
        }

        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String key = preference.getKey();

        if (PULSE_PREF.equals(key)) {
            mLightEnabled = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    mLightEnabled ? 1 : 0);
            mDefaultPref.setEnabled(mLightEnabled);
            mCustomEnabledPref.setEnabled(mLightEnabled);
            setCustomEnabled();
        } else if (CUSTOM_PREF.equals(key)) {
            mCustomEnabled = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                    mCustomEnabled ? 1 : 0);
            setCustomEnabled();
        } else {
            ApplicationLightPreference tPref = (ApplicationLightPreference) preference;
            updateValues(key, tPref.getColor(), tPref.getOnValue(), tPref.getOffValue());
        }

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu = menu;
        mMenu.add(0, MENU_ADD, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                showDialog(DIALOG_APPS);
                return true;
        }
        return false;
    }

    /**
     * Utility classes and supporting methods
     */
    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Dialog dialog;
        switch (id) {
            case DIALOG_APPS:
                final ListView list = new ListView(getActivity());
                list.setAdapter(mAppAdapter);

                builder.setTitle(R.string.profile_choose_app);
                builder.setView(list);
                dialog = builder.create();

                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Add empty application definition, the user will be able to edit it later
                        AppItem info = (AppItem) parent.getItemAtPosition(position);
                        addCustomApplication(info.packageName);
                        dialog.cancel();
                    }
                });
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    /**
     * Application class
     */
    private static class Application {
        public String name;
        public Integer color;
        public Integer timeon;
        public Integer timeoff;

        /**
         * Stores all the application values in one call
         * @param name
         * @param color
         * @param timeon
         * @param timeoff
         */
        public Application(String name, Integer color, Integer timeon, Integer timeoff) {
            this.name = name;
            this.color = color;
            this.timeon = timeon;
            this.timeoff = timeoff;
        }

        public Application() {
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append("=");
            builder.append(color);
            builder.append(";");
            builder.append(timeon);
            builder.append(";");
            builder.append(timeoff);
            return builder.toString();
        }

        public static Application fromString(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }
            String[] app = value.split("=", -1);
            if (app.length != 2)
                return null;

            String[] values = app[1].split(";", -1);
            if (values.length != 3)
                return null;

            try {
                Application item = new Application(app[0], Integer.parseInt(values[0]), Integer
                        .parseInt(values[1]), Integer.parseInt(values[2]));
                return item;
            } catch (NumberFormatException e) {
                return null;
            }
        }

    };

    /**
     * AppItem class
     */
    class AppItem implements Comparable<AppItem> {
        CharSequence title;
        String packageName;
        Drawable icon;

        @Override
        public int compareTo(AppItem another) {
            return this.title.toString().compareTo(another.title.toString());
        }
    }

    /**
     * AppAdapter class
     */
    class AppAdapter extends BaseAdapter {
        protected List<ResolveInfo> mInstalledAppInfo;
        protected List<AppItem> mInstalledApps = new LinkedList<AppItem>();

        private void reloadList() {
            final Handler handler = new Handler();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    synchronized (mInstalledApps) {
                        mInstalledApps.clear();
                        for (ResolveInfo info : mInstalledAppInfo) {
                            final AppItem item = new AppItem();
                            item.title = info.loadLabel(mPackageManager);
                            item.icon = info.loadIcon(mPackageManager);
                            item.packageName = info.activityInfo.packageName;
                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    int index = Collections.binarySearch(mInstalledApps, item);
                                    if (index < 0) {
                                        index = -index - 1;
                                        mInstalledApps.add(index, item);
                                    }
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    }
                }
            }).start();
        }

        public AppAdapter(List<ResolveInfo> installedAppsInfo) {
            mInstalledAppInfo = installedAppsInfo;
        }

        public void update() {
            reloadList();
        }

        @Override
        public int getCount() {
            return mInstalledApps.size();
        }

        @Override
        public AppItem getItem(int position) {
            return mInstalledApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mInstalledApps.get(position).packageName.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.preference_icon, null, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.title = (TextView) convertView.findViewById(com.android.internal.R.id.title);
                holder.summary = (TextView) convertView.findViewById(com.android.internal.R.id.summary);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            }
            AppItem applicationInfo = getItem(position);

            if (holder.title != null) {
                holder.title.setText(applicationInfo.title);
            }
            if (holder.summary != null) {
                holder.summary.setVisibility(View.GONE);
            }
            if (holder.icon != null) {
                Drawable loadIcon = applicationInfo.icon;
                holder.icon.setImageDrawable(loadIcon);
            }
            return convertView;
        }
    }

    static class ViewHolder {
        TextView title;
        TextView summary;
        ImageView icon;
    }
}
