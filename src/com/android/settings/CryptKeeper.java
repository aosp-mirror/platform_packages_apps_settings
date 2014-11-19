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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;

import static com.android.internal.widget.LockPatternView.DisplayMode;

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
public class CryptKeeper extends Activity implements TextView.OnEditorActionListener,
        OnKeyListener, OnTouchListener, TextWatcher {
    private static final String TAG = "CryptKeeper";

    private static final String DECRYPT_STATE = "trigger_restart_framework";
    /** Message sent to us to indicate encryption update progress. */
    private static final int MESSAGE_UPDATE_PROGRESS = 1;
    /** Message sent to us to cool-down (waste user's time between password attempts) */
    private static final int MESSAGE_COOLDOWN = 2;
    /** Message sent to us to indicate alerting the user that we are waiting for password entry */
    private static final int MESSAGE_NOTIFY = 3;

    // Constants used to control policy.
    private static final int MAX_FAILED_ATTEMPTS = 30;
    private static final int COOL_DOWN_ATTEMPTS = 10;
    private static final int COOL_DOWN_INTERVAL = 30; // 30 seconds

    // Intent action for launching the Emergency Dialer activity.
    static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";

    // Debug Intent extras so that this Activity may be started via adb for debugging UI layouts
    private static final String EXTRA_FORCE_VIEW =
            "com.android.settings.CryptKeeper.DEBUG_FORCE_VIEW";
    private static final String FORCE_VIEW_PROGRESS = "progress";
    private static final String FORCE_VIEW_ERROR = "error";
    private static final String FORCE_VIEW_PASSWORD = "password";

    /** When encryption is detected, this flag indicates whether or not we've checked for errors. */
    private boolean mValidationComplete;
    private boolean mValidationRequested;
    /** A flag to indicate that the volume is in a bad state (e.g. partially encrypted). */
    private boolean mEncryptionGoneBad;
    /** If gone bad, should we show encryption failed (false) or corrupt (true)*/
    private boolean mCorrupt;
    /** A flag to indicate when the back event should be ignored */
    private boolean mIgnoreBack = false;
    private int mCooldown;
    PowerManager.WakeLock mWakeLock;
    private EditText mPasswordEntry;
    private LockPatternView mLockPatternView;
    /** Number of calls to {@link #notifyUser()} to ignore before notifying. */
    private int mNotificationCountdown = 0;
    /** Number of calls to {@link #notifyUser()} before we release the wakelock */
    private int mReleaseWakeLockCountdown = 0;
    private int mStatusString = R.string.enter_password;

    // how long we wait to clear a wrong pattern
    private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 1500;

    // how long we wait to clear a right pattern
    private static final int RIGHT_PATTERN_CLEAR_TIMEOUT_MS = 500;

    // When the user enters a short pin/password, run this to show an error,
    // but don't count it against attempts.
    private final Runnable mFakeUnlockAttemptRunnable = new Runnable() {
        public void run() {
            handleBadAttempt(1 /* failedAttempt */);
        }
    };

    // TODO: this should be tuned to match minimum decryption timeout
    private static final int FAKE_ATTEMPT_DELAY = 1000;

    private Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    /**
     * Used to propagate state through configuration changes (e.g. screen rotation)
     */
    private static class NonConfigurationInstanceState {
        final PowerManager.WakeLock wakelock;

        NonConfigurationInstanceState(PowerManager.WakeLock _wakelock) {
            wakelock = _wakelock;
        }
    }

    private class DecryptTask extends AsyncTask<String, Void, Integer> {
        private void hide(int id) {
            View view = findViewById(id);
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }

        @Override
        protected Integer doInBackground(String... params) {
            final IMountService service = getMountService();
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
                // The password was entered successfully. Simply do nothing
                // and wait for the service restart to switch to surfacefligner
                if (mLockPatternView != null) {
                    mLockPatternView.removeCallbacks(mClearPatternRunnable);
                    mLockPatternView.postDelayed(mClearPatternRunnable, RIGHT_PATTERN_CLEAR_TIMEOUT_MS);
                }
                hide(R.id.passwordEntry);
                hide(R.id.switch_ime_button);
                hide(R.id.lockPattern);
                hide(R.id.status);
                hide(R.id.owner_info);
                hide(R.id.emergencyCallButton);
            } else if (failedAttempts == MAX_FAILED_ATTEMPTS) {
                // Factory reset the device.
                Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                intent.putExtra(Intent.EXTRA_REASON, "CryptKeeper.MAX_FAILED_ATTEMPTS");
                sendBroadcast(intent);
            } else if (failedAttempts == -1) {
                // Right password, but decryption failed. Tell user bad news ...
                setContentView(R.layout.crypt_keeper_progress);
                showFactoryReset(true);
                return;
            } else {
                handleBadAttempt(failedAttempts);
            }
        }
    }

    private void handleBadAttempt(Integer failedAttempts) {
        // Wrong entry. Handle pattern case.
        if (mLockPatternView != null) {
            mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable, WRONG_PATTERN_CLEAR_TIMEOUT_MS);
        }
        if ((failedAttempts % COOL_DOWN_ATTEMPTS) == 0) {
            mCooldown = COOL_DOWN_INTERVAL;
            cooldown();
        } else {
            final TextView status = (TextView) findViewById(R.id.status);

            int remainingAttempts = MAX_FAILED_ATTEMPTS - failedAttempts;
            if (remainingAttempts < COOL_DOWN_ATTEMPTS) {
                CharSequence warningTemplate = getText(R.string.crypt_keeper_warn_wipe);
                CharSequence warning = TextUtils.expandTemplate(warningTemplate,
                        Integer.toString(remainingAttempts));
                status.setText(warning);
            } else {
                status.setText(R.string.try_again);
            }

            if (mLockPatternView != null) {
                mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            }
            // Reenable the password entry
            if (mPasswordEntry != null) {
                mPasswordEntry.setEnabled(true);
                final InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mPasswordEntry, 0);
                setBackFunctionality(true);
            }
            if (mLockPatternView != null) {
                mLockPatternView.setEnabled(true);
            }
        }
    }

    private class ValidationTask extends AsyncTask<Void, Void, Boolean> {
        int state;

        @Override
        protected Boolean doInBackground(Void... params) {
            final IMountService service = getMountService();
            try {
                Log.d(TAG, "Validating encryption state.");
                state = service.getEncryptionState();
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
                mCorrupt = state == IMountService.ENCRYPTION_STATE_ERROR_CORRUPT;
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
            case MESSAGE_UPDATE_PROGRESS:
                updateProgress();
                break;

            case MESSAGE_COOLDOWN:
                cooldown();
                break;

            case MESSAGE_NOTIFY:
                notifyUser();
                break;
            }
        }
    };

    private AudioManager mAudioManager;
    /** The status bar where back/home/recent buttons are shown. */
    private StatusBarManager mStatusBar;

    /** All the widgets to disable in the status bar */
    final private static int sWidgetsToDisable = StatusBarManager.DISABLE_EXPAND
            | StatusBarManager.DISABLE_NOTIFICATION_ICONS
            | StatusBarManager.DISABLE_NOTIFICATION_ALERTS
            | StatusBarManager.DISABLE_SYSTEM_INFO
            | StatusBarManager.DISABLE_HOME
            | StatusBarManager.DISABLE_SEARCH
            | StatusBarManager.DISABLE_RECENT;

    protected static final int MIN_LENGTH_BEFORE_REPORT = LockPatternUtils.MIN_LOCK_PATTERN_SIZE;

    /** @return whether or not this Activity was started for debugging the UI only. */
    private boolean isDebugView() {
        return getIntent().hasExtra(EXTRA_FORCE_VIEW);
    }

    /** @return whether or not this Activity was started for debugging the specific UI view only. */
    private boolean isDebugView(String viewType /* non-nullable */) {
        return viewType.equals(getIntent().getStringExtra(EXTRA_FORCE_VIEW));
    }

    /**
     * Notify the user that we are awaiting input. Currently this sends an audio alert.
     */
    private void notifyUser() {
        if (mNotificationCountdown > 0) {
            --mNotificationCountdown;
        } else if (mAudioManager != null) {
            try {
                // Play the standard keypress sound at full volume. This should be available on
                // every device. We cannot play a ringtone here because media services aren't
                // available yet. A DTMF-style tone is too soft to be noticed, and might not exist
                // on tablet devices. The idea is to alert the user that something is needed: this
                // does not have to be pleasing.
                mAudioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 100);
            } catch (Exception e) {
                Log.w(TAG, "notifyUser: Exception while playing sound: " + e);
            }
        }
        // Notify the user again in 5 seconds.
        mHandler.removeMessages(MESSAGE_NOTIFY);
        mHandler.sendEmptyMessageDelayed(MESSAGE_NOTIFY, 5 * 1000);

        if (mWakeLock.isHeld()) {
            if (mReleaseWakeLockCountdown > 0) {
                --mReleaseWakeLockCountdown;
            } else {
                mWakeLock.release();
            }
        }
    }

    /**
     * Ignore back events after the user has entered the decrypt screen and while the device is
     * encrypting.
     */
    @Override
    public void onBackPressed() {
        // In the rare case that something pressed back even though we were disabled.
        if (mIgnoreBack)
            return;
        super.onBackPressed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If we are not encrypted or encrypting, get out quickly.
        final String state = SystemProperties.get("vold.decrypt");
        if (!isDebugView() && ("".equals(state) || DECRYPT_STATE.equals(state))) {
            // Disable the crypt keeper.
            PackageManager pm = getPackageManager();
            ComponentName name = new ComponentName(this, CryptKeeper.class);
            pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            // Typically CryptKeeper is launched as the home app.  We didn't
            // want to be running, so need to finish this activity.  We can count
            // on the activity manager re-launching the new home app upon finishing
            // this one, since this will leave the activity stack empty.
            // NOTE: This is really grungy.  I think it would be better for the
            // activity manager to explicitly launch the crypt keeper instead of
            // home in the situation where we need to decrypt the device
            finish();
            return;
        }

        try {
            if (getResources().getBoolean(R.bool.crypt_keeper_allow_rotation)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } catch (NotFoundException e) {
        }

        // Disable the status bar, but do NOT disable back because the user needs a way to go
        // from keyboard settings and back to the password screen.
        mStatusBar = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        mStatusBar.disable(sWidgetsToDisable);

        setAirplaneModeIfNecessary();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Check for (and recover) retained instance data
        final Object lastInstance = getLastNonConfigurationInstance();
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
            showFactoryReset(mCorrupt);
            return;
        }

        final String progress = SystemProperties.get("vold.encrypt_progress");
        if (!"".equals(progress) || isDebugView(FORCE_VIEW_PROGRESS)) {
            setContentView(R.layout.crypt_keeper_progress);
            encryptionProgressInit();
        } else if (mValidationComplete || isDebugView(FORCE_VIEW_PASSWORD)) {
            new AsyncTask<Void, Void, Void>() {
                int type = StorageManager.CRYPT_TYPE_PASSWORD;
                String owner_info;
                boolean pattern_visible;

                @Override
                public Void doInBackground(Void... v) {
                    try {
                        final IMountService service = getMountService();
                        type = service.getPasswordType();
                        owner_info = service.getField("OwnerInfo");
                        pattern_visible = !("0".equals(service.getField("PatternVisible")));
                    } catch (Exception e) {
                        Log.e(TAG, "Error calling mount service " + e);
                    }

                    return null;
                }

                @Override
                public void onPostExecute(java.lang.Void v) {
                    if(type == StorageManager.CRYPT_TYPE_PIN) {
                        setContentView(R.layout.crypt_keeper_pin_entry);
                        mStatusString = R.string.enter_pin;
                    } else if (type == StorageManager.CRYPT_TYPE_PATTERN) {
                        setContentView(R.layout.crypt_keeper_pattern_entry);
                        setBackFunctionality(false);
                        mStatusString = R.string.enter_pattern;
                    } else {
                        setContentView(R.layout.crypt_keeper_password_entry);
                        mStatusString = R.string.enter_password;
                    }
                    final TextView status = (TextView) findViewById(R.id.status);
                    status.setText(mStatusString);

                    final TextView ownerInfo = (TextView) findViewById(R.id.owner_info);
                    ownerInfo.setText(owner_info);
                    ownerInfo.setSelected(true); // Required for marquee'ing to work

                    passwordEntryInit();

                    if (mLockPatternView != null) {
                        mLockPatternView.setInStealthMode(!pattern_visible);
                    }

                    if (mCooldown > 0) {
                        setBackFunctionality(false);
                        cooldown(); // in case we are cooling down and coming back from emergency dialler
                    }
                }
            }.execute();
        } else if (!mValidationRequested) {
            // We're supposed to be encrypted, but no validation has been done.
            new ValidationTask().execute((Void[]) null);
            mValidationRequested = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeMessages(MESSAGE_COOLDOWN);
        mHandler.removeMessages(MESSAGE_UPDATE_PROGRESS);
        mHandler.removeMessages(MESSAGE_NOTIFY);
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

    /**
     * Start encrypting the device.
     */
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

        ((ProgressBar) findViewById(R.id.progress_bar)).setIndeterminate(true);
        // Ignore all back presses from now, both hard and soft keys.
        setBackFunctionality(false);
        // Start the first run of progress manually. This method sets up messages to occur at
        // repeated intervals.
        updateProgress();
    }

    /**
     * Show factory reset screen allowing the user to reset their phone when
     * there is nothing else we can do
     * @param corrupt true if userdata is corrupt, false if encryption failed
     *        partway through
     */
    private void showFactoryReset(final boolean corrupt) {
        // Hide the encryption-bot to make room for the "factory reset" button
        findViewById(R.id.encroid).setVisibility(View.GONE);

        // Show the reset button, failure text, and a divider
        final Button button = (Button) findViewById(R.id.factory_reset);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v) {
                // Factory reset the device.
                Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                intent.putExtra(Intent.EXTRA_REASON,
                        "CryptKeeper.showFactoryReset() corrupt=" + corrupt);
                sendBroadcast(intent);
            }
        });

        // Alert the user of the failure.
        if (corrupt) {
            ((TextView) findViewById(R.id.title)).setText(R.string.crypt_keeper_data_corrupt_title);
            ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_data_corrupt_summary);
        } else {
            ((TextView) findViewById(R.id.title)).setText(R.string.crypt_keeper_failed_title);
            ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_failed_summary);
        }

        final View view = findViewById(R.id.bottom_divider);
        // TODO(viki): Why would the bottom divider be missing in certain layouts? Investigate.
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private void updateProgress() {
        final String state = SystemProperties.get("vold.encrypt_progress");

        if ("error_partially_encrypted".equals(state)) {
            showFactoryReset(false);
            return;
        }

        // Get status as percentage first
        CharSequence status = getText(R.string.crypt_keeper_setup_description);
        int percent = 0;
        try {
            // Force a 50% progress state when debugging the view.
            percent = isDebugView() ? 50 : Integer.parseInt(state);
        } catch (Exception e) {
            Log.w(TAG, "Error parsing progress: " + e.toString());
        }
        String progress = Integer.toString(percent);

        // Now try to get status as time remaining and replace as appropriate
        Log.v(TAG, "Encryption progress: " + progress);
        try {
            final String timeProperty = SystemProperties.get("vold.encrypt_time_remaining");
            int time = Integer.parseInt(timeProperty);
            if (time >= 0) {
                // Round up to multiple of 10 - this way display is less jerky
                time = (time + 9) / 10 * 10;
                progress = DateUtils.formatElapsedTime(time);
                status = getText(R.string.crypt_keeper_setup_time_remaining);
            }
        } catch (Exception e) {
            // Will happen if no time etc - show percentage
        }

        final TextView tv = (TextView) findViewById(R.id.status);
        if (tv != null) {
            tv.setText(TextUtils.expandTemplate(status, progress));
        }

        // Check the progress every 1 seconds
        mHandler.removeMessages(MESSAGE_UPDATE_PROGRESS);
        mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, 1000);
    }

    /** Disable password input for a while to force the user to waste time between retries */
    private void cooldown() {
        final TextView status = (TextView) findViewById(R.id.status);

        if (mCooldown <= 0) {
            // Re-enable the password entry and back presses.
            if (mPasswordEntry != null) {
                mPasswordEntry.setEnabled(true);
                final InputMethodManager imm = (InputMethodManager) getSystemService(
                                          Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mPasswordEntry, 0);
                setBackFunctionality(true);
            }
            if (mLockPatternView != null) {
                mLockPatternView.setEnabled(true);
            }
            status.setText(mStatusString);
        } else {
            // Disable the password entry and back presses.
            if (mPasswordEntry != null) {
                mPasswordEntry.setEnabled(false);
            }
            if (mLockPatternView != null) {
                mLockPatternView.setEnabled(false);
            }

            CharSequence template = getText(R.string.crypt_keeper_cooldown);
            status.setText(TextUtils.expandTemplate(template, Integer.toString(mCooldown)));

            mCooldown--;
            mHandler.removeMessages(MESSAGE_COOLDOWN);
            mHandler.sendEmptyMessageDelayed(MESSAGE_COOLDOWN, 1000); // Tick every second
        }
    }

    /**
     * Sets the back status: enabled or disabled according to the parameter.
     * @param isEnabled true if back is enabled, false otherwise.
     */
    private final void setBackFunctionality(boolean isEnabled) {
        mIgnoreBack = !isEnabled;
        if (isEnabled) {
            mStatusBar.disable(sWidgetsToDisable);
        } else {
            mStatusBar.disable(sWidgetsToDisable | StatusBarManager.DISABLE_BACK);
        }
    }

    private void fakeUnlockAttempt(View postingView) {
        postingView.postDelayed(mFakeUnlockAttemptRunnable, FAKE_ATTEMPT_DELAY);
    }

    protected LockPatternView.OnPatternListener mChooseNewLockPatternListener =
        new LockPatternView.OnPatternListener() {

        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
        }

        @Override
        public void onPatternCleared() {
        }

        @Override
        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            mLockPatternView.setEnabled(false);
            if (pattern.size() >= MIN_LENGTH_BEFORE_REPORT) {
                new DecryptTask().execute(LockPatternUtils.patternToString(pattern));
            } else {
                // Allow user to make as many of these as they want.
                fakeUnlockAttempt(mLockPatternView);
            }
        }

        @Override
        public void onPatternCellAdded(List<Cell> pattern) {
        }
     };

     private void passwordEntryInit() {
        // Password/pin case
        mPasswordEntry = (EditText) findViewById(R.id.passwordEntry);
        if (mPasswordEntry != null){
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.requestFocus();
            // Become quiet when the user interacts with the Edit text screen.
            mPasswordEntry.setOnKeyListener(this);
            mPasswordEntry.setOnTouchListener(this);
            mPasswordEntry.addTextChangedListener(this);
        }

        // Pattern case
        mLockPatternView = (LockPatternView) findViewById(R.id.lockPattern);
        if (mLockPatternView != null) {
            mLockPatternView.setOnPatternListener(mChooseNewLockPatternListener);
        }

        // Disable the Emergency call button if the device has no voice telephone capability
        if (!getTelephonyManager().isVoiceCapable()) {
            final View emergencyCall = findViewById(R.id.emergencyCallButton);
            if (emergencyCall != null) {
                Log.d(TAG, "Removing the emergency Call button");
                emergencyCall.setVisibility(View.GONE);
            }
        }

        final View imeSwitcher = findViewById(R.id.switch_ime_button);
        final InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imeSwitcher != null && hasMultipleEnabledIMEsOrSubtypes(imm, false)) {
            imeSwitcher.setVisibility(View.VISIBLE);
            imeSwitcher.setOnClickListener(new OnClickListener() {
                    @Override
                public void onClick(View v) {
                    imm.showInputMethodPicker();
                }
            });
        }

        // We want to keep the screen on while waiting for input. In minimal boot mode, the device
        // is completely non-functional, and we want the user to notice the device and enter a
        // password.
        if (mWakeLock == null) {
            Log.d(TAG, "Acquiring wakelock.");
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
                mWakeLock.acquire();
                // Keep awake for 10 minutes - if the user hasn't been alerted by then
                // best not to just drain their battery
                mReleaseWakeLockCountdown = 96; // 96 * 5 secs per click + 120 secs before we show this = 600
            }
        }

        // Asynchronously throw up the IME, since there are issues with requesting it to be shown
        // immediately.
        if (mLockPatternView == null && mCooldown <= 0) {
            mHandler.postDelayed(new Runnable() {
                @Override public void run() {
                    imm.showSoftInputUnchecked(0, null);
                }
            }, 0);
        }

        updateEmergencyCallButtonState();
        // Notify the user in 120 seconds that we are waiting for him to enter the password.
        mHandler.removeMessages(MESSAGE_NOTIFY);
        mHandler.sendEmptyMessageDelayed(MESSAGE_NOTIFY, 120 * 1000);

        // Dismiss secure & non-secure keyguards while this screen is showing.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
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
        final IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IMountService.Stub.asInterface(service);
        }
        return null;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {
            // Get the password
            final String password = v.getText().toString();

            if (TextUtils.isEmpty(password)) {
                return true;
            }

            // Now that we have the password clear the password field.
            v.setText(null);

            // Disable the password entry and back keypress while checking the password. These
            // we either be re-enabled if the password was wrong or after the cooldown period.
            mPasswordEntry.setEnabled(false);
            setBackFunctionality(false);

            if (password.length() >= LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
                Log.d(TAG, "Attempting to send command to decrypt");
                new DecryptTask().execute(password);
            } else {
                // Allow user to make as many of these as they want.
                fakeUnlockAttempt(mPasswordEntry);
            }

            return true;
        }
        return false;
    }

    /**
     * Set airplane mode on the device if it isn't an LTE device.
     * Full story: In minimal boot mode, we cannot save any state. In particular, we cannot save
     * any incoming SMS's. So SMSs that are received here will be silently dropped to the floor.
     * That is bad. Also, we cannot receive any telephone calls in this state. So to avoid
     * both these problems, we turn the radio off. However, on certain networks turning on and
     * off the radio takes a long time. In such cases, we are better off leaving the radio
     * running so the latency of an E911 call is short.
     * The behavior after this is:
     * 1. Emergency dialing: the emergency dialer has logic to force the device out of
     *    airplane mode and restart the radio.
     * 2. Full boot: we read the persistent settings from the previous boot and restore the
     *    radio to whatever it was before it restarted. This also happens when rebooting a
     *    phone that has no encryption.
     */
    private final void setAirplaneModeIfNecessary() {
        final boolean isLteDevice =
                getTelephonyManager().getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
        if (!isLteDevice) {
            Log.d(TAG, "Going into airplane mode.");
            Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
            final Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", true);
            sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /**
     * Code to update the state of, and handle clicks from, the "Emergency call" button.
     *
     * This code is mostly duplicated from the corresponding code in
     * LockPatternUtils and LockPatternKeyguardView under frameworks/base.
     */
    private void updateEmergencyCallButtonState() {
        final Button emergencyCall = (Button) findViewById(R.id.emergencyCallButton);
        // The button isn't present at all in some configurations.
        if (emergencyCall == null)
            return;

        if (isEmergencyCallCapable()) {
            emergencyCall.setVisibility(View.VISIBLE);
            emergencyCall.setOnClickListener(new View.OnClickListener() {
                    @Override

                    public void onClick(View v) {
                        takeEmergencyCallAction();
                    }
                });
        } else {
            emergencyCall.setVisibility(View.GONE);
            return;
        }

        int textId;
        if (getTelecomManager().isInCall()) {
            // Show "return to call"
            textId = R.string.cryptkeeper_return_to_call;
        } else {
            textId = R.string.cryptkeeper_emergency_call;
        }
        emergencyCall.setText(textId);
    }

    private boolean isEmergencyCallCapable() {
        return getResources().getBoolean(com.android.internal.R.bool.config_voice_capable);
    }

    private void takeEmergencyCallAction() {
        TelecomManager telecomManager = getTelecomManager();
        if (telecomManager.isInCall()) {
            telecomManager.showInCallScreen(false /* showDialpad */);
        } else {
            launchEmergencyDialer();
        }
    }


    private void launchEmergencyDialer() {
        final Intent intent = new Intent(ACTION_EMERGENCY_DIAL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        setBackFunctionality(true);
        startActivity(intent);
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    private TelecomManager getTelecomManager() {
        return (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
    }

    /**
     * Listen to key events so we can disable sounds when we get a keyinput in EditText.
     */
    private void delayAudioNotification() {
        mNotificationCountdown = 20;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        delayAudioNotification();
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        delayAudioNotification();
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        return;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        delayAudioNotification();
    }

    @Override
    public void afterTextChanged(Editable s) {
        return;
    }
}
