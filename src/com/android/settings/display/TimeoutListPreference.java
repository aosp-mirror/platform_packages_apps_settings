/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog.Builder;

import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settingslib.RestrictedLockUtils;

import java.util.ArrayList;
import java.util.List;


public class TimeoutListPreference extends RestrictedListPreference {
    private static final String TAG = "TimeoutListPreference";
    private EnforcedAdmin mAdmin;
    private CharSequence[] mInitialEntries;
    private CharSequence[] mInitialValues;

    public TimeoutListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateInitialValues();
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        if (mAdmin != null) {
            builder.setView(R.layout.admin_disabled_other_options_footer);
        } else {
            builder.setView(null);
        }
    }

    @Override
    protected void onDialogCreated(Dialog dialog) {
        super.onDialogCreated(dialog);
        dialog.create();
        if (mAdmin != null) {
            View footerView = dialog.findViewById(R.id.admin_disabled_other_options);
            footerView.findViewById(R.id.admin_more_details_link).setOnClickListener(
                    view -> RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                            getContext(), mAdmin));
        }
    }

    public void removeUnusableTimeouts(long maxTimeout, EnforcedAdmin admin) {
        final DevicePolicyManager dpm = (DevicePolicyManager) getContext().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) {
            return;
        }

        if (admin == null && mAdmin == null && !isDisabledByAdmin()) {
            return;
        }
        if (admin == null) {
            maxTimeout = Long.MAX_VALUE;
        }

        final ArrayList<CharSequence> revisedEntries = new ArrayList<>();
        final ArrayList<CharSequence> revisedValues = new ArrayList<>();
        for (int i = 0; i < mInitialValues.length; ++i) {
            long timeout = Long.parseLong(mInitialValues[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(mInitialEntries[i]);
                revisedValues.add(mInitialValues[i]);
            }
        }

        // If there are no possible options for the user, then set this preference as disabled
        // by admin, otherwise remove the padlock in case it was set earlier.
        if (revisedValues.isEmpty()) {
            setDisabledByAdmin(admin);
            return;
        } else {
            setDisabledByAdmin(null);
        }

        if (revisedEntries.size() != getEntries().length) {
            final int userPreference = Integer.parseInt(getValue());
            setEntries(revisedEntries.toArray(new CharSequence[0]));
            setEntryValues(revisedValues.toArray(new CharSequence[0]));
            mAdmin = admin;
            if (userPreference <= maxTimeout) {
                setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                setValue(String.valueOf(maxTimeout));
            } else {
                // The selected time out value is longer than the max timeout allowed by the admin.
                // Select the largest value from the list by default.
                Log.w(TAG, "Default to longest timeout. Value disabled by admin:" + userPreference);
                setValue(revisedValues.get(revisedValues.size() - 1).toString());
            }
        }
    }

    @VisibleForTesting
    void updateInitialValues() {
        // Read default list of candidate values.
        final CharSequence[] entries = getEntries();
        final CharSequence[] values = getEntryValues();
        // Filter out values based on config
        final List<CharSequence> revisedEntries = new ArrayList<>();
        final List<CharSequence> revisedValues = new ArrayList<>();
        final long maxTimeout = getContext().getResources().getInteger(
                R.integer.max_lock_after_timeout_ms);
        if (entries == null || values == null) {
            return;
        }
        Log.d(TAG, "max timeout: " + maxTimeout);
        for (int i = 0; i < values.length; ++i) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                Log.d(TAG, "keeping timeout: " + values[i]);
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            } else {
                Log.d(TAG, "Dropping timeout: " + values[i]);
            }
        }

        // Store final candidates in initial value lists.
        mInitialEntries = revisedEntries.toArray(new CharSequence[0]);
        setEntries(mInitialEntries);
        mInitialValues = revisedValues.toArray(new CharSequence[0]);
        setEntryValues(mInitialValues);
    }
}
