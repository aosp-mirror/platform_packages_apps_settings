package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;

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
    @Config(qualifiers = "mcc999")
    public void testNonIndexableKeys_ifPageDisabled_shouldNotIndexResource() {
        final List<String> niks =
            ConfigureWifiSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
        final int xmlId = new ConfigureWifiSettings().getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);
        assertThat(keys).isNotNull();
        assertThat(niks).containsAllIn(keys);
    }
}
