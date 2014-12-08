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

package com.android.settings;

import com.android.setupwizard.navigationbar.SetupWizardNavBar;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Setup Wizard's version of ChooseLockPattern screen. It inherits the logic and basic structure
 * from ChooseLockPattern class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPattern class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockPattern extends ChooseLockPattern
        implements SetupWizardNavBar.NavigationBarListener {

    public static Intent createIntent(Context context, final boolean isFallback,
            boolean requirePassword, boolean confirmCredentials) {
        Intent intent = ChooseLockPattern.createIntent(context, isFallback, requirePassword,
                confirmCredentials);
        intent.setClass(context, SetupChooseLockPattern.class);
        return intent;
    }

    private SetupWizardNavBar mNavigationBar;
    private SetupChooseLockPatternFragment mFragment;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPatternFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPatternFragment.class;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent(), resid);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        mNavigationBar = bar;
        SetupWizardUtils.setImmersiveMode(this, bar);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        if (mFragment != null) {
            mFragment.handleRightButton();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof ChooseLockPatternFragment) {
            mFragment = (SetupChooseLockPatternFragment) fragment;
        }
    }

    public static class SetupChooseLockPatternFragment extends ChooseLockPatternFragment {

        private Button mRetryButton;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.setup_template, container, false);
            ViewGroup setupContent = (ViewGroup) view.findViewById(R.id.setup_content);
            inflater.inflate(R.layout.setup_choose_lock_pattern, setupContent, true);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            mRetryButton = (Button) view.findViewById(R.id.retryButton);
            mRetryButton.setOnClickListener(this);
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setIllustration(getActivity(),
                    R.drawable.setup_illustration_lock_screen);
            SetupWizardUtils.setHeaderText(getActivity(), getActivity().getTitle());
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            Intent intent = SetupRedactionInterstitial.createStartIntent(context);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        public void onClick(View v) {
            if (v == mRetryButton) {
                handleLeftButton();
            } else {
                super.onClick(v);
            }
        }

        @Override
        protected void setRightButtonEnabled(boolean enabled) {
            SetupChooseLockPattern activity = (SetupChooseLockPattern) getActivity();
            activity.mNavigationBar.getNextButton().setEnabled(enabled);
        }

        @Override
        protected void setRightButtonText(int text) {
            SetupChooseLockPattern activity = (SetupChooseLockPattern) getActivity();
            activity.mNavigationBar.getNextButton().setText(text);
        }

        @Override
        protected void updateStage(Stage stage) {
            super.updateStage(stage);
            // Only enable the button for retry
            mRetryButton.setEnabled(stage == Stage.FirstChoiceValid);
        }
    }
}
