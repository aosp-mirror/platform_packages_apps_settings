package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.XmlTestUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DisplaySettingsTest {

    @Test
    public void testPreferenceControllers_getPreferenceKeys_existInPreferenceScreen() {
        final Context context = ApplicationProvider.getApplicationContext();
        final DisplaySettings fragment = new DisplaySettings();
        final List<String> preferenceScreenKeys = XmlTestUtils.getKeysFromPreferenceXml(context,
                fragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (AbstractPreferenceController controller : fragment.createPreferenceControllers(context)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }
        // Nightmode is currently hidden
        preferenceKeys.remove("night_mode");

        assertThat(preferenceScreenKeys).containsAtLeastElementsIn(preferenceKeys);
    }
}
