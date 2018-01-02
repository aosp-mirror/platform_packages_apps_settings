package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.TtsSpan;
import android.util.IconDrawableFactory;
import android.widget.EditText;
import android.widget.TextView;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.DevicePolicyManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UtilsTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int USER_ID = 1;

    @Mock
    private WifiManager wifiManager;
    @Mock
    private Network network;
    @Mock
    private ConnectivityManager connectivityManager;
    @Mock
    private DevicePolicyManagerWrapper mDevicePolicyManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private IconDrawableFactory mIconDrawableFactory;
    @Mock
    private ApplicationInfo mApplicationInfo;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(wifiManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
    }

    @Test
    public void testGetWifiIpAddresses_succeeds() throws Exception {
        when(wifiManager.getCurrentNetwork()).thenReturn(network);
        LinkAddress address = new LinkAddress(InetAddress.getByName("127.0.0.1"), 0);
        LinkProperties lp = new LinkProperties();
        lp.addLinkAddress(address);
        when(connectivityManager.getLinkProperties(network)).thenReturn(lp);

        assertThat(Utils.getWifiIpAddresses(mContext)).isEqualTo("127.0.0.1");
    }

    @Test
    public void testGetWifiIpAddresses_nullLinkProperties() {
        when(wifiManager.getCurrentNetwork()).thenReturn(network);
        // Explicitly set the return value to null for readability sake.
        when(connectivityManager.getLinkProperties(network)).thenReturn(null);

        assertThat(Utils.getWifiIpAddresses(mContext)).isNull();
    }

    @Test
    public void testGetWifiIpAddresses_nullNetwork() {
        // Explicitly set the return value to null for readability sake.
        when(wifiManager.getCurrentNetwork()).thenReturn(null);

        assertThat(Utils.getWifiIpAddresses(mContext)).isNull();
    }

    @Test
    public void testFormatElapsedTime_WithSeconds_ShowSeconds() {
        final double testMillis = 5 * DateUtils.MINUTE_IN_MILLIS + 30 * DateUtils.SECOND_IN_MILLIS;
        final String expectedTime = "5m 30s";

        assertThat(Utils.formatElapsedTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatElapsedTime_NoSeconds_DoNotShowSeconds() {
        final double testMillis = 5 * DateUtils.MINUTE_IN_MILLIS + 30 * DateUtils.SECOND_IN_MILLIS;
        final String expectedTime = "6m";

        assertThat(Utils.formatElapsedTime(mContext, testMillis, false).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatElapsedTime_TimeMoreThanOneDay_ShowCorrectly() {
        final double testMillis = 2 * DateUtils.DAY_IN_MILLIS
                + 4 * DateUtils.HOUR_IN_MILLIS + 15 * DateUtils.MINUTE_IN_MILLIS;
        final String expectedTime = "2d 4h 15m";

        assertThat(Utils.formatElapsedTime(mContext, testMillis, false).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatElapsedTime_ZeroFieldsInTheMiddleDontShow() {
        final double testMillis = 2 * DateUtils.DAY_IN_MILLIS + 15 * DateUtils.MINUTE_IN_MILLIS;
        final String expectedTime = "2d 15m";

        assertThat(Utils.formatElapsedTime(mContext, testMillis, false).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatElapsedTime_FormatZero_WithSeconds() {
        final double testMillis = 0;
        final String expectedTime = "0s";

        assertThat(Utils.formatElapsedTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatElapsedTime_FormatZero_NoSeconds() {
        final double testMillis = 0;
        final String expectedTime = "0m";

        assertThat(Utils.formatElapsedTime(mContext, testMillis, false).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatElapsedTime_onlyContainsMinute_hasTtsSpan() {
        final double testMillis = 15 * DateUtils.MINUTE_IN_MILLIS;

        final CharSequence charSequence = Utils.formatElapsedTime(mContext, testMillis, false);
        assertThat(charSequence).isInstanceOf(SpannableStringBuilder.class);

        final SpannableStringBuilder expectedString = (SpannableStringBuilder) charSequence;
        final TtsSpan[] ttsSpans = expectedString.getSpans(0, expectedString.length(),
                TtsSpan.class);

        assertThat(ttsSpans).asList().hasSize(1);
        assertThat(ttsSpans[0].getType()).isEqualTo(TtsSpan.TYPE_MEASURE);
    }

    @Test
    public void testFormatRelativeTime_WithSeconds_ShowSeconds() {
        final double testMillis = 40 * DateUtils.SECOND_IN_MILLIS;
        final String expectedTime = "40 sec. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_NoSeconds_DoNotShowSeconds() {
        final double testMillis = 40 * DateUtils.SECOND_IN_MILLIS;
        final String expectedTime = "1 min. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, false).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_LessThanTwoMinutes_withSeconds() {
        final double testMillis = 119 * DateUtils.SECOND_IN_MILLIS;
        final String expectedTime = "119 sec. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_LessThanTwoMinutes_NoSeconds() {
        final double testMillis = 119 * DateUtils.SECOND_IN_MILLIS;
        final String expectedTime = "2 min. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, false).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_TwoMinutes_withSeconds() {
        final double testMillis = 2 * DateUtils.MINUTE_IN_MILLIS;
        final String expectedTime = "2 min. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_LessThanTwoHours_withSeconds() {
        final double testMillis = 119 * DateUtils.MINUTE_IN_MILLIS;
        final String expectedTime = "119 min. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_TwoHours_withSeconds() {
        final double testMillis = 2 * DateUtils.HOUR_IN_MILLIS;
        final String expectedTime = "2 hr. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_LessThanTwoDays_withSeconds() {
        final double testMillis = 47 * DateUtils.HOUR_IN_MILLIS;
        final String expectedTime = "47 hr. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_TwoDays_withSeconds() {
        final double testMillis = 2 * DateUtils.DAY_IN_MILLIS;
        final String expectedTime = "2 days ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_FormatZero_WithSeconds() {
        final double testMillis = 0;
        final String expectedTime = "0 sec. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, true).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testFormatRelativeTime_FormatZero_NoSeconds() {
        final double testMillis = 0;
        final String expectedTime = "0 min. ago";

        assertThat(Utils.formatRelativeTime(mContext, testMillis, false).toString()).isEqualTo(
                expectedTime);
    }

    @Test
    public void testInitializeVolumeDoesntBreakOnNullVolume() {
        VolumeInfo info = new VolumeInfo("id", 0, new DiskInfo("id", 0), "");
        StorageManager storageManager = mock(StorageManager.class, RETURNS_DEEP_STUBS);
        when(storageManager.findVolumeById(anyString())).thenReturn(info);

        Utils.maybeInitializeVolume(storageManager, new Bundle());
    }

    @Test
    public void testGetInstallationStatus_notInstalled_shouldReturnUninstalled() {
        assertThat(Utils.getInstallationStatus(new ApplicationInfo()))
                .isEqualTo(R.string.not_installed);
    }

    @Test
    public void testGetInstallationStatus_enabled_shouldReturnInstalled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;

        assertThat(Utils.getInstallationStatus(info)).isEqualTo(R.string.installed);
    }

    @Test
    public void testGetInstallationStatus_disabled_shouldReturnDisabled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = false;

        assertThat(Utils.getInstallationStatus(info)).isEqualTo(R.string.disabled);
    }

    @Test
    public void testIsProfileOrDeviceOwner_deviceOwnerApp_returnTrue() {
        when(mDevicePolicyManager.isDeviceOwnerAppOnAnyUser(PACKAGE_NAME)).thenReturn(true);

        assertThat(Utils.isProfileOrDeviceOwner(mUserManager, mDevicePolicyManager,
                PACKAGE_NAME)).isTrue();
    }

    @Test
    public void testIsProfileOrDeviceOwner_profileOwnerApp_returnTrue() {
        final List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo());

        when(mUserManager.getUsers()).thenReturn(userInfos);
        when(mDevicePolicyManager.getProfileOwnerAsUser(userInfos.get(0).id)).thenReturn(
                new ComponentName(PACKAGE_NAME, ""));

        assertThat(Utils.isProfileOrDeviceOwner(mUserManager, mDevicePolicyManager,
                PACKAGE_NAME)).isTrue();
    }

    @Test
    public void testSetEditTextCursorPosition_shouldGetExpectedEditTextLenght() {
        final EditText editText = new EditText(mContext);
        final CharSequence text = "test";
        editText.setText(text, TextView.BufferType.EDITABLE);
        final int length = editText.getText().length();
        Utils.setEditTextCursorPosition(editText);

        assertThat(editText.getSelectionEnd()).isEqualTo(length);
    }

    @Test
    public void testGetBadgedIcon_usePackageNameAndUserId() throws
            PackageManager.NameNotFoundException {
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(PACKAGE_NAME,
                PackageManager.GET_META_DATA);

        Utils.getBadgedIcon(mIconDrawableFactory, mPackageManager, PACKAGE_NAME, USER_ID);

        // Verify that it uses the correct user id
        verify(mIconDrawableFactory).getBadgedIcon(mApplicationInfo, USER_ID);
    }

}
