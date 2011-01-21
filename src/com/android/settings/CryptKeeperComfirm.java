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

import android.app.Fragment;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class CryptKeeperComfirm extends Fragment {

    private View mContentView;
    private Button mFinalButton;
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }
            
            IBinder service = ServiceManager.getService("mount");
            if (service == null) {
                return;
            }

            IMountService mountService = IMountService.Stub.asInterface(service);
            try {
                Bundle args = getArguments();
                mountService.encryptStorage(args.getString("password"));
            } catch (Exception e) {
                Log.e("CryptKeeper", "Error while encrypting...", e);
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
