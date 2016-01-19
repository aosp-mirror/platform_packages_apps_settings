/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.NetworkPolicyManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import com.android.settings.CustomDialogPreference;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.conditional.BackgroundDataCondition;
import com.android.settings.dashboard.conditional.ConditionManager;

public class RestrictBackgroundDataPreference extends CustomDialogPreference {

    private NetworkPolicyManager mPolicyManager;
    private boolean mChecked;

    public RestrictBackgroundDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mPolicyManager = NetworkPolicyManager.from(getContext());
        setChecked(mPolicyManager.getRestrictBackground());
    }

    public void setRestrictBackground(boolean restrictBackground) {
        mPolicyManager.setRestrictBackground(restrictBackground);
        setChecked(restrictBackground);
        ConditionManager.get(getContext()).getCondition(BackgroundDataCondition.class)
                .refreshState();
    }

    private void setChecked(boolean checked) {
        if (mChecked == checked) return;
        mChecked = checked;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View switchView = holder.findViewById(android.R.id.switch_widget);
        switchView.setClickable(false);
        ((Checkable) switchView).setChecked(mChecked);
    }

    @Override
    protected void performClick(View view) {
        if (mChecked) {
            setRestrictBackground(false);
        } else {
            super.performClick(view);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        builder.setTitle(R.string.data_usage_restrict_background_title);
        if (Utils.hasMultipleUsers(getContext())) {
            builder.setMessage(R.string.data_usage_restrict_background_multiuser);
        } else {
            builder.setMessage(R.string.data_usage_restrict_background);
        }

        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNegativeButton(android.R.string.cancel, null);
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
            return;
        }
        setRestrictBackground(true);
    }
}
