package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ConfigureWifiSettingsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final List<String> niks =
            ConfigureWifiSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
        final int xmlId = new ConfigureWifiSettings().getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testNonIndexableKeys_ifPageDisabled_shouldNotIndexResource() {
        final List<String> niks =
            ConfigureWifiSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
        final int xmlId = new ConfigureWifiSettings().getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);
        assertThat(keys).isNotNull();
        assertThat(niks).containsAllIn(keys);
    }

    @Test
    public void testNonIndexableKeys_noConnection_blocksIP() {
        ConnectivityManager manager = mock(ConnectivityManager.class);
        when(manager.getActiveNetworkInfo()).thenReturn(null);
        doReturn(manager).when(mContext).getSystemService(Context.CONNECTIVITY_SERVICE);

        final List<String> niks =
            ConfigureWifiSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
        assertThat(niks).contains(ConfigureWifiSettings.KEY_IP_ADDRESS);
    }

    @Test
    public void testNonIndexableKeys_wifiConnection_blocksIP() {
        ConnectivityManager manager = mock(ConnectivityManager.class);
        NetworkInfo info = mock(NetworkInfo.class);
        when(info.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
        when(manager.getActiveNetworkInfo()).thenReturn(info);
        doReturn(manager).when(mContext).getSystemService(Context.CONNECTIVITY_SERVICE);

        final List<String> niks =
            ConfigureWifiSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
        assertThat(niks).contains(ConfigureWifiSettings.KEY_IP_ADDRESS);
    }

    @Test
    public void testNonIndexableKeys_mobileConnection_blocksIP() {
        ConnectivityManager manager = mock(ConnectivityManager.class);
        NetworkInfo info = mock(NetworkInfo.class);
        when(info.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(manager.getActiveNetworkInfo()).thenReturn(info);
        doReturn(manager).when(mContext).getSystemService(Context.CONNECTIVITY_SERVICE);

        final List<String> niks =
            ConfigureWifiSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
        assertThat(niks).doesNotContain(ConfigureWifiSettings.KEY_IP_ADDRESS);
    }
}
