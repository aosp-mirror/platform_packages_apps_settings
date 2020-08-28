package com.android.settings.notification.zen;

import android.app.Application;
import android.app.NotificationChannel;
import android.content.Context;
import android.icu.text.ListFormatter;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;

import androidx.core.text.BidiFormatter;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Controls the summary for preference found at:
 *  Settings > Sound > Do Not Disturb > Apps
 */
public class ZenModeBypassingAppsPreferenceController extends AbstractZenModePreferenceController
        implements PreferenceControllerMixin {

    protected static final String KEY = "zen_mode_behavior_apps";

    @VisibleForTesting protected Preference mPreference;
    private ApplicationsState.Session mAppSession;
    private NotificationBackend mNotificationBackend = new NotificationBackend();

    private String mSummary;

    public ZenModeBypassingAppsPreferenceController(Context context, Application app,
            Fragment host, Lifecycle lifecycle) {
        this(context, app == null ? null : ApplicationsState.getInstance(app), host, lifecycle);
    }

    private ZenModeBypassingAppsPreferenceController(Context context, ApplicationsState appState,
            Fragment host, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
        if (appState != null && host != null) {
            mAppSession = appState.newSession(mAppSessionCallbacks, host.getLifecycle());
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(KEY);
        updateAppsBypassingDndSummaryText();
        super.displayPreference(screen);
    }

    @Override
    public String getSummary() {
        return mSummary;
    }

    private void updateAppsBypassingDndSummaryText() {
        if (mAppSession == null) {
            return;
        }

        ApplicationsState.AppFilter filter = ApplicationsState.FILTER_ALL_ENABLED;
        List<ApplicationsState.AppEntry> apps = mAppSession.rebuild(filter,
                ApplicationsState.ALPHA_COMPARATOR);
        updateAppsBypassingDndSummaryText(apps);
    }

    @VisibleForTesting
    void updateAppsBypassingDndSummaryText(List<ApplicationsState.AppEntry> apps) {
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                // users cannot change their DND settings when an app puts the device total
                // silence or alarms only (both deprecated) modes
                mPreference.setEnabled(false);
                mSummary = mContext.getResources().getString(
                        R.string.zen_mode_bypassing_apps_subtext_none);
                return;
            default:
                mPreference.setEnabled(true);
        }

        if (apps == null) {
            return;
        }

        Set<String> appsBypassingDnd = new ArraySet<>();
        for (ApplicationsState.AppEntry entry : apps) {
            String pkg = entry.info.packageName;
            for (NotificationChannel channel : mNotificationBackend
                    .getNotificationChannelsBypassingDnd(pkg, entry.info.uid).getList()) {
                if (!TextUtils.isEmpty(channel.getConversationId()) && !channel.isDemoted()) {
                    // conversation channels that bypass dnd will be shown on the People page
                    continue;
                }
                appsBypassingDnd.add(BidiFormatter.getInstance().unicodeWrap(entry.label));
                continue;
            }
        }

        final int numAppsBypassingDnd = appsBypassingDnd.size();
        if (numAppsBypassingDnd == 0) {
            mSummary = mContext.getResources().getString(
                    R.string.zen_mode_bypassing_apps_subtext_none);
            refreshSummary(mPreference);
            return;
        }

        List<String> displayAppsBypassing = new ArrayList<>();
        if (numAppsBypassingDnd <= 2) {
            displayAppsBypassing.addAll(appsBypassingDnd);
        } else {
            String[] appsBypassingDndArr =
                    appsBypassingDnd.toArray(new String[numAppsBypassingDnd]);
            displayAppsBypassing.add(appsBypassingDndArr[0]);
            displayAppsBypassing.add(appsBypassingDndArr[1]);
            displayAppsBypassing.add(mContext.getResources().getString(
                    R.string.zen_mode_apps_bypassing_list_count,
                    numAppsBypassingDnd - 2));
        }
        mSummary = mContext.getResources().getQuantityString(
                R.plurals.zen_mode_bypassing_apps_subtext,
                numAppsBypassingDnd,
                ListFormatter.getInstance().format(displayAppsBypassing));
        refreshSummary(mPreference);
    }

    private final ApplicationsState.Callbacks mAppSessionCallbacks =
            new ApplicationsState.Callbacks() {

                @Override
                public void onRunningStateChanged(boolean running) {
                    updateAppsBypassingDndSummaryText();
                }

                @Override
                public void onPackageListChanged() {
                    updateAppsBypassingDndSummaryText();
                }

                @Override
                public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                    updateAppsBypassingDndSummaryText(apps);
                }

                @Override
                public void onPackageIconChanged() { }

                @Override
                public void onPackageSizeChanged(String packageName) {
                    updateAppsBypassingDndSummaryText();
                }

                @Override
                public void onAllSizesComputed() { }

                @Override
                public void onLauncherInfoChanged() { }

                @Override
                public void onLoadEntriesCompleted() {
                    updateAppsBypassingDndSummaryText();
                }
            };
}
