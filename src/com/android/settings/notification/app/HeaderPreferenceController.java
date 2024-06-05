/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.app;

import static com.android.settings.widget.EntityHeaderController.PREF_KEY_APP_HEADER;

import android.app.Activity;
import android.content.Context;
import android.text.BidiFormatter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

public class HeaderPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver {

    private final DashboardFragment mFragment;
    private EntityHeaderController mHeaderController;
    private boolean mStarted = false;

    public HeaderPreferenceController(Context context, DashboardFragment fragment) {
        super(context, null);
        mFragment = fragment;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_APP_HEADER;
    }

    @Override
    public boolean isAvailable() {
        return mAppRow != null;
    }

    @Override
    boolean isIncludedInFilter() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (mAppRow != null && mFragment != null) {

            Activity activity = null;
            if (mStarted) {
                // don't call done on an activity if it hasn't started yet
                activity = mFragment.getActivity();
            }

            if (activity == null) {
                return;
            }

            LayoutPreference pref = (LayoutPreference) preference;
            mHeaderController = EntityHeaderController.newInstance(
                    activity, mFragment, pref.findViewById(R.id.entity_header));
            pref = mHeaderController.setIcon(mAppRow.icon)
                    .setLabel(getLabel())
                    .setSummary(getSummary())
                    .setSecondSummary(getSecondSummary())
                    .setPackageName(mAppRow.pkg)
                    .setUid(mAppRow.uid)
                    .setButtonActions(EntityHeaderController.ActionType.ACTION_NOTIF_PREFERENCE,
                            EntityHeaderController.ActionType.ACTION_NONE)
                    .setHasAppInfoLink(true)
                    .done(mContext);
            pref.findViewById(R.id.entity_header).setVisibility(View.VISIBLE);
            pref.findViewById(R.id.entity_header).setBackground(null);
        }
    }

    public CharSequence getLabel() {
        if (mChannel != null && !isDefaultChannel()) {
            return mChannel.getName();
        } else {
            return mAppRow.label;
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mStarted = true;
    }

    @Override
    public CharSequence getSummary() {
        if (mChannel != null) {
            if (mChannelGroup != null
                    && !TextUtils.isEmpty(mChannelGroup.getName())) {
                final SpannableStringBuilder summary = new SpannableStringBuilder();
                BidiFormatter bidi = BidiFormatter.getInstance();
                summary.append(bidi.unicodeWrap(mAppRow.label));
                summary.append(bidi.unicodeWrap(mContext.getText(
                        R.string.notification_header_divider_symbol_with_spaces)));
                summary.append(bidi.unicodeWrap(mChannelGroup.getName().toString()));
                return summary.toString();
            } else {
                return mAppRow.label.toString();
            }
        }
        return "";
    }

    public CharSequence getSecondSummary() {
        return mChannel == null ? null : mChannel.getDescription();
    }
}
