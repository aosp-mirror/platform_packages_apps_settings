package com.android.settings.location;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.permission.PermissionControllerManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TopLevelLocationPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {
    private static final IntentFilter INTENT_FILTER_LOCATION_MODE_CHANGED =
            new IntentFilter(LocationManager.MODE_CHANGED_ACTION);
    private final LocationManager mLocationManager;
    /** Total number of apps that has location permission. */
    private int mNumTotal = -1;
    private int mNumTotalLoading = 0;
    private BroadcastReceiver mReceiver;
    private Preference mPreference;
    private AtomicInteger loadingInProgress = new AtomicInteger(0);

    public TopLevelLocationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        if (mLocationManager.isLocationEnabled()) {
            if (mNumTotal == -1) {
                return mContext.getString(R.string.location_settings_loading_app_permission_stats);
            }
            return mContext.getResources().getQuantityString(
                    R.plurals.location_settings_summary_location_on,
                    mNumTotal, mNumTotal);
        } else {
            return mContext.getString(R.string.location_settings_summary_location_off);
        }
    }

    @VisibleForTesting
    void setLocationAppCount(int numApps) {
        mNumTotal = numApps;
        refreshSummary(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mPreference = preference;
        refreshSummary(preference);
        // Bail out if location has been disabled, or there's another loading request in progress.
        if (!mLocationManager.isLocationEnabled() ||
                loadingInProgress.get() != 0) {
            return;
        }
        mNumTotalLoading = 0;
        // Retrieve a list of users inside the current user profile group.
        final List<UserHandle> users = mContext.getSystemService(
                UserManager.class).getUserProfiles();
        loadingInProgress.set(users.size());
        for (UserHandle user : users) {
            final Context userContext = Utils.createPackageContextAsUser(mContext,
                    user.getIdentifier());
            if (userContext == null) {
                if (loadingInProgress.decrementAndGet() == 0) {
                    setLocationAppCount(mNumTotalLoading);
                }
                continue;
            }
            final PermissionControllerManager permController =
                    userContext.getSystemService(PermissionControllerManager.class);
            permController.countPermissionApps(
                    Arrays.asList(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                    PermissionControllerManager.COUNT_ONLY_WHEN_GRANTED,
                    (numApps) -> {
                        mNumTotalLoading += numApps;
                        if (loadingInProgress.decrementAndGet() == 0) {
                            setLocationAppCount(mNumTotalLoading);
                        }
                    }, null);
        }
    }

    @Override
    public void onStart() {
        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    refreshLocationMode();
                }
            };
        }
        mContext.registerReceiver(mReceiver, INTENT_FILTER_LOCATION_MODE_CHANGED);
        refreshLocationMode();
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    private void refreshLocationMode() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }
}
