/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.datausage;

import android.annotation.AttrRes;
import android.content.Context;
import android.graphics.Typeface;
import android.icu.text.MessageFormat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.utils.StringUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Provides a summary of data usage.
 */
public class DataUsageSummaryPreference extends Preference {
    private static final long MILLIS_IN_A_DAY = TimeUnit.DAYS.toMillis(1);
    private static final long WARNING_AGE = TimeUnit.HOURS.toMillis(6L);
    @VisibleForTesting
    static final Typeface SANS_SERIF_MEDIUM =
            Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private boolean mChartEnabled = true;
    private CharSequence mStartLabel;
    private CharSequence mEndLabel;

    private int mNumPlans;
    /** The ending time of the billing cycle in milliseconds since epoch. */
    @Nullable
    private Long mCycleEndTimeMs;
    /** The time of the last update in standard milliseconds since the epoch */
    private long mSnapshotTimeMs;
    /** Name of carrier, or null if not available */
    private CharSequence mCarrierName;
    private CharSequence mLimitInfoText;

    /** Progress to display on ProgressBar */
    private float mProgress;

    /**
     * The size of the first registered plan if one exists or the size of the warning if it is set.
     * -1 if no information is available.
     */
    private long mDataplanSize;

    /** The number of bytes used since the start of the cycle. */
    private long mDataplanUse;

    public DataUsageSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.data_usage_summary_preference);
    }

    public void setLimitInfo(CharSequence text) {
        if (!Objects.equals(text, mLimitInfoText)) {
            mLimitInfoText = text;
            notifyChanged();
        }
    }

    public void setProgress(float progress) {
        mProgress = progress;
        notifyChanged();
    }

    /**
     * Sets the usage info.
     */
    public void setUsageInfo(@Nullable Long cycleEnd, long snapshotTime, CharSequence carrierName,
            int numPlans) {
        mCycleEndTimeMs = cycleEnd;
        mSnapshotTimeMs = snapshotTime;
        mCarrierName = carrierName;
        mNumPlans = numPlans;
        notifyChanged();
    }

    public void setChartEnabled(boolean enabled) {
        if (mChartEnabled != enabled) {
            mChartEnabled = enabled;
            notifyChanged();
        }
    }

    public void setLabels(CharSequence start, CharSequence end) {
        mStartLabel = start;
        mEndLabel = end;
        notifyChanged();
    }

    /**
     * Sets the usage numbers.
     */
    public void setUsageNumbers(long used, long dataPlanSize) {
        mDataplanUse = used;
        mDataplanSize = dataPlanSize;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ProgressBar bar = getProgressBar(holder);
        if (mChartEnabled && (!TextUtils.isEmpty(mStartLabel) || !TextUtils.isEmpty(mEndLabel))) {
            bar.setVisibility(View.VISIBLE);
            getLabelBar(holder).setVisibility(View.VISIBLE);
            bar.setProgress((int) (mProgress * 100));
            (getLabel1(holder)).setText(mStartLabel);
            (getLabel2(holder)).setText(mEndLabel);
        } else {
            bar.setVisibility(View.GONE);
            getLabelBar(holder).setVisibility(View.GONE);
        }

        updateDataUsageLabels(holder);

        TextView usageTitle = getUsageTitle(holder);
        TextView carrierInfo = getCarrierInfo(holder);
        TextView limitInfo = getDataLimits(holder);

        usageTitle.setVisibility(mNumPlans > 1 ? View.VISIBLE : View.GONE);
        updateCycleTimeText(holder);
        updateCarrierInfo(carrierInfo);
        limitInfo.setVisibility(TextUtils.isEmpty(mLimitInfoText) ? View.GONE : View.VISIBLE);
        limitInfo.setText(mLimitInfoText);
    }

    private void updateDataUsageLabels(PreferenceViewHolder holder) {
        TextView usageNumberField = getDataUsed(holder);

        final Formatter.BytesResult usedResult = Formatter.formatBytes(getContext().getResources(),
                mDataplanUse, Formatter.FLAG_CALCULATE_ROUNDED | Formatter.FLAG_IEC_UNITS);
        final SpannableString usageNumberText = new SpannableString(usedResult.value);
        final int textSize =
                getContext().getResources().getDimensionPixelSize(R.dimen.usage_number_text_size);
        usageNumberText.setSpan(new AbsoluteSizeSpan(textSize), 0, usageNumberText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence template = getContext().getText(R.string.data_used_formatted);

        CharSequence usageText =
                TextUtils.expandTemplate(template, usageNumberText, usedResult.units);
        usageNumberField.setText(usageText);

        final MeasurableLinearLayout layout = getLayout(holder);

        if (mDataplanSize > 0L) {
            TextView usageRemainingField = getDataRemaining(holder);
            long dataRemaining = mDataplanSize - mDataplanUse;
            if (dataRemaining >= 0) {
                usageRemainingField.setText(
                        TextUtils.expandTemplate(getContext().getText(R.string.data_remaining),
                                DataUsageUtils.formatDataUsage(getContext(), dataRemaining)));
                usageRemainingField.setTextColor(
                        Utils.getColorAttr(getContext(), android.R.attr.colorAccent));
            } else {
                usageRemainingField.setText(
                        TextUtils.expandTemplate(getContext().getText(R.string.data_overusage),
                                DataUsageUtils.formatDataUsage(getContext(), -dataRemaining)));
                usageRemainingField.setTextColor(
                        Utils.getColorAttr(getContext(), android.R.attr.colorError));
            }
            layout.setChildren(usageNumberField, usageRemainingField);
        } else {
            layout.setChildren(usageNumberField, null);
        }
    }

    private void updateCycleTimeText(PreferenceViewHolder holder) {
        TextView cycleTime = getCycleTime(holder);

        // Takes zero as a special case which value is never set.
        if (mCycleEndTimeMs == null) {
            cycleTime.setVisibility(View.GONE);
            return;
        }

        cycleTime.setVisibility(View.VISIBLE);
        long millisLeft = mCycleEndTimeMs - System.currentTimeMillis();
        if (millisLeft <= 0) {
            cycleTime.setText(getContext().getString(R.string.billing_cycle_none_left));
        } else {
            int daysLeft = (int) (millisLeft / MILLIS_IN_A_DAY);
            MessageFormat msgFormat = new MessageFormat(
                    getContext().getResources().getString(R.string.billing_cycle_days_left),
                    Locale.getDefault());
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("count", daysLeft);
            cycleTime.setText(daysLeft < 1
                    ? getContext().getString(R.string.billing_cycle_less_than_one_day_left)
                    : msgFormat.format(arguments));
        }
    }


    private void updateCarrierInfo(TextView carrierInfo) {
        if (mSnapshotTimeMs >= 0L) {
            carrierInfo.setVisibility(View.VISIBLE);
            long updateAgeMillis = calculateTruncatedUpdateAge();

            int textResourceId;
            CharSequence updateTime = null;
            if (updateAgeMillis == 0) {
                if (mCarrierName != null) {
                    textResourceId = R.string.carrier_and_update_now_text;
                } else {
                    textResourceId = R.string.no_carrier_update_now_text;
                }
            } else {
                if (mCarrierName != null) {
                    textResourceId = R.string.carrier_and_update_text;
                } else {
                    textResourceId = R.string.no_carrier_update_text;
                }
                updateTime = StringUtil.formatElapsedTime(
                        getContext(),
                        updateAgeMillis,
                        false /* withSeconds */,
                        false /* collapseTimeUnit */);
            }
            carrierInfo.setText(TextUtils.expandTemplate(
                    getContext().getText(textResourceId),
                    mCarrierName,
                    updateTime));

            if (updateAgeMillis <= WARNING_AGE) {
                setCarrierInfoTextStyle(
                        carrierInfo, android.R.attr.textColorSecondary, Typeface.SANS_SERIF);
            } else {
                setCarrierInfoTextStyle(carrierInfo, android.R.attr.colorError, SANS_SERIF_MEDIUM);
            }
        } else {
            carrierInfo.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the time since the last carrier update, as defined by {@link #mSnapshotTimeMs},
     * truncated to the nearest day / hour / minute in milliseconds, or 0 if less than 1 min.
     */
    private long calculateTruncatedUpdateAge() {
        long updateAgeMillis = System.currentTimeMillis() - mSnapshotTimeMs;

        // Round to nearest whole unit
        if (updateAgeMillis >= TimeUnit.DAYS.toMillis(1)) {
            return (updateAgeMillis / TimeUnit.DAYS.toMillis(1)) * TimeUnit.DAYS.toMillis(1);
        } else if (updateAgeMillis >= TimeUnit.HOURS.toMillis(1)) {
            return (updateAgeMillis / TimeUnit.HOURS.toMillis(1)) * TimeUnit.HOURS.toMillis(1);
        } else if (updateAgeMillis >= TimeUnit.MINUTES.toMillis(1)) {
            return (updateAgeMillis / TimeUnit.MINUTES.toMillis(1)) * TimeUnit.MINUTES.toMillis(1);
        } else {
            return 0;
        }
    }

    private void setCarrierInfoTextStyle(
            TextView carrierInfo, @AttrRes int colorId, Typeface typeface) {
        carrierInfo.setTextColor(Utils.getColorAttr(getContext(), colorId));
        carrierInfo.setTypeface(typeface);
    }

    @VisibleForTesting
    protected TextView getUsageTitle(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.usage_title);
    }

    @VisibleForTesting
    protected TextView getCycleTime(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.cycle_left_time);
    }

    @VisibleForTesting
    protected TextView getCarrierInfo(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.carrier_and_update);
    }

    @VisibleForTesting
    protected TextView getDataLimits(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.data_limits);
    }

    @VisibleForTesting
    protected TextView getDataUsed(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.data_usage_view);
    }

    @VisibleForTesting
    protected TextView getDataRemaining(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.data_remaining_view);
    }

    @VisibleForTesting
    protected LinearLayout getLabelBar(PreferenceViewHolder holder) {
        return (LinearLayout) holder.findViewById(R.id.label_bar);
    }

    @VisibleForTesting
    protected TextView getLabel1(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(android.R.id.text1);
    }

    @VisibleForTesting
    protected TextView getLabel2(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(android.R.id.text2);
    }

    @VisibleForTesting
    protected ProgressBar getProgressBar(PreferenceViewHolder holder) {
        return (ProgressBar) holder.findViewById(R.id.determinateBar);
    }

    @VisibleForTesting
    protected MeasurableLinearLayout getLayout(PreferenceViewHolder holder) {
        return (MeasurableLinearLayout) holder.findViewById(R.id.usage_layout);
    }
}
