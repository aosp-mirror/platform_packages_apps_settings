package com.android.settings.fuelgauge;

import android.os.Process;
import android.util.SparseArray;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.PowerUsageAdvanced.PowerUsageData;
import com.android.settings.fuelgauge.PowerUsageAdvanced.PowerUsageData.UsageType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PowerUsageAdvancedTest {
    private static final int FAKE_UID_1 = 50;
    private static final int FAKE_UID_2 = 100;
    private static final double TYPE_APP_USAGE = 80;
    private static final double TYPE_BLUETOOTH_USAGE = 50;
    private static final double TYPE_WIFI_USAGE = 0;
    private static final double TOTAL_USAGE = TYPE_APP_USAGE * 2 + TYPE_BLUETOOTH_USAGE
            + TYPE_WIFI_USAGE;
    private static final double PRECISION = 0.001;
    private static final String STRING_NOT_FOUND = "not_found";
    @Mock
    private BatterySipper mBatterySipper;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    private PowerUsageAdvanced mPowerUsageAdvanced;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPowerUsageAdvanced = new PowerUsageAdvanced();

        List<BatterySipper> batterySippers = new ArrayList<>();
        batterySippers.add(new BatterySipper(DrainType.APP,
                new FakeUid(FAKE_UID_1), TYPE_APP_USAGE));
        batterySippers.add(new BatterySipper(DrainType.APP,
                new FakeUid(FAKE_UID_2), TYPE_APP_USAGE));
        batterySippers.add(new BatterySipper(DrainType.BLUETOOTH, new FakeUid(FAKE_UID_1),
                TYPE_BLUETOOTH_USAGE));
        batterySippers.add(new BatterySipper(DrainType.WIFI, new FakeUid(FAKE_UID_1),
                TYPE_WIFI_USAGE));

        when(mBatteryStatsHelper.getUsageList()).thenReturn(batterySippers);
        when(mBatteryStatsHelper.getTotalPower()).thenReturn(TOTAL_USAGE);
    }

    @Test
    public void testExtractUsageType_TypeSystem_ReturnSystem() {
        mBatterySipper.drainType = DrainType.APP;
        final int uids[] = {Process.SYSTEM_UID, Process.ROOT_UID};

        for (int uid : uids) {
            when(mBatterySipper.getUid()).thenReturn(uid);
            assertThat(mPowerUsageAdvanced.extractUsageType(mBatterySipper))
                    .isEqualTo(UsageType.SYSTEM);
        }
    }

    @Test
    public void testExtractUsageType_TypeEqualsToDrainType_ReturnRelevantType() {
        final DrainType drainTypes[] = {DrainType.WIFI, DrainType.BLUETOOTH, DrainType.IDLE,
                DrainType.USER, DrainType.CELL};
        final int usageTypes[] = {UsageType.WIFI, UsageType.BLUETOOTH, UsageType.IDLE,
                UsageType.USER, UsageType.CELL};

        assertThat(drainTypes.length).isEqualTo(usageTypes.length);
        for (int i = 0, size = drainTypes.length; i < size; i++) {
            mBatterySipper.drainType = drainTypes[i];
            assertThat(mPowerUsageAdvanced.extractUsageType(mBatterySipper))
                    .isEqualTo(usageTypes[i]);
        }
    }

    @Test
    public void testParsePowerUsageData_PercentageCalculatedCorrectly() {
        final double percentApp = TYPE_APP_USAGE * 2 / TOTAL_USAGE * 100;
        final double percentWifi = TYPE_WIFI_USAGE / TOTAL_USAGE * 100;
        final double percentBluetooth = TYPE_BLUETOOTH_USAGE / TOTAL_USAGE * 100;

        mPowerUsageAdvanced.init();
        List<PowerUsageData> batteryData =
                mPowerUsageAdvanced.parsePowerUsageData(mBatteryStatsHelper);
        for (PowerUsageData data : batteryData) {
            switch (data.usageType) {
                case UsageType.WIFI:
                    assertThat(data.percentage).isWithin(PRECISION).of(percentWifi);
                    break;
                case UsageType.APP:
                    assertThat(data.percentage).isWithin(PRECISION).of(percentApp);
                    break;
                case UsageType.BLUETOOTH:
                    assertThat(data.percentage).isWithin(PRECISION).of(percentBluetooth);
                    break;
                default:
                    break;
            }
        }
    }

    @Test
    public void testInit_ContainsAllUsageType() {
        mPowerUsageAdvanced.init();
        final SparseArray<String> array = mPowerUsageAdvanced.mUsageTypeMap;

        assertThat(array.get(UsageType.APP, STRING_NOT_FOUND))
                .isNotEqualTo(STRING_NOT_FOUND);
        assertThat(array.get(UsageType.WIFI, STRING_NOT_FOUND))
                .isNotEqualTo(STRING_NOT_FOUND);
        assertThat(array.get(UsageType.CELL, STRING_NOT_FOUND))
                .isNotEqualTo(STRING_NOT_FOUND);
        assertThat(array.get(UsageType.BLUETOOTH, STRING_NOT_FOUND))
                .isNotEqualTo(STRING_NOT_FOUND);
        assertThat(array.get(UsageType.IDLE, STRING_NOT_FOUND))
                .isNotEqualTo(STRING_NOT_FOUND);
        assertThat(array.get(UsageType.SERVICE, STRING_NOT_FOUND))
                .isNotEqualTo(STRING_NOT_FOUND);
        assertThat(array.get(UsageType.USER, STRING_NOT_FOUND))
                .isNotEqualTo(STRING_NOT_FOUND);
        assertThat(array.get(UsageType.SYSTEM, STRING_NOT_FOUND))
                .isNotEqualTo(STRING_NOT_FOUND);
    }
}
