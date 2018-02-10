package com.android.settings.connecteddevice.usb;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UsbModePreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private UsbBackend mUsbBackend;
    @Mock
    private UsbConnectionBroadcastReceiver mUsbConnectionBroadcastReceiver;

    private Context mContext;
    private UsbModePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mController = new UsbModePreferenceController(mContext, mUsbBackend, null /* lifecycle */);
        mController.mUsbReceiver = mUsbConnectionBroadcastReceiver;
    }

    @Test
    public void testGetSummary_chargeDevice() {
        assertThat(mController.getSummary(0))
                .isEqualTo(R.string.usb_summary_charging_only);
    }

    @Test
    public void testGetSummary_supplyPower() {
        assertThat(mController.getSummary(UsbBackend.MODE_POWER_SOURCE))
                .isEqualTo(R.string.usb_summary_power_only);
    }

    @Test
    public void testGetSummary_TransferFiles() {
        assertThat(mController.getSummary(UsbBackend.MODE_DATA_MTP))
                .isEqualTo(R.string.usb_summary_file_transfers);
    }

    @Test
    public void testGetSummary_TransferPhoto() {
        assertThat(mController.getSummary(UsbBackend.MODE_DATA_PTP))
                .isEqualTo(R.string.usb_summary_photo_transfers);
    }

    @Test
    public void testGetSummary_MIDI() {
        assertThat(mController.getSummary(UsbBackend.MODE_DATA_MIDI))
                .isEqualTo(R.string.usb_summary_MIDI);
    }

    @Test
    public void testGetSummary_Tethering() {
        assertThat(mController.getSummary(UsbBackend.MODE_DATA_TETHER))
                .isEqualTo(R.string.usb_summary_tether);
    }

    @Test
    public void testPreferenceSummary_usbDisconnected() {
        final Preference preference = new Preference(mContext);
        preference.setKey("usb_mode");
        preference.setEnabled(true);
        when(mUsbBackend.getCurrentMode()).thenReturn(UsbBackend.MODE_POWER_SINK);
        when(mUsbConnectionBroadcastReceiver.isConnected()).thenReturn(false);
        mController.updateState(preference);

        assertThat(preference.getKey()).isEqualTo("usb_mode");
        assertThat(preference.getSummary()).isEqualTo(
                mContext.getString(R.string.disconnected));
    }

    @Test
    public void testUsbBroadcastReceiver_usbConnected_shouldUpdateSummary() {
        final Preference preference = new Preference(mContext);
        preference.setKey("usb_mode");
        preference.setEnabled(true);
        when(mUsbBackend.getCurrentMode()).thenReturn(UsbBackend.MODE_POWER_SINK);
        when(mUsbConnectionBroadcastReceiver.isConnected()).thenReturn(true);
        mController.updateState(preference);

        assertThat(preference.getSummary()).isEqualTo(
                mContext.getString(R.string.usb_summary_charging_only));
    }

}