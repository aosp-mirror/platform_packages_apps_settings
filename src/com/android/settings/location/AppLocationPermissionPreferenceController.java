package com.android.settings.location;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.content.Context;
import android.location.LocationManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.permission.PermissionControllerManager;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AppLocationPermissionPreferenceController extends
        LocationBasePreferenceController implements PreferenceControllerMixin {

    /** Total number of apps that has location permission. */
    @VisibleForTesting
    int mNumTotal = -1;
    /** Total number of apps that has background location permission. */
    @VisibleForTesting
    int mNumHasLocation = -1;

    final AtomicInteger loadingInProgress = new AtomicInteger(0);
    private int mNumTotalLoading = 0;
    private int mNumHasLocationLoading = 0;

    private final LocationManager mLocationManager;
    private Preference mPreference;

    public AppLocationPermissionPreferenceController(Context context, String key) {
        super(context, key);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED, 1) == 1 ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        if (mLocationManager.isLocationEnabled()) {
            if (mNumTotal == -1 || mNumHasLocation == -1) {
                return mContext.getString(R.string.location_settings_loading_app_permission_stats);
            }
            return mContext.getResources().getQuantityString(
                    R.plurals.location_app_permission_summary_location_on, mNumHasLocation,
                    mNumHasLocation, mNumTotal);
        } else {
            return mContext.getString(R.string.location_app_permission_summary_location_off);
        }
    }

    private void setAppCounts(int numTotal, int numHasLocation) {
        mNumTotal = numTotal;
        mNumHasLocation = numHasLocation;
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
        mNumHasLocationLoading = 0;
        // Retrieve a list of users inside the current user profile group.
        final List<UserHandle> users = mContext.getSystemService(
                UserManager.class).getUserProfiles();
        loadingInProgress.set(2 * users.size());
        for (UserHandle user : users) {
            final Context userContext = Utils.createPackageContextAsUser(mContext,
                    user.getIdentifier());
            if (userContext == null) {
                for (int i = 0; i < 2; ++i) {
                    if (loadingInProgress.decrementAndGet() == 0) {
                        setAppCounts(mNumTotalLoading, mNumHasLocationLoading);
                    }
                }
                continue;
            }
            final PermissionControllerManager permController =
                    userContext.getSystemService(PermissionControllerManager.class);
            permController.countPermissionApps(
                    Arrays.asList(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), 0,
                    (numApps) -> {
                        mNumTotalLoading += numApps;
                        if (loadingInProgress.decrementAndGet() == 0) {
                            setAppCounts(mNumTotalLoading, mNumHasLocationLoading);
                        }
                    }, null);
            permController.countPermissionApps(
                    Arrays.asList(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                    PermissionControllerManager.COUNT_ONLY_WHEN_GRANTED,
                    (numApps) -> {
                        mNumHasLocationLoading += numApps;
                        if (loadingInProgress.decrementAndGet() == 0) {
                            setAppCounts(mNumTotalLoading, mNumHasLocationLoading);
                        }
                    }, null);
        }
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }
}
