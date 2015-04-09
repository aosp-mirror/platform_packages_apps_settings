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

package com.android.settings.vpn2;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.settings.R;

/**
 * Preference with an additional gear icon. Touching the gear icon triggers an
 * onChange event.
 */
public class ManageablePreference extends Preference {
    OnClickListener mListener;
    View mManageView;

    public ManageablePreference(Context context, AttributeSet attrs, OnClickListener onManage) {
        super(context, attrs);
        mListener = onManage;
        setPersistent(false);
        setOrder(0);
        setWidgetLayoutResource(R.layout.preference_vpn);
    }

    @Override
    protected void onBindView(View view) {
        mManageView = view.findViewById(R.id.manage);
        mManageView.setOnClickListener(mListener);
        mManageView.setTag(this);
        super.onBindView(view);
    }
}
