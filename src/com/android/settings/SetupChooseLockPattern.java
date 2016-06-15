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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.UserHandle;

/**
 * Setup Wizard's version of ChooseLockPattern screen. It inherits the logic and basic structure
 * from ChooseLockPattern class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPattern class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockPattern extends ChooseLockPattern {

    public static Intent createIntent(Context context, boolean requirePassword,
            boolean confirmCredentials) {
        Intent intent = ChooseLockPattern.createIntent(context, requirePassword,
                confirmCredentials, UserHandle.myUserId());
        intent.setClass(context, SetupChooseLockPattern.class);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, String pattern) {
        Intent intent = ChooseLockPattern.createIntent(
                context, requirePassword, pattern, UserHandle.myUserId());
        intent.setClass(context, SetupChooseLockPattern.class);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, long challenge) {
        Intent intent = ChooseLockPattern.createIntent(
                context, requirePassword, challenge, UserHandle.myUserId());
        intent.setClass(context, SetupChooseLockPattern.class);
        return intent;
    }

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
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    public static class SetupChooseLockPatternFragment extends ChooseLockPatternFragment {

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            return null;
        }
    }
}
