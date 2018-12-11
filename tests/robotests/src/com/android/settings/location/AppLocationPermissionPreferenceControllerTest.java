package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = new AppLocationPermissionPreferenceController(mContext, mLifecycle);
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
}
