/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TimePicker;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class ZenModeAutomationSettings extends ZenModeSettingsBase implements Indexable {
    private static final String KEY_DOWNTIME = "downtime";
    private static final String KEY_DAYS = "days";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_DOWNTIME_MODE = "downtime_mode";

    private static final String KEY_AUTOMATION = "automation";
    private static final String KEY_ENTRY = "entry";
    private static final String KEY_CONDITION_PROVIDERS = "manage_condition_providers";

    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("EEE");

    private PackageManager mPM;
    private boolean mDisableListeners;
    private boolean mDowntimeSupported;

    private Preference mDays;
    private TimePickerPreference mStart;
    private TimePickerPreference mEnd;
    private DropDownPreference mDowntimeMode;
    private PreferenceCategory mAutomationCategory;
    private Preference mEntry;
    private Preference mConditionProviders;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPM = mContext.getPackageManager();

        addPreferencesFromResource(R.xml.zen_mode_automation_settings);
        final PreferenceScreen root = getPreferenceScreen();

        onCreateDowntimeSettings(root);

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
    }

    private void onCreateDowntimeSettings(PreferenceScreen root) {
        mDowntimeSupported = isDowntimeSupported(mContext);
        if (!mDowntimeSupported) {
            removePreference(KEY_DOWNTIME);
            return;
        }
        mDays = root.findPreference(KEY_DAYS);
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
        root.addPreference(mStart);
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
        root.addPreference(mEnd);
        mEnd.setDependency(mDays.getKey());

        mDowntimeMode = (DropDownPreference) root.findPreference(KEY_DOWNTIME_MODE);
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

    private void updateDays() {
        // Compute an ordered, delimited list of day names based on the persisted user config.
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

    @Override
    protected void onZenModeChanged() {
        // don't care
    }

    @Override
    protected void updateControls() {
        mDisableListeners = true;
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

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_AUTOMATION;
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

    private static SparseArray<String> allKeyTitles(Context context) {
        final SparseArray<String> rt = new SparseArray<String>();
        rt.put(R.string.zen_mode_downtime_category, KEY_DOWNTIME);
        rt.put(R.string.zen_mode_downtime_days, KEY_DAYS);
        rt.put(R.string.zen_mode_start_time, KEY_START_TIME);
        rt.put(R.string.zen_mode_end_time, KEY_END_TIME);
        rt.put(R.string.zen_mode_downtime_mode_title, KEY_DOWNTIME_MODE);
        rt.put(R.string.zen_mode_automation_category, KEY_AUTOMATION);
        rt.put(R.string.manage_condition_providers, KEY_CONDITION_PROVIDERS);
        return rt;
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
                    data.screenTitle = res.getString(R.string.zen_mode_automation_settings_title);
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
