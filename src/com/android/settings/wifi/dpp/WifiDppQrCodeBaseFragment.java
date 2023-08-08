/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.android.settings.core.InstrumentedFragment;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/**
 * There are below 4 fragments for Wi-Fi DPP UI flow, to reduce redundant code of UI components,
 * this parent fragment instantiates common UI components
 *
 * {@code WifiDppQrCodeScannerFragment}
 * {@code WifiDppQrCodeGeneratorFragment}
 * {@code WifiDppChooseSavedWifiNetworkFragment}
 * {@code WifiDppAddDeviceFragment}
 */
public abstract class WifiDppQrCodeBaseFragment extends InstrumentedFragment {
    private static final String TAG = "WifiDppQrCodeBaseFragment";

    private GlifLayout mGlifLayout;
    protected TextView mSummary;
    protected FooterButton mLeftButton;
    protected FooterButton mRightButton;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mGlifLayout = (GlifLayout) view;
        mSummary = view.findViewById(android.R.id.summary);

        if (isFooterAvailable()) {
            mLeftButton = new FooterButton.Builder(getContext())
                    .setButtonType(FooterButton.ButtonType.CANCEL)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                    .build();
            mGlifLayout.getMixin(FooterBarMixin.class).setSecondaryButton(mLeftButton);

            mRightButton = new FooterButton.Builder(getContext())
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build();
            mGlifLayout.getMixin(FooterBarMixin.class).setPrimaryButton(mRightButton);
        }

        mGlifLayout.getHeaderTextView().setAccessibilityLiveRegion(
                View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    protected void setHeaderIconImageResource(@DrawableRes int iconResId) {
        mGlifLayout.setIcon(getDrawable(iconResId));
    }

    private Drawable getDrawable(@DrawableRes int iconResId) {
        Drawable buttonIcon = null;

        try {
            buttonIcon = getContext().getDrawable(iconResId);
        } catch (Resources.NotFoundException exception) {
            Log.e(TAG, "Resource does not exist: " + iconResId);
        }
        return buttonIcon;
    }

    protected void setHeaderTitle(String title) {
        mGlifLayout.setHeaderText(title);
    }

    protected void setHeaderTitle(int resId, Object... formatArgs) {
        mGlifLayout.setHeaderText(getString(resId, formatArgs));
    }

    protected void setProgressBarShown(boolean shown) {
        mGlifLayout.setProgressBarShown(shown);
    }

    protected abstract boolean isFooterAvailable();
}
