package com.android.settings.development;

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes.REQUEST_MOCK_LOCATION_APP;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.wrapper.PackageManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;

@RunWith(SettingsRobolectricTestRunner.class)
public class MockLocationAppPreferenceControllerTest {

    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private PackageManagerWrapper mPackageManager;
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private AppOpsManager.PackageOps mPackageOps;
    @Mock
    private AppOpsManager.OpEntry mOpEntry;
    @Mock
    private ApplicationInfo mApplicationInfo;

    private Context mContext;
    private MockLocationAppPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new MockLocationAppPreferenceController(mContext, mFragment));
        ReflectionHelpers.setField(mController, "mAppsOpsManager", mAppOpsManager);
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void updateState_foobarAppSelected_shouldSetSummaryToFoobar() {
        final String appName = "foobar";
        when(mAppOpsManager.getPackagesForOps(any())).thenReturn(
                Collections.singletonList(mPackageOps));
        when(mPackageOps.getOps()).thenReturn(Collections.singletonList(mOpEntry));
        when(mOpEntry.getMode()).thenReturn(AppOpsManager.MODE_ALLOWED);
        when(mPackageOps.getPackageName()).thenReturn(appName);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.mock_location_app_set, appName));
    }

    @Test
    public void updateState_noAppSelected_shouldSetSummaryToDefault() {
        when(mAppOpsManager.getPackagesForOps(any())).thenReturn(Collections.emptyList());

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.mock_location_app_not_set));
    }

    @Test
    public void onActivityResult_fooPrevAppBarNewApp_shouldRemoveFooAndSetBarAsMockLocationApp()
            throws PackageManager.NameNotFoundException {
        final String prevAppName = "foo";
        final String newAppName = "bar";
        final Intent intent = new Intent();
        intent.setAction(newAppName);
        when(mAppOpsManager.getPackagesForOps(any()))
            .thenReturn(Collections.singletonList(mPackageOps));
        when(mPackageOps.getOps()).thenReturn(Collections.singletonList(mOpEntry));
        when(mOpEntry.getMode()).thenReturn(AppOpsManager.MODE_ALLOWED);
        when(mPackageOps.getPackageName()).thenReturn(prevAppName);
        when(mPackageManager.getApplicationInfo(anyString(),
                eq(PackageManager.MATCH_DISABLED_COMPONENTS))).thenReturn(mApplicationInfo);

        final boolean handled =
            mController.onActivityResult(REQUEST_MOCK_LOCATION_APP, Activity.RESULT_OK, intent);

        assertThat(handled).isTrue();
        verify(mAppOpsManager).setMode(anyInt(), anyInt(), eq(newAppName),
                eq(AppOpsManager.MODE_ALLOWED));
        verify(mAppOpsManager).setMode(anyInt(), anyInt(), eq(prevAppName),
                eq(AppOpsManager.MODE_ERRORED));
    }

    @Test
    public void onActivityResult_incorrectCode_shouldReturnFalse() {
        final boolean handled = mController.onActivityResult(30983150 /* request code */,
                Activity.RESULT_OK, null /* intent */);

        assertThat(handled).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_samePreferenceKey_shouldLaunchActivity() {
        final String preferenceKey = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(preferenceKey);

        final boolean handled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(handled).isTrue();
        verify(mFragment).startActivityForResult(any(), eq(REQUEST_MOCK_LOCATION_APP));
    }

    @Test
    public void handlePreferenceTreeClick_incorrectPreferenceKey_shouldReturnFalse() {
        when(mPreference.getKey()).thenReturn("SomeRandomKey");

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
    }
}
