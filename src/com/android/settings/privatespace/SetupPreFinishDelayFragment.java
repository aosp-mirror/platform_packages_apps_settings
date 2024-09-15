/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.privatespace;

import static android.content.Intent.ACTION_PROFILE_INACCESSIBLE;
import static android.content.Intent.ACTION_PROFILE_UNAVAILABLE;

import static com.android.settings.privatespace.PrivateSpaceMaintainer.PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import com.google.android.setupdesign.GlifLayout;

public class SetupPreFinishDelayFragment extends InstrumentedFragment {
    private static final String TAG = "SetupPreFinishDelayFrag";
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static final int MAX_DELAY_BEFORE_SETUP_FINISH = 5000;
    private boolean mActionProfileUnavailable;
    private boolean mActionProfileInaccessible;

    protected final BroadcastReceiver mBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) {
                        return;
                    }
                    String action = intent.getAction();
                    Log.i(TAG, "Received broadcast: " + action);
                    if (ACTION_PROFILE_UNAVAILABLE.equals(action)) {
                        mActionProfileUnavailable = true;
                    } else if (ACTION_PROFILE_INACCESSIBLE.equals(action)) {
                        mActionProfileInaccessible = true;
                    }
                    if (mActionProfileUnavailable && mActionProfileInaccessible) {
                        showSetupSuccessScreen();
                    }
                }
            };

    private Runnable mRunnable =
            () -> {
                showSetupSuccessScreen();
            };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enablePrivateSpaceFeatures()) {
            super.onCreate(savedInstanceState);
        }
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        GlifLayout rootView =
                (GlifLayout)
                        inflater.inflate(R.layout.private_space_wait_screen, container, false);
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        // Handle the back button event. We intentionally don't want to allow back
                        // button to work in this screen during the setup flow.
                    }
                };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
        if (savedInstanceState == null) {
            // TODO(b/307729746): Add test to verify PS is locked and auto-lock value is set to
            // auto-lock on device lock after setup completion.
            PrivateSpaceMaintainer privateSpaceMaintainer =
                    PrivateSpaceMaintainer.getInstance(getActivity());
            privateSpaceMaintainer.setPrivateSpaceAutoLockSetting(
                    PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL);
            privateSpaceMaintainer.lockPrivateSpace();
        }
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PROFILE_UNAVAILABLE);
        intentFilter.addAction(ACTION_PROFILE_INACCESSIBLE);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);
        sHandler.postDelayed(mRunnable, MAX_DELAY_BEFORE_SETUP_FINISH);
    }

    @Override
    public void onDestroy() {
        sHandler.removeCallbacks(mRunnable);
        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETUP_PRE_FINISH;
    }

    private void showSetupSuccessScreen() {
        sHandler.removeCallbacks(mRunnable);
        NavHostFragment.findNavController(SetupPreFinishDelayFragment.this)
                .navigate(R.id.action_success_fragment);
    }
}
