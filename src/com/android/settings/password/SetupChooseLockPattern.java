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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;

/**
 * Setup Wizard's version of ChooseLockPattern screen. It inherits the logic and basic structure
 * from ChooseLockPattern class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPattern class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockPattern extends ChooseLockPattern {

    public static Intent modifyIntentForSetup(Context context, Intent chooseLockPatternIntent) {
        chooseLockPatternIntent.setClass(context, SetupChooseLockPattern.class);
        return chooseLockPatternIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPatternFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPatternFragment.class;
    }

    public static class SetupChooseLockPatternFragment extends ChooseLockPatternFragment
            implements ChooseLockTypeDialogFragment.OnLockTypeSelectedListener {

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            Button optionsButton = (Button) view.findViewById(R.id.screen_lock_options);
            optionsButton.setVisibility(View.VISIBLE);
            optionsButton.setOnClickListener((btn) ->
                    ChooseLockTypeDialogFragment.newInstance(mUserId)
                            .show(getChildFragmentManager(), null));
        }

        @Override
        public void onLockTypeSelected(ScreenLockType lock) {
            if (ScreenLockType.PATTERN == lock) {
                return;
            }
            startChooseLockActivity(lock, getActivity());
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            // Setup wizard's redaction interstitial is deferred to optional step. Enable that
            // optional step if the lock screen was set up.
            SetupRedactionInterstitial.setEnabled(context, true);
            return null;
        }
    }
}
