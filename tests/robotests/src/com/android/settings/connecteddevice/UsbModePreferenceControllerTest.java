package com.android.settings.connecteddevice;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.deviceinfo.UsbBackend;
import com.android.settings.deviceinfo.UsbModeChooserActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UsbModePreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private UsbBackend mUsbBackend;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private Context mContext;
    private UsbModePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mController = new UsbModePreferenceController(mContext, mUsbBackend);
    }

    @Test
    public void testGetSummary_chargeDevice() {
        assertThat(mController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[0]))
                .isEqualTo(R.string.usb_summary_charging_only);
    }

    @Test
    public void testGetSummary_supplyPower() {
        assertThat(mController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[1]))
                .isEqualTo(R.string.usb_summary_power_only);
    }

    @Test
    public void testGetSummary_TransferFiles() {
        assertThat(mController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[2]))
                .isEqualTo(R.string.usb_summary_file_transfers);
    }

    @Test
    public void testGetSummary_TransferPhoto() {
        assertThat(mController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[3]))
                .isEqualTo(R.string.usb_summary_photo_transfers);
    }

    @Test
    public void testGetSummary_MIDI() {
        assertThat(mController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[4]))
                .isEqualTo(R.string.usb_summary_MIDI);
    }

    @Test
    public void testPreferenceSummary_usbDisconnected() {
        final Preference preference = new Preference(mContext);
        preference.setKey("usb_mode");
        preference.setEnabled(true);
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo(
                mContext.getString(R.string.disconnected));
    }

    @Test
    public void testUsbBoradcastReceiver_usbConnected_shouldUpdateSummary() {
        final Preference preference = new Preference(mContext);
        preference.setKey("usb_mode");
        preference.setEnabled(true);
        when(mUsbBackend.getCurrentMode()).thenReturn(UsbModeChooserActivity.DEFAULT_MODES[0]);
        when(mScreen.findPreference("usb_mode")).thenReturn(preference);

        mController.displayPreference(mScreen);
        mController.onResume();
        final Intent intent = new Intent(UsbManager.ACTION_USB_STATE);
        intent.putExtra(UsbManager.USB_CONNECTED, true);
        mContext.sendStickyBroadcast(intent);

        assertThat(preference.getSummary()).isEqualTo(
                mContext.getString(R.string.usb_summary_charging_only));
    }

}