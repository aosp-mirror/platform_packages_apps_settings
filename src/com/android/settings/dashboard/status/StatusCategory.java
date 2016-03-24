package com.android.settings.dashboard.status;

import android.annotation.StringRes;
import android.content.Context;
import android.graphics.drawable.Icon;

import com.android.settings.dashboard.DashboardStatusAdapter;

/**
 * Data item for status category in dashboard.
 */
public final class StatusCategory {

    private final String mTitle;

    public StatusCategory(Context context, @StringRes int titleResId) {
        mTitle = context.getString(titleResId);
    }

    public void bindToViewHolder(DashboardStatusAdapter.ViewHolder viewHolder) {
        viewHolder.title.setText(mTitle);
        viewHolder.icon.setImageIcon(getIcon());
        viewHolder.summary.setText(getSummary());
        viewHolder.icon2.setImageIcon(getSecondaryIcon());
        viewHolder.summary2.setText(getSecondarySummary());
    }

    private Icon getIcon() {
        return null;
    }

    private String getSummary() {
        return null;
    }

    private Icon getSecondaryIcon() {
        return null;
    }

    private String getSecondarySummary() {
        return null;
    }
}
