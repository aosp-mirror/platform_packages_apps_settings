/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.IMountService;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;

public class CryptKeeper extends Activity implements TextView.OnEditorActionListener {
    private static final String TAG = "CryptKeeper";
    
    private static final String DECRYPT_STATE = "trigger_restart_framework";

    private static final int UPDATE_PROGRESS = 1;
    private static final int COOLDOWN = 2;

    private static final int MAX_FAILED_ATTEMPTS = 30;
    private static final int COOL_DOWN_ATTEMPTS = 10;
    private static final int COOL_DOWN_INTERVAL = 30; // 30 seconds


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            
            switch (msg.what) {
            
            case UPDATE_PROGRESS:
                String state = SystemProperties.get("vold.encrypt_progress");
                
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                progressBar.setProgress(0);
                
                try {
                    int progress = Integer.parseInt(state);
                    progressBar.setProgress(progress);
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing progress: " + e.toString());
                }
                
                // Check the status every 1 second
                sendEmptyMessageDelayed(0, 1000);
                break;
            
            case COOLDOWN:
                TextView tv = (TextView) findViewById(R.id.status);
                if (mCooldown <= 0) {
                    // Re-enable the password entry
                    EditText passwordEntry = (EditText) findViewById(R.id.passwordEntry);
                    passwordEntry.setEnabled(true);
                    
                    tv.setText(R.string.try_again);
                    
                } else {
                    
                    CharSequence tempalte = getText(R.string.crypt_keeper_cooldown);
                    tv.setText(TextUtils.expandTemplate(tempalte, Integer.toString(mCooldown)));
                    
                    mCooldown--;
                    mHandler.sendEmptyMessageDelayed(COOLDOWN, 1000); // Tick every second
                }
                break;
            }
        }
    };
    
    private int mFailedAttempts = 0;
    private int mCooldown;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String state = SystemProperties.get("vold.decrypt");
        if ("".equals(state) || DECRYPT_STATE.equals(state)) {
            // Disable the crypt keeper. 
            PackageManager pm = getPackageManager();
            ComponentName name = new ComponentName(this, CryptKeeper.class);
            pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            return;
        }
        
        // Check to see why we were started.
        String progress = SystemProperties.get("vold.encrypt_progress");
        if ("startup".equals(progress)) {
            setContentView(R.layout.crypt_keeper_progress);
            encryptionProgressInit();
        } else {
            setContentView(R.layout.crypt_keeper_password_entry);
            passwordEntryInit();
        }
    }
    
    private void encryptionProgressInit() {
        mHandler.sendEmptyMessage(UPDATE_PROGRESS);
    }
    
    private void passwordEntryInit() {
        TextView passwordEntry = (TextView) findViewById(R.id.passwordEntry);
        passwordEntry.setOnEditorActionListener(this);
        
        KeyboardView keyboardView = (PasswordEntryKeyboardView) findViewById(R.id.keyboard);
        
        PasswordEntryKeyboardHelper keyboardHelper = new PasswordEntryKeyboardHelper(this,
                keyboardView, passwordEntry, false);
        keyboardHelper.setKeyboardMode(PasswordEntryKeyboardHelper.KEYBOARD_MODE_ALPHA);


        passwordEntry.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_idle_lock,
                0, 0, 0);

        String dateFormatString = getString(com.android.internal.R.string.full_wday_month_day_no_year);
        TextView date = (TextView) findViewById(R.id.date);
        date.setText(DateFormat.format(dateFormatString, new Date()));

        // Disable the status bar
        StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        sbm.disable(StatusBarManager.DISABLE_EXPAND | StatusBarManager.DISABLE_NOTIFICATION_ICONS
                | StatusBarManager.DISABLE_NOTIFICATION_ALERTS
                | StatusBarManager.DISABLE_SYSTEM_INFO | StatusBarManager.DISABLE_NAVIGATION);
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IMountService.Stub.asInterface(service);
        }
        return null;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_NULL) {
            // Get the password
            String password = v.getText().toString();

            if (TextUtils.isEmpty(password)) {
                return true;
            }
            
            // Now that we have the password clear the password field.
            v.setText(null);

            IMountService service = getMountService();
            try {
                service.decryptStorage(password);

                // For now the only way to get here is for the password to be
                // wrong.

                mFailedAttempts++;
                
                if (mFailedAttempts == MAX_FAILED_ATTEMPTS) {
                    // Factory reset the device.
                    sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                } else if ((mFailedAttempts % COOL_DOWN_ATTEMPTS) == 0) {
                    mCooldown = COOL_DOWN_INTERVAL;
                    EditText passwordEntry = (EditText) findViewById(R.id.passwordEntry);
                    passwordEntry.setEnabled(false);
                    mHandler.sendEmptyMessage(COOLDOWN);
                } else {
                    TextView tv = (TextView) findViewById(R.id.status);
                    tv.setText(R.string.try_again);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while decrypting...", e);
            }
            
            return true;
        }
        return false;
    }
}