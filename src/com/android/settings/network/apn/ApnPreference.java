/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.network.apn;

import static com.android.settings.network.apn.ApnEditPageProviderKt.EDIT_URL;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.spa.SpaActivity;
import com.android.settingslib.widget.TwoTargetPreference;

/**
 * Preference of APN UI entry
 */
public class ApnPreference extends TwoTargetPreference
        implements CompoundButton.OnCheckedChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "ApnPreference";
    private boolean mIsChecked = false;
    private RadioButton mRadioButton;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private boolean mProtectFromCheckedChange = false;
    private boolean mDefaultSelectable = true;
    private boolean mHideDetails = false;

    /**
     * Constructor of Preference
     */
    public ApnPreference(Context context) {
        super(context);
        setOnPreferenceClickListener(this);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final RadioButton rb = (RadioButton) holder.findViewById(android.R.id.checkbox);
        final View radioButtonFrame = holder.findViewById(android.R.id.widget_frame);
        if (rb == null || radioButtonFrame == null) {
            throw new RuntimeException("Failed to load system layout.");
        }

        mRadioButton = rb;
        radioButtonFrame.setOnClickListener(v -> rb.performClick());
        rb.setOnCheckedChangeListener(this);
        setIsChecked(mIsChecked);
        rb.setVisibility(View.VISIBLE);
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return !mDefaultSelectable;
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_radiobutton;
    }

    /**
     * Set preference isChecked.
     */
    public void setIsChecked(boolean isChecked) {
        mIsChecked = isChecked;
        if (mRadioButton != null) {
            mProtectFromCheckedChange = true;
            mRadioButton.setChecked(mIsChecked);
            mProtectFromCheckedChange = false;
        }
    }

    /**
     * Change the preference status.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(TAG, "ID: " + getKey() + " :" + isChecked);
        if (mProtectFromCheckedChange) {
            return;
        }

        if (isChecked) {
            callChangeListener(getKey());
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        final Context context = getContext();
        final int pos = Integer.parseInt(getKey());

        if (mHideDetails) {
            Toast.makeText(context, context.getString(R.string.cannot_change_apn_toast),
                    Toast.LENGTH_LONG).show();
            return true;
        }

        final Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);

        if (Flags.newApnPageEnabled()) {
            String route = ApnEditPageProvider.INSTANCE.getRoute(EDIT_URL, url, mSubId);
            SpaActivity.startSpaActivity(context, route);
        } else {
            final Intent editIntent = new Intent(Intent.ACTION_EDIT, url);
            editIntent.putExtra(ApnSettings.SUB_ID, mSubId);
            editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(editIntent);
        }
        return true;
    }

    public void setDefaultSelectable(boolean defaultSelectable) {
        mDefaultSelectable = defaultSelectable;
    }

    public void setSubId(int subId) {
        mSubId = subId;
    }

    /**
     * Hide details
     */
    public void setHideDetails() {
        mHideDetails = true;
    }
}
