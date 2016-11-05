package com.android.settings.connecteddevice;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
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
        assertThat(UsbModePreferenceController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[0]))
                .isEqualTo(R.string.usb_use_charging_only_desc);
    }

    @Test
    public void testGetSummary_supplyPower() {
        assertThat(UsbModePreferenceController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[1]))
                .isEqualTo(R.string.usb_use_power_only_desc);
    }

    @Test
    public void testGetSummary_TransferFiles() {
        assertThat(UsbModePreferenceController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[2]))
                .isEqualTo(R.string.usb_use_file_transfers_desc);
    }

    @Test
    public void testGetSummary_TransferPhoto() {
        assertThat(UsbModePreferenceController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[3]))
                .isEqualTo(R.string.usb_use_photo_transfers_desc);
    }

    @Test
    public void testGetSummary_MIDI() {
        assertThat(UsbModePreferenceController.getSummary(UsbModeChooserActivity.DEFAULT_MODES[4]))
                .isEqualTo(R.string.usb_use_MIDI_desc);
    }

    @Test
    public void testGetTitle_chargeDevice() {
        assertThat(UsbModePreferenceController.getTitle(UsbModeChooserActivity.DEFAULT_MODES[0]))
                .isEqualTo(R.string.usb_use_charging_only);
    }

    @Test
    public void testGetTitle_supplyPower() {
        assertThat(UsbModePreferenceController.getTitle(UsbModeChooserActivity.DEFAULT_MODES[1]))
                .isEqualTo(R.string.usb_use_power_only);
    }

    @Test
    public void testGetTitle_TransferFiles() {
        assertThat(UsbModePreferenceController.getTitle(UsbModeChooserActivity.DEFAULT_MODES[2]))
                .isEqualTo(R.string.usb_use_file_transfers);
    }

    @Test
    public void testGetTitle_TransferPhoto() {
        assertThat(UsbModePreferenceController.getTitle(UsbModeChooserActivity.DEFAULT_MODES[3]))
                .isEqualTo(R.string.usb_use_photo_transfers);
    }

    @Test
    public void testGetTitle_MIDI() {
        assertThat(UsbModePreferenceController.getTitle(UsbModeChooserActivity.DEFAULT_MODES[4]))
                .isEqualTo(R.string.usb_use_MIDI);
    }

    @Test
    public void testPreferenceSummary_usbDisconnected() {
        final Preference preference = new Preference(mContext);
        preference.setKey("usb_mode");
        preference.setEnabled(true);
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo(
                mContext.getString(R.string.usb_nothing_connected));
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
                mContext.getString(R.string.usb_use_charging_only));
    }

}