package com.android.settings.location;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.content.Context;
import android.location.LocationManager;
import android.permission.PermissionControllerManager;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class AppLocationPermissionPreferenceController extends
        LocationBasePreferenceController implements PreferenceControllerMixin {

    private static final String KEY_APP_LEVEL_PERMISSIONS = "app_level_permissions";
    /** Total number of apps that has location permission. */
    @VisibleForTesting
    int mNumTotal = -1;
    /** Total number of apps that has background location permission. */
    @VisibleForTesting
    int mNumBackground = -1;
    private final LocationManager mLocationManager;
    private Preference mPreference;

    public AppLocationPermissionPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_APP_LEVEL_PERMISSIONS;
    }

    @Override
    public boolean isAvailable() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED, 1) == 1;
    }

    @Override
    public CharSequence getSummary() {
        if (mLocationManager.isLocationEnabled()) {
            if (mNumTotal == -1 || mNumBackground == -1) {
                return mContext.getString(R.string.location_settings_loading_app_permission_stats);
            }
            return mContext.getResources().getQuantityString(
                    R.plurals.location_app_permission_summary_location_on, mNumBackground,
                    mNumBackground, mNumTotal);
        } else {
            return mContext.getString(R.string.location_app_permission_summary_location_off);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mPreference = preference;
        final AtomicInteger loadingInProgress = new AtomicInteger(2);
        refreshSummary(preference);
        // Bail out if location has been disabled.
        if (!mLocationManager.isLocationEnabled()) {
            return;
        }
        PermissionControllerManager permController =
                mContext.getSystemService(PermissionControllerManager.class);
        permController.countPermissionApps(
                Arrays.asList(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), 0,
                (numApps) -> {
                    mNumTotal = numApps;
                    if (loadingInProgress.decrementAndGet() == 0) {
                        refreshSummary(preference);
                    }
                }, null);

        permController.countPermissionApps(
                Collections.singletonList(ACCESS_BACKGROUND_LOCATION),
                PermissionControllerManager.COUNT_ONLY_WHEN_GRANTED,
                (numApps) -> {
                    mNumBackground = numApps;
                    if (loadingInProgress.decrementAndGet() == 0) {
                        refreshSummary(preference);
                    }
                }, null);
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        // 'null' is checked inside updateState(), so no need to check here.
        updateState(mPreference);
    }
}
