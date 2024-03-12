/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.vpn2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedPreference;


/**
 * A preference with an Info icon on the side
 */
public class VpnInfoPreference extends RestrictedPreference implements View.OnClickListener {

    private boolean mIsInsecureVpn = false;
    private String mHelpUrl;

    public VpnInfoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHelpUrl = context.getString(R.string.help_url_insecure_vpn);
    }

    @Override
    protected int getSecondTargetResId() {
        // Note: in the future, we will probably want to provide a configuration option
        // for this info to not be the warning color.
        return R.layout.preference_widget_warning;
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return false;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final View icon = holder.findViewById(R.id.warning_button);
        if (mIsInsecureVpn && !TextUtils.isEmpty(mHelpUrl)) {
            icon.setVisibility(View.VISIBLE);
            icon.setOnClickListener(this);
            icon.setEnabled(true);
        } else {
            icon.setVisibility(View.GONE);
            icon.setOnClickListener(this);
            icon.setEnabled(false);
        }

        // Hide the divider from view
        final View divider =
                holder.findViewById(com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider);
        divider.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.warning_button) {
            final Intent intent = HelpUtils.getHelpIntent(
                    getContext(), mHelpUrl, this.getClass().getName());

            if (intent != null) {
                ((Activity) getContext()).startActivityForResult(intent, 0);
            }
        }
    }

    /**
     * Sets whether this preference corresponds to an insecure VPN. This will also affect whether
     * the warning icon appears to the user.
     */
    public void setInsecureVpn(boolean isInsecureVpn) {
        if (mIsInsecureVpn != isInsecureVpn) {
            mIsInsecureVpn = isInsecureVpn;
            notifyChanged();
        }
    }
}
