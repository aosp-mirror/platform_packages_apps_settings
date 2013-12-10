/*
 * Copyright (C) 2013 Android Open Kang Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mahdi.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.provider.ContactsContract.PhoneLookup;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;

import java.util.Calendar;

import com.android.settings.R;

public class SmsCallHelper {

    private final static String TAG = "SmsCallHelper";

    private static final String KEY_AUTO_SMS = "auto_sms";
    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";
    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final String KEY_LOOP_BYPASS_RINGTONE = "loop_bypass_ringtone";
    private static final String KEY_BYPASS_RINGTONE = "bypass_ringtone";
    private static final String KEY_CALL_BYPASS = "call_bypass";
    private static final String KEY_SMS_BYPASS = "sms_bypass";
    private static final String KEY_REQUIRED_CALLS = "required_calls";
    private static final String KEY_SMS_BYPASS_CODE = "sms_bypass_code";
    private static final String SCHEDULE_SERVICE_COMMAND =
            "com.android.settings.service.SCHEDULE_SERVICE_COMMAND";

    private static final int FULL_DAY = 1440; // 1440 minutes in a day
    private static final int TIME_LIMIT = 30; // 30 minute bypass limit
    public static final int DEFAULT_DISABLED = 0;
    public static final int ALL_NUMBERS = 1;
    public static final int CONTACTS_ONLY = 2;
    public static final int DEFAULT_TWO = 2;

    // Return the current time
    public static int returnTimeInMinutes() {
        Calendar calendar = Calendar.getInstance();
        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        return currentMinutes;
    }

    // Return current day of month
    public static int returnDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        return dayOfMonth;
    }

    // Return if last call versus current call less than 30 minute apart
    public static boolean returnTimeConstraintMet(
                Context context, int firstCallTime, int dayOfFirstCall) {
        int currentMinutes = returnTimeInMinutes();
        int dayOfMonth = returnDayOfMonth();
        // New Day, start at zero
        if (dayOfMonth != dayOfFirstCall) {
            // Less or Equal to 30 minutes until midnight
            if (firstCallTime >= (FULL_DAY - TIME_LIMIT)) {
                if ((currentMinutes >= 0) && (currentMinutes <= TIME_LIMIT)) {
                    int remainderDayOne = FULL_DAY - firstCallTime;
                    if ((remainderDayOne + currentMinutes) <= TIME_LIMIT) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                // new day and prior call happened with more than
                // 30 minutes remaining in day
                return false;
            }
        } else {
            // Same day - simple subtraction: or you need to get out more
            // and it's been a month since your last call, reboot, or reschedule
            if ((currentMinutes - firstCallTime) <= TIME_LIMIT) {
                return true;
            } else {
                return false;
            }
        }
    }

    /* True: Ringtone loops until alert dismissed
     * False: Ringtone plays only once
     */
    public static boolean returnUserRingtoneLoop(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        boolean loop = prefs.getBoolean(KEY_LOOP_BYPASS_RINGTONE, true);
        return loop;
    }

    /* Returns user-selected alert Ringtone
     * Parsed from saved string or default ringtone
     */
    public static Uri returnUserRingtone(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        String ringtoneString = prefs.getString(KEY_BYPASS_RINGTONE, null);
        if (ringtoneString == null) {
            // Value not set, defaults to Default Ringtone
            Uri alertSoundUri = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_RINGTONE);
            return alertSoundUri;
        } else {
            Uri ringtoneUri = Uri.parse(ringtoneString);
            return ringtoneUri;
        }
    }

    // Code sender can deliver to start an alert
    public static String returnUserTextBypassCode(Context context) {
        String code = null;
        String defaultCode = context.getResources().getString(
                R.string.quiet_hours_sms_code_null);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        code = prefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);
        return code;
    }

    // Number of missed calls within time constraint to start alert
    public static int returnUserCallBypassCount(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_REQUIRED_CALLS, String.valueOf(DEFAULT_TWO)));
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    public static int returnUserTextBypass(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_SMS_BYPASS, String.valueOf(DEFAULT_DISABLED)));
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    public static int returnUserCallBypass(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_CALL_BYPASS, String.valueOf(DEFAULT_DISABLED)));
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    public static int returnUserAutoCall(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_AUTO_SMS_CALL, String.valueOf(DEFAULT_DISABLED)));
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    public static int returnUserAutoText(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_AUTO_SMS, String.valueOf(DEFAULT_DISABLED)));
    }

    // Pull current settings and send message if applicable
    public static void checkSmsQualifiers(Context context, String incomingNumber,
            int userAutoSms, boolean isContact) {
        String message = null;
        String defaultSms = context.getResources().getString(
                R.string.quiet_hours_auto_sms_null);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        message = prefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
        switch (userAutoSms) {
            case ALL_NUMBERS:
                sendAutoReply(message, incomingNumber);
                break;
            case CONTACTS_ONLY:
                if (isContact) {
                    sendAutoReply(message, incomingNumber);
                }
                break;
        }
    }

    /* True: Contact
     * False: Not a contact
     */
    public static boolean isContact(Context context, String phoneNumber) {
        boolean isContact = false;
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = {
                PhoneLookup._ID,
                PhoneLookup.NUMBER,
                PhoneLookup.DISPLAY_NAME };
        Cursor c = context.getContentResolver().query(
                lookupUri, numberProject, null, null, null);
        try {
            if (c.moveToFirst()) {
                isContact = true;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }
        return isContact;
    }

    // Returns the contact name or number
    public static String returnContactName(Context context, String phoneNumber) {
        String contactName = null;
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = { PhoneLookup.DISPLAY_NAME };
        Cursor c = context.getContentResolver().query(
            lookupUri, numberProject, null, null, null);

        try {
            if (c.moveToFirst()) {
                contactName = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            } else {
                // Not in contacts, return number again
                contactName = phoneNumber;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }

        return contactName;
    }

    // Send the message
    private static void sendAutoReply(String message, String phoneNumber) {
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (IllegalArgumentException e) {
        }
    }

    // Pending intent to start/stop SmsCallservice
    private static PendingIntent makeServiceIntent(Context context,
            String action, int requestCode) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /*
     * Called when:
     * QuietHours Toggled
     * QuietHours TimeChanged
     * AutoSMS Preferences Changed
     * At Boot
     * Time manually adjusted or Timezone Changed
     * AutoSMS service Stopped - Schedule again for next day
     */
    public static void scheduleService(Context context) {
        boolean quietHoursEnabled = Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0,
                UserHandle.USER_CURRENT_OR_SELF) != 0;
        int quietHoursStart = Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QUIET_HOURS_START, 0,
                UserHandle.USER_CURRENT_OR_SELF);
        int quietHoursEnd = Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QUIET_HOURS_END, 0,
                UserHandle.USER_CURRENT_OR_SELF);
        int autoCall = returnUserAutoCall(context);
        int autoText = returnUserAutoText(context);
        int callBypass = returnUserCallBypass(context);
        int smsBypass = returnUserTextBypass(context);
        Intent serviceTriggerIntent = new Intent(context, SmsCallService.class);
        PendingIntent startIntent = makeServiceIntent(context, SCHEDULE_SERVICE_COMMAND, 1);
        PendingIntent stopIntent = makeServiceIntent(context, SCHEDULE_SERVICE_COMMAND, 2);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.cancel(startIntent);
        am.cancel(stopIntent);

        if (!quietHoursEnabled
                || (autoCall == DEFAULT_DISABLED
                && autoText == DEFAULT_DISABLED
                && callBypass == DEFAULT_DISABLED
                && smsBypass == DEFAULT_DISABLED)) {
            context.stopService(serviceTriggerIntent);
            return;
        }

        if (quietHoursStart == quietHoursEnd) {
            // 24 hours, start without stop
            context.startService(serviceTriggerIntent);
            return;
        }


        Calendar calendar = Calendar.getInstance();
        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        boolean inQuietHours = false;
        // time from now on (in minutes) when the service start/stop should be scheduled
        int serviceStartMinutes = -1, serviceStopMinutes = -1;

        if (quietHoursEnd < quietHoursStart) {
            // Starts at night, ends in the morning.
            if (currentMinutes >= quietHoursStart) {
                // In QuietHours - quietHoursEnd in new day
                inQuietHours = true;
                serviceStopMinutes = FULL_DAY - currentMinutes + quietHoursEnd;
            } else if (currentMinutes <= quietHoursEnd) {
                // In QuietHours - quietHoursEnd in same day
                inQuietHours = true;
                serviceStopMinutes = quietHoursEnd - currentMinutes;
            } else {
                // Out of QuietHours
                // Current time less than quietHoursStart, greater than quietHoursEnd
                inQuietHours = false;
                serviceStartMinutes = quietHoursStart - currentMinutes;
                serviceStopMinutes = FULL_DAY - currentMinutes + quietHoursEnd;
            }
        } else {
            // Starts in the morning, ends at night.
            if (currentMinutes >= quietHoursStart && currentMinutes <= quietHoursEnd) {
                // In QuietHours
                inQuietHours = true;
                serviceStopMinutes = quietHoursEnd - currentMinutes;
            } else {
                // Out of QuietHours
                inQuietHours = false;
                if (currentMinutes <= quietHoursStart) {
                    serviceStartMinutes = quietHoursStart - currentMinutes;
                    serviceStopMinutes = quietHoursEnd - currentMinutes;
                } else {
                    // Current Time greater than quietHoursEnd
                    serviceStartMinutes = FULL_DAY - currentMinutes + quietHoursStart;
                    serviceStopMinutes = FULL_DAY - currentMinutes + quietHoursEnd;
                }
            }
        }

        if (inQuietHours) {
            context.startService(serviceTriggerIntent);
        } else {
            context.stopService(serviceTriggerIntent);
        }

        if (serviceStartMinutes >= 0) {
            // Start service a minute early
            serviceStartMinutes--;
            calendar.add(Calendar.MINUTE, serviceStartMinutes);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), startIntent);
            calendar.add(Calendar.MINUTE, -serviceStartMinutes);
        }

        if (serviceStopMinutes >= 0) {
            // Stop service a minute late
            serviceStopMinutes++;
            calendar.add(Calendar.MINUTE, serviceStopMinutes);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), stopIntent);
            calendar.add(Calendar.MINUTE, -serviceStopMinutes);
        }
    }
}
