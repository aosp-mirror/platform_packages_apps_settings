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

import com.android.internal.widget.LockPatternUtils;
import com.android.setupwizardlib.SetupWizardListLayout;
import com.android.setupwizardlib.view.NavigationBar;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Setup Wizard's version of ChooseLockGeneric screen. It inherits the logic and basic structure
 * from ChooseLockGeneric class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockGeneric class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockGeneric extends ChooseLockGeneric {

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
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    public static class SetupChooseLockGenericFragment extends ChooseLockGenericFragment
            implements NavigationBar.NavigationBarListener {

        private static final String EXTRA_PASSWORD_QUALITY = ":settings:password_quality";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final SetupWizardListLayout layout = (SetupWizardListLayout) inflater.inflate(
                    R.layout.setup_choose_lock_generic, container, false);
            layout.setHeaderText(getActivity().getTitle());

            final NavigationBar navigationBar = layout.getNavigationBar();
            navigationBar.getNextButton().setEnabled(false);
            navigationBar.setNavigationBarListener(this);

            return layout;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setImmersiveMode(getActivity());
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode != RESULT_CANCELED) {
                if (data == null) {
                    data = new Intent();
                }
                // Add the password quality extra to the intent data that will be sent back for
                // Setup Wizard.
                LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
                data.putExtra(EXTRA_PASSWORD_QUALITY,
                        lockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()));

                super.onActivityResult(requestCode, resultCode, data);
            }
            // If the started activity was cancelled (e.g. the user presses back), then this
            // activity will be resumed to foreground.
        }

        /***
         * Disables preferences that are less secure than required quality and shows only secure
         * screen lock options here.
         *
         * @param quality the requested quality.
         */
        @Override
        protected void disableUnusablePreferences(final int quality, boolean hideDisabled) {
            // At this part of the flow, the user has already indicated they want to add a pin,
            // pattern or password, so don't show "None" or "Slide". We disable them here and set
            // the HIDE_DISABLED flag to true to hide them. This only happens for setup wizard.
            // We do the following max check here since the device may already have a Device Admin
            // installed with a policy we need to honor.
            final int newQuality = Math.max(quality,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
            super.disableUnusablePreferencesImpl(newQuality, true /* hideDisabled */);
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, boolean confirmCredentials) {
            final Intent intent = SetupChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, confirmCredentials);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, long challenge) {
            final Intent intent = SetupChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, challenge);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality, int minLength,
                final int maxLength, boolean requirePasswordToDecrypt, String password) {
            final Intent intent = SetupChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, password);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final boolean confirmCredentials) {
            final Intent intent = SetupChooseLockPattern.createIntent(context, requirePassword,
                    confirmCredentials);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                long challenge) {
            final Intent intent = SetupChooseLockPattern.createIntent(context, requirePassword,
                    challenge);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final String pattern) {
            final Intent intent = SetupChooseLockPattern.createIntent(context, requirePassword,
                    pattern);
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

        @Override
        public void onNavigateBack() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        @Override
        public void onNavigateNext() {
        }
    }
}
