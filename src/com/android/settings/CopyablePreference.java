/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings;

import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

public class CopyablePreference extends Preference {

    public CopyablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CopyablePreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(true);
        holder.setDividerAllowedBelow(true);
        holder.itemView.setLongClickable(true);
        holder.itemView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyPreference(getContext(), CopyablePreference.this);
                return true;
            }
        });
    }

    public CharSequence getCopyableText() {
        return getSummary();
    }

    public static void copyPreference(Context context, CopyablePreference pref) {
        ClipboardManager cm =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(pref.getCopyableText());
        Toast.makeText(context, com.android.internal.R.string.text_copied, Toast.LENGTH_SHORT)
                .show();
    }
}
