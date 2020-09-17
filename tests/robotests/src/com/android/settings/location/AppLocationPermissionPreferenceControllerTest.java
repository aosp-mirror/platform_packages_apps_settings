package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.location.LocationManager;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppLocationPermissionPreferenceControllerTest {

    private AppLocationPermissionPreferenceController mController;

    @Mock
    private Context mContext;

    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private LocationManager mLocationManager;
    private LocationSettings mLocationSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLocationSettings = spy(new LocationSettings());
        when(mLocationSettings.getSettingsLifecycle()).thenReturn(mLifecycle);
        mController = new AppLocationPermissionPreferenceController(mContext, "key");
        mController.init(mLocationSettings);
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    }

    @Test
    public void isAvailable_noLocationLinkPermission_shouldReturnFalse() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED, 0);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_hasLocationLinkPermission_shouldReturnTrue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED, 1);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getSummary_whenLocationIsOff_shouldReturnStringForOff() {
        mLocationManager.setLocationEnabledForUser(false, android.os.Process.myUserHandle());

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.location_app_permission_summary_location_off));
    }

    @Test
    public void getSummary_whenLocationIsOn_shouldReturnLoadingString() {
        mLocationManager.setLocationEnabledForUser(true, android.os.Process.myUserHandle());

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.location_settings_loading_app_permission_stats));
    }

    @Test
    public void getSummary_whenLocationAppCountIsOne_shouldShowSingularString() {
        mLocationManager.setLocationEnabledForUser(true, android.os.Process.myUserHandle());
        mController.mNumHasLocation = 1;
        mController.mNumTotal = 1;

        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getQuantityString(
                R.plurals.location_app_permission_summary_location_on, 1, 1, 1));
    }

    @Test
    public void getSummary_whenLocationAppCountIsGreaterThanOne_shouldShowPluralString() {
        mLocationManager.setLocationEnabledForUser(true, android.os.Process.myUserHandle());
        mController.mNumHasLocation = 5;
        mController.mNumTotal = 10;

        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getQuantityString(
                R.plurals.location_app_permission_summary_location_on, 5, 5, 10));
    }
}
