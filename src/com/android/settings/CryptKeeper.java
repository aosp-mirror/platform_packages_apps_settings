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
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.IMountService;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CryptKeeper extends Activity implements TextView.OnEditorActionListener {
    private static final String TAG = "CryptKeeper";

    private static final String DECRYPT_STATE = "trigger_restart_framework";

    private static final int UPDATE_PROGRESS = 1;
    private static final int COOLDOWN = 2;

    private static final int MAX_FAILED_ATTEMPTS = 30;
    private static final int COOL_DOWN_ATTEMPTS = 10;
    private static final int COOL_DOWN_INTERVAL = 30; // 30 seconds

    private int mCooldown;
    PowerManager.WakeLock mWakeLock;

    /**
     * Used to propagate state through configuration changes (e.g. screen rotation)
     */
    private static class NonConfigurationInstanceState {
        final PowerManager.WakeLock wakelock;

        NonConfigurationInstanceState(PowerManager.WakeLock _wakelock) {
            wakelock = _wakelock;
        }
    }

    // This activity is used to fade the screen to black after the password is entered.
    public static class Blank extends Activity {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.crypt_keeper_blank);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case UPDATE_PROGRESS:
                updateProgress();
                break;

            case COOLDOWN:
                cooldown();
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If we are not encrypted or encrypting, get out quickly.
        String state = SystemProperties.get("vold.decrypt");
        if ("".equals(state) || DECRYPT_STATE.equals(state)) {
            // Disable the crypt keeper.
            PackageManager pm = getPackageManager();
            ComponentName name = new ComponentName(this, CryptKeeper.class);
            pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            return;
        }

        // Disable the status bar
        StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        sbm.disable(StatusBarManager.DISABLE_EXPAND
                | StatusBarManager.DISABLE_NOTIFICATION_ICONS
                | StatusBarManager.DISABLE_NOTIFICATION_ALERTS
                | StatusBarManager.DISABLE_SYSTEM_INFO
                | StatusBarManager.DISABLE_NAVIGATION
                | StatusBarManager.DISABLE_BACK);

        // Check for (and recover) retained instance data
        Object lastInstance = getLastNonConfigurationInstance();
        if (lastInstance instanceof NonConfigurationInstanceState) {
            NonConfigurationInstanceState retained = (NonConfigurationInstanceState) lastInstance;
            mWakeLock = retained.wakelock;
        }
    }

    /**
     * Note, we defer the state check and screen setup to onStart() because this will be
     * re-run if the user clicks the power button (sleeping/waking the screen), and this is
     * especially important if we were to lose the wakelock for any reason.
     */
    @Override
    public void onStart() {
        super.onStart();

        // Check to see why we were started.
        String progress = SystemProperties.get("vold.encrypt_progress");
        if (!"".equals(progress)) {
            setContentView(R.layout.crypt_keeper_progress);
            encryptionProgressInit();
        } else {
            setContentView(R.layout.crypt_keeper_password_entry);
            passwordEntryInit();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mHandler.removeMessages(COOLDOWN);
        mHandler.removeMessages(UPDATE_PROGRESS);
    }

    /**
     * Reconfiguring, so propagate the wakelock to the next instance.  This runs between onStop()
     * and onDestroy() and only if we are changing configuration (e.g. rotation).  Also clears
     * mWakeLock so the subsequent call to onDestroy does not release it.
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        NonConfigurationInstanceState state = new NonConfigurationInstanceState(mWakeLock);
        mWakeLock = null;
        return state;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void encryptionProgressInit() {
        // Accquire a partial wakelock to prevent the device from sleeping. Note
        // we never release this wakelock as we will be restarted after the device
        // is encrypted.

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);

        mWakeLock.acquire();

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setIndeterminate(true);

        updateProgress();
    }

    private void showFactoryReset() {
        // Hide the encryption-bot to make room for the "factory reset" button
        findViewById(R.id.encroid).setVisibility(View.GONE);

        // Show the reset button, failure text, and a divider
        Button button = (Button) findViewById(R.id.factory_reset);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Factory reset the device.
                sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
            }
        });

        TextView tv = (TextView) findViewById(R.id.title);
        tv.setText(R.string.crypt_keeper_failed_title);

        tv = (TextView) findViewById(R.id.status);
        tv.setText(R.string.crypt_keeper_failed_summary);

        View view = findViewById(R.id.bottom_divider);
        view.setVisibility(View.VISIBLE);
    }

    private void updateProgress() {
        String state = SystemProperties.get("vold.encrypt_progress");

        if ("error_partially_encrypted".equals(state)) {
            showFactoryReset();
            return;
        }

        int progress = 0;
        try {
            progress = Integer.parseInt(state);
        } catch (Exception e) {
            Log.w(TAG, "Error parsing progress: " + e.toString());
        }

        CharSequence status = getText(R.string.crypt_keeper_setup_description);
        TextView tv = (TextView) findViewById(R.id.status);
        tv.setText(TextUtils.expandTemplate(status, Integer.toString(progress)));

        // Check the progress every 5 seconds
        mHandler.removeMessages(UPDATE_PROGRESS);
        mHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 5000);
    }

    private void cooldown() {
        TextView tv = (TextView) findViewById(R.id.status);

        if (mCooldown <= 0) {
            // Re-enable the password entry
            EditText passwordEntry = (EditText) findViewById(R.id.passwordEntry);
            passwordEntry.setEnabled(true);

            tv.setVisibility(View.GONE);
        } else {
            CharSequence template = getText(R.string.crypt_keeper_cooldown);
            tv.setText(TextUtils.expandTemplate(template, Integer.toString(mCooldown)));

            tv.setVisibility(View.VISIBLE);

            mCooldown--;
            mHandler.removeMessages(COOLDOWN);
            mHandler.sendEmptyMessageDelayed(COOLDOWN, 1000); // Tick every second
        }
    }

    private void passwordEntryInit() {
        TextView passwordEntry = (TextView) findViewById(R.id.passwordEntry);
        passwordEntry.setOnEditorActionListener(this);

        KeyboardView keyboardView = (PasswordEntryKeyboardView) findViewById(R.id.keyboard);

        PasswordEntryKeyboardHelper keyboardHelper = new PasswordEntryKeyboardHelper(this,
                keyboardView, passwordEntry, false);
        keyboardHelper.setKeyboardMode(PasswordEntryKeyboardHelper.KEYBOARD_MODE_ALPHA);
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
                int failedAttempts = service.decryptStorage(password);

                if (failedAttempts == 0) {
                    // The password was entered successfully. Start the Blank activity
                    // so this activity animates to black before the devices starts. Note
                    // It has 1 second to complete the animation or it will be frozen
                    // until the boot animation comes back up.
                    Intent intent = new Intent(this, Blank.class);
                    finish();
                    startActivity(intent);
                } else if (failedAttempts == MAX_FAILED_ATTEMPTS) {
                    // Factory reset the device.
                    sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                } else if ((failedAttempts % COOL_DOWN_ATTEMPTS) == 0) {
                    mCooldown = COOL_DOWN_INTERVAL;
                    EditText passwordEntry = (EditText) findViewById(R.id.passwordEntry);
                    passwordEntry.setEnabled(false);
                    cooldown();
                } else {
                    TextView tv = (TextView) findViewById(R.id.status);
                    tv.setText(R.string.try_again);
                    tv.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while decrypting...", e);
            }

            return true;
        }
        return false;
    }
}