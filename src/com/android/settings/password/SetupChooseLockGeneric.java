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

import static android.Manifest.permission.REQUEST_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.EXTRA_PASSWORD_COMPLEXITY;

import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupEncryptionInterstitial;
import com.android.settings.SetupWizardUtils;
import com.android.settings.biometrics.BiometricEnrollActivity;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollFindSensor;
import com.android.settings.utils.SettingsDividerItemDecoration;

import com.google.android.setupdesign.GlifPreferenceLayout;

/**
 * Setup Wizard's version of ChooseLockGeneric screen. It inherits the logic and basic structure
 * from ChooseLockGeneric class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockGeneric class instead and let this class inherit
 * those changes.
 */
// TODO(b/123225425): Restrict SetupChooseLockGeneric to be accessible by SUW only
public class SetupChooseLockGeneric extends ChooseLockGeneric {

    private static final String KEY_UNLOCK_SET_DO_LATER = "unlock_set_do_later";

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockGenericFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends PreferenceFragmentCompat> getFragmentClass() {
        return SetupChooseLockGenericFragment.class;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        if(getIntent().hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)) {
            IBinder activityToken = getActivityToken();
            boolean hasPermission = PasswordUtils.isCallingAppPermitted(
                    this, activityToken, REQUEST_PASSWORD_COMPLEXITY);
            if (!hasPermission) {
                PasswordUtils.crashCallingApplication(activityToken,
                        "Must have permission " + REQUEST_PASSWORD_COMPLEXITY
                                + " to use extra " + EXTRA_PASSWORD_COMPLEXITY);
                finish();
                return;
            }
        }

        findViewById(R.id.content_parent).setFitsSystemWindows(false);
    }

    public static class SetupChooseLockGenericFragment extends ChooseLockGenericFragment {

        public static final String EXTRA_PASSWORD_QUALITY = ":settings:password_quality";

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            layout.setDividerItemDecoration(new SettingsDividerItemDecoration(getContext()));
            layout.setDividerInset(getContext().getResources().getDimensionPixelSize(
                    R.dimen.sud_items_glif_text_divider_inset));

            layout.setIcon(getContext().getDrawable(R.drawable.ic_lock));

            int titleResource = isForBiometric() ?
                    R.string.lock_settings_picker_title : R.string.setup_lock_settings_picker_title;
            if (getActivity() != null) {
                getActivity().setTitle(titleResource);
            }

            layout.setHeaderText(titleResource);
            // Use the dividers in SetupWizardRecyclerLayout. Suppress the dividers in
            // PreferenceFragment.
            setDivider(null);
        }

        @Override
        protected void addHeaderView() {
            if (isForBiometric()) {
                setHeaderView(R.layout.setup_choose_lock_generic_biometrics_header);
            } else {
                setHeaderView(R.layout.setup_choose_lock_generic_header);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
                Bundle savedInstanceState) {
            GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }

        @Override
        protected boolean canRunBeforeDeviceProvisioned() {
            return true;
        }

        @Override
        protected Class<? extends ChooseLockGeneric.InternalActivity> getInternalActivityClass() {
            return SetupChooseLockGeneric.InternalActivity.class;
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
        protected void addPreferences() {
            if (isForBiometric()) {
                super.addPreferences();
            } else {
                addPreferencesFromResource(R.xml.setup_security_settings_picker);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_UNLOCK_SET_DO_LATER.equals(key)) {
                // show warning.
                SetupSkipDialog dialog = SetupSkipDialog.newInstance(
                        getActivity().getIntent()
                                .getBooleanExtra(SetupSkipDialog.EXTRA_FRP_SUPPORTED, false),
                        /* isPatternMode= */ false,
                        /* isAlphaMode= */ false,
                        /* isFingerprintSupported= */ false,
                        /* isFaceSupported= */ false
                );
                dialog.show(getFragmentManager());
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        protected Intent getLockPasswordIntent(int quality) {
            final Intent intent = SetupChooseLockPassword.modifyIntentForSetup(
                    getContext(), super.getLockPasswordIntent(quality));
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent() {
            final Intent intent = SetupChooseLockPattern.modifyIntentForSetup(
                    getContext(), super.getLockPatternIntent());
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getEncryptionInterstitialIntent(Context context, int quality,
                boolean required, Intent unlockMethodIntent) {
            Intent intent = SetupEncryptionInterstitial.createStartIntent(context, quality,
                    required, unlockMethodIntent);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getBiometricEnrollIntent(Context context) {
            final Intent intent = super.getBiometricEnrollIntent(context);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        private boolean isForBiometric() {
            return mForFingerprint || mForFace;
        }
    }

    public static class InternalActivity extends ChooseLockGeneric.InternalActivity {
        @Override
        protected boolean isValidFragment(String fragmentName) {
            return InternalSetupChooseLockGenericFragment.class.getName().equals(fragmentName);
        }

        @Override
        /* package */ Class<? extends Fragment> getFragmentClass() {
            return InternalSetupChooseLockGenericFragment.class;
        }

        public static class InternalSetupChooseLockGenericFragment
                extends ChooseLockGenericFragment {
            @Override
            protected boolean canRunBeforeDeviceProvisioned() {
                return true;
            }
        }
    }
}
