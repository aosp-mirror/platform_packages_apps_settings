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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

/** Fragment to a show loading screen and create private profile during private space setup flow */
public class PrivateSpaceCreationFragment extends InstrumentedFragment {
    private static final String TAG = "PrivateSpaceCreateFrag";
    private static final int PRIVATE_SPACE_CREATE_POST_DELAY_MS = 1000;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private Runnable mRunnable =
            () -> {
                createPrivateSpace();
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
            if (isConnectedToInternet()) {
                NavHostFragment.findNavController(PrivateSpaceCreationFragment.this)
                        .navigate(R.id.action_account_intro_fragment);
            } else {
                NavHostFragment.findNavController(PrivateSpaceCreationFragment.this)
                        .navigate(R.id.action_set_lock_fragment);
            }
        } else {
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_SPACE_CREATED, false);
            showPrivateSpaceErrorScreen();
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

    /** Returns true if device has an active internet connection, false otherwise. */
    private boolean isConnectedToInternet() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
