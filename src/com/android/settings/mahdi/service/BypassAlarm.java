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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.settings.R;

public class BypassAlarm extends Activity {

    private FrameLayout mDismissButton;

    private String mNumbers;

    private boolean mUserDestroy;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mUserDestroy = false;

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        if (SmsCallHelper.returnUserRingtoneLoop(this)) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        Bundle extras = getIntent().getExtras();
        mNumbers = extras.getString("number");

        final LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(getLayoutResId(), null);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(view);

        mDismissButton = ((FrameLayout)
                this.findViewById(R.id.dismissalert));
        mDismissButton.setOnClickListener(mDismissButtonListener);

        setAlertText(mNumbers);

        startService();
    }

    @Override
    protected void onDestroy() {
        if (mUserDestroy) {
            stopService();
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        boolean noSound = intent.getBooleanExtra("norun", false);
        String newNumber = extras.getString("number");
        if (!mNumbers.contains(newNumber)) {
            mNumbers += getResources().getString(
                    R.string.quiet_hours_alarm_and) + newNumber;
            setAlertText(mNumbers);
        }
        if (!noSound) {
            startService();
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow dismissal
        return;
    }

    protected int getLayoutResId() {
        return R.layout.bypass_alarm;
    }

    private void setAlertText(String numbers) {
        TextView alertText = (TextView) findViewById(R.id.bypasstext);
        alertText.setText(numbers + getResources().getString(
                R.string.quiet_hours_alarm_message));
    }

    private FrameLayout.OnClickListener mDismissButtonListener = new FrameLayout.OnClickListener() {
        public void onClick(View v) {
            mUserDestroy = true;
            finish();
        }
    };

    private void startService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        serviceIntent.putExtra("number", mNumbers);
        this.startService(serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        this.stopService(serviceIntent);
    }
}
