package com.android.settings.location;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppLocationPermissionPreferenceControllerTest {
    @Mock
    private Preference mPreference;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private AppLocationPermissionPreferenceController mController;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new AppLocationPermissionPreferenceController(mContext);
    }

    @Test
    public void displayPreference_shouldRemovePreference() {
        Settings.System.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED,
                0);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void displayPreference_shouldNotRemovePreference() {
        Settings.System.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED,
                1);
        mController.displayPreference(mScreen);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }

}
