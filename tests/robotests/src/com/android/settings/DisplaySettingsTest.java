package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowPowerManagerWrapper;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DisplaySettingsTest {

    @Test
    @Config(shadows = ShadowPowerManagerWrapper.class)
    public void testPreferenceControllers_getPreferenceKeys_existInPreferenceScreen() {
        final Context context = RuntimeEnvironment.application;
//        PowerManager wrapper = mock(PowerManager.class);
//        doReturn(wrapper).when(context).getSystemService(Context.POWER_SERVICE);
        final DisplaySettings fragment = new DisplaySettings();
        final List<String> preferenceScreenKeys = XmlTestUtils.getKeysFromPreferenceXml(context,
                fragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (AbstractPreferenceController controller : fragment.getPreferenceControllers(context)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }
        // Nightmode is currently hidden
        preferenceKeys.remove("night_mode");

        assertThat(preferenceScreenKeys).containsAllIn(preferenceKeys);
    }
}
