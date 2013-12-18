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

package com.android.settings.mahdi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.mahdi.service.SmsCallHelper;
import com.android.settings.SettingsPreferenceFragment;

public class QuietHours extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String TAG = "QuietHours";
    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    private static final String KEY_QUIET_HOURS_RING = "quiet_hours_ring";
    private static final String KEY_QUIET_HOURS_MUTE = "quiet_hours_mute";
    private static final String KEY_QUIET_HOURS_STILL = "quiet_hours_still";
    private static final String KEY_QUIET_HOURS_DIM = "quiet_hours_dim";
    private static final String KEY_QUIET_HOURS_HAPTIC = "quiet_hours_haptic";
    private static final String KEY_QUIET_HOURS_TIMERANGE = "quiet_hours_timerange";
    private static final String KEY_LOOP_BYPASS_RINGTONE = "loop_bypass_ringtone";
    private static final String KEY_AUTO_SMS = "auto_sms";
    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";
    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final String KEY_CALL_BYPASS = "call_bypass";
    private static final String KEY_SMS_BYPASS = "sms_bypass";
    private static final String KEY_REQUIRED_CALLS = "required_calls";
    private static final String KEY_SMS_BYPASS_CODE = "sms_bypass_code";
    private static final String KEY_BYPASS_RINGTONE = "bypass_ringtone";

    private static final int DLG_AUTO_SMS_MESSAGE = 0;
    private static final int DLG_SMS_BYPASS_CODE = 1;

    private CheckBoxPreference mQuietHoursEnabled;
    private CheckBoxPreference mQuietHoursRing;
    private CheckBoxPreference mQuietHoursMute;
    private CheckBoxPreference mQuietHoursStill;
    private CheckBoxPreference mQuietHoursDim;
    private CheckBoxPreference mQuietHoursHaptic;
    private CheckBoxPreference mRingtoneLoop;
    private ListPreference mAutoSms;
    private ListPreference mAutoSmsCall;
    private ListPreference mSmsBypass;
    private ListPreference mCallBypass;
    private ListPreference mCallBypassNumber;
    private Preference mSmsBypassCode;
    private Preference mAutoSmsMessage;
    private RingtonePreference mBypassRingtone;
    private TimeRangePreference mQuietHoursTimeRange;

    private Context mContext;

    private int mSmsPref;
    private int mCallPref;
    private int mSmsBypassPref;
    private int mCallBypassPref;

    private SharedPreferences mPrefs;
    private OnSharedPreferenceChangeListener mPreferencesChangeListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.quiet_hours_settings);

            mContext = getActivity().getApplicationContext();

            ContentResolver resolver = mContext.getContentResolver();

            PreferenceScreen prefSet = getPreferenceScreen();

            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            // Load the preferences
            mQuietHoursEnabled =
                (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_ENABLED);
            mQuietHoursTimeRange =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE);
            mQuietHoursRing =
                (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_RING);
            mQuietHoursMute =
                (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_MUTE);
            mQuietHoursStill =
                (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_STILL);
            mQuietHoursHaptic =
                (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_HAPTIC);
            mQuietHoursDim =
                (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_DIM);
            mRingtoneLoop =
                (CheckBoxPreference) findPreference(KEY_LOOP_BYPASS_RINGTONE);
            mAutoSms =
                (ListPreference) findPreference(KEY_AUTO_SMS);
            mAutoSmsCall =
                (ListPreference) findPreference(KEY_AUTO_SMS_CALL);
            mAutoSmsMessage =
                (Preference) findPreference(KEY_AUTO_SMS_MESSAGE);
            mSmsBypass =
                (ListPreference) findPreference(KEY_SMS_BYPASS);
            mCallBypass =
                (ListPreference) findPreference(KEY_CALL_BYPASS);
            mCallBypassNumber =
                (ListPreference) findPreference(KEY_REQUIRED_CALLS);
            mSmsBypassCode =
                (Preference) findPreference(KEY_SMS_BYPASS_CODE);
            mBypassRingtone =
                (RingtonePreference) findPreference(KEY_BYPASS_RINGTONE);

            // Set the preference state and listeners where applicable
            mQuietHoursEnabled.setChecked(
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_ENABLED, 0) == 1);
            mQuietHoursEnabled.setOnPreferenceChangeListener(this);
            mQuietHoursTimeRange.setTimeRange(
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_START, 0),
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_END, 0));
            mQuietHoursTimeRange.setOnPreferenceChangeListener(this);
            mQuietHoursRing.setChecked(
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_RINGER, 0) == 1);
            mQuietHoursRing.setOnPreferenceChangeListener(this);
            mQuietHoursMute.setChecked(
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_MUTE, 0) == 1);
            mQuietHoursMute.setOnPreferenceChangeListener(this);
            mQuietHoursStill.setChecked(
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_STILL, 0) == 1);
            mQuietHoursStill.setOnPreferenceChangeListener(this);
            mQuietHoursHaptic.setChecked(
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_HAPTIC, 0) == 1);
            mQuietHoursHaptic.setOnPreferenceChangeListener(this);
            mRingtoneLoop.setOnPreferenceChangeListener(this);
            mAutoSms.setValue(mPrefs.getString(KEY_AUTO_SMS, "0"));
            mAutoSms.setOnPreferenceChangeListener(this);
            mAutoSmsCall.setValue(mPrefs.getString(KEY_AUTO_SMS_CALL, "0"));
            mAutoSmsCall.setOnPreferenceChangeListener(this);
            mSmsBypass.setValue(mPrefs.getString(KEY_SMS_BYPASS, "0"));
            mSmsBypass.setOnPreferenceChangeListener(this);
            mCallBypass.setValue(mPrefs.getString(KEY_CALL_BYPASS, "0"));
            mCallBypass.setOnPreferenceChangeListener(this);
            mCallBypassNumber.setValue(mPrefs.getString(KEY_REQUIRED_CALLS, "2"));
            mCallBypassNumber.setOnPreferenceChangeListener(this);
            mBypassRingtone.setOnPreferenceChangeListener(this);

            TelephonyManager telephonyManager =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                prefSet.removePreference(mQuietHoursRing);
                prefSet.removePreference((PreferenceGroup) findPreference("sms_respond"));
                prefSet.removePreference((PreferenceGroup) findPreference("quiethours_bypass"));
            } else {
                int callBypassNumber = Integer.parseInt(mPrefs.getString(KEY_REQUIRED_CALLS, "2"));
                boolean loopRingtone = mPrefs.getBoolean(KEY_LOOP_BYPASS_RINGTONE, true);
                mSmsBypassPref = Integer.parseInt(mPrefs.getString(KEY_SMS_BYPASS, "0"));
                mSmsPref = Integer.parseInt(mPrefs.getString(KEY_AUTO_SMS, "0"));
                mCallPref = Integer.parseInt(mPrefs.getString(KEY_AUTO_SMS_CALL, "0"));
                mCallBypassPref = Integer.parseInt(mPrefs.getString(KEY_CALL_BYPASS, "0"));
                Uri alertSoundUri = SmsCallHelper.returnUserRingtone(mContext);
                Ringtone ringtoneAlarm = RingtoneManager.getRingtone(mContext, alertSoundUri);
                mBypassRingtone.setSummary(ringtoneAlarm.getTitle(mContext));
                mRingtoneLoop.setChecked(loopRingtone);
                mRingtoneLoop.setSummary(loopRingtone
                        ? R.string.quiet_hours_bypass_ringtone_loop_summary_on
                        : R.string.quiet_hours_bypass_ringtone_loop_summary_off);
                mSmsBypass.setSummary(mSmsBypass.getEntries()[mSmsBypassPref]);
                mCallBypass.setSummary(mCallBypass.getEntries()[mCallBypassPref]);
                mCallBypassNumber.setSummary(mCallBypassNumber.getEntries()[callBypassNumber-2]
                        + getResources().getString(R.string.quiet_hours_calls_required_summary));
                mAutoSms.setSummary(mAutoSms.getEntries()[mSmsPref]);
                mAutoSmsCall.setSummary(mAutoSmsCall.getEntries()[mCallPref]);
                mCallBypassNumber.setEnabled(mCallBypassPref != 0);
                mSmsBypassCode.setEnabled(mSmsBypassPref != 0);
                shouldDisplayRingerPrefs();
                shouldDisplayTextPref();
                setSmsBypassCodeSummary();
            }

            // Remove the notification light setting if the device does not support it
            if (mQuietHoursDim != null && getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
                getPreferenceScreen().removePreference(mQuietHoursDim);
            } else {
                mQuietHoursDim.setChecked(Settings.System.getInt(
                        resolver, Settings.System.QUIET_HOURS_DIM, 0) == 1);
                mQuietHoursDim.setOnPreferenceChangeListener(this);
            }

            mPreferencesChangeListener = new OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.equals(KEY_AUTO_SMS_CALL)
                            || key.equals(KEY_AUTO_SMS)
                            || key.equals(KEY_CALL_BYPASS)
                            || key.equals(KEY_SMS_BYPASS)) {
                        SmsCallHelper.scheduleService(mContext);
                    }
                    if (key.equals(KEY_SMS_BYPASS_CODE)) {
                        setSmsBypassCodeSummary();
                    }
                }
            };
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPreferencesChangeListener);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = mContext.getContentResolver();
        if (preference == mAutoSmsMessage) {
            showDialogInner(DLG_AUTO_SMS_MESSAGE);
            return true;
        } else if (preference == mSmsBypassCode) {
            showDialogInner(DLG_SMS_BYPASS_CODE);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = mContext.getContentResolver();
        if (preference == mQuietHoursTimeRange) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_START,
                    mQuietHoursTimeRange.getStartTime());
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_END,
                    mQuietHoursTimeRange.getEndTime());
            SmsCallHelper.scheduleService(mContext);
            return true;
        } else if (preference == mQuietHoursEnabled) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_ENABLED,
                    (Boolean) newValue ? 1 : 0);
            SmsCallHelper.scheduleService(mContext);
            return true;
        } else if (preference == mQuietHoursRing) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_RINGER,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursMute) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_MUTE,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursStill) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_STILL,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursDim) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_DIM,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursHaptic) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_HAPTIC,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mRingtoneLoop) {
            mRingtoneLoop.setSummary((Boolean) newValue
                    ? R.string.quiet_hours_bypass_ringtone_loop_summary_on
                    : R.string.quiet_hours_bypass_ringtone_loop_summary_off);
            return true;
        } else if (preference == mAutoSms) {
            mSmsPref = Integer.parseInt((String) newValue);
            mAutoSms.setSummary(mAutoSms.getEntries()[mSmsPref]);
            shouldDisplayTextPref();
            return true;
        } else if (preference == mAutoSmsCall) {
            mCallPref = Integer.parseInt((String) newValue);
            mAutoSmsCall.setSummary(mAutoSmsCall.getEntries()[mCallPref]);
            shouldDisplayTextPref();
            return true;
        } else if (preference == mSmsBypass) {
            mSmsBypassPref = Integer.parseInt((String) newValue);
            mSmsBypass.setSummary(mSmsBypass.getEntries()[mSmsBypassPref]);
            mSmsBypassCode.setEnabled(mSmsBypassPref != 0);
            shouldDisplayRingerPrefs();
            return true;
        } else if (preference == mCallBypass) {
            mCallBypassPref = Integer.parseInt((String) newValue);
            mCallBypass.setSummary(mCallBypass.getEntries()[mCallBypassPref]);
            mCallBypassNumber.setEnabled(mCallBypassPref != 0);
            shouldDisplayRingerPrefs();
            return true;
        } else if (preference == mCallBypassNumber) {
            int val = Integer.parseInt((String) newValue);
            mCallBypassNumber.setSummary(mCallBypassNumber.getEntries()[val-2]
                    + getResources().getString(R.string.quiet_hours_calls_required_summary));
            return true;
        } else if (preference == mBypassRingtone) {
            Uri val = Uri.parse((String) newValue);
            SharedPreferences.Editor editor = mPrefs.edit();
            Ringtone ringtone = RingtoneManager.getRingtone(mContext, val);
            if (ringtone != null) {
                editor.putString(KEY_BYPASS_RINGTONE, val.toString()).apply();
                mBypassRingtone.setSummary(ringtone.getTitle(mContext));
            } else {
                // No silent option, won't reach here
                editor.putString(KEY_BYPASS_RINGTONE, null).apply();
            }
            return true;
        }
        return false;
    }

    private void shouldDisplayTextPref() {
        mAutoSmsMessage.setEnabled(mSmsPref != 0 || mCallPref != 0);
    }

    private void shouldDisplayRingerPrefs() {
        mBypassRingtone.setEnabled(mSmsBypassPref != 0 || mCallBypassPref != 0);
        mRingtoneLoop.setEnabled(mSmsBypassPref != 0 || mCallBypassPref != 0);
    }

    private void setSmsBypassCodeSummary() {
        final String defaultCode = getResources().getString(R.string.quiet_hours_sms_code_null);
        final String code = mPrefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);
        mSmsBypassCode.setSummary(code);
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        QuietHours getOwner() {
            return (QuietHours) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_AUTO_SMS_MESSAGE:
                    final String defaultText =
                        getResources().getString(R.string.quiet_hours_auto_sms_null);
                    final String autoText =
                        getOwner().mPrefs.getString(KEY_AUTO_SMS_MESSAGE, defaultText);

                    final EditText input = new EditText(getActivity());
                    InputFilter[] filter = new InputFilter[1];
                    // No multi/split messages for ease of compatibility
                    filter[0] = new InputFilter.LengthFilter(160);
                    input.append(autoText);
                    input.setFilters(filter);

                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.quiet_hours_auto_string_title)
                    .setMessage(R.string.quiet_hours_auto_string_explain)
                    .setView(input)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String value = input.getText().toString();
                            if (TextUtils.isEmpty(value)) {
                                value = defaultText;
                            }
                            SharedPreferences.Editor editor = getOwner().mPrefs.edit();
                            editor.putString(KEY_AUTO_SMS_MESSAGE, value).apply();
                        }
                    })
                    .create();
                case DLG_SMS_BYPASS_CODE:
                    final String defaultCode =
                        getResources().getString(R.string.quiet_hours_sms_code_null);
                    final String code =
                        getOwner().mPrefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);

                    final EditText inputCode = new EditText(getActivity());
                    InputFilter[] filterCode = new InputFilter[1];
                    filterCode[0] = new InputFilter.LengthFilter(20);
                    inputCode.append(code);
                    inputCode.setFilters(filterCode);

                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.quiet_hours_sms_code_title)
                    .setMessage(R.string.quiet_hours_sms_code_explain)
                    .setView(inputCode)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String value = inputCode.getText().toString();
                            if (TextUtils.isEmpty(value)) {
                                value = defaultCode;
                            }
                            SharedPreferences.Editor editor = getOwner().mPrefs.edit();
                            editor.putString(KEY_SMS_BYPASS_CODE, value).apply();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

}
