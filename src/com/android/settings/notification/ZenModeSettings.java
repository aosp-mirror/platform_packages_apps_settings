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

package com.android.settings.notification;

import static com.android.settings.notification.ZenModeDowntimeDaysSelection.DAYS;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ScrollView;
import android.widget.TimePicker;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class ZenModeSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = "ZenModeSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_ZEN_MODE = "zen_mode";
    private static final String KEY_IMPORTANT = "important";
    private static final String KEY_CALLS = "calls";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_STARRED = "starred";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_ALARM_INFO = "alarm_info";

    private static final String KEY_DOWNTIME = "downtime";
    private static final String KEY_DAYS = "days";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_DOWNTIME_MODE = "downtime_mode";

    private static final String KEY_AUTOMATION = "automation";
    private static final String KEY_ENTRY = "entry";
    private static final String KEY_CONDITION_PROVIDERS = "manage_condition_providers";

    private static final SettingPrefWithCallback PREF_ZEN_MODE = new SettingPrefWithCallback(
            SettingPref.TYPE_GLOBAL, KEY_ZEN_MODE, Global.ZEN_MODE, Global.ZEN_MODE_OFF,
            Global.ZEN_MODE_OFF, Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS,
            Global.ZEN_MODE_NO_INTERRUPTIONS) {
        protected String getCaption(Resources res, int value) {
            switch (value) {
                case Global.ZEN_MODE_NO_INTERRUPTIONS:
                    return res.getString(R.string.zen_mode_option_no_interruptions);
                case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                    return res.getString(R.string.zen_mode_option_important_interruptions);
                default:
                    return res.getString(R.string.zen_mode_option_off);
            }
        }
    };

    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("EEE");

    private static SparseArray<String> allKeyTitles(Context context) {
        final SparseArray<String> rt = new SparseArray<String>();
        rt.put(R.string.zen_mode_important_category, KEY_IMPORTANT);
        rt.put(R.string.zen_mode_calls, KEY_CALLS);
        rt.put(R.string.zen_mode_option_title, KEY_ZEN_MODE);
        rt.put(R.string.zen_mode_messages, KEY_MESSAGES);
        rt.put(R.string.zen_mode_from_starred, KEY_STARRED);
        rt.put(R.string.zen_mode_events, KEY_EVENTS);
        rt.put(R.string.zen_mode_alarm_info, KEY_ALARM_INFO);
        rt.put(R.string.zen_mode_downtime_category, KEY_DOWNTIME);
        rt.put(R.string.zen_mode_downtime_days, KEY_DAYS);
        rt.put(R.string.zen_mode_start_time, KEY_START_TIME);
        rt.put(R.string.zen_mode_end_time, KEY_END_TIME);
        rt.put(R.string.zen_mode_downtime_mode_title, KEY_DOWNTIME_MODE);
        rt.put(R.string.zen_mode_automation_category, KEY_AUTOMATION);
        rt.put(R.string.manage_condition_providers, KEY_CONDITION_PROVIDERS);
        return rt;
    }

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private Context mContext;
    private PackageManager mPM;
    private ZenModeConfig mConfig;
    private boolean mDisableListeners;
    private SwitchPreference mCalls;
    private SwitchPreference mMessages;
    private DropDownPreference mStarred;
    private SwitchPreference mEvents;
    private boolean mDowntimeSupported;
    private Preference mDays;
    private TimePickerPreference mStart;
    private TimePickerPreference mEnd;
    private DropDownPreference mDowntimeMode;
    private PreferenceCategory mAutomationCategory;
    private Preference mEntry;
    private Preference mConditionProviders;
    private AlertDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mPM = mContext.getPackageManager();

        addPreferencesFromResource(R.xml.zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mConfig = getZenModeConfig();
        if (DEBUG) Log.d(TAG, "Loaded mConfig=" + mConfig);

        PREF_ZEN_MODE.init(this);
        PREF_ZEN_MODE.setCallback(new SettingPrefWithCallback.Callback() {
            @Override
            public void onSettingSelected(int value) {
                if (value != Global.ZEN_MODE_OFF) {
                    showConditionSelection(value);
                }
            }
        });

        final PreferenceCategory important =
                (PreferenceCategory) root.findPreference(KEY_IMPORTANT);

        mCalls = (SwitchPreference) important.findPreference(KEY_CALLS);
        mCalls.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                if (val == mConfig.allowCalls) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowCalls=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowCalls = val;
                return setZenModeConfig(newConfig);
            }
        });

        mMessages = (SwitchPreference) important.findPreference(KEY_MESSAGES);
        mMessages.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                if (val == mConfig.allowMessages) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowMessages=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowMessages = val;
                return setZenModeConfig(newConfig);
            }
        });

        mStarred = (DropDownPreference) important.findPreference(KEY_STARRED);
        mStarred.addItem(R.string.zen_mode_from_anyone, ZenModeConfig.SOURCE_ANYONE);
        mStarred.addItem(R.string.zen_mode_from_starred, ZenModeConfig.SOURCE_STAR);
        mStarred.addItem(R.string.zen_mode_from_contacts, ZenModeConfig.SOURCE_CONTACT);
        mStarred.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object newValue) {
                if (mDisableListeners) return true;
                final int val = (Integer) newValue;
                if (val == mConfig.allowFrom) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowFrom=" +
                        ZenModeConfig.sourceToString(val));
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowFrom = val;
                return setZenModeConfig(newConfig);
            }
        });
        important.addPreference(mStarred);

        mEvents = (SwitchPreference) important.findPreference(KEY_EVENTS);
        mEvents.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                if (val == mConfig.allowEvents) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowEvents=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowEvents = val;
                return setZenModeConfig(newConfig);
            }
        });

        final PreferenceCategory downtime = (PreferenceCategory) root.findPreference(KEY_DOWNTIME);
        mDowntimeSupported = isDowntimeSupported(mContext);
        if (!mDowntimeSupported) {
            removePreference(KEY_DOWNTIME);
        } else {
            mDays = downtime.findPreference(KEY_DAYS);
            mDays.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(mContext)
                            .setTitle(R.string.zen_mode_downtime_days)
                            .setView(new ZenModeDowntimeDaysSelection(mContext, mConfig.sleepMode) {
                                  @Override
                                  protected void onChanged(String mode) {
                                      if (mDisableListeners) return;
                                      if (Objects.equals(mode, mConfig.sleepMode)) return;
                                      if (DEBUG) Log.d(TAG, "days.onChanged sleepMode=" + mode);
                                      final ZenModeConfig newConfig = mConfig.copy();
                                      newConfig.sleepMode = mode;
                                      setZenModeConfig(newConfig);
                                  }
                            })
                            .setOnDismissListener(new OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    updateDays();
                                }
                            })
                            .setPositiveButton(R.string.done_button, null)
                            .show();
                    return true;
                }
            });

            final FragmentManager mgr = getFragmentManager();

            mStart = new TimePickerPreference(mContext, mgr);
            mStart.setKey(KEY_START_TIME);
            mStart.setTitle(R.string.zen_mode_start_time);
            mStart.setCallback(new TimePickerPreference.Callback() {
                @Override
                public boolean onSetTime(int hour, int minute) {
                    if (mDisableListeners) return true;
                    if (!ZenModeConfig.isValidHour(hour)) return false;
                    if (!ZenModeConfig.isValidMinute(minute)) return false;
                    if (hour == mConfig.sleepStartHour && minute == mConfig.sleepStartMinute) {
                        return true;
                    }
                    if (DEBUG) Log.d(TAG, "onPrefChange sleepStart h=" + hour + " m=" + minute);
                    final ZenModeConfig newConfig = mConfig.copy();
                    newConfig.sleepStartHour = hour;
                    newConfig.sleepStartMinute = minute;
                    return setZenModeConfig(newConfig);
                }
            });
            downtime.addPreference(mStart);
            mStart.setDependency(mDays.getKey());

            mEnd = new TimePickerPreference(mContext, mgr);
            mEnd.setKey(KEY_END_TIME);
            mEnd.setTitle(R.string.zen_mode_end_time);
            mEnd.setCallback(new TimePickerPreference.Callback() {
                @Override
                public boolean onSetTime(int hour, int minute) {
                    if (mDisableListeners) return true;
                    if (!ZenModeConfig.isValidHour(hour)) return false;
                    if (!ZenModeConfig.isValidMinute(minute)) return false;
                    if (hour == mConfig.sleepEndHour && minute == mConfig.sleepEndMinute) {
                        return true;
                    }
                    if (DEBUG) Log.d(TAG, "onPrefChange sleepEnd h=" + hour + " m=" + minute);
                    final ZenModeConfig newConfig = mConfig.copy();
                    newConfig.sleepEndHour = hour;
                    newConfig.sleepEndMinute = minute;
                    return setZenModeConfig(newConfig);
                }
            });
            downtime.addPreference(mEnd);
            mEnd.setDependency(mDays.getKey());

            mDowntimeMode = (DropDownPreference) downtime.findPreference(KEY_DOWNTIME_MODE);
            mDowntimeMode.addItem(R.string.zen_mode_downtime_mode_priority, false);
            mDowntimeMode.addItem(R.string.zen_mode_downtime_mode_none, true);
            mDowntimeMode.setCallback(new DropDownPreference.Callback() {
                @Override
                public boolean onItemSelected(int pos, Object value) {
                    if (mDisableListeners) return true;
                    final boolean sleepNone = value instanceof Boolean ? ((Boolean) value) : false;
                    if (mConfig == null || mConfig.sleepNone == sleepNone) return false;
                    final ZenModeConfig newConfig = mConfig.copy();
                    newConfig.sleepNone = sleepNone;
                    if (DEBUG) Log.d(TAG, "onPrefChange sleepNone=" + sleepNone);
                    return setZenModeConfig(newConfig);
                }
            });
            mDowntimeMode.setOrder(10);  // sort at the bottom of the category
            mDowntimeMode.setDependency(mDays.getKey());
        }

        mAutomationCategory = (PreferenceCategory) findPreference(KEY_AUTOMATION);
        mEntry = findPreference(KEY_ENTRY);
        mEntry.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(mContext)
                    .setTitle(R.string.zen_mode_entry_conditions_title)
                    .setView(new ZenModeAutomaticConditionSelection(mContext))
                    .setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            refreshAutomationSection();
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok, null)
                    .show();
                return true;
            }
        });
        mConditionProviders = findPreference(KEY_CONDITION_PROVIDERS);

        updateControls();
    }

    private void updateDays() {
        if (mConfig != null) {
            final int[] days = ZenModeConfig.tryParseDays(mConfig.sleepMode);
            if (days != null && days.length != 0) {
                final StringBuilder sb = new StringBuilder();
                final Calendar c = Calendar.getInstance();
                for (int i = 0; i < DAYS.length; i++) {
                    final int day = DAYS[i];
                    for (int j = 0; j < days.length; j++) {
                        if (day == days[j]) {
                            c.set(Calendar.DAY_OF_WEEK, day);
                            if (sb.length() > 0) {
                                sb.append(mContext.getString(R.string.summary_divider_text));
                            }
                            sb.append(DAY_FORMAT.format(c.getTime()));
                            break;
                        }
                    }
                }
                if (sb.length() > 0) {
                    mDays.setSummary(sb);
                    mDays.notifyDependencyChange(false);
                    return;
                }
            }
        }
        mDays.setSummary(R.string.zen_mode_downtime_days_none);
        mDays.notifyDependencyChange(true);
    }

    private void updateEndSummary() {
        if (!mDowntimeSupported) return;
        final int startMin = 60 * mConfig.sleepStartHour + mConfig.sleepStartMinute;
        final int endMin = 60 * mConfig.sleepEndHour + mConfig.sleepEndMinute;
        final boolean nextDay = startMin >= endMin;
        final int summaryFormat;
        if (mConfig.sleepNone) {
            summaryFormat = nextDay ? R.string.zen_mode_end_time_none_next_day_summary_format
                    : R.string.zen_mode_end_time_none_same_day_summary_format;
        } else {
            summaryFormat = nextDay ? R.string.zen_mode_end_time_priority_next_day_summary_format
                    : 0;
        }
        mEnd.setSummaryFormat(summaryFormat);
    }

    private void updateControls() {
        mDisableListeners = true;
        if (mCalls != null) {
            mCalls.setChecked(mConfig.allowCalls);
        }
        mMessages.setChecked(mConfig.allowMessages);
        mStarred.setSelectedValue(mConfig.allowFrom);
        mEvents.setChecked(mConfig.allowEvents);
        updateStarredEnabled();
        if (mDowntimeSupported) {
            updateDays();
            mStart.setTime(mConfig.sleepStartHour, mConfig.sleepStartMinute);
            mEnd.setTime(mConfig.sleepEndHour, mConfig.sleepEndMinute);
            mDowntimeMode.setSelectedValue(mConfig.sleepNone);
        }
        mDisableListeners = false;
        refreshAutomationSection();
        updateEndSummary();
    }

    private void updateStarredEnabled() {
        mStarred.setEnabled(mConfig.allowCalls || mConfig.allowMessages);
    }

    private void refreshAutomationSection() {
        if (mConditionProviders != null) {
            final int total = ConditionProviderSettings.getProviderCount(mPM);
            if (total == 0) {
                getPreferenceScreen().removePreference(mAutomationCategory);
            } else {
                final int n = ConditionProviderSettings.getEnabledProviderCount(mContext);
                if (n == 0) {
                    mConditionProviders.setSummary(getResources().getString(
                            R.string.manage_condition_providers_summary_zero));
                } else {
                    mConditionProviders.setSummary(String.format(getResources().getQuantityString(
                            R.plurals.manage_condition_providers_summary_nonzero,
                            n, n)));
                }
                final String entrySummary = getEntryConditionSummary();
                if (n == 0 || entrySummary == null) {
                    mEntry.setSummary(R.string.zen_mode_entry_conditions_summary_none);
                } else {
                    mEntry.setSummary(entrySummary);
                }
            }
        }
    }

    private String getEntryConditionSummary() {
        final INotificationManager nm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        try {
            final Condition[] automatic = nm.getAutomaticZenModeConditions();
            if (automatic == null || automatic.length == 0) {
                return null;
            }
            final String divider = getString(R.string.summary_divider_text);
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < automatic.length; i++) {
                if (i > 0) sb.append(divider);
                sb.append(automatic[i].summary);
            }
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "Error calling getAutomaticZenModeConditions", e);
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateControls();
        mSettingsObserver.register();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.unregister();
    }

    private void updateZenModeConfig() {
        final ZenModeConfig config = getZenModeConfig();
        if (Objects.equals(config, mConfig)) return;
        mConfig = config;
        if (DEBUG) Log.d(TAG, "updateZenModeConfig mConfig=" + mConfig);
        updateControls();
    }

    private ZenModeConfig getZenModeConfig() {
        final INotificationManager nm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        try {
            return nm.getZenModeConfig();
        } catch (Exception e) {
           Log.w(TAG, "Error calling NoMan", e);
           return new ZenModeConfig();
        }
    }

    private boolean setZenModeConfig(ZenModeConfig config) {
        final INotificationManager nm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        try {
            final boolean success = nm.setZenModeConfig(config);
            if (success) {
                mConfig = config;
                if (DEBUG) Log.d(TAG, "Saved mConfig=" + mConfig);
                updateEndSummary();
                updateStarredEnabled();
            }
            return success;
        } catch (Exception e) {
           Log.w(TAG, "Error calling NoMan", e);
           return false;
        }
    }

    protected void putZenModeSetting(int value) {
        Global.putInt(getContentResolver(), Global.ZEN_MODE, value);
    }

    protected void showConditionSelection(final int newSettingsValue) {
        if (mDialog != null) return;

        final ZenModeConditionSelection zenModeConditionSelection =
                new ZenModeConditionSelection(mContext);
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                zenModeConditionSelection.confirmCondition();
                mDialog = null;
            }
        };
        final int oldSettingsValue = PREF_ZEN_MODE.getValue(mContext);
        ScrollView scrollView = new ScrollView(mContext);
        scrollView.addView(zenModeConditionSelection);
        mDialog = new AlertDialog.Builder(getActivity())
                .setTitle(PREF_ZEN_MODE.getCaption(getResources(), newSettingsValue))
                .setView(scrollView)
                .setPositiveButton(R.string.okay, positiveListener)
                .setNegativeButton(R.string.cancel_all_caps, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelDialog(oldSettingsValue);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancelDialog(oldSettingsValue);
                    }
                }).create();
        mDialog.show();
    }

    protected void cancelDialog(int oldSettingsValue) {
        // If not making a decision, reset drop down to current setting.
        PREF_ZEN_MODE.setValueWithoutCallback(mContext, oldSettingsValue);
        mDialog = null;
    }

    private static boolean isDowntimeSupported(Context context) {
        return NotificationManager.from(context)
                .isSystemConditionProviderEnabled(ZenModeConfig.DOWNTIME_PATH);
    }

    // Enable indexing of searchable data
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final SparseArray<String> keyTitles = allKeyTitles(context);
                final int N = keyTitles.size();
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>(N);
                final Resources res = context.getResources();
                for (int i = 0; i < N; i++) {
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.key = keyTitles.valueAt(i);
                    data.title = res.getString(keyTitles.keyAt(i));
                    data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                    result.add(data);
                }
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> rt = new ArrayList<String>();
                if (!isDowntimeSupported(context)) {
                    rt.add(KEY_DOWNTIME);
                    rt.add(KEY_DAYS);
                    rt.add(KEY_START_TIME);
                    rt.add(KEY_END_TIME);
                    rt.add(KEY_DOWNTIME_MODE);
                }
                return rt;
            }
        };

    private static class SettingPrefWithCallback extends SettingPref {

        private Callback mCallback;
        private int mValue;

        public SettingPrefWithCallback(int type, String key, String setting, int def,
                int... values) {
            super(type, key, setting, def, values);
        }

        public void setCallback(Callback callback) {
            mCallback = callback;
        }

        @Override
        public void update(Context context) {
            // Avoid callbacks from non-user changes.
            mValue = getValue(context);
            super.update(context);
        }

        @Override
        protected boolean setSetting(Context context, int value) {
            if (value == mValue) return true;
            mValue = value;
            if (mCallback != null) {
                mCallback.onSettingSelected(value);
            }
            return super.setSetting(context, value);
        }

        @Override
        public Preference init(SettingsPreferenceFragment settings) {
            Preference ret = super.init(settings);
            mValue = getValue(settings.getActivity());

            return ret;
        }

        public boolean setValueWithoutCallback(Context context, int value) {
            // Set the current value ahead of time, this way we won't trigger a callback.
            mValue = value;
            return putInt(mType, context.getContentResolver(), mSetting, value);
        }

        public int getValue(Context context) {
            return getInt(mType, context.getContentResolver(), mSetting, mDefault);
        }

        public interface Callback {
            void onSettingSelected(int value);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE_URI = Global.getUriFor(Global.ZEN_MODE);
        private final Uri ZEN_MODE_CONFIG_ETAG_URI = Global.getUriFor(Global.ZEN_MODE_CONFIG_ETAG);

        public SettingsObserver() {
            super(mHandler);
        }

        public void register() {
            getContentResolver().registerContentObserver(ZEN_MODE_URI, false, this);
            getContentResolver().registerContentObserver(ZEN_MODE_CONFIG_ETAG_URI, false, this);
        }

        public void unregister() {
            getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (ZEN_MODE_URI.equals(uri)) {
                PREF_ZEN_MODE.update(mContext);
            }
            if (ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
                updateZenModeConfig();
            }
        }
    }

    private static class TimePickerPreference extends Preference {
        private final Context mContext;

        private int mSummaryFormat;
        private int mHourOfDay;
        private int mMinute;
        private Callback mCallback;

        public TimePickerPreference(Context context, final FragmentManager mgr) {
            super(context);
            mContext = context;
            setPersistent(false);
            setOnPreferenceClickListener(new OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final TimePickerFragment frag = new TimePickerFragment();
                    frag.pref = TimePickerPreference.this;
                    frag.show(mgr, TimePickerPreference.class.getName());
                    return true;
                }
            });
        }

        public void setCallback(Callback callback) {
            mCallback = callback;
        }

        public void setSummaryFormat(int resId) {
            mSummaryFormat = resId;
            updateSummary();
        }

        public void setTime(int hourOfDay, int minute) {
            if (mCallback != null && !mCallback.onSetTime(hourOfDay, minute)) return;
            mHourOfDay = hourOfDay;
            mMinute = minute;
            updateSummary();
        }

        private void updateSummary() {
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, mHourOfDay);
            c.set(Calendar.MINUTE, mMinute);
            String time = DateFormat.getTimeFormat(mContext).format(c.getTime());
            if (mSummaryFormat != 0) {
                time = mContext.getResources().getString(mSummaryFormat, time);
            }
            setSummary(time);
        }

        public static class TimePickerFragment extends DialogFragment implements
                TimePickerDialog.OnTimeSetListener {
            public TimePickerPreference pref;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final boolean usePref = pref != null && pref.mHourOfDay >= 0 && pref.mMinute >= 0;
                final Calendar c = Calendar.getInstance();
                final int hour = usePref ? pref.mHourOfDay : c.get(Calendar.HOUR_OF_DAY);
                final int minute = usePref ? pref.mMinute : c.get(Calendar.MINUTE);
                return new TimePickerDialog(getActivity(), this, hour, minute,
                        DateFormat.is24HourFormat(getActivity()));
            }

            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (pref != null) {
                    pref.setTime(hourOfDay, minute);
                }
            }
        }

        public interface Callback {
            boolean onSetTime(int hour, int minute);
        }
    }
}
