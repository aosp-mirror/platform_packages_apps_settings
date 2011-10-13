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

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.IMountService;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;

import java.util.List;

/**
 * Settings screens to show the UI flows for encrypting/decrypting the device.
 *
 * This may be started via adb for debugging the UI layout, without having to go through
 * encryption flows everytime. It should be noted that starting the activity in this manner
 * is only useful for verifying UI-correctness - the behavior will not be identical.
 * <pre>
 * $ adb shell pm enable com.android.settings/.CryptKeeper
 * $ adb shell am start \
 *     -e "com.android.settings.CryptKeeper.DEBUG_FORCE_VIEW" "progress" \
 *     -n com.android.settings/.CryptKeeper
 * </pre>
 */
public class CryptKeeper extends Activity implements TextView.OnEditorActionListener {
    private static final String TAG = "CryptKeeper";

    private static final String DECRYPT_STATE = "trigger_restart_framework";

    private static final int UPDATE_PROGRESS = 1;
    private static final int COOLDOWN = 2;

    private static final int MAX_FAILED_ATTEMPTS = 30;
    private static final int COOL_DOWN_ATTEMPTS = 10;
    private static final int COOL_DOWN_INTERVAL = 30; // 30 seconds

    // Intent action for launching the Emergency Dialer activity.
    static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";

    // Debug Intent extras so that this Activity may be started via adb for debugging UI layouts
    private static final String EXTRA_FORCE_VIEW =
            "com.android.settings.CryptKeeper.DEBUG_FORCE_VIEW";
    private static final String FORCE_VIEW_PROGRESS = "progress";
    private static final String FORCE_VIEW_ENTRY = "entry";
    private static final String FORCE_VIEW_ERROR = "error";

    /** When encryption is detected, this flag indivates whether or not we've checked for erros. */
    private boolean mValidationComplete;
    private boolean mValidationRequested;
    /** A flag to indicate that the volume is in a bad state (e.g. partially encrypted). */
    private boolean mEncryptionGoneBad;

    private int mCooldown;
    PowerManager.WakeLock mWakeLock;
    private EditText mPasswordEntry;

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

    private class DecryptTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            IMountService service = getMountService();
            try {
                return service.decryptStorage(params[0]);
            } catch (Exception e) {
                Log.e(TAG, "Error while decrypting...", e);
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer failedAttempts) {
            if (failedAttempts == 0) {
                // The password was entered successfully. Start the Blank activity
                // so this activity animates to black before the devices starts. Note
                // It has 1 second to complete the animation or it will be frozen
                // until the boot animation comes back up.
                Intent intent = new Intent(CryptKeeper.this, Blank.class);
                finish();
                startActivity(intent);
            } else if (failedAttempts == MAX_FAILED_ATTEMPTS) {
                // Factory reset the device.
                sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
            } else if ((failedAttempts % COOL_DOWN_ATTEMPTS) == 0) {
                mCooldown = COOL_DOWN_INTERVAL;
                cooldown();
            } else {
                TextView tv = (TextView) findViewById(R.id.status);
                tv.setText(R.string.try_again);
                tv.setVisibility(View.VISIBLE);

                // Reenable the password entry
                mPasswordEntry.setEnabled(true);
            }
        }
    }

    private class ValidationTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            IMountService service = getMountService();
            try {
                Log.d(TAG, "Validating encryption state.");
                int state = service.getEncryptionState();
                if (state == IMountService.ENCRYPTION_STATE_NONE) {
                    Log.w(TAG, "Unexpectedly in CryptKeeper even though there is no encryption.");
                    return true; // Unexpected, but fine, I guess...
                }
                return state == IMountService.ENCRYPTION_STATE_OK;
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to get encryption state properly");
                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mValidationComplete = true;
            if (Boolean.FALSE.equals(result)) {
                Log.w(TAG, "Incomplete, or corrupted encryption detected. Prompting user to wipe.");
                mEncryptionGoneBad = true;
            } else {
                Log.d(TAG, "Encryption state validated. Proceeding to configure UI");
            }
            setupUi();
        }
    }

    private final Handler mHandler = new Handler() {
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

    /** @return whether or not this Activity was started for debugging the UI only. */
    private boolean isDebugView() {
        return getIntent().hasExtra(EXTRA_FORCE_VIEW);
    }

    /** @return whether or not this Activity was started for debugging the specific UI view only. */
    private boolean isDebugView(String viewType /* non-nullable */) {
        return viewType.equals(getIntent().getStringExtra(EXTRA_FORCE_VIEW));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If we are not encrypted or encrypting, get out quickly.
        String state = SystemProperties.get("vold.decrypt");
        if (!isDebugView() && ("".equals(state) || DECRYPT_STATE.equals(state))) {
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
                | StatusBarManager.DISABLE_HOME
                | StatusBarManager.DISABLE_RECENT
                | StatusBarManager.DISABLE_BACK);

        // Check for (and recover) retained instance data
        Object lastInstance = getLastNonConfigurationInstance();
        if (lastInstance instanceof NonConfigurationInstanceState) {
            NonConfigurationInstanceState retained = (NonConfigurationInstanceState) lastInstance;
            mWakeLock = retained.wakelock;
            Log.d(TAG, "Restoring wakelock from NonConfigurationInstanceState");
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

        setupUi();
    }

    /**
     * Initializes the UI based on the current state of encryption.
     * This is idempotent - calling repeatedly will simply re-initialize the UI.
     */
    private void setupUi() {
        if (mEncryptionGoneBad || isDebugView(FORCE_VIEW_ERROR)) {
            setContentView(R.layout.crypt_keeper_progress);
            showFactoryReset();
            return;
        }

        String progress = SystemProperties.get("vold.encrypt_progress");
        if (!"".equals(progress) || isDebugView(FORCE_VIEW_PROGRESS)) {
            setContentView(R.layout.crypt_keeper_progress);
            encryptionProgressInit();
        } else if (mValidationComplete) {
            setContentView(R.layout.crypt_keeper_password_entry);
            passwordEntryInit();
        } else if (!mValidationRequested) {
            // We're supposed to be encrypted, but no validation has been done.
            new ValidationTask().execute((Void[]) null);
            mValidationRequested = true;
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
        Log.d(TAG, "Handing wakelock off to NonConfigurationInstanceState");
        mWakeLock = null;
        return state;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mWakeLock != null) {
            Log.d(TAG, "Releasing and destroying wakelock");
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void encryptionProgressInit() {
        // Accquire a partial wakelock to prevent the device from sleeping. Note
        // we never release this wakelock as we will be restarted after the device
        // is encrypted.

        Log.d(TAG, "Encryption progress screen initializing.");
        if (mWakeLock == null) {
            Log.d(TAG, "Acquiring wakelock.");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }

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
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private void updateProgress() {
        String state = SystemProperties.get("vold.encrypt_progress");

        if ("error_partially_encrypted".equals(state)) {
            showFactoryReset();
            return;
        }

        int progress = 0;
        try {
            // Force a 50% progress state when debugging the view.
            progress = isDebugView() ? 50 : Integer.parseInt(state);
        } catch (Exception e) {
            Log.w(TAG, "Error parsing progress: " + e.toString());
        }

        CharSequence status = getText(R.string.crypt_keeper_setup_description);
        Log.v(TAG, "Encryption progress: " + progress);
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
            mPasswordEntry.setEnabled(true);

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
        mPasswordEntry = (EditText) findViewById(R.id.passwordEntry);
        mPasswordEntry.setOnEditorActionListener(this);
        mPasswordEntry.requestFocus();

        View imeSwitcher = findViewById(R.id.switch_ime_button);
        final InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imeSwitcher != null && hasMultipleEnabledIMEsOrSubtypes(imm, false)) {
            imeSwitcher.setVisibility(View.VISIBLE);
            imeSwitcher.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    imm.showInputMethodPicker();
                }
            });
        }

        // Asynchronously throw up the IME, since there are issues with requesting it to be shown
        // immediately.
        mHandler.postDelayed(new Runnable() {
            @Override public void run() {
                imm.showSoftInputUnchecked(0, null);
            }
        }, 0);

        updateEmergencyCallButtonState();
    }

    /**
     * Method adapted from com.android.inputmethod.latin.Utils
     *
     * @param imm The input method manager
     * @param shouldIncludeAuxiliarySubtypes
     * @return true if we have multiple IMEs to choose from
     */
    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager imm,
            final boolean shouldIncludeAuxiliarySubtypes) {
        final List<InputMethodInfo> enabledImis = imm.getEnabledInputMethodList();

        // Number of the filtered IMEs
        int filteredImisCount = 0;

        for (InputMethodInfo imi : enabledImis) {
            // We can return true immediately after we find two or more filtered IMEs.
            if (filteredImisCount > 1) return true;
            final List<InputMethodSubtype> subtypes =
                    imm.getEnabledInputMethodSubtypeList(imi, true);
            // IMEs that have no subtypes should be counted.
            if (subtypes.isEmpty()) {
                ++filteredImisCount;
                continue;
            }

            int auxCount = 0;
            for (InputMethodSubtype subtype : subtypes) {
                if (subtype.isAuxiliary()) {
                    ++auxCount;
                }
            }
            final int nonAuxCount = subtypes.size() - auxCount;

            // IMEs that have one or more non-auxiliary subtypes should be counted.
            // If shouldIncludeAuxiliarySubtypes is true, IMEs that have two or more auxiliary
            // subtypes should be counted as well.
            if (nonAuxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                ++filteredImisCount;
                continue;
            }
        }

        return filteredImisCount > 1
        // imm.getEnabledInputMethodSubtypeList(null, false) will return the current IME's enabled
        // input method subtype (The current IME should be LatinIME.)
                || imm.getEnabledInputMethodSubtypeList(null, false).size() > 1;
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
        if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {
            // Get the password
            String password = v.getText().toString();

            if (TextUtils.isEmpty(password)) {
                return true;
            }

            // Now that we have the password clear the password field.
            v.setText(null);

            // Disable the password entry while checking the password. This
            // we either be reenabled if the password was wrong or after the
            // cooldown period.
            mPasswordEntry.setEnabled(false);

            Log.d(TAG, "Attempting to send command to decrypt");
            new DecryptTask().execute(password);

            return true;
        }
        return false;
    }

    //
    // Code to update the state of, and handle clicks from, the "Emergency call" button.
    //
    // This code is mostly duplicated from the corresponding code in
    // LockPatternUtils and LockPatternKeyguardView under frameworks/base.
    //

    private void updateEmergencyCallButtonState() {
        Button button = (Button) findViewById(R.id.emergencyCallButton);
        // The button isn't present at all in some configurations.
        if (button == null) return;

        if (isEmergencyCallCapable()) {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        takeEmergencyCallAction();
                    }
                });
        } else {
            button.setVisibility(View.GONE);
            return;
        }

        int newState = TelephonyManager.getDefault().getCallState();
        int textId;
        if (newState == TelephonyManager.CALL_STATE_OFFHOOK) {
            // show "return to call" text and show phone icon
            textId = R.string.cryptkeeper_return_to_call;
            int phoneCallIcon = R.drawable.stat_sys_phone_call;
            button.setCompoundDrawablesWithIntrinsicBounds(phoneCallIcon, 0, 0, 0);
        } else {
            textId = R.string.cryptkeeper_emergency_call;
            int emergencyIcon = R.drawable.ic_emergency;
            button.setCompoundDrawablesWithIntrinsicBounds(emergencyIcon, 0, 0, 0);
        }
        button.setText(textId);
    }

    private boolean isEmergencyCallCapable() {
        return getResources().getBoolean(com.android.internal.R.bool.config_voice_capable);
    }

    private void takeEmergencyCallAction() {
        if (TelephonyManager.getDefault().getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
            resumeCall();
        } else {
            launchEmergencyDialer();
        }
    }

    private void resumeCall() {
        ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (phone != null) {
            try {
                phone.showCallScreen();
            } catch (RemoteException e) {
                Log.e(TAG, "Error calling ITelephony service: " + e);
            }
        }
    }

    private void launchEmergencyDialer() {
        Intent intent = new Intent(ACTION_EMERGENCY_DIAL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }
}
