/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import com.google.android.setupdesign.GlifLayout;
import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Fragment to show screens that auto advance during private space setup flow */
public class AutoAdvanceSetupFragment extends InstrumentedFragment {
    private static final String TAG = "AutoAdvanceFragment";
    private static final String TITLE_INDEX = "title_index";
    private static final int DELAY_BETWEEN_SCREENS = 5000; // 5 seconds in millis
    private static final int ANIMATION_DURATION_MILLIS = 500;
    private static final int HEADER_TEXT_MAX_LINES = 4;
    private GlifLayout mRootView;
    private Handler mHandler;
    private int mScreenTitleIndex;
    private static final List<Pair<Integer, Integer>> HEADER_IMAGE_PAIRS =
            ImmutableList.of(
                    new Pair(R.string.private_space_notifications_hidden_title,
                            R.drawable.privatespace_setup_flow_placeholder),
                    new Pair(R.string.private_space_share_photos_title,
                            R.drawable.privatespace_setup_flow_placeholder),
                    new Pair(R.string.private_space_apps_installed_title,
                            R.drawable.privatespace_setup_flow_placeholder));

    private Runnable mUpdateScreenResources =
            new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        if (++mScreenTitleIndex < HEADER_IMAGE_PAIRS.size()) {
                            startFadeOutAnimation();
                            mHandler.postDelayed(mUpdateScreenResources, DELAY_BETWEEN_SCREENS);
                        } else if (PrivateSpaceMaintainer.getInstance(getActivity())
                                .doesPrivateSpaceExist()) {
                            mMetricsFeatureProvider.action(
                                    getContext(),
                                    SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_SPACE_CREATED,
                                    true);
                            NavHostFragment.findNavController(AutoAdvanceSetupFragment.this)
                                    .navigate(R.id.action_set_lock_fragment);
                        } else {
                            mMetricsFeatureProvider.action(
                                    getContext(),
                                    SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_SPACE_CREATED,
                                    false);
                            showPrivateSpaceErrorScreen();
                        }
                    }
                }
            };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (android.os.Flags.allowPrivateProfile()) {
            super.onCreate(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (PrivateSpaceMaintainer.getInstance(getActivity()).createPrivateSpace()) {
                Log.i(TAG, "Private Space created");
            }
        } else {
            mScreenTitleIndex = savedInstanceState.getInt(TITLE_INDEX);
            if (mScreenTitleIndex >= HEADER_IMAGE_PAIRS.size()) {
                return super.onCreateView(inflater, container, savedInstanceState);
            }
        }
        mRootView =
                (GlifLayout)
                        inflater.inflate(R.layout.privatespace_advancing_screen, container, false);
        mRootView.getHeaderTextView().setMaxLines(HEADER_TEXT_MAX_LINES);
        updateHeaderAndImage();
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(mUpdateScreenResources, DELAY_BETWEEN_SCREENS);
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        // Handle the back button event. We intentionally don't want to allow back
                        // button to work in this screen during the setup flow.
                    }
                };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
        return mRootView;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TITLE_INDEX, mScreenTitleIndex);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mUpdateScreenResources);
        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETUP_SPACE_CREATION;
    }

    private void showPrivateSpaceErrorScreen() {
        NavHostFragment.findNavController(AutoAdvanceSetupFragment.this)
                .navigate(R.id.action_advance_profile_error);
    }

    private void updateHeaderAndImage() {
        mRootView.setHeaderText(HEADER_IMAGE_PAIRS.get(mScreenTitleIndex).first);
        ((ImageView) mRootView.findViewById(R.id.placeholder_image))
                .setImageResource(HEADER_IMAGE_PAIRS.get(mScreenTitleIndex).second);
        startFadeInAnimation();
    }

    private  void startFadeInAnimation() {
        ValueAnimator textView =  ObjectAnimator.ofFloat(
                mRootView.getHeaderTextView(), View.ALPHA, 0f, 1f);
        ValueAnimator imageView = ObjectAnimator.ofFloat(
                mRootView.findViewById(R.id.placeholder_image), View.ALPHA, 0, 1f);
        AnimatorSet fadeIn = new AnimatorSet();
        fadeIn.playTogether(textView, imageView);
        fadeIn.setDuration(ANIMATION_DURATION_MILLIS).start();
    }

    private void startFadeOutAnimation() {
        AnimatorSet fadeOut = new AnimatorSet();
        ValueAnimator textView =  ObjectAnimator.ofFloat(
                mRootView.getHeaderTextView(), View.ALPHA, 1f, 0f);
        ValueAnimator imageView = ObjectAnimator.ofFloat(
                mRootView.findViewById(R.id.placeholder_image), View.ALPHA, 1f, 0f);
        fadeOut.playTogether(textView, imageView);
        fadeOut.setDuration(ANIMATION_DURATION_MILLIS).start();
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                updateHeaderAndImage();
            }
        });
    }
}
