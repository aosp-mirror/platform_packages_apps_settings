package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import com.android.settings.core.PreferenceController;
import com.android.settings.R;

/**
 * StorgaeSummaryPreferenceController updates the donut storage summary preference to have the
 * correct sizes showing.
 */
public class StorageSummaryDonutPreferenceController extends PreferenceController {
    private long mUsedBytes;
    private long mTotalBytes;

    public StorageSummaryDonutPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        Log.d("dhnishi", "Preference displayed!");
        StorageSummaryDonutPreference summary = (StorageSummaryDonutPreference)
                screen.findPreference("pref_summary");
        summary.setEnabled(true);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        StorageSummaryDonutPreference summary = (StorageSummaryDonutPreference) preference;
        final Formatter.BytesResult result = Formatter.formatBytes(mContext.getResources(),
                mUsedBytes, 0);
        summary.setTitle(TextUtils.expandTemplate(mContext.getText(R.string.storage_size_large),
                result.value, result.units));
        summary.setSummary(mContext.getString(R.string.storage_volume_used,
                Formatter.formatFileSize(mContext, mTotalBytes)));
        summary.setEnabled(true);
        summary.setPercent(mUsedBytes, mTotalBytes);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return "pref_summary";
    }

    /**
     * Updates the state of the donut preference for the next update.
     * @param used Total number of used bytes on the summarized volume.
     * @param total Total number of bytes on the summarized volume.
     */
    public void updateBytes(long used, long total) {
        mUsedBytes = used;
        mTotalBytes = total;
    }
}
