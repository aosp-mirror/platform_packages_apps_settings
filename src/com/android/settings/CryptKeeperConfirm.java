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
import android.app.Fragment;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.storage.IMountService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.internal.widget.LockPatternUtils;

import java.util.Locale;

public class CryptKeeperConfirm extends Fragment {

    private static final String TAG = "CryptKeeperConfirm";

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

                    IMountService mountService = IMountService.Stub.asInterface(service);
                    try {
                        Bundle args = getIntent().getExtras();
                        mountService.encryptStorage(args.getInt("type", -1), args.getString("password"));
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
            utils.setVisiblePatternEnabled(utils.isVisiblePatternEnabled());
            if (utils.isOwnerInfoEnabled()) {
                utils.setOwnerInfo(utils.getOwnerInfo(UserHandle.USER_OWNER),
                                   UserHandle.USER_OWNER);
            }
            Intent intent = new Intent(getActivity(), Blank.class);
            intent.putExtras(getArguments());
            startActivity(intent);

            // 2. The system locale.
            try {
                IBinder service = ServiceManager.getService("mount");
                IMountService mountService = IMountService.Stub.asInterface(service);
                mountService.setField("SystemLocale", Locale.getDefault().toLanguageTag());
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.crypt_keeper_confirm, null);
        establishFinalConfirmationState();
        return mContentView;
    }
}
