package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.SearchIndexableResource;
import android.util.ArraySet;
import com.android.settings.DateTimeSettings;
import com.android.settings.R;
import com.android.settings.SecuritySettings;
import com.android.settings.TestConfig;
import com.android.settings.core.codeinspection.CodeInspector;
import com.android.settings.datausage.DataPlanUsageSummary;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        assetDir = "/tests/robotests/assets")
public class DataIntegrityTest {

    @Test
    @Config(shadows = {
            SettingsShadowResources.class,
            SettingsShadowResources.SettingsShadowTheme.class,
    })
    public void testIndexableResources_uniqueKeys() {
        final Context context = RuntimeEnvironment.application;
        // Aggregation of all keys
        final Set<String> masterKeys = new ArraySet<>();
        // Aggregation of the incorrectly duplicate keys
        final Set<String> duplicateKeys = new ArraySet<>();
        // Keys for a specific page
        final Set<String> pageKeys = new ArraySet<>();
        // List of all Xml preferences
        final Set<Integer> xmlList = new ArraySet<>();
        // Duplicates we know about.
        List<String> grandfatheredKeys = new ArrayList<>();
        CodeInspector.initializeGrandfatherList(grandfatheredKeys,
                "whitelist_duplicate_index_key");

        // Get a list of all Xml.
        for (SearchIndexableResource val : SearchIndexableResources.values()) {
            final int xmlResId = val.xmlResId;
            if (xmlResId != 0) {
                xmlList.add(xmlResId);
            } else {
                // Take class and get all keys
                final Class clazz = DatabaseIndexingUtils.getIndexableClass(val.className);

                // Skip classes that are invalid or cannot be mocked. Add them as special Xml below.
                if (clazz == null
                        || clazz == DateTimeSettings.class
                        || clazz == DataPlanUsageSummary.class
                        || clazz == DataUsageSummary.class
                        || clazz == SecuritySettings.class) {
                    continue;
                }

                Indexable.SearchIndexProvider provider = DatabaseIndexingUtils
                        .getSearchIndexProvider(clazz);

                if (provider == null) {
                    continue;
                }

                List<SearchIndexableResource> subXml =
                        provider.getXmlResourcesToIndex(context, true);

                if (subXml == null) {
                    continue;
                }
                for (SearchIndexableResource resource : subXml) {
                    final int subXmlResId = resource.xmlResId;
                    if (subXmlResId != 0) {
                        xmlList.add(subXmlResId);
                    }
                }
            }
        }
        addSpecialXml(xmlList);

        // Get keys from all Xml and check for duplicates.
        for (Integer xmlResId : xmlList) {
            // Get all keys to be indexed
            final List<String> prefKeys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlResId);
            pageKeys.addAll(prefKeys);
            // Find all already-existing keys.
            pageKeys.retainAll(masterKeys);
            // Keep list of offending duplicate keys.
            duplicateKeys.addAll(pageKeys);
            // Add all keys to master key list.
            masterKeys.addAll(prefKeys);
            pageKeys.clear();
        }
        assertThat(duplicateKeys).containsExactlyElementsIn(grandfatheredKeys);
    }

    /**
     * Add XML preferences from Fragments which have issues being instantiated in robolectric.
     */
    private void addSpecialXml(Set<Integer> xmlList) {
        xmlList.add(R.xml.date_time_prefs);
        xmlList.add(R.xml.data_usage);
        xmlList.add(R.xml.data_usage_cellular);
        xmlList.add(R.xml.data_usage_wifi);
        xmlList.add(R.xml.security_settings_misc);
    }


}
