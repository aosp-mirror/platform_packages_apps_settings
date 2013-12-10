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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Telephony.Sms.Intents;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

import com.android.internal.util.mahdi.QuietHoursHelper;

import com.android.settings.R;

public class SmsCallService extends Service {

    private final static String TAG = "SmsCallService";

    private static TelephonyManager mTelephony;

    private boolean mIncomingCall = false;

    private boolean mKeepCounting = false;

    private String mIncomingNumber;

    private String mNumberSent;

    private int mMinuteSent;

    private int mBypassCallCount;

    private int mMinutes;

    private int mDay;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                mIncomingCall = true;
                mIncomingNumber = incomingNumber;
                int bypassPreference = SmsCallHelper.returnUserCallBypass(SmsCallService.this);
                boolean isContact = SmsCallHelper.isContact(SmsCallService.this, mIncomingNumber);

                if (!mKeepCounting) {
                    mKeepCounting = true;
                    mBypassCallCount = 0;
                    mDay = SmsCallHelper.returnDayOfMonth();
                    mMinutes = SmsCallHelper.returnTimeInMinutes();
                }

                boolean timeConstraintMet = SmsCallHelper.returnTimeConstraintMet(
                        SmsCallService.this, mMinutes, mDay);
                if (timeConstraintMet) {
                    switch (bypassPreference) {
                        case SmsCallHelper.DEFAULT_DISABLED:
                            break;
                        case SmsCallHelper.ALL_NUMBERS:
                            mBypassCallCount++;
                            break;
                        case SmsCallHelper.CONTACTS_ONLY:
                            if (isContact) {
                                mBypassCallCount++;
                            }
                            break;
                    }

                    if (mBypassCallCount == 0) {
                        mKeepCounting = false;
                    }
                } else {
                    switch (bypassPreference) {
                        case SmsCallHelper.DEFAULT_DISABLED:
                            break;
                        case SmsCallHelper.ALL_NUMBERS:
                            mBypassCallCount = 1;
                            break;
                        case SmsCallHelper.CONTACTS_ONLY:
                            if (isContact) {
                                mBypassCallCount = 1;
                            } else {
                                // Reset call count and time at next call
                                mKeepCounting = false;
                            }
                            break;
                    }
                    mDay = SmsCallHelper.returnDayOfMonth();
                    mMinutes = SmsCallHelper.returnTimeInMinutes();
                }
                if ((mBypassCallCount
                        == SmsCallHelper.returnUserCallBypassCount(SmsCallService.this))
                        && QuietHoursHelper.inQuietHours(SmsCallService.this, null)
                        && timeConstraintMet) {
                    // Don't auto-respond if alarm fired
                    mIncomingCall = false;
                    mKeepCounting = false;
                    startAlarm(SmsCallService.this, mIncomingNumber);
                }
            }
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // Don't message or alarm if call was answered
                mIncomingCall = false;
                // Call answered, reset Incoming number
                // Stop AlarmSound
                mKeepCounting = false;
                Intent serviceIntent = new Intent(SmsCallService.this, AlarmService.class);
                SmsCallService.this.stopService(serviceIntent);
            }
            if (state == TelephonyManager.CALL_STATE_IDLE && mIncomingCall) {
                // Call Received and now inactive
                mIncomingCall = false;
                int userAutoSms = SmsCallHelper.returnUserAutoCall(SmsCallService.this);

                if (userAutoSms != SmsCallHelper.DEFAULT_DISABLED
                        && QuietHoursHelper.inQuietHours(SmsCallService.this, null)) {
                    boolean isContact =
                        SmsCallHelper.isContact(SmsCallService.this, mIncomingNumber);
                    checkTimeAndNumber(
                            SmsCallService.this, mIncomingNumber, userAutoSms, isContact);
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            SmsMessage msg = msgs[0];
            String incomingNumber = msg.getOriginatingAddress();
            boolean nawDawg = false;
            int userAutoSms = SmsCallHelper.returnUserAutoText(context);
            int bypassCodePref = SmsCallHelper.returnUserTextBypass(context);
            boolean isContact = SmsCallHelper.isContact(context, incomingNumber);

            if ((bypassCodePref != SmsCallHelper.DEFAULT_DISABLED
                    || userAutoSms != SmsCallHelper.DEFAULT_DISABLED)
                    && QuietHoursHelper.inQuietHours(context, null)) {
                String bypassCode = SmsCallHelper.returnUserTextBypassCode(context);
                String messageBody = msg.getMessageBody();
                if (messageBody.contains(bypassCode)) {
                    switch (bypassCodePref) {
                        case SmsCallHelper.DEFAULT_DISABLED:
                            break;
                        case SmsCallHelper.ALL_NUMBERS:
                            // Sound Alarm && Don't auto-respond
                            nawDawg = true;
                            startAlarm(SmsCallService.this, incomingNumber);
                            break;
                        case SmsCallHelper.CONTACTS_ONLY:
                            if (isContact) {
                                // Sound Alarm && Don't auto-respond
                                nawDawg = true;
                                startAlarm(SmsCallService.this, incomingNumber);
                            }
                            break;
                    }
                }
                if (userAutoSms != SmsCallHelper.DEFAULT_DISABLED && nawDawg == false) {
                    checkTimeAndNumber(context, incomingNumber, userAutoSms, isContact);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        mTelephony = (TelephonyManager)
                this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.SMS_RECEIVED_ACTION);
        registerReceiver(smsReceiver, filter);
    }

    @Override
    public void onDestroy() {
        if (mTelephony != null) {
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        mPhoneStateListener = null;
        unregisterReceiver(smsReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * Dont send if alarm fired
     * If in same minute, don't send. This prevents message looping if sent to self
     * or another quiet-hours enabled device with this feature on.
     */
    private void checkTimeAndNumber(Context context, String incomingNumber,
            int userSetting, boolean isContact) {
        int minutesNow = SmsCallHelper.returnTimeInMinutes();
        if (minutesNow != mMinuteSent) {
            mNumberSent = incomingNumber;
            mMinuteSent = SmsCallHelper.returnTimeInMinutes();
            SmsCallHelper.checkSmsQualifiers(
                    context, incomingNumber, userSetting, isContact);
        } else {
            // Let's try to send if number doesn't match prior
            if (!incomingNumber.equals(mNumberSent)) {
                mNumberSent = incomingNumber;
                mMinuteSent = SmsCallHelper.returnTimeInMinutes();
                    SmsCallHelper.checkSmsQualifiers(
                            context, incomingNumber, userSetting, isContact);
            }
        }
    }

    private void startAlarm(Context context, String phoneNumber) {
        String contactName = SmsCallHelper.returnContactName(context, phoneNumber);
        Intent alarmDialog = new Intent();
        alarmDialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmDialog.setClass(context, com.android.settings.mahdi.service.BypassAlarm.class);
        alarmDialog.putExtra("number", contactName);
        startActivity(alarmDialog);
    }
}
