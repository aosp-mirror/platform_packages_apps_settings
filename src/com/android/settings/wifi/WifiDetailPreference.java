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

package com.android.settings.wifi;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.settings.R;

/**
 * A Preference to be used with the Wifi Network Detail Fragment that allows a summary text to be
 * set inside the widget resource
 */
public class WifiDetailPreference extends Preference {
    private String mDetailText;

    public WifiDetailPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_summary);
    }

    public void setDetailText(String text) {
        if (TextUtils.equals(mDetailText, text)) return;
        mDetailText = text;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        TextView textView = ((TextView) view.findViewById(R.id.widget_summary));
        textView.setText(mDetailText);
        textView.setPadding(0, 0, 10, 0);
    }
}
