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
package com.android.settings.nfc;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.CustomDialogPreferenceCompat;

public class NfcPaymentPreference extends CustomDialogPreferenceCompat {

    private Listener mListener;

    interface Listener {
        void onBindViewHolder(PreferenceViewHolder view);

        void onPrepareDialogBuilder(Builder builder,
                DialogInterface.OnClickListener listener);
    }

    public NfcPaymentPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public NfcPaymentPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NfcPaymentPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void initialize(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        if (mListener != null) {
            mListener.onBindViewHolder(view);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);

        if (mListener != null) {
            mListener.onPrepareDialogBuilder(builder, listener);
        }
    }
}
