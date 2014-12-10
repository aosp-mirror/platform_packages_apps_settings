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
import android.view.WindowInsets;

/**
 * Setup Wizard's version of ChooseLockPassword screen. It inherits the logic and basic structure
 * from ChooseLockPassword class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPassword class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockPassword extends ChooseLockPassword
        implements SetupWizardNavBar.NavigationBarListener {

    public static Intent createIntent(Context context, int quality, final boolean isFallback,
            int minLength, final int maxLength, boolean requirePasswordToDecrypt,
            boolean confirmCredentials) {
        Intent intent = ChooseLockPassword.createIntent(context, quality, isFallback, minLength,
                maxLength, requirePasswordToDecrypt, confirmCredentials);
        intent.setClass(context, SetupChooseLockPassword.class);
        intent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false);
        return intent;
    }

    private SetupWizardNavBar mNavigationBar;
    private SetupChooseLockPasswordFragment mFragment;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPasswordFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPasswordFragment.class;
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
            mFragment.handleNext();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof SetupChooseLockPasswordFragment) {
            mFragment = (SetupChooseLockPasswordFragment) fragment;
        }
    }

    public static class SetupChooseLockPasswordFragment extends ChooseLockPasswordFragment
            implements View.OnApplyWindowInsetsListener {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.setup_template, container, false);
            View scrollView = view.findViewById(R.id.bottom_scroll_view);
            scrollView.setOnApplyWindowInsetsListener(this);
            ViewGroup setupContent = (ViewGroup) view.findViewById(R.id.setup_content);
            inflater.inflate(R.layout.setup_choose_lock_password, setupContent, true);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
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
        protected void setNextEnabled(boolean enabled) {
            SetupChooseLockPassword activity = (SetupChooseLockPassword) getActivity();
            activity.mNavigationBar.getNextButton().setEnabled(enabled);
        }

        @Override
        protected void setNextText(int text) {
            SetupChooseLockPassword activity = (SetupChooseLockPassword) getActivity();
            activity.mNavigationBar.getNextButton().setText(text);
        }

        @Override
        public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
            SetupChooseLockPassword activity = (SetupChooseLockPassword) getActivity();
            final int bottomMargin = Math.max(insets.getSystemWindowInsetBottom()
                    - activity.mNavigationBar.getView().getHeight(), 0);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, bottomMargin);
            view.setLayoutParams(lp);
            return insets.replaceSystemWindowInsets(
                    insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    0 /* bottom */);
        }
    }
}
