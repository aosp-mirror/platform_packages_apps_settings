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

import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ChooseLockPatternExample extends PreferenceActivity {

    // required constructor for fragments
    public ChooseLockPatternExample() {

    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ChooseLockPatternExampleFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    public static class ChooseLockPatternExampleFragment extends Fragment
            implements View.OnClickListener {
        private static final long START_DELAY = 1000;
        protected static final String TAG = "Settings";
        private View mNextButton;
        private View mSkipButton;
        private View mImageView;
        private AnimationDrawable mAnimation;
        private Handler mHandler = new Handler();
        private Runnable mRunnable = new Runnable() {
            public void run() {
                startAnimation(mAnimation);
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.choose_lock_pattern_example, null);
            mNextButton = view.findViewById(R.id.next_button);
            mNextButton.setOnClickListener(this);

            mSkipButton = view.findViewById(R.id.skip_button);
            mSkipButton.setOnClickListener(this);

            mImageView = (ImageView) view.findViewById(R.id.lock_anim);
            mImageView.setBackgroundResource(R.drawable.lock_anim);
            mImageView.setOnClickListener(this);
            mAnimation = (AnimationDrawable) mImageView.getBackground();
            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            mHandler.postDelayed(mRunnable, START_DELAY);
        }

        @Override
        public void onPause() {
            super.onPause();
            stopAnimation(mAnimation);
        }

        public void onClick(View v) {
            if (v == mSkipButton) {
                // Canceling, so finish all
                getActivity().setResult(ChooseLockPattern.RESULT_FINISHED);
                getActivity().finish();
            } else if (v == mNextButton) {
                stopAnimation(mAnimation);
                Intent intent = new Intent(getActivity(), ChooseLockPattern.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                intent.putExtra("confirm_credentials", false);
                startActivity(intent);
                getActivity().finish();
            }
        }

        protected void startAnimation(final AnimationDrawable animation) {
            if (animation != null && !animation.isRunning()) {
                animation.run();
            }
        }

        protected void stopAnimation(final AnimationDrawable animation) {
            if (animation != null && animation.isRunning()) animation.stop();
        }
    }
}

