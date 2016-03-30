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
 * limitations under the License
 */

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.SetupWizardPreferenceLayout;
import com.android.setupwizardlib.view.NavigationBar;

/**
 * Setup Wizard's version of EncryptionInterstitial screen. It inherits the logic and basic
 * structure from EncryptionInterstitial class, and should remain similar to that behaviorally. This
 * class should only overload base methods for minor theme and behavior differences specific to
 * Setup Wizard. Other changes should be done to EncryptionInterstitial class instead and let this
 * class inherit those changes.
 */
public class SetupEncryptionInterstitial extends EncryptionInterstitial {

    public static Intent createStartIntent(Context ctx, int quality,
            boolean requirePasswordDefault, Intent unlockMethodIntent) {
        Intent startIntent = EncryptionInterstitial.createStartIntent(ctx, quality,
                requirePasswordDefault, unlockMethodIntent);
        startIntent.setClass(ctx, SetupEncryptionInterstitial.class);
        startIntent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
        return startIntent;
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT,
                SetupEncryptionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupEncryptionInterstitialFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    public static class SetupEncryptionInterstitialFragment extends EncryptionInterstitialFragment
            implements NavigationBar.NavigationBarListener {

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            final SetupWizardPreferenceLayout layout = (SetupWizardPreferenceLayout) view;
            layout.setDividerInset(getContext().getResources().getDimensionPixelSize(
                    R.dimen.suw_items_icon_divider_inset));
            layout.setIllustration(R.drawable.setup_illustration_lock_screen,
                    R.drawable.setup_illustration_horizontal_tile);

            final NavigationBar navigationBar = layout.getNavigationBar();
            navigationBar.setNavigationBarListener(this);
            Button nextButton = navigationBar.getNextButton();
            nextButton.setText(null);
            nextButton.setEnabled(false);

            layout.setHeaderText(R.string.encryption_interstitial_header);
            Activity activity = getActivity();
            if (activity != null) {
                SetupWizardUtils.setImmersiveMode(activity);
            }

            // Use the dividers in SetupWizardRecyclerLayout. Suppress the dividers in
            // PreferenceFragment.
            setDivider(null);
        }

        @Override
        protected TextView createHeaderView() {
            TextView message = (TextView) LayoutInflater.from(getActivity()).inflate(
                    R.layout.setup_encryption_interstitial_header, null, false);
            return message;
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
                                                 Bundle savedInstanceState) {
            SetupWizardPreferenceLayout layout = (SetupWizardPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }

        @Override
        public void onNavigateBack() {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        @Override
        public void onNavigateNext() {
            // next is handled via the onPreferenceTreeClick method in EncryptionInterstitial
        }
    }
}
