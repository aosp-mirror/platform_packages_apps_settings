/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.tether;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.widget.ValidatedEditTextPreference;

/**
 * Support a QR code share button for {@code EditTextPreference} that supports input validation.
 */
public class WifiTetherSsidPreference extends ValidatedEditTextPreference {
    private static final String TAG = "WifiTetherSsidPreference";

    private ImageButton mImageButton;
    private Drawable mButtonIcon;
    private View mDivider;
    private View.OnClickListener mClickListener;
    private boolean mVisible;

    public WifiTetherSsidPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initialize();
    }

    public WifiTetherSsidPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initialize();
    }

    public WifiTetherSsidPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize();
    }

    public WifiTetherSsidPreference(Context context) {
        super(context);

        initialize();
    }

    private void initialize() {
        // TODO(b/129019971): use methods of divider line in parent object
        setLayoutResource(com.android.settingslib.R.layout.preference_two_target);
        setWidgetLayoutResource(R.layout.wifi_button_preference_widget);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (mImageButton == null) {
            mImageButton = (ImageButton) holder.findViewById(R.id.button_icon);

            mImageButton.setContentDescription(
                    getContext().getString(R.string.wifi_dpp_share_hotspot));
            setButtonIcon(R.drawable.ic_qrcode_24dp);
            mImageButton.setImageDrawable(mButtonIcon);

            mDivider = holder.findViewById(R.id.two_target_divider);
        }

        if (mVisible) {
            mImageButton.setOnClickListener(mClickListener);
            mImageButton.setVisibility(View.VISIBLE);
            mDivider.setVisibility(View.VISIBLE);
        } else {
            mImageButton.setVisibility(View.GONE);
            mDivider.setVisibility(View.GONE);
        }
    }

    public void setButtonOnClickListener(View.OnClickListener listener) {
        mClickListener = listener;
    }

    public void setButtonVisible(boolean visible) {
        mVisible = visible;
    }

    private void setButtonIcon(@DrawableRes int iconResId) {
        try {
            mButtonIcon = getContext().getDrawable(iconResId);
        } catch (Resources.NotFoundException exception) {
            Log.e(TAG, "Resource does not exist: " + iconResId);
        }
    }

    @VisibleForTesting
    boolean isQrCodeButtonAvailable() {
        return mVisible && mClickListener != null;
    }
}
