/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets.Type;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.network.ProxySubscriptionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the preference screen to enable/disable ICC lock and
 * also the dialogs to change the ICC PIN. In the former case, enabling/disabling
 * the ICC lock will prompt the user for the current PIN.
 * In the Change PIN case, it prompts the user for old pin, new pin and new pin
 * again before attempting to change it. Calls the SimCard interface to execute
 * these operations.
 *
 */
public class IccLockSettings extends SettingsPreferenceFragment
        implements EditPinPreference.OnPinEnteredListener {
    private static final String TAG = "IccLockSettings";
    private static final boolean DBG = false;

    private static final int OFF_MODE = 0;
    // State when enabling/disabling ICC lock
    private static final int ICC_LOCK_MODE = 1;
    // State when entering the old pin
    private static final int ICC_OLD_MODE = 2;
    // State when entering the new pin - first time
    private static final int ICC_NEW_MODE = 3;
    // State when entering the new pin - second time
    private static final int ICC_REENTER_MODE = 4;

    // Keys in xml file
    private static final String PIN_DIALOG = "sim_pin";
    private static final String PIN_TOGGLE = "sim_toggle";
    // Keys in icicle
    private static final String DIALOG_SUB_ID = "dialogSubId";
    private static final String DIALOG_STATE = "dialogState";
    private static final String DIALOG_PIN = "dialogPin";
    private static final String DIALOG_ERROR = "dialogError";
    private static final String ENABLE_TO_STATE = "enableState";
    private static final String CURRENT_TAB = "currentTab";

    // Save and restore inputted PIN code when configuration changed
    // (ex. portrait<-->landscape) during change PIN code
    private static final String OLD_PINCODE = "oldPinCode";
    private static final String NEW_PINCODE = "newPinCode";

    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
    // Which dialog to show next when popped up
    private int mDialogState = OFF_MODE;

    private String mPin;
    private String mOldPin;
    private String mNewPin;
    private String mError;
    // Are we trying to enable or disable ICC lock?
    private boolean mToState;

    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private ListView mListView;

    private ProxySubscriptionManager mProxySubscriptionMgr;

    private EditPinPreference mPinDialog;
    private SwitchPreference mPinToggle;

    private Resources mRes;

    // For async handler to identify request type
    private static final int MSG_SIM_STATE_CHANGED = 102;

    // @see android.widget.Toast$TN
    private static final long LONG_DURATION_TIMEOUT = 7000;

    private int mSlotId = -1;
    private int mSubId;
    private TelephonyManager mTelephonyManager;

    // For replies from IccCard interface
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SIM_STATE_CHANGED:
                    updatePreferences();
                    break;
            }

            return;
        }
    };

    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SIM_STATE_CHANGED.equals(action)) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SIM_STATE_CHANGED));
            }
        }
    };

    // For top-level settings screen to query
    private boolean isIccLockEnabled() {
        mTelephonyManager = mTelephonyManager.createForSubscriptionId(mSubId);
        return mTelephonyManager.isIccLockEnabled();
    }

    private String getSummary(Context context) {
        final Resources res = context.getResources();
        final String summary = isIccLockEnabled()
                ? res.getString(R.string.sim_lock_on)
                : res.getString(R.string.sim_lock_off);
        return summary;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Utils.isMonkeyRunning()) {
            finish();
            return;
        }

        // enable ProxySubscriptionMgr with Lifecycle support for all controllers
        // live within this fragment
        mProxySubscriptionMgr = ProxySubscriptionManager.getInstance(getContext());
        mProxySubscriptionMgr.setLifecycle(getLifecycle());

        mTelephonyManager = getContext().getSystemService(TelephonyManager.class);

        addPreferencesFromResource(R.xml.sim_lock_settings);

        mPinDialog = (EditPinPreference) findPreference(PIN_DIALOG);
        mPinToggle = (SwitchPreference) findPreference(PIN_TOGGLE);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(DIALOG_STATE)
                    && restoreDialogStates(savedInstanceState)) {
                Log.d(TAG, "onCreate: restore dialog for slotId=" + mSlotId + ", subId=" + mSubId);
            } else if (savedInstanceState.containsKey(CURRENT_TAB)
                    && restoreTabFocus(savedInstanceState)) {
                Log.d(TAG, "onCreate: restore focus on slotId=" + mSlotId + ", subId=" + mSubId);
            }
        }

        mPinDialog.setOnPinEnteredListener(this);

        // Don't need any changes to be remembered
        getPreferenceScreen().setPersistent(false);

        mRes = getResources();
    }

    private boolean restoreDialogStates(Bundle savedInstanceState) {
        final SubscriptionInfo subInfo = mProxySubscriptionMgr
                .getActiveSubscriptionInfo(savedInstanceState.getInt(DIALOG_SUB_ID));
        if (subInfo == null) {
            return false;
        }

        final SubscriptionInfo visibleSubInfo = getVisibleSubscriptionInfoForSimSlotIndex(
                subInfo.getSimSlotIndex());
        if (visibleSubInfo == null) {
            return false;
        }
        if (visibleSubInfo.getSubscriptionId() != subInfo.getSubscriptionId()) {
            return false;
        }

        mSlotId = subInfo.getSimSlotIndex();
        mSubId = subInfo.getSubscriptionId();
        mDialogState = savedInstanceState.getInt(DIALOG_STATE);
        mPin = savedInstanceState.getString(DIALOG_PIN);
        mError = savedInstanceState.getString(DIALOG_ERROR);
        mToState = savedInstanceState.getBoolean(ENABLE_TO_STATE);

        // Restore inputted PIN code
        switch (mDialogState) {
            case ICC_NEW_MODE:
                mOldPin = savedInstanceState.getString(OLD_PINCODE);
                break;

            case ICC_REENTER_MODE:
                mOldPin = savedInstanceState.getString(OLD_PINCODE);
                mNewPin = savedInstanceState.getString(NEW_PINCODE);
                break;
        }
        return true;
    }

    private boolean restoreTabFocus(Bundle savedInstanceState) {
        int slotId = 0;
        try {
            slotId = Integer.parseInt(savedInstanceState.getString(CURRENT_TAB));
        } catch (NumberFormatException exception) {
            return false;
        }

        final SubscriptionInfo subInfo = getVisibleSubscriptionInfoForSimSlotIndex(slotId);
        if (subInfo == null) {
            return false;
        }

        mSlotId = subInfo.getSimSlotIndex();
        mSubId = subInfo.getSubscriptionId();
        if (mTabHost != null) {
            mTabHost.setCurrentTabByTag(getTagForSlotId(mSlotId));
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final int numSims = mProxySubscriptionMgr.getActiveSubscriptionInfoCountMax();
        final List<SubscriptionInfo> componenterList = new ArrayList<>();

        for (int i = 0; i < numSims; ++i) {
            final SubscriptionInfo subInfo = getVisibleSubscriptionInfoForSimSlotIndex(i);
            if (subInfo != null) {
                componenterList.add(subInfo);
            }
        }

        if (componenterList.size() == 0) {
            Log.e(TAG, "onCreateView: no sim info");
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        if (mSlotId < 0) {
            mSlotId = componenterList.get(0).getSimSlotIndex();
            mSubId = componenterList.get(0).getSubscriptionId();
            Log.d(TAG, "onCreateView: default slotId=" + mSlotId + ", subId=" + mSubId);
        }

        if (componenterList.size() > 1) {
            final View view = inflater.inflate(R.layout.icc_lock_tabs, container, false);
            final ViewGroup prefs_container = (ViewGroup) view.findViewById(R.id.prefs_container);
            Utils.prepareCustomPreferencesList(container, view, prefs_container, false);
            final View prefs = super.onCreateView(inflater, prefs_container, savedInstanceState);
            prefs_container.addView(prefs);

            mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
            mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
            mListView = (ListView) view.findViewById(android.R.id.list);

            mTabHost.setup();
            mTabHost.clearAllTabs();

            for (SubscriptionInfo subInfo : componenterList) {
                final int slot = subInfo.getSimSlotIndex();
                final String tag = getTagForSlotId(slot);
                mTabHost.addTab(buildTabSpec(tag,
                        String.valueOf(subInfo == null
                                ? getContext().getString(R.string.sim_editor_title, slot + 1)
                                : subInfo.getDisplayName())));
            }

            mTabHost.setCurrentTabByTag(getTagForSlotId(mSlotId));
            mTabHost.setOnTabChangedListener(mTabListener);
            return view;
        } else {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePreferences();
    }

    private void updatePreferences() {

        final SubscriptionInfo sir = getVisibleSubscriptionInfoForSimSlotIndex(mSlotId);
        final int subId = (sir != null) ? sir.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        if (mSubId != subId) {
            mSubId = subId;
            resetDialogState();
            if ((mPinDialog != null) && mPinDialog.isDialogOpen()) {
                mPinDialog.getDialog().dismiss();
            }
        }

        if (mPinDialog != null) {
            mPinDialog.setEnabled(sir != null);
        }
        if (mPinToggle != null) {
            mPinToggle.setEnabled(sir != null);

            if (sir != null) {
                mPinToggle.setChecked(isIccLockEnabled());
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ICC_LOCK;
    }

    @Override
    public void onResume() {
        super.onResume();

        // ACTION_SIM_STATE_CHANGED is sticky, so we'll receive current state after this call,
        // which will call updatePreferences().
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SIM_STATE_CHANGED);
        getContext().registerReceiver(mSimStateReceiver, filter);

        if (mDialogState != OFF_MODE) {
            showPinDialog();
        } else {
            // Prep for standard click on "Change PIN"
            resetDialogState();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mSimStateReceiver);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_icc_lock;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        // Need to store this state for slider open/close
        // There is one case where the dialog is popped up by the preference
        // framework. In that case, let the preference framework store the
        // dialog state. In other cases, where this activity manually launches
        // the dialog, store the state of the dialog.
        if (mPinDialog.isDialogOpen()) {
            out.putInt(DIALOG_SUB_ID, mSubId);
            out.putInt(DIALOG_STATE, mDialogState);
            out.putString(DIALOG_PIN, mPinDialog.getEditText().getText().toString());
            out.putString(DIALOG_ERROR, mError);
            out.putBoolean(ENABLE_TO_STATE, mToState);

            // Save inputted PIN code
            switch (mDialogState) {
                case ICC_NEW_MODE:
                    out.putString(OLD_PINCODE, mOldPin);
                    break;

                case ICC_REENTER_MODE:
                    out.putString(OLD_PINCODE, mOldPin);
                    out.putString(NEW_PINCODE, mNewPin);
                    break;
            }
        } else {
            super.onSaveInstanceState(out);
        }

        if (mTabHost != null) {
            out.putString(CURRENT_TAB, mTabHost.getCurrentTabTag());
        }
    }

    private void showPinDialog() {
        if (mDialogState == OFF_MODE) {
            return;
        }
        setDialogValues();

        mPinDialog.showPinDialog();

        final EditText editText = mPinDialog.getEditText();
        if (!TextUtils.isEmpty(mPin) && editText != null) {
            editText.setSelection(mPin.length());
        }
    }

    private void setDialogValues() {
        mPinDialog.setText(mPin);
        String message = "";
        switch (mDialogState) {
            case ICC_LOCK_MODE:
                message = mRes.getString(R.string.sim_enter_pin);
                mPinDialog.setDialogTitle(mToState
                        ? mRes.getString(R.string.sim_enable_sim_lock)
                        : mRes.getString(R.string.sim_disable_sim_lock));
                break;
            case ICC_OLD_MODE:
                message = mRes.getString(R.string.sim_enter_old);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            case ICC_NEW_MODE:
                message = mRes.getString(R.string.sim_enter_new);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            case ICC_REENTER_MODE:
                message = mRes.getString(R.string.sim_reenter_new);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
        }
        if (mError != null) {
            message = mError + "\n" + message;
            mError = null;
        }
        mPinDialog.setDialogMessage(message);
    }

    @Override
    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (!positiveResult) {
            resetDialogState();
            return;
        }

        mPin = preference.getText();
        if (!reasonablePin(mPin)) {
            // inject error message and display dialog again
            mError = mRes.getString(R.string.sim_bad_pin);
            showPinDialog();
            return;
        }
        switch (mDialogState) {
            case ICC_LOCK_MODE:
                tryChangeIccLockState();
                break;
            case ICC_OLD_MODE:
                mOldPin = mPin;
                mDialogState = ICC_NEW_MODE;
                mError = null;
                mPin = null;
                showPinDialog();
                break;
            case ICC_NEW_MODE:
                mNewPin = mPin;
                mDialogState = ICC_REENTER_MODE;
                mPin = null;
                showPinDialog();
                break;
            case ICC_REENTER_MODE:
                if (!mPin.equals(mNewPin)) {
                    mError = mRes.getString(R.string.sim_pins_dont_match);
                    mDialogState = ICC_NEW_MODE;
                    mPin = null;
                    showPinDialog();
                } else {
                    mError = null;
                    tryChangePin();
                }
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mPinToggle) {
            // Get the new, preferred state
            mToState = mPinToggle.isChecked();
            // Flip it back and pop up pin dialog
            mPinToggle.setChecked(!mToState);
            mDialogState = ICC_LOCK_MODE;
            showPinDialog();
        } else if (preference == mPinDialog) {
            mDialogState = ICC_OLD_MODE;
            return false;
        }
        return true;
    }

    private void tryChangeIccLockState() {
        // Try to change icc lock. If it succeeds, toggle the lock state and
        // reset dialog state. Else inject error message and show dialog again.
        new SetIccLockEnabled(mToState, mPin).execute();
        // Disable the setting till the response is received.
        mPinToggle.setEnabled(false);
    }

    private class SetIccLockEnabled extends AsyncTask<Void, Void, Void> {
        private final boolean mState;
        private final String mPassword;
        private int mAttemptsRemaining;

        private SetIccLockEnabled(boolean state, String pin) {
            mState = state;
            mPassword = pin;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mTelephonyManager =  mTelephonyManager.createForSubscriptionId(mSubId);
            mAttemptsRemaining = mTelephonyManager.setIccLockEnabled(mState, mPassword);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mAttemptsRemaining == TelephonyManager.CHANGE_ICC_LOCK_SUCCESS) {
                iccLockChanged(true, mAttemptsRemaining);
            } else {
                iccLockChanged(false, mAttemptsRemaining);
            }
        }
    }

    private void iccLockChanged(boolean success, int attemptsRemaining) {
        Log.d(TAG, "iccLockChanged: success = " + success);
        if (success) {
            mPinToggle.setChecked(mToState);
        } else {
            if (attemptsRemaining >= 0) {
                createCustomTextToast(getPinPasswordErrorMessage(attemptsRemaining));
            } else {
                if (mToState) {
                    Toast.makeText(getContext(), mRes.getString(
                            R.string.sim_pin_enable_failed), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), mRes.getString(
                            R.string.sim_pin_disable_failed), Toast.LENGTH_LONG).show();
                }
            }
        }
        mPinToggle.setEnabled(true);
        resetDialogState();
    }

    private void createCustomTextToast(CharSequence errorMessage) {
        // Cannot overlay Toast on PUK unlock screen.
        // The window type of Toast is set by NotificationManagerService.
        // It can't be overwritten by LayoutParams.type.
        // Ovarlay a custom window with LayoutParams (TYPE_STATUS_BAR_PANEL) on PUK unlock screen.
        final View v = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(com.android.internal.R.layout.transient_notification, null);
        final TextView tv = (TextView) v.findViewById(com.android.internal.R.id.message);
        tv.setText(errorMessage);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        final Configuration config = v.getContext().getResources().getConfiguration();
        final int gravity = Gravity.getAbsoluteGravity(
                getContext().getResources().getInteger(
                        com.android.internal.R.integer.config_toastDefaultGravity),
                config.getLayoutDirection());
        params.gravity = gravity;
        if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
            params.horizontalWeight = 1.0f;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
            params.verticalWeight = 1.0f;
        }
        params.y = getContext().getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.toast_y_offset);

        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.format = PixelFormat.TRANSLUCENT;
        params.windowAnimations = com.android.internal.R.style.Animation_Toast;
        params.type = WindowManager.LayoutParams.TYPE_STATUS_BAR_SUB_PANEL;
        params.setFitInsetsTypes(params.getFitInsetsTypes() & ~Type.statusBars());
        params.setTitle(errorMessage);
        params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.addView(v, params);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wm.removeViewImmediate(v);
            }
        }, LONG_DURATION_TIMEOUT);
    }

    private void iccPinChanged(boolean success, int attemptsRemaining) {
        Log.d(TAG, "iccPinChanged: success = " + success);
        if (!success) {
            createCustomTextToast(getPinPasswordErrorMessage(attemptsRemaining));
        } else {
            Toast.makeText(getContext(), mRes.getString(R.string.sim_change_succeeded),
                    Toast.LENGTH_SHORT)
                    .show();
        }
        resetDialogState();
    }

    private void tryChangePin() {
        new ChangeIccLockPassword(mOldPin, mNewPin).execute();
    }

    private class ChangeIccLockPassword extends AsyncTask<Void, Void, Void> {
        private final String mOldPwd;
        private final String mNewPwd;
        private int mAttemptsRemaining;

        private ChangeIccLockPassword(String oldPin, String newPin) {
            mOldPwd = oldPin;
            mNewPwd = newPin;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(mSubId);
            mAttemptsRemaining = mTelephonyManager.changeIccLockPassword(mOldPwd, mNewPwd);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mAttemptsRemaining == TelephonyManager.CHANGE_ICC_LOCK_SUCCESS) {
                iccPinChanged(true, mAttemptsRemaining);
            } else {
                iccPinChanged(false, mAttemptsRemaining);
            }
        }
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;

        if (attemptsRemaining == 0) {
            displayMessage = mRes.getString(R.string.wrong_pin_code_pukked);
        } else if (attemptsRemaining == 1) {
            displayMessage = mRes.getString(R.string.wrong_pin_code_one, attemptsRemaining);
        } else if (attemptsRemaining > 1) {
            displayMessage = mRes
                    .getQuantityString(R.plurals.wrong_pin_code, attemptsRemaining,
                            attemptsRemaining);
        } else {
            displayMessage = mRes.getString(R.string.pin_failed);
        }
        if (DBG) Log.d(TAG, "getPinPasswordErrorMessage:"
                + " attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    private boolean reasonablePin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH || pin.length() > MAX_PIN_LENGTH) {
            return false;
        } else {
            return true;
        }
    }

    private void resetDialogState() {
        mError = null;
        mDialogState = ICC_OLD_MODE; // Default for when Change PIN is clicked
        mPin = "";
        setDialogValues();
        mDialogState = OFF_MODE;
    }

    private String getTagForSlotId(int slotId) {
        return String.valueOf(slotId);
    }

    private int getSlotIndexFromTag(String tag) {
        int slotId = -1;
        try {
            slotId = Integer.parseInt(tag);
        } catch (NumberFormatException exception) {
        }
        return slotId;
    }

    private SubscriptionInfo getVisibleSubscriptionInfoForSimSlotIndex(int slotId) {
        final List<SubscriptionInfo> subInfoList =
                mProxySubscriptionMgr.getActiveSubscriptionsInfo();
        if (subInfoList == null) {
            return null;
        }
        final CarrierConfigManager carrierConfigManager = getContext().getSystemService(
                CarrierConfigManager.class);
        for (SubscriptionInfo subInfo : subInfoList) {
            if ((isSubscriptionVisible(carrierConfigManager, subInfo)
                    && (subInfo.getSimSlotIndex() == slotId))) {
                return subInfo;
            }
        }
        return null;
    }

    private boolean isSubscriptionVisible(CarrierConfigManager carrierConfigManager,
            SubscriptionInfo subInfo) {
        final PersistableBundle bundle = carrierConfigManager
                .getConfigForSubId(subInfo.getSubscriptionId());
        if (bundle == null) {
            return false;
        }
        return !bundle.getBoolean(CarrierConfigManager.KEY_HIDE_SIM_LOCK_SETTINGS_BOOL);
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            mSlotId = getSlotIndexFromTag(tabId);

            // The User has changed tab; update the body.
            updatePreferences();
        }
    };

    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    private TabSpec buildTabSpec(String tag, String title) {
        return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                mEmptyTabContent);
    }
}
