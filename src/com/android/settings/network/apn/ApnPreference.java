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
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.spa.SpaActivity;

/**
 * Preference of APN UI entry
 */
public class ApnPreference extends Preference
        implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private static final String TAG = "ApnPreference";
    private boolean mIsChecked = false;
    @Nullable
    private RadioButton mRadioButton = null;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private boolean mProtectFromCheckedChange = false;
    private boolean mDefaultSelectable = true;
    private boolean mHideDetails = false;

    /**
     * Constructor of Preference
     */
    public ApnPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // Primary target and radio button could be selectable, but entire preference itself is not
        // selectable.
        setSelectable(false);
    }

    /**
     * Constructor of Preference
     */
    public ApnPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.apnPreferenceStyle);
    }

    /**
     * Constructor of Preference
     */
    public ApnPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        final RelativeLayout textArea = (RelativeLayout) view.findViewById(R.id.text_layout);
        textArea.setOnClickListener(this);

        final View radioButtonFrame = view.itemView.requireViewById(R.id.apn_radio_button_frame);
        final RadioButton rb = view.itemView.requireViewById(R.id.apn_radiobutton);
        mRadioButton = rb;
        if (mDefaultSelectable) {
            radioButtonFrame.setOnClickListener((v) -> {
                rb.performClick();
            });
            rb.setOnCheckedChangeListener(this);

            mProtectFromCheckedChange = true;
            rb.setChecked(mIsChecked);
            mProtectFromCheckedChange = false;
            radioButtonFrame.setVisibility(View.VISIBLE);
        } else {
            radioButtonFrame.setVisibility(View.GONE);
        }
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
    public void onClick(View layoutView) {
        super.onClick();
        final Context context = getContext();
        final int pos = Integer.parseInt(getKey());
        if (context == null) {
            Log.w(TAG, "No context available for pos=" + pos);
            return;
        }

        if (mHideDetails) {
            Toast.makeText(context, context.getString(R.string.cannot_change_apn_toast),
                    Toast.LENGTH_LONG).show();
            return;
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
