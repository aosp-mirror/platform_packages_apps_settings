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

import static com.android.settings.privatespace.PrivateSpaceSetupActivity.ACCOUNT_LOGIN_ACTION;
import static com.android.settings.privatespace.PrivateSpaceSetupActivity.EXTRA_ACTION_TYPE;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
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

/** Fragment to a show loading screen and create private profile during private space setup flow */
public class PrivateSpaceCreationFragment extends InstrumentedFragment {
    private static final String TAG = "PrivateSpaceCreateFrag";
    private static final int PRIVATE_SPACE_CREATE_POST_DELAY_MS = 1000;
    private static final int PRIVATE_SPACE_ACCOUNT_LOGIN_POST_DELAY_MS = 5000;
    private static final int PRIVATE_SPACE_SETUP_NO_ERROR = 0;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private Runnable mRunnable =
            () -> {
                createPrivateSpace();
            };

    private Runnable mAccountLoginRunnable =
            () -> {
                unRegisterReceiver();
                startAccountLogin();
            };

    final BroadcastReceiver mProfileAccessReceiver = new  BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_PROFILE_ACCESSIBLE)) {
                Log.i(TAG, "onReceive " + action);
                sHandler.removeCallbacks(mAccountLoginRunnable);
                sHandler.post(mAccountLoginRunnable);
            }
        }
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
                        inflater.inflate(R.layout.private_space_create_screen, container, false);
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        // Handle the back button event. We intentionally don't want to allow back
                        // button to work in this screen during the setup flow.
                    }
                };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ensures screen visibility to user by introducing a 1-second delay before creating private
        // space.
        sHandler.removeCallbacks(mRunnable);
        sHandler.postDelayed(mRunnable, PRIVATE_SPACE_CREATE_POST_DELAY_MS);
    }

    @Override
    public void onDestroy() {
        sHandler.removeCallbacks(mRunnable);
        super.onDestroy();
    }

    private void createPrivateSpace() {
        if (PrivateSpaceMaintainer.getInstance(getActivity()).createPrivateSpace()) {
            Log.i(TAG, "Private Space created");
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_SPACE_CREATED, true);
            if (android.multiuser.Flags.showDifferentCreationErrorForUnsupportedDevices()) {
                mMetricsFeatureProvider.action(
                        getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_SPACE_ERRORS,
                        PRIVATE_SPACE_SETUP_NO_ERROR);
            }
            if (isConnectedToInternet()) {
                registerReceiver();
                sHandler.postDelayed(
                        mAccountLoginRunnable, PRIVATE_SPACE_ACCOUNT_LOGIN_POST_DELAY_MS);
            } else {
                NavHostFragment.findNavController(PrivateSpaceCreationFragment.this)
                        .navigate(R.id.action_set_lock_fragment);
            }
        } else {
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_SPACE_CREATED,
                    false);
            if (android.multiuser.Flags.showDifferentCreationErrorForUnsupportedDevices()) {
                int errorCode = PrivateSpaceMaintainer.getInstance(
                        getActivity()).getPrivateSpaceCreateError();
                mMetricsFeatureProvider.action(
                        getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_SPACE_ERRORS,
                        errorCode);
                showPrivateSpaceErrorScreen(errorCode);
            } else {
                showPrivateSpaceErrorScreen();
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETUP_SPACE_CREATION;
    }

    private void showPrivateSpaceErrorScreen() {
        NavHostFragment.findNavController(PrivateSpaceCreationFragment.this)
                .navigate(R.id.action_create_profile_error);
    }

    private void showPrivateSpaceErrorScreen(int errorCode) {
        if (errorCode == UserManager.USER_OPERATION_ERROR_USER_RESTRICTED
                || errorCode == UserManager.USER_OPERATION_ERROR_PRIVATE_PROFILE) {
            NavHostFragment.findNavController(PrivateSpaceCreationFragment.this)
                    .navigate(R.id.action_create_profile_error_restrict);
        } else {
            showPrivateSpaceErrorScreen();
        }
    }

    /** Returns true if device has an active internet connection, false otherwise. */
    private boolean isConnectedToInternet() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /** Start new activity in private profile to add an account to private profile */
    private void startAccountLogin() {
        if (isAdded() && getContext() != null && getActivity() != null) {
            Intent intent = new Intent(getContext(), PrivateProfileContextHelperActivity.class);
            intent.putExtra(EXTRA_ACTION_TYPE, ACCOUNT_LOGIN_ACTION);
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_ACCOUNT_LOGIN_START);
            getActivity().startActivityForResult(intent, ACCOUNT_LOGIN_ACTION);
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PROFILE_ACCESSIBLE);
        if (getContext() != null) {
            getContext().registerReceiver(mProfileAccessReceiver, filter);
        }
    }

    private void unRegisterReceiver() {
        if (mProfileAccessReceiver != null && isAdded() && getContext() != null) {
            getContext().unregisterReceiver(mProfileAccessReceiver);
        }
    }
}
