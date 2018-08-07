package com.android.settings.datetime;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.datetime.ZoneGetter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@RunWith(SettingsRobolectricTestRunner.class)
public class ZonePickerComparatorTest {

    // Strings in Chinese are sorted by alphabet order of their Pinyin.
    // "伦敦" -> "lundun";  "纽约" -> "niuyue";  "悉尼" -> "xini"
    // "开罗" -> "kailuo";  "雅典" -> "yadian";  "上海" -> "shanghai"
    private static final String[] TEST_CHINESE_NAME =
            {"伦敦", "纽约", "悉尼", "开罗", "雅典", "上海"};
    private static final String[] ORDERED_CHINESE_NAME =
            {"开罗", "伦敦", "纽约", "上海", "悉尼", "雅典"};

    private static final String[] TEST_ENGLISH_NAME =
            {"London", "New York", "Sydney", "Cairo", "Athens", "Shanghai"};
    private static final String[] ORDERED_ENGLISH_NAME =
            {"Athens", "Cairo", "London", "New York", "Shanghai", "Sydney"};

    private static final Locale INIT_LOCALE = Locale.getDefault();

    private Map<String, List<String>> mTestDataMap;
    private List<Map<String, Object>> mTestList;

    @Before
    public void setUp() {
        mTestDataMap = new HashMap<>();
        mTestDataMap.put("zh_CN", Arrays.asList(TEST_CHINESE_NAME));
        mTestDataMap.put("en_US", Arrays.asList(TEST_ENGLISH_NAME));
    }

    @After
    public void tearDown() {
        Locale.setDefault(INIT_LOCALE);
    }

    @Test
    public void testComparator_sortChineseString() {
        String sortKey = ZoneGetter.KEY_DISPLAY_LABEL;
        mTestList = getMockZonesList("zh_CN");
        Locale.setDefault(new Locale("zh"));
        mTestList.sort(new ZonePicker.MyComparator(sortKey));
        for (int i = 0; i < mTestList.size(); i++) {
            assertThat(mTestList.get(i).get(sortKey).toString()).isEqualTo(ORDERED_CHINESE_NAME[i]);
        }
    }

    @Test
    public void testComparator_sortEnglishString() {
        String sortKey = ZoneGetter.KEY_DISPLAY_LABEL;
        mTestList = getMockZonesList("en_US");
        Locale.setDefault(new Locale("en"));
        mTestList.sort(new ZonePicker.MyComparator(sortKey));
        for (int i = 0; i < mTestList.size(); i++) {
            assertThat(mTestList.get(i).get(sortKey).toString()).isEqualTo(ORDERED_ENGLISH_NAME[i]);
        }
    }

    @Test
    public void testComparator_sortInteger() {
        String sortKey = ZoneGetter.KEY_OFFSET;
        // TestList of any locale can be selected to test integer sorting.
        mTestList = getMockZonesList("en_US");
        mTestList.sort(new ZonePicker.MyComparator(sortKey));
        for (int i = 0; i < mTestList.size(); i++) {
            assertThat(mTestList.get(i).get(sortKey)).isEqualTo(i);
        }
    }

    private List<Map<String, Object>> getMockZonesList(String locale) {
         List<Map<String, Object>> zones = new ArrayList<>();
         List<String> testData = mTestDataMap.get(locale);
         TimeZone tz = TimeZone.getDefault();
         int testSize = testData.size();
         for (int i = 0; i < testSize; i++) {
             zones.add(createMockDisplayEntry(tz, "GMT+08:00", testData.get(i), testSize - i - 1));
         }
         return zones;
    }

    private Map<String, Object> createMockDisplayEntry(
            TimeZone tz, CharSequence gmtOffsetText, CharSequence displayName, int offsetMillis) {
         Map<String, Object> map = new HashMap<>();
         map.put(ZoneGetter.KEY_ID, tz.getID());
         map.put(ZoneGetter.KEY_DISPLAYNAME, displayName.toString());
         map.put(ZoneGetter.KEY_DISPLAY_LABEL, displayName);
         map.put(ZoneGetter.KEY_GMT, gmtOffsetText.toString());
         map.put(ZoneGetter.KEY_OFFSET_LABEL, gmtOffsetText);
         map.put(ZoneGetter.KEY_OFFSET, offsetMillis);
         return map;
    }
}
