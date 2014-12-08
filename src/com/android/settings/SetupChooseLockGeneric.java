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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.MutableBoolean;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Setup Wizard's version of ChooseLockGeneric screen. It inherits the logic and basic structure
 * from ChooseLockGeneric class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockGeneric class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockGeneric extends ChooseLockGeneric
        implements SetupWizardNavBar.NavigationBarListener {

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockGenericFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends PreferenceFragment> getFragmentClass() {
        return SetupChooseLockGenericFragment.class;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent(), resid);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        SetupWizardUtils.setImmersiveMode(this, bar);
        bar.getNextButton().setEnabled(false);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
    }

    public static class SetupChooseLockGenericFragment extends ChooseLockGenericFragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.setup_preference, container, false);
            ListView list = (ListView) view.findViewById(android.R.id.list);
            View title = view.findViewById(R.id.title);
            if (title == null) {
                final View header = inflater.inflate(R.layout.setup_wizard_header, list, false);
                list.addHeaderView(header, null, false);
            }
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setIllustration(getActivity(),
                    R.drawable.setup_illustration_lock_screen);
            SetupWizardUtils.setHeaderText(getActivity(), getActivity().getTitle());
        }

        /***
         * Disables preferences that are less secure than required quality and shows only secure
         * screen lock options here.
         *
         * @param quality the requested quality.
         * @param allowBiometric whether to allow biometic screen lock
         */
        @Override
        protected void disableUnusablePreferences(final int quality,
                MutableBoolean allowBiometric) {
            // At this part of the flow, the user has already indicated they want to add a pin,
            // pattern or password, so don't show "None" or "Slide". We disable them here and set
            // the HIDE_DISABLED flag to true to hide them. This only happens for setup wizard.
            // We do the following max check here since the device may already have a Device Admin
            // installed with a policy we need to honor.
            final int newQuality = Math.max(quality,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
            super.disableUnusablePreferencesImpl(newQuality, allowBiometric,
                    true /* hideDisabled */);
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality, boolean isFallback,
                int minLength, int maxLength, boolean requirePasswordToDecrypt,
                boolean confirmCredentials) {
            final Intent intent = SetupChooseLockPassword.createIntent(context, quality,
                    isFallback, minLength, maxLength, requirePasswordToDecrypt, confirmCredentials);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, boolean isFallback,
                boolean requirePassword, boolean confirmCredentials) {
            final Intent intent = SetupChooseLockPattern.createIntent(context, isFallback,
                    requirePassword, confirmCredentials);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getEncryptionInterstitialIntent(Context context, int quality,
                boolean required) {
            Intent intent = SetupEncryptionInterstitial.createStartIntent(context, quality,
                    required);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }
    }
}
