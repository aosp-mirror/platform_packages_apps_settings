/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.password;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.password.ChooseLockTypeDialogFragment.OnLockTypeSelectedListener;

/**
 * Setup Wizard's version of ChooseLockPassword screen. It inherits the logic and basic structure
 * from ChooseLockPassword class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPassword class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockPassword extends ChooseLockPassword {

    private static final String TAG = "SetupChooseLockPassword";

    public static Intent modifyIntentForSetup(
            Context context,
            Intent chooseLockPasswordIntent) {
        chooseLockPasswordIntent.setClass(context, SetupChooseLockPassword.class);
        chooseLockPasswordIntent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false);
        return chooseLockPasswordIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPasswordFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPasswordFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    public static class SetupChooseLockPasswordFragment extends ChooseLockPasswordFragment
            implements OnLockTypeSelectedListener {

        private static final String TAG_SKIP_SCREEN_LOCK_DIALOG = "skip_screen_lock_dialog";

        @Nullable
        private Button mOptionsButton;
        private boolean mLeftButtonIsSkip;

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            final Activity activity = getActivity();
            ChooseLockGenericController chooseLockGenericController =
                    new ChooseLockGenericController(activity, mUserId);
            boolean anyOptionsShown = chooseLockGenericController.getVisibleScreenLockTypes(
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, false).size() > 0;
            boolean showOptionsButton = activity.getIntent().getBooleanExtra(
                    ChooseLockGeneric.ChooseLockGenericFragment.EXTRA_SHOW_OPTIONS_BUTTON, false);
            if (!anyOptionsShown) {
                Log.w(TAG, "Visible screen lock types is empty!");
            }

            if (showOptionsButton && anyOptionsShown) {
                mOptionsButton = view.findViewById(R.id.screen_lock_options);
                mOptionsButton.setVisibility(View.VISIBLE);
                mOptionsButton.setOnClickListener((btn) ->
                        ChooseLockTypeDialogFragment.newInstance(mUserId)
                                .show(getChildFragmentManager(), TAG_SKIP_SCREEN_LOCK_DIALOG));
            }
        }

        @Override
        protected void onSkipOrClearButtonClick(View view) {
            if (mLeftButtonIsSkip) {
                SetupSkipDialog dialog = SetupSkipDialog.newInstance(
                        getActivity().getIntent()
                                .getBooleanExtra(SetupSkipDialog.EXTRA_FRP_SUPPORTED, false),
                        /* isPatternMode= */ false,
                        mIsAlphaMode,
                        getActivity().getIntent()
                                .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT,
                                        false),
                        getActivity().getIntent()
                                .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, false)

                );
                dialog.show(getFragmentManager());
                return;
            }
            super.onSkipOrClearButtonClick(view);
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            // Setup wizard's redaction interstitial is deferred to optional step. Enable that
            // optional step if the lock screen was set up.
            SetupRedactionInterstitial.setEnabled(context, true);
            return null;
        }

        @Override
        public void onLockTypeSelected(ScreenLockType lock) {
            ScreenLockType currentLockType = mIsAlphaMode ?
                    ScreenLockType.PASSWORD : ScreenLockType.PIN;
            if (lock == currentLockType) {
                return;
            }
            startChooseLockActivity(lock, getActivity());
        }

        @Override
        protected int getStageType() {
            // Return TYPE_NONE to make generic lock screen launch in Setup wizard flow before
            // fingerprint and face setup.
            return Stage.TYPE_NONE;
        }

        @Override
        protected void updateUi() {
            super.updateUi();
            // Show the skip button during SUW but not during Settings > Biometric Enrollment
            if (mUiStage == Stage.Introduction) {
                mSkipOrClearButton.setText(getActivity(), R.string.skip_label);
                mLeftButtonIsSkip = true;
            } else {
                mSkipOrClearButton.setText(getActivity(), R.string.lockpassword_clear_label);
                mLeftButtonIsSkip = false;
            }

            if (mOptionsButton != null) {
                mOptionsButton.setVisibility(
                        mUiStage == Stage.Introduction ? View.VISIBLE : View.GONE);
            }
        }
    }
}
