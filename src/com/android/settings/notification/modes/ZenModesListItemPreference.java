/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.notification.modes;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.base.Strings;

import java.util.concurrent.Executor;

/**
 * Preference representing a single mode item on the modes aggregator page. Clicking on this
 * preference leads to an individual mode's configuration page.
 */
class ZenModesListItemPreference extends RestrictedPreference {

    private final Context mContext;
    private final ZenIconLoader mIconLoader;
    private final Executor mUiExecutor;
    private ZenMode mZenMode;

    private TextView mTitleView;
    private TextView mSummaryView;

    ZenModesListItemPreference(Context context, ZenIconLoader iconLoader, ZenMode zenMode) {
        this(context, iconLoader, context.getMainExecutor(), zenMode);
    }

    @VisibleForTesting
    ZenModesListItemPreference(Context context, ZenIconLoader iconLoader, Executor uiExecutor,
            ZenMode zenMode) {
        super(context);
        mContext = context;
        mIconLoader = iconLoader;
        mUiExecutor = uiExecutor;
        setZenMode(zenMode);
        setKey(zenMode.getId());
    }


    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (holder.findViewById(android.R.id.title) instanceof TextView titleView) {
            mTitleView = titleView;
        }
        if (holder.findViewById(android.R.id.summary) instanceof TextView summaryView) {
            mSummaryView = summaryView;
        }
        updateTextColor(mZenMode);
    }

    @Override
    public void onClick() {
        ZenSubSettingLauncher.forModeFragment(mContext, ZenModeFragment.class, mZenMode.getId(),
                SettingsEnums.ZEN_PRIORITY_MODES_LIST).launch();
    }

    public void setZenMode(ZenMode zenMode) {
        ZenMode previous = mZenMode;
        mZenMode = zenMode;
        if (zenMode.equals(previous)) {
            return;
        }

        setTitle(mZenMode.getName());
        String dynamicDescription = zenMode.getDynamicDescription(mContext);
        CharSequence statusText = switch (mZenMode.getStatus()) {
            case ENABLED_AND_ACTIVE ->
                    Strings.isNullOrEmpty(dynamicDescription)
                            ? mContext.getString(R.string.zen_mode_active_text)
                            : mContext.getString(
                                    R.string.zen_mode_format_status_and_trigger,
                                    mContext.getString(R.string.zen_mode_active_text),
                                    dynamicDescription);
            case ENABLED -> dynamicDescription;
            case DISABLED_BY_USER -> mContext.getString(R.string.zen_mode_disabled_by_user);
            case DISABLED_BY_OTHER -> mContext.getString(R.string.zen_mode_disabled_needs_setup);
        };
        setSummary(statusText);

        setIconSize(ICON_SIZE_SMALL);
        FutureUtil.whenDone(
                mIconLoader.getIcon(mContext, mZenMode),
                icon -> setIcon(
                        zenMode.isActive()
                                ? IconUtil.applyAccentTint(mContext, icon.drawable())
                                : IconUtil.applyNormalTint(mContext, icon.drawable())),
                mUiExecutor);

        updateTextColor(zenMode);
    }

    private void updateTextColor(@Nullable ZenMode zenMode) {
        boolean isActive = zenMode != null && zenMode.isActive();
        if (mTitleView != null) {
            mTitleView.setTextColor(Utils.getColorAttr(mContext,
                    isActive ? android.R.attr.colorAccent : android.R.attr.textColorPrimary));
        }
        if (mSummaryView != null) {
            mSummaryView.setTextColor(Utils.getColorAttr(mContext,
                    isActive ? android.R.attr.colorAccent : android.R.attr.textColorSecondary));
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ZenMode getZenMode() {
        return mZenMode;
    }
}
