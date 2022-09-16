package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public final class AppStateAppBatteryUsageBridgeTest {
  private static final String TEST_PACKAGE_1 = "com.example.test.pkg1";
  private static final String TEST_PACKAGE_2 = "com.example.test.pkg2";
  private static final int UID_1 = 12345;
  private static final int UID_2 = 7654321;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Context mContext;
  @Mock
  private AppOpsManager mAppOpsManager;
  @Mock
  private PowerAllowlistBackend mPowerAllowlistBackend;

  @Before
  public void initMocks() {
      MockitoAnnotations.initMocks(this);
  }

  @Test
  public void updateExtraInfo_updatesRestricted() {
    when(mPowerAllowlistBackend.isAllowlisted(TEST_PACKAGE_1)).thenReturn(false);
    when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND,
            UID_1, TEST_PACKAGE_1)).thenReturn(AppOpsManager.MODE_IGNORED);
    AppStateAppBatteryUsageBridge bridge =
            new AppStateAppBatteryUsageBridge(mContext, null, null);
    bridge.mAppOpsManager = mAppOpsManager;
    bridge.mPowerAllowlistBackend = mPowerAllowlistBackend;
    AppEntry entry = new AppEntry(mContext, null, 0);

    bridge.updateExtraInfo(entry, TEST_PACKAGE_1, UID_1);

    assertThat(entry.extraInfo.getClass())
            .isEqualTo(AppStateAppBatteryUsageBridge.AppBatteryUsageDetails.class);
    assertThat(AppStateAppBatteryUsageBridge.getAppBatteryUsageDetailsMode(entry))
            .isEqualTo(AppStateAppBatteryUsageBridge.MODE_RESTRICTED);
  }

  @Test
  public void updateExtraInfo_updatesUnrestricted() {
    when(mPowerAllowlistBackend.isAllowlisted(TEST_PACKAGE_1)).thenReturn(true);
    when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND,
            UID_2, TEST_PACKAGE_2)).thenReturn(AppOpsManager.MODE_ALLOWED);
    AppStateAppBatteryUsageBridge bridge =
            new AppStateAppBatteryUsageBridge(mContext, null, null);
    bridge.mAppOpsManager = mAppOpsManager;
    bridge.mPowerAllowlistBackend = mPowerAllowlistBackend;
    AppEntry entry = new AppEntry(mContext, null, 0);

    bridge.updateExtraInfo(entry, TEST_PACKAGE_2, UID_2);

    assertThat(entry.extraInfo.getClass())
            .isEqualTo(AppStateAppBatteryUsageBridge.AppBatteryUsageDetails.class);
    assertThat(AppStateAppBatteryUsageBridge.getAppBatteryUsageDetailsMode(entry))
            .isEqualTo(AppStateAppBatteryUsageBridge.MODE_UNRESTRICTED);
  }
}
