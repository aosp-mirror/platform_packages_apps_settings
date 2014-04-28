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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.INotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class ZenModeSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = "ZenModeSettings";
    private static final boolean DEBUG = true;

    private static final String KEY_ZEN_MODE = "zen_mode";
    private static final String KEY_GENERAL = "general";
    private static final String KEY_CALLS = "phone_calls";
    private static final String KEY_MESSAGES = "messages";

    private static final String KEY_AUTOMATIC = "automatic";
    private static final String KEY_WHEN = "when";

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private SwitchPreference mSwitch;
    private Context mContext;
    private ZenModeConfig mConfig;
    private boolean mDisableListeners;
    private SwitchPreference mCalls;
    private SwitchPreference mMessages;
    private DropDownPreference mStarred;
    private DropDownPreference mWhen;
    private TimePickerPreference mStart;
    private TimePickerPreference mEnd;
    private AlertDialog mDialog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getActivity();
        final Resources res = mContext.getResources();
        final int p = res.getDimensionPixelSize(R.dimen.content_margin_left);

        addPreferencesFromResource(R.xml.zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mConfig = getZenModeConfig();
        if (DEBUG) Log.d(TAG, "Loaded mConfig=" + mConfig);

        mSwitch = (SwitchPreference) root.findPreference(KEY_ZEN_MODE);

        final PreferenceCategory general = (PreferenceCategory) root.findPreference(KEY_GENERAL);

        mCalls = (SwitchPreference) root.findPreference(KEY_CALLS);
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

        mMessages = (SwitchPreference) root.findPreference(KEY_MESSAGES);
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

        mStarred = new DropDownPreference(mContext);
        mStarred.setEnabled(false);
        mStarred.setTitle(R.string.zen_mode_from);
        mStarred.setDropDownWidth(R.dimen.zen_mode_dropdown_width);
        mStarred.addItem(R.string.zen_mode_from_anyone);
        mStarred.addItem(R.string.zen_mode_from_starred);
        mStarred.addItem(R.string.zen_mode_from_contacts);
        general.addPreference(mStarred);

        final Preference alarmInfo = new Preference(mContext) {
            @Override
            public View getView(View convertView, ViewGroup parent) {
                final TextView tv = new TextView(mContext);
                tv.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
                tv.setPadding(p, p, p, p);
                tv.setText(R.string.zen_mode_alarm_info);
                return tv;
            }
        };
        alarmInfo.setPersistent(false);
        alarmInfo.setSelectable(false);
        general.addPreference(alarmInfo);

        final PreferenceCategory auto = (PreferenceCategory) root.findPreference(KEY_AUTOMATIC);

        mWhen = new DropDownPreference(mContext);
        mWhen.setKey(KEY_WHEN);
        mWhen.setTitle(R.string.zen_mode_when);
        mWhen.setDropDownWidth(R.dimen.zen_mode_dropdown_width);
        mWhen.addItem(R.string.zen_mode_when_every_night);
        mWhen.addItem(R.string.zen_mode_when_weeknights);
        mWhen.addItem(R.string.zen_mode_when_never);
        mWhen.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos) {
                if (mDisableListeners) return true;
                final String mode = pos == 1 ? ZenModeConfig.SLEEP_MODE_NIGHTS :
                    pos == 2 ? ZenModeConfig.SLEEP_MODE_WEEKNIGHTS : null;
                if (Objects.equals(mode, mConfig.sleepMode)) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange sleepMode=" + mode);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.sleepMode = mode;
                return setZenModeConfig(newConfig);
            }
        });
        auto.addPreference(mWhen);

        final FragmentManager mgr = getFragmentManager();

        mStart = new TimePickerPreference(mContext, mgr);
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
        auto.addPreference(mStart);

        mEnd = new TimePickerPreference(mContext, mgr);
        mEnd.setTitle(R.string.zen_mode_end_time);
        mEnd.setSummaryFormat(R.string.zen_mode_end_time_summary_format);
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
        auto.addPreference(mEnd);

        mStart.setDependency(mWhen.getKey());
        mEnd.setDependency(mWhen.getKey());

        updateZenMode();
        updateControls();
    }

    private void updateControls() {
        mDisableListeners = true;
        mCalls.setChecked(mConfig.allowCalls);
        mMessages.setChecked(mConfig.allowMessages);
        mStarred.setSelectedItem(0);
        mWhen.setSelectedItem(
                ZenModeConfig.SLEEP_MODE_NIGHTS.equals(mConfig.sleepMode) ? 1 :
                ZenModeConfig.SLEEP_MODE_WEEKNIGHTS.equals(mConfig.sleepMode) ? 2 : 0);
        mStart.setTime(mConfig.sleepStartHour, mConfig.sleepStartMinute);
        mEnd.setTime(mConfig.sleepEndHour, mConfig.sleepEndMinute);
        mDisableListeners = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateZenMode();
        mSettingsObserver.register();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.unregister();
    }

    private void updateZenMode() {
        mSwitch.setOnPreferenceChangeListener(null);
        final boolean zenMode = Global.getInt(getContentResolver(),
                Global.ZEN_MODE, Global.ZEN_MODE_OFF) != Global.ZEN_MODE_OFF;
        mSwitch.setChecked(zenMode);
        mSwitch.setTitle(zenMode ? R.string.zen_mode_option_on : R.string.zen_mode_option_off);
        mSwitch.setOnPreferenceChangeListener(mSwitchListener);
    }

    private void updateZenModeConfig() {
        final ZenModeConfig config = getZenModeConfig();
        if (Objects.equals(config, mConfig)) return;
        if (DEBUG) Log.d(TAG, "updateZenModeConfig");
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

    protected ZenModeConditionSelection newConditionSelection() {
        return new ZenModeConditionSelection(mContext);
    }

    private final Runnable mHideDialog = new Runnable() {
        @Override
        public void run() {
            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }
        }
    };

    private final Runnable mShowDialog = new Runnable() {
        @Override
        public void run() {
            mDialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.zen_mode_settings_title)
                    .setView(newConditionSelection())
                    .setNegativeButton(R.string.dlg_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            putZenModeSetting(Global.ZEN_MODE_OFF);
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // noop
                        }
                    })
                    .show();
        }
    };

    private final OnPreferenceChangeListener mSwitchListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final boolean isChecked = (Boolean) newValue;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final int v = isChecked ? Global.ZEN_MODE_ON : Global.ZEN_MODE_OFF;
                    putZenModeSetting(v);
                    mHandler.post(isChecked ? mShowDialog : mHideDialog);
                }
            });
            return true;
        }
    };

    // Enable indexing of searchable data
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                add(result, context, R.string.zen_mode_settings_title);
                add(result, context, R.string.zen_mode_general_category);
                add(result, context, R.string.zen_mode_phone_calls);
                add(result, context, R.string.zen_mode_messages);
                add(result, context, R.string.zen_mode_automatic_category);
                add(result, context, R.string.zen_mode_when);
                add(result, context, R.string.zen_mode_start_time);
                add(result, context, R.string.zen_mode_end_time);
                return result;
            }

            private void add(List<SearchIndexableRaw> result, Context context, int title) {
                final Resources res = context.getResources();
                final SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(title);
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);
            }
        };

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
                updateZenMode();
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
