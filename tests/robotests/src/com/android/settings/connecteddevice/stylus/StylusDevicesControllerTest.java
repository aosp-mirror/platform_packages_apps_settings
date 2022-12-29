/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.connecteddevice.stylus;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.role.RoleManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class StylusDevicesControllerTest {
    private static final String NOTES_PACKAGE_NAME = "notes.package";
    private static final CharSequence NOTES_APP_LABEL = "App Label";

    private Context mContext;
    private StylusDevicesController mController;
    private PreferenceCategory mPreferenceContainer;
    private PreferenceScreen mScreen;
    private InputDevice mInputDevice;

    @Mock
    private InputMethodManager mImm;
    @Mock
    private PackageManager mPm;
    @Mock
    private RoleManager mRm;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreferenceContainer = new PreferenceCategory(mContext);
        mPreferenceContainer.setKey(StylusDevicesController.KEY_STYLUS);
        mScreen.addPreference(mPreferenceContainer);

        when(mContext.getSystemService(InputMethodManager.class)).thenReturn(mImm);
        when(mContext.getSystemService(RoleManager.class)).thenReturn(mRm);
        doNothing().when(mContext).startActivity(any());

        // TODO(b/254834764): notes role placeholder
        when(mRm.getRoleHoldersAsUser(eq(RoleManager.ROLE_ASSISTANT), any(UserHandle.class)))
                .thenReturn(Collections.singletonList(NOTES_PACKAGE_NAME));
        when(mContext.getPackageManager()).thenReturn(mPm);
        when(mPm.getApplicationInfo(eq(NOTES_PACKAGE_NAME),
                any(PackageManager.ApplicationInfoFlags.class))).thenReturn(new ApplicationInfo());
        when(mPm.getApplicationLabel(any(ApplicationInfo.class))).thenReturn(NOTES_APP_LABEL);

        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);

        mInputDevice = spy(new InputDevice.Builder()
                .setId(1)
                .setSources(InputDevice.SOURCE_STYLUS)
                .build());
        when(mInputDevice.getBluetoothAddress()).thenReturn("SOME:ADDRESS");

        mController = new StylusDevicesController(mContext, mInputDevice, null, mLifecycle);
    }

    @Test
    public void isDeviceStylus_noDevices_false() {
        assertThat(StylusDevicesController.isDeviceStylus(null, null)).isFalse();
    }

    @Test
    public void isDeviceStylus_nonStylusInputDevice_false() {
        InputDevice inputDevice = new InputDevice.Builder()
                .setSources(InputDevice.SOURCE_DPAD)
                .build();

        assertThat(StylusDevicesController.isDeviceStylus(inputDevice, null)).isFalse();
    }

    @Test
    public void isDeviceStylus_stylusInputDevice_true() {
        InputDevice inputDevice = new InputDevice.Builder()
                .setSources(InputDevice.SOURCE_STYLUS)
                .build();

        assertThat(StylusDevicesController.isDeviceStylus(inputDevice, null)).isTrue();
    }

    @Test
    public void isDeviceStylus_nonStylusBluetoothDevice_false() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_WATCH.getBytes());

        assertThat(StylusDevicesController.isDeviceStylus(null, mCachedBluetoothDevice)).isFalse();
    }

    @Test
    public void isDeviceStylus_stylusBluetoothDevice_true() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_STYLUS.getBytes());
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);

        assertThat(StylusDevicesController.isDeviceStylus(null, mCachedBluetoothDevice)).isTrue();
    }

    @Test
    public void noInputDevice_noBluetoothDevice_noPreference() {
        StylusDevicesController controller = new StylusDevicesController(
                mContext, null, null, mLifecycle
        );

        showScreen(controller);

        assertThat(mPreferenceContainer.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void noInputDevice_nonStylusBluetoothDevice_noPreference() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_WATCH.getBytes());
        StylusDevicesController controller = new StylusDevicesController(
                mContext, null, mCachedBluetoothDevice, mLifecycle
        );

        showScreen(controller);

        assertThat(mPreferenceContainer.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void noInputDevice_stylusBluetoothDevice_showsPreference() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_STYLUS.getBytes());
        StylusDevicesController controller = new StylusDevicesController(
                mContext, null, mCachedBluetoothDevice, mLifecycle
        );

        showScreen(controller);

        assertThat(mPreferenceContainer.getPreferenceCount()).isEqualTo(3);
    }

    @Test
    public void btStylusInputDevice_showsAllPreferences() {
        showScreen(mController);
        Preference defaultNotesPref = mPreferenceContainer.getPreference(0);
        Preference handwritingPref = mPreferenceContainer.getPreference(1);
        Preference buttonPref = mPreferenceContainer.getPreference(2);

        assertThat(mPreferenceContainer.getPreferenceCount()).isEqualTo(3);
        assertThat(defaultNotesPref.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.stylus_default_notes_app));
        assertThat(handwritingPref.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.stylus_textfield_handwriting));
        assertThat(buttonPref.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.stylus_ignore_button));
    }

    @Test
    @Ignore // TODO(b/255732419): unignore when InputMethodInfo available
    public void btStylusInputDevice_noHandwritingIme_showsSomePreferences() {
        showScreen(mController);
        Preference defaultNotesPref = mPreferenceContainer.getPreference(0);
        Preference buttonPref = mPreferenceContainer.getPreference(1);

        assertThat(mPreferenceContainer.getPreferenceCount()).isEqualTo(2);
        assertThat(defaultNotesPref.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.stylus_default_notes_app));
        assertThat(buttonPref.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.stylus_ignore_button));
    }

    @Test
    public void defaultNotesPreference_showsNotesRoleApp() {
        showScreen(mController);
        Preference defaultNotesPref = mPreferenceContainer.getPreference(0);

        assertThat(defaultNotesPref.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.stylus_default_notes_app));
        assertThat(defaultNotesPref.getSummary().toString()).isEqualTo(NOTES_APP_LABEL.toString());
    }

    @Test
    public void defaultNotesPreference_noRoleHolder_hidesNotesRoleApp() {
        // TODO(b/254834764): replace with notes role once merged
        when(mRm.getRoleHoldersAsUser(eq(RoleManager.ROLE_ASSISTANT), any(UserHandle.class)))
                .thenReturn(Collections.emptyList());
        showScreen(mController);

        for (int i = 0; i < mPreferenceContainer.getPreferenceCount(); i++) {
            Preference pref = mPreferenceContainer.getPreference(i);
            assertThat(pref.getTitle().toString()).isNotEqualTo(
                    mContext.getString(R.string.stylus_default_notes_app));
        }
    }

    @Test
    public void defaultNotesPreferenceClick_sendsManageDefaultRoleIntent() {
        final String permissionPackageName = "permissions.package";
        when(mPm.getPermissionControllerPackageName()).thenReturn(permissionPackageName);
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        showScreen(mController);
        Preference defaultNotesPref = mPreferenceContainer.getPreference(0);
        mController.onPreferenceClick(defaultNotesPref);
        verify(mContext).startActivity(captor.capture());

        Intent intent = captor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MANAGE_DEFAULT_APP);
        assertThat(intent.getPackage()).isEqualTo(permissionPackageName);
        // TODO(b/254834764): when notes role is merged
        assertThat(intent.getStringExtra(Intent.EXTRA_ROLE_NAME)).isEqualTo(
                RoleManager.ROLE_ASSISTANT);
    }

    @Test
    public void handwritingPreference_checkedWhenFlagTrue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.STYLUS_HANDWRITING_ENABLED, 1);

        showScreen(mController);
        SwitchPreference handwritingPref = (SwitchPreference) mPreferenceContainer.getPreference(1);

        assertThat(handwritingPref.isChecked()).isEqualTo(true);
    }

    @Test
    public void handwritingPreference_uncheckedWhenFlagFalse() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.STYLUS_HANDWRITING_ENABLED, 0);

        showScreen(mController);
        SwitchPreference handwritingPref = (SwitchPreference) mPreferenceContainer.getPreference(1);

        assertThat(handwritingPref.isChecked()).isEqualTo(false);
    }

    @Test
    public void handwritingPreference_updatesFlagOnClick() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.STYLUS_HANDWRITING_ENABLED, 0);
        showScreen(mController);
        SwitchPreference handwritingPref = (SwitchPreference) mPreferenceContainer.getPreference(1);

        handwritingPref.performClick();

        assertThat(handwritingPref.isChecked()).isEqualTo(true);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.STYLUS_HANDWRITING_ENABLED, -1)).isEqualTo(1);
    }

    @Test
    public void buttonsPreference_checkedWhenFlagTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_BUTTONS_DISABLED, 1);

        showScreen(mController);
        SwitchPreference buttonsPref = (SwitchPreference) mPreferenceContainer.getPreference(2);

        assertThat(buttonsPref.isChecked()).isEqualTo(true);
    }

    @Test
    public void buttonsPreference_uncheckedWhenFlagFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_BUTTONS_DISABLED, 0);

        showScreen(mController);
        SwitchPreference buttonsPref = (SwitchPreference) mPreferenceContainer.getPreference(2);

        assertThat(buttonsPref.isChecked()).isEqualTo(false);
    }

    @Test
    public void buttonsPreference_updatesFlagOnClick() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_BUTTONS_DISABLED, 1);
        showScreen(mController);
        SwitchPreference buttonsPref = (SwitchPreference) mPreferenceContainer.getPreference(2);

        buttonsPref.performClick();

        assertThat(buttonsPref.isChecked()).isEqualTo(false);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Secure.STYLUS_BUTTONS_DISABLED, -1)).isEqualTo(0);
    }

    private void showScreen(StylusDevicesController controller) {
        controller.displayPreference(mScreen);
    }
}
