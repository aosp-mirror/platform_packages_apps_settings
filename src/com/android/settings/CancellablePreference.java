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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class CancellablePreference extends Preference implements OnClickListener {

    private boolean mCancellable;
    private OnCancelListener mListener;

    public CancellablePreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.cancel_pref_widget);
    }

    public CancellablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.cancel_pref_widget);
    }

    public void setCancellable(boolean isCancellable) {
        mCancellable = isCancellable;
        notifyChanged();
    }

    public void setOnCancelListener(OnCancelListener listener) {
        mListener = listener;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        ImageView cancel = (ImageView) view.findViewById(R.id.cancel);
        cancel.setVisibility(mCancellable ? View.VISIBLE : View.INVISIBLE);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onCancel(this);
        }
    }

    public interface OnCancelListener {
        void onCancel(CancellablePreference preference);
    }

}
