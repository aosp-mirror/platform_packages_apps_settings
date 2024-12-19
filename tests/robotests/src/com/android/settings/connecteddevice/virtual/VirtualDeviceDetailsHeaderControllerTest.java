/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.connecteddevice.virtual;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.companion.AssociationInfo;
import android.companion.Flags;
import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class VirtualDeviceDetailsHeaderControllerTest {

    private static final CharSequence DEVICE_NAME = "Device Name";
    private static final int DEVICE_ID = 42;
    private static final String PERSISTENT_DEVICE_ID = "PersistentDeviceId";

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    VirtualDeviceManager mVirtualDeviceManager;
    @Mock
    AssociationInfo mAssociationInfo;
    @Mock
    PreferenceScreen mScreen;

    private VirtualDeviceWrapper mDevice;
    private VirtualDeviceDetailsHeaderController mController;
    private TextView mTitle;
    private ImageView mIcon;
    private TextView mSummary;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(VirtualDeviceManager.class))
                .thenReturn(mVirtualDeviceManager);
        mDevice = new VirtualDeviceWrapper(mAssociationInfo, PERSISTENT_DEVICE_ID, DEVICE_ID);
        LayoutPreference headerPreference = new LayoutPreference(mContext,
                LayoutInflater.from(mContext).inflate(R.layout.settings_entity_header, null));
        View view = headerPreference.findViewById(R.id.entity_header);
        mTitle = view.findViewById(R.id.entity_header_title);
        mIcon = headerPreference.findViewById(R.id.entity_header_icon);
        mSummary = view.findViewById(R.id.entity_header_summary);
        when(mScreen.findPreference(any())).thenReturn(headerPreference);

        mController = new VirtualDeviceDetailsHeaderController(mContext);
        mController.init(mDevice);
    }

    @Test
    public void title_matchesDeviceName() {
        when(mAssociationInfo.getDisplayName()).thenReturn(DEVICE_NAME);

        mController.displayPreference(mScreen);
        assertThat(mTitle.getText().toString()).isEqualTo(DEVICE_NAME.toString());
    }

    @Test
    @DisableFlags(Flags.FLAG_ASSOCIATION_DEVICE_ICON)
    public void icon_genericIcon() {
        mController.displayPreference(mScreen);
        assertThat(Shadows.shadowOf(mIcon.getDrawable()).getCreatedFromResId())
                .isEqualTo(R.drawable.ic_devices_other);
    }

    @Test
    @EnableFlags(Flags.FLAG_ASSOCIATION_DEVICE_ICON)
    public void icon_noAssociationIcon_genericIcon() {
        mController.displayPreference(mScreen);
        assertThat(Shadows.shadowOf(mIcon.getDrawable()).getCreatedFromResId())
                .isEqualTo(R.drawable.ic_devices_other);
    }

    @Test
    @EnableFlags(Flags.FLAG_ASSOCIATION_DEVICE_ICON)
    public void icon_fromAssociation() {
        Icon icon = Icon.createWithResource(mContext, R.drawable.ic_android);
        when(mAssociationInfo.getDeviceIcon()).thenReturn(icon);

        mController.displayPreference(mScreen);
        assertThat(Shadows.shadowOf(mIcon.getDrawable()).getCreatedFromResId())
                .isEqualTo(R.drawable.ic_android);
    }

    @Test
    public void summary_activeDevice_changeToInactive() {
        mDevice.setDeviceId(DEVICE_ID);
        mController.displayPreference(mScreen);
        assertThat(mSummary.getText().toString())
                .isEqualTo(mContext.getString(R.string.virtual_device_connected));

        mController.onVirtualDeviceClosed(DEVICE_ID);
        ShadowLooper.idleMainLooper();

        assertThat(mSummary.getText().toString())
                .isEqualTo(mContext.getString(R.string.virtual_device_disconnected));
    }

    @Test
    public void summary_inactiveDevice_changeToActive() {
        mDevice.setDeviceId(Context.DEVICE_ID_INVALID);
        mController.displayPreference(mScreen);
        assertThat(mSummary.getText().toString())
                .isEqualTo(mContext.getString(R.string.virtual_device_disconnected));

        VirtualDevice virtualDevice = mock(VirtualDevice.class);
        when(mDevice.getPersistentDeviceId()).thenReturn(PERSISTENT_DEVICE_ID);
        when(mVirtualDeviceManager.getVirtualDevice(DEVICE_ID)).thenReturn(virtualDevice);
        when(virtualDevice.getPersistentDeviceId()).thenReturn(PERSISTENT_DEVICE_ID);

        mController.onVirtualDeviceCreated(DEVICE_ID);
        ShadowLooper.idleMainLooper();

        assertThat(mSummary.getText().toString())
                .isEqualTo(mContext.getString(R.string.virtual_device_connected));
    }
}
