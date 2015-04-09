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

import static com.android.settings.notification.ZenModeScheduleDaysSelection.DAYS;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ZenModeScheduleRuleSettings extends ZenModeSettingsBase
        implements SwitchBar.OnSwitchChangeListener {
    private static final String TAG = ZenModeSettingsBase.TAG;
    private static final boolean DEBUG = ZenModeSettingsBase.DEBUG;

    private static final String KEY_RULE_NAME = "rule_name";
    private static final String KEY_DAYS = "days";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_ZEN_MODE = "zen_mode";

    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("EEE");

    public static final String ACTION = Settings.ACTION_ZEN_MODE_SCHEDULE_RULE_SETTINGS;
    public static final String EXTRA_RULE_ID = "rule_id";

    private Context mContext;
    private boolean mDisableListeners;
    private SwitchBar mSwitchBar;
    private Preference mRuleName;
    private Preference mDays;
    private TimePickerPreference mStart;
    private TimePickerPreference mEnd;
    private DropDownPreference mZenMode;

    private String mRuleId;
    private ZenRule mRule;
    private ScheduleInfo mSchedule;
    private boolean mDeleting;

    @Override
    protected void onZenModeChanged() {
        // noop
    }

    @Override
    protected void onZenModeConfigChanged() {
        if (!refreshRuleOrFinish()) {
            updateControls();
        }
    }

    private boolean refreshRuleOrFinish() {
        mRule = mConfig.automaticRules.get(mRuleId);
        if (DEBUG) Log.d(TAG, "mRule=" + mRule);
        mSchedule = mRule != null ? ZenModeConfig.tryParseScheduleConditionId(mRule.conditionId)
                : null;
        if (mSchedule == null) {
            toastAndFinish();
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.zen_mode_rule, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(TAG, "onOptionsItemSelected " + item.getItemId());
        if (item.getItemId() == R.id.delete) {
            showDeleteRuleDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();

        final Intent intent = getActivity().getIntent();
        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + intent);
        if (intent == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        mRuleId = intent.getStringExtra(EXTRA_RULE_ID);
        if (DEBUG) Log.d(TAG, "mRuleId=" + mRuleId);
        if (refreshRuleOrFinish()) {
            return;
        }

        addPreferencesFromResource(R.xml.zen_mode_schedule_rule_settings);
        final PreferenceScreen root = getPreferenceScreen();

        setHasOptionsMenu(true);

        mRuleName = root.findPreference(KEY_RULE_NAME);
        mRuleName.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showRuleNameDialog();
                return true;
            }
        });

        mDays = root.findPreference(KEY_DAYS);
        mDays.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDaysDialog();
                return true;
            }
        });

        final FragmentManager mgr = getFragmentManager();

        mStart = new TimePickerPreference(mContext, mgr);
        mStart.setKey(KEY_START_TIME);
        mStart.setTitle(R.string.zen_mode_start_time);
        mStart.setCallback(new TimePickerPreference.Callback() {
            @Override
            public boolean onSetTime(final int hour, final int minute) {
                if (mDisableListeners) return true;
                if (!ZenModeConfig.isValidHour(hour)) return false;
                if (!ZenModeConfig.isValidMinute(minute)) return false;
                if (hour == mSchedule.startHour && minute == mSchedule.startMinute) {
                    return true;
                }
                if (DEBUG) Log.d(TAG, "onPrefChange start h=" + hour + " m=" + minute);
                mSchedule.startHour = hour;
                mSchedule.startMinute = minute;
                mRule.conditionId = ZenModeConfig.toScheduleConditionId(mSchedule);
                mRule.condition = null;
                mRule.snoozing = false;
                setZenModeConfig(mConfig);
                return true;
            }
        });
        root.addPreference(mStart);
        mStart.setDependency(mDays.getKey());

        mEnd = new TimePickerPreference(mContext, mgr);
        mEnd.setKey(KEY_END_TIME);
        mEnd.setTitle(R.string.zen_mode_end_time);
        mEnd.setCallback(new TimePickerPreference.Callback() {
            @Override
            public boolean onSetTime(final int hour, final int minute) {
                if (mDisableListeners) return true;
                if (!ZenModeConfig.isValidHour(hour)) return false;
                if (!ZenModeConfig.isValidMinute(minute)) return false;
                if (hour == mSchedule.endHour && minute == mSchedule.endMinute) {
                    return true;
                }
                if (DEBUG) Log.d(TAG, "onPrefChange end h=" + hour + " m=" + minute);
                mSchedule.startHour = hour;
                mSchedule.startMinute = minute;
                mRule.conditionId = ZenModeConfig.toScheduleConditionId(mSchedule);
                mRule.condition = null;
                mRule.snoozing = false;
                setZenModeConfig(mConfig);
                return true;
            }
        });
        root.addPreference(mEnd);
        mEnd.setDependency(mDays.getKey());

        mZenMode = (DropDownPreference) root.findPreference(KEY_ZEN_MODE);
        mZenMode.addItem(R.string.zen_mode_option_important_interruptions, Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        mZenMode.addItem(R.string.zen_mode_option_alarms, Global.ZEN_MODE_ALARMS);
        mZenMode.addItem(R.string.zen_mode_option_no_interruptions, Global.ZEN_MODE_NO_INTERRUPTIONS);
        mZenMode.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                if (mDisableListeners) return true;
                final int zenMode = (Integer) value;
                if (zenMode == mRule.zenMode) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange zenMode=" + zenMode);
                mRule.zenMode = zenMode;
                setZenModeConfig(mConfig);
                return true;
            }
        });
        mZenMode.setOrder(10);  // sort at the bottom of the category
        mZenMode.setDependency(mDays.getKey());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (DEBUG) Log.d(TAG, "onSwitchChanged " + isChecked);
        if (mDisableListeners) return;
        final boolean enabled = isChecked;
        if (enabled == mRule.enabled) return;
        if (DEBUG) Log.d(TAG, "onSwitchChanged enabled=" + enabled);
        mRule.enabled = enabled;
        mRule.snoozing = false;
        setZenModeConfig(mConfig);
    }

    private void updateDays() {
        // Compute an ordered, delimited list of day names based on the persisted user config.
        final int[] days = mSchedule.days;
        if (days != null && days.length > 0) {
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
        mDays.setSummary(R.string.zen_mode_schedule_rule_days_none);
        mDays.notifyDependencyChange(true);
    }

    private void updateEndSummary() {
        final int startMin = 60 * mSchedule.startHour + mSchedule.startMinute;
        final int endMin = 60 * mSchedule.endHour + mSchedule.endMinute;
        final boolean nextDay = startMin >= endMin;
        final int summaryFormat = nextDay ? R.string.zen_mode_end_time_next_day_summary_format : 0;
        mEnd.setSummaryFormat(summaryFormat);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateControls();
    }

    private void updateRuleName() {
        getActivity().setTitle(mRule.name);
        mRuleName.setSummary(mRule.name);
    }

    private void updateControls() {
        mDisableListeners = true;
        updateRuleName();
        updateDays();
        mStart.setTime(mSchedule.startHour, mSchedule.startMinute);
        mEnd.setTime(mSchedule.endHour, mSchedule.endMinute);
        mZenMode.setSelectedValue(mRule.zenMode);
        mDisableListeners = false;
        updateEndSummary();
        if (mSwitchBar != null) {
            mSwitchBar.setChecked(mRule.enabled);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_SCHEDULE_RULE;
    }

    private void showDeleteRuleDialog() {
        new AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.zen_mode_delete_rule_confirmation, mRule.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_delete_rule_button, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDeleting = true;
                        mConfig.automaticRules.remove(mRuleId);
                        setZenModeConfig(mConfig);
                    }
                })
                .show();
    }

    private void showRuleNameDialog() {
        new ZenRuleNameDialog(mContext, mRule.name, mConfig.getAutomaticRuleNames()) {
            @Override
            public void onOk(String ruleName) {
                final ZenModeConfig newConfig = mConfig.copy();
                final ZenRule rule = newConfig.automaticRules.get(mRuleId);
                if (rule == null) return;
                rule.name = ruleName;
                setZenModeConfig(newConfig);
            }
        }.show();
    }

    private void showDaysDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.zen_mode_schedule_rule_days)
                .setView(new ZenModeScheduleDaysSelection(mContext, mSchedule.days) {
                      @Override
                      protected void onChanged(final int[] days) {
                          if (mDisableListeners) return;
                          if (Arrays.equals(days, mSchedule.days)) return;
                          if (DEBUG) Log.d(TAG, "days.onChanged days=" + Arrays.asList(days));
                          mSchedule.days = days;
                          mRule.conditionId = ZenModeConfig.toScheduleConditionId(mSchedule);
                          mRule.condition = null;
                          mRule.snoozing = false;
                          setZenModeConfig(mConfig);
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
    }

    private void toastAndFinish() {
        if (!mDeleting) {
            Toast.makeText(mContext, R.string.zen_mode_rule_not_found_text, Toast.LENGTH_SHORT)
                    .show();
        }
        getActivity().finish();
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
