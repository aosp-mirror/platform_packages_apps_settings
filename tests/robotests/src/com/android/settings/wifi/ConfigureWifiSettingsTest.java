package com.android.settings.wifi;

import static com.android.settings.wifi.ConfigureWifiSettings.KEY_INSTALL_CREDENTIALS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ConfigureWifiSettingsTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    UserManager mUserManager;
    @Mock
    WifiManager mWifiManager;
    @Mock
    FragmentActivity mActivity;
    @Mock
    WifiWakeupPreferenceController mWifiWakeupPreferenceController;
    @Mock
    Preference mInstallCredentialsPref;
    @Mock
    PreferenceScreen mPreferenceScreen;
    @Mock
    TextView mEmptyView;

    TestConfigureWifiSettings mSettings;

    @Before
    public void setUp() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mActivity.getSystemService(WifiManager.class)).thenReturn(mWifiManager);

        mSettings = spy(new TestConfigureWifiSettings());
        when(mSettings.getContext()).thenReturn(mContext);
        when(mSettings.getActivity()).thenReturn(mActivity);
        when(mSettings.use(WifiWakeupPreferenceController.class))
                .thenReturn(mWifiWakeupPreferenceController);
        when(mSettings.findPreference(KEY_INSTALL_CREDENTIALS)).thenReturn(mInstallCredentialsPref);
    }

    @Test
    public void onAttach_isNotGuestUser_setupController() {
        when(mUserManager.isGuestUser()).thenReturn(false);

        mSettings.onAttach(mContext);

        verify(mWifiWakeupPreferenceController).setFragment(any());
    }

    @Test
    public void onAttach_isGuestUser_doNotSetupController() {
        when(mUserManager.isGuestUser()).thenReturn(true);

        mSettings.onAttach(mContext);

        verify(mWifiWakeupPreferenceController, never()).setFragment(any());
    }

    @Test
    @Config(shadows = ShadowDashboardFragment.class)
    public void onCreate_isNotGuestUser_setupPreference() {
        when(mUserManager.isGuestUser()).thenReturn(false);

        mSettings.onCreate(null);

        verify(mInstallCredentialsPref).setOnPreferenceClickListener(any());
    }

    @Test
    @Config(shadows = ShadowDashboardFragment.class)
    public void onCreate_isGuestUser_doNotSetupPreference() {
        when(mUserManager.isGuestUser()).thenReturn(true);

        mSettings.onCreate(null);

        verify(mInstallCredentialsPref, never()).setOnPreferenceClickListener(any());
    }

    @Test
    @Config(shadows = ShadowDashboardFragment.class)
    public void onViewCreated_isNotGuestUser_doNotRestrictUi() {
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mActivity.findViewById(android.R.id.empty)).thenReturn(mEmptyView);
        doReturn(mPreferenceScreen).when(mSettings).getPreferenceScreen();

        mSettings.onViewCreated(mock(View.class), null);

        verify(mEmptyView, never()).setVisibility(View.VISIBLE);
        verify(mPreferenceScreen, never()).removeAll();
    }

    @Test
    @Config(shadows = ShadowDashboardFragment.class)
    public void onViewCreated_isGuestUser_restrictUi() {
        when(mUserManager.isGuestUser()).thenReturn(true);
        when(mActivity.findViewById(android.R.id.empty)).thenReturn(mEmptyView);
        doReturn(mPreferenceScreen).when(mSettings).getPreferenceScreen();

        mSettings.onViewCreated(mock(View.class), null);

        verify(mEmptyView).setVisibility(View.VISIBLE);
        verify(mPreferenceScreen).removeAll();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getNonIndexableKeys_ifPageDisabled_shouldNotIndexResource() {
        final List<String> niks =
            ConfigureWifiSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        final int xmlId = mSettings.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);
        assertThat(keys).isNotNull();
        assertThat(niks).containsAtLeastElementsIn(keys);
    }

    public static class TestConfigureWifiSettings extends ConfigureWifiSettings {
        @Override
        public <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return super.use(clazz);
        }
    }

    @Implements(DashboardFragment.class)
    public static class ShadowDashboardFragment {
        @Implementation
        public void onCreate(Bundle icicle) {
            // do nothing
        }

        @Implementation
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            // do nothing
        }
    }
}
