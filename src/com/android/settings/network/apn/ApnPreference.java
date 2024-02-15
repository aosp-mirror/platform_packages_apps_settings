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
    private static String sSelectedKey = null;
    private static CompoundButton sCurrentChecked = null;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private boolean mProtectFromCheckedChange = false;
    private boolean mSelectable = true;
    private boolean mHideDetails = false;

    /**
     * Constructor of Preference
     */
    public ApnPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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

        final View widget = view.findViewById(R.id.apn_radiobutton);
        if ((widget != null) && widget instanceof RadioButton) {
            final RadioButton rb = (RadioButton) widget;
            if (mSelectable) {
                rb.setOnCheckedChangeListener(this);

                final boolean isChecked = getKey().equals(sSelectedKey);
                if (isChecked) {
                    sCurrentChecked = rb;
                    sSelectedKey = getKey();
                }

                mProtectFromCheckedChange = true;
                rb.setChecked(isChecked);
                mProtectFromCheckedChange = false;
                rb.setVisibility(View.VISIBLE);
            } else {
                rb.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Return the preference is checked or not.
     */
    public boolean isChecked() {
        return getKey().equals(sSelectedKey);
    }

    /**
     * Set preference checked.
     */
    public void setChecked() {
        sSelectedKey = getKey();
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
            if (sCurrentChecked != null) {
                sCurrentChecked.setChecked(false);
            }
            sCurrentChecked = buttonView;
            sSelectedKey = getKey();
            callChangeListener(sSelectedKey);
        } else {
            sCurrentChecked = null;
            sSelectedKey = null;
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

    public boolean getSelectable() {
        return mSelectable;
    }

    public void setSelectable(boolean selectable) {
        mSelectable = selectable;
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
