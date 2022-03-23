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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.StatusBarManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.InstrumentedFragment;

import java.util.Arrays;
import java.util.Locale;

public class CryptKeeperConfirm extends InstrumentedFragment {

    private static final String TAG = "CryptKeeperConfirm";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CRYPT_KEEPER_CONFIRM;
    }

    public static class Blank extends Activity {
        private Handler mHandler = new Handler();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.crypt_keeper_blank);

            if (Utils.isMonkeyRunning()) {
                finish();
            }

            StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
            sbm.disable(StatusBarManager.DISABLE_EXPAND
                    | StatusBarManager.DISABLE_NOTIFICATION_ICONS
                    | StatusBarManager.DISABLE_NOTIFICATION_ALERTS
                    | StatusBarManager.DISABLE_SYSTEM_INFO
                    | StatusBarManager.DISABLE_HOME
                    | StatusBarManager.DISABLE_SEARCH
                    | StatusBarManager.DISABLE_RECENT
                    | StatusBarManager.DISABLE_BACK);

            // Post a delayed message in 700 milliseconds to enable encryption.
            // NOTE: The animation on this activity is set for 500 milliseconds
            // I am giving it a little extra time to complete.
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    IBinder service = ServiceManager.getService("mount");
                    if (service == null) {
                        Log.e("CryptKeeper", "Failed to find the mount service");
                        finish();
                        return;
                    }

                    IStorageManager storageManager = IStorageManager.Stub.asInterface(service);
                    try {
                        Bundle args = getIntent().getExtras();
                        // TODO(b/120484642): Update vold to accept a password as a byte array
                        byte[] passwordBytes = args.getByteArray("password");
                        String password = passwordBytes != null ? new String(passwordBytes) : null;
                        Arrays.fill(passwordBytes, (byte) 0);
                        storageManager.encryptStorage(args.getInt("type", -1),
                                password);
                    } catch (Exception e) {
                        Log.e("CryptKeeper", "Error while encrypting...", e);
                    }
                }
            }, 700);
        }
    }

    private View mContentView;
    private Button mFinalButton;
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }

            /* WARNING - nasty hack!
               Settings for the lock screen are not available to the crypto
               screen (CryptKeeper) at boot. We duplicate the ones that
               CryptKeeper needs to the crypto key/value store when they are
               modified (see LockPatternUtils).
               However, prior to encryption, the crypto key/value store is not
               persisted across reboots, thus modified settings are lost to
               CryptKeeper.
               In order to make sure CryptKeeper had the correct settings after
               first encrypting, we thus need to rewrite them, which ensures the
               crypto key/value store is up to date. On encryption, this store
               is then persisted, and the settings will be there on future
               reboots.
             */

            // 1. The owner info.
            LockPatternUtils utils = new LockPatternUtils(getActivity());
            utils.setVisiblePatternEnabled(
                    utils.isVisiblePatternEnabled(UserHandle.USER_SYSTEM),
                    UserHandle.USER_SYSTEM);
            if (utils.isOwnerInfoEnabled(UserHandle.USER_SYSTEM)) {
                utils.setOwnerInfo(utils.getOwnerInfo(UserHandle.USER_SYSTEM),
                                   UserHandle.USER_SYSTEM);
            }
            int value = Settings.System.getInt(getContext().getContentResolver(),
                                               Settings.System.TEXT_SHOW_PASSWORD,
                                               1);
            utils.setVisiblePasswordEnabled(value != 0, UserHandle.USER_SYSTEM);

            Intent intent = new Intent(getActivity(), Blank.class);
            intent.putExtras(getArguments());
            startActivity(intent);

            // 2. The system locale.
            try {
                IBinder service = ServiceManager.getService("mount");
                IStorageManager storageManager = IStorageManager.Stub.asInterface(service);
                storageManager.setField("SystemLocale", Locale.getDefault().toLanguageTag());
            } catch (Exception e) {
                Log.e(TAG, "Error storing locale for decryption UI", e);
            }
        }
    };

    private void establishFinalConfirmationState() {
        mFinalButton = (Button) mContentView.findViewById(R.id.execute_encrypt);
        mFinalButton.setOnClickListener(mFinalClickListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.crypt_keeper_confirm_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.crypt_keeper_confirm, null);
        establishFinalConfirmationState();
        return mContentView;
    }
}
