package com.android.settings.accessibility;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.XmlTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccessibilitySettingsTest {

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks = AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        final List<String> keys = new ArrayList<>();

        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.accessibility_settings));

        assertThat(keys).containsAllIn(niks);
    }
}
