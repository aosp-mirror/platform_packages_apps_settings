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

import java.util.ArrayList;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ChooseLockPatternTutorial extends PreferenceActivity {

    // required constructor for fragments
    public ChooseLockPatternTutorial() {

    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ChooseLockPatternTutorialFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_pattern_header);
        showBreadCrumbs(msg, msg);
    }

    public static class ChooseLockPatternTutorialFragment extends Fragment
            implements View.OnClickListener {
        private View mNextButton;
        private View mSkipButton;
        private LockPatternView mPatternView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Don't show the tutorial if the user has seen it before.
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            if (savedInstanceState == null && lockPatternUtils.isPatternEverChosen()) {
                Intent intent = new Intent(getActivity(), ChooseLockPattern.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                intent.putExtra("confirm_credentials", false);
                final boolean isFallback = getActivity().getIntent()
                    .getBooleanExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, false);
                intent.putExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK,
                                isFallback);
                startActivity(intent);
                getActivity().finish();
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.choose_lock_pattern_tutorial, null);
            mNextButton = view.findViewById(R.id.next_button);
            mNextButton.setOnClickListener(this);
            mSkipButton = view.findViewById(R.id.skip_button);
            mSkipButton.setOnClickListener(this);

            // Set up LockPatternView to be a non-interactive demo animation
            mPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            ArrayList<LockPatternView.Cell> demoPattern = new ArrayList<LockPatternView.Cell>();
            demoPattern.add(LockPatternView.Cell.of(0,0));
            demoPattern.add(LockPatternView.Cell.of(0,1));
            demoPattern.add(LockPatternView.Cell.of(1,1));
            demoPattern.add(LockPatternView.Cell.of(2,1));
            mPatternView.setPattern(LockPatternView.DisplayMode.Animate, demoPattern);
            mPatternView.disableInput();

            return view;
        }

        public void onClick(View v) {
            if (v == mSkipButton) {
                // Canceling, so finish all
                getActivity().setResult(ChooseLockPattern.RESULT_FINISHED);
                getActivity().finish();
            } else if (v == mNextButton) {
                final boolean isFallback = getActivity().getIntent()
                    .getBooleanExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, false);
                Intent intent = new Intent(getActivity(), ChooseLockPattern.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                intent.putExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK,
                                isFallback);
                startActivity(intent);
                getActivity().overridePendingTransition(0, 0); // no animation
                getActivity().finish();
            }
        }
    }
}
