/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.applications.credentials;

import android.content.Context;
import android.credentials.flags.Flags;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.widget.GearPreference;

/**
 * This preference is shown at the top of the "passwords & accounts" screen and allows the user to
 * pick their primary credential manager provider.
 */
public class PrimaryProviderPreference extends GearPreference {

    public static boolean shouldUseNewSettingsUi() {
        return Flags.newSettingsUi();
    }

    private @Nullable Button mChangeButton = null;
    private @Nullable Button mOpenButton = null;
    private @Nullable View mButtonFrameView = null;
    private @Nullable View mGearView = null;
    private @Nullable Delegate mDelegate = null;
    private boolean mButtonsVisible = false;
    private boolean mOpenButtonVisible = false;

    /** Called to send messages back to the parent controller. */
    public static interface Delegate {
        void onOpenButtonClicked();

        void onChangeButtonClicked();
    }

    public PrimaryProviderPreference(
            @NonNull Context context,
            @NonNull AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeNewSettingsUi();
    }

    public PrimaryProviderPreference(
           @NonNull Context context,
           @NonNull AttributeSet attrs) {
        super(context, attrs);
        initializeNewSettingsUi();
    }

    private void initializeNewSettingsUi() {
        if (!shouldUseNewSettingsUi()) {
            return;
        }

        // Change the layout to the new settings ui.
        setLayoutResource(R.layout.preference_credential_manager_with_buttons);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (shouldUseNewSettingsUi()) {
            onBindViewHolderNewSettingsUi(holder);
        } else {
            onBindViewHolderOldSettingsUi(holder);
        }
    }

    private void onBindViewHolderOldSettingsUi(PreferenceViewHolder holder) {
        setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        if (mDelegate != null) {
                            mDelegate.onOpenButtonClicked();
                            return true;
                        }

                        return false;
                    }
                });

        // Setup the gear icon to handle opening the change provider scenario.
        mGearView = holder.findViewById(R.id.settings_button);
        mGearView.setVisibility(View.VISIBLE);
        mGearView.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(@NonNull View v) {
                        if (mDelegate != null) {
                            mDelegate.onChangeButtonClicked();
                        }
                    }
                });
    }

    private void onBindViewHolderNewSettingsUi(PreferenceViewHolder holder) {
        mOpenButton = (Button) holder.findViewById(R.id.open_button);
        mOpenButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(@NonNull View v) {
                        if (mDelegate != null) {
                            mDelegate.onOpenButtonClicked();
                        }
                    }
                });
        setVisibility(mOpenButton, mOpenButtonVisible);

        mChangeButton = (Button) holder.findViewById(R.id.change_button);
        mChangeButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(@NonNull View v) {
                        if (mDelegate != null) {
                            mDelegate.onChangeButtonClicked();
                        }
                    }
                });

        mButtonFrameView = holder.findViewById(R.id.credman_button_frame);
        mButtonFrameView.setVisibility(mButtonsVisible ? View.VISIBLE : View.GONE);

        // There is a special case where if the provider == none then we should
        // hide the buttons and when the preference is tapped we can open the
        // provider selection dialog.
        setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        return handlePreferenceClickNewSettingsUi();
                    }
                });
    }

    private boolean handlePreferenceClickNewSettingsUi() {
        if (mDelegate != null && !mButtonsVisible) {
            mDelegate.onChangeButtonClicked();
            return true;
        }

        return false;
    }

    public void setOpenButtonVisible(boolean isVisible) {
        if (mOpenButton != null) {
            mOpenButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            setVisibility(mOpenButton, isVisible);
        }

        mOpenButtonVisible = isVisible;
    }

    public void setButtonsVisible(boolean isVisible) {
        if (mButtonFrameView != null) {
            setVisibility(mButtonFrameView, isVisible);
        }

        mButtonsVisible = isVisible;
    }

    public void setDelegate(@NonNull Delegate delegate) {
        mDelegate = delegate;
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return shouldUseNewSettingsUi();
    }

    @VisibleForTesting
    public @Nullable Button getOpenButton() {
        return mOpenButton;
    }

    @VisibleForTesting
    public @Nullable Button getChangeButton() {
        return mChangeButton;
    }

    @VisibleForTesting
    public @Nullable View getButtonFrameView() {
        return mButtonFrameView;
    }

    @VisibleForTesting
    public @Nullable View getGearView() {
        return mGearView;
    }

    private static void setVisibility(View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
