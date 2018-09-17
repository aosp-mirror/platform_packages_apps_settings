package com.android.settings.deviceinfo.deviceinfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.DialogInterface;

import com.android.settings.deviceinfo.aboutphone.DeviceNameWarningDialog;
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment;
import com.android.settings.testutils.FragmentController;
import com.android.settings.testutils.Robolectric;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SettingsRobolectricTestRunner.class)
public class DeviceNameWarningDialogTest {
    DeviceNameWarningDialog mDialog;

    @Test
    public void onClick_okSetsName() {
        final FragmentController<DeviceNameWarningDialog> fragmentController =
                Robolectric.buildFragment(DeviceNameWarningDialog.class);
        final DeviceNameWarningDialog fragment = spy(fragmentController.get());
        final MyDeviceInfoFragment deviceInfoFragment = mock(MyDeviceInfoFragment.class);
        fragment.setTargetFragment(deviceInfoFragment, 0);
        fragmentController.create().start().resume();
        fragment.onClick(null, DialogInterface.BUTTON_POSITIVE);

        verify(deviceInfoFragment).onSetDeviceNameConfirm(true);
    }

    @Test
    public void onClick_cancelDoesNothing() {
        final FragmentController<DeviceNameWarningDialog> fragmentController =
                Robolectric.buildFragment(DeviceNameWarningDialog.class);
        final DeviceNameWarningDialog fragment = spy(fragmentController.get());
        final MyDeviceInfoFragment deviceInfoFragment = mock(MyDeviceInfoFragment.class);
        fragment.setTargetFragment(deviceInfoFragment, 0);
        fragmentController.create().start().resume();
        fragment.onClick(null, DialogInterface.BUTTON_NEGATIVE);

        verify(deviceInfoFragment).onSetDeviceNameConfirm(false);
    }
}
