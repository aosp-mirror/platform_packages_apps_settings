package com.android.settings.notification;

import android.content.res.Resources;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingPrefTest {

    @Test
    public void update_setsDropDownSummaryText() {
        Context context = RuntimeEnvironment.application;
        String testSetting = "test_setting";
        int[] values = new int[] {1,2,3};
        String[] entries = new String[] {"one", "two", "three"};
        SettingPref settingPref =
                spy(new SettingPref(SettingPref.TYPE_GLOBAL, "key", testSetting, 0, values) {
                    @Override
                    protected String getCaption(Resources res, int value) {
                        return "temp";
                    }
                });
        DropDownPreference dropdownPref = spy(new DropDownPreference(context));
        dropdownPref.setEntries(entries);
        settingPref.mDropDown = dropdownPref;
        Settings.Global.putInt(context.getContentResolver(), testSetting, values[2]);

        settingPref.update(context);

        assertThat(settingPref.mDropDown.getSummary()).isEqualTo(entries[2]);
    }

    @Test
    public void update_setsDropDownSummaryText_noMatch_noError() {
        Context context = RuntimeEnvironment.application;
        String testSetting = "test_setting";
        int[] values = new int[] {1,2,3};
        String[] entries = new String[] {"one", "two", "three"};
        SettingPref settingPref =
                spy(new SettingPref(SettingPref.TYPE_GLOBAL, "key", testSetting, 0, values) {
                    @Override
                    protected String getCaption(Resources res, int value) {
                        return "temp";
                    }
                });
        DropDownPreference dropdownPref = spy(new DropDownPreference(context));
        dropdownPref.setEntries(entries);
        settingPref.mDropDown = dropdownPref;
        Settings.Global.putInt(context.getContentResolver(), testSetting, -1);

        settingPref.update(context);

        assertThat(settingPref.mDropDown.getSummary()).isNull();
    }
}
