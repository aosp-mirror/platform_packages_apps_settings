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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import static java.util.Collections.emptyList;
import static java.util.Collections.list;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothUtils.class,
        })
public class AudioStreamsQrCodeFragmentTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String VALID_METADATA =
            "BLUETOOTH:UUID:184F;BN:VGVzdA==;AT:1;AD:00A1A1A1A1A1;BI:1E240;BC:VGVzdENvZGU=;"
                    + "MD:BgNwVGVzdA==;AS:1;PI:A0;NS:1;BS:3;NB:2;SM:BQNUZXN0BARlbmc=;;";
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mBtEventManager;
    @Mock private LocalBluetoothProfileManager mBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    private Context mContext;
    private AudioStreamsQrCodeFragment mFragment;

    @Before
    public void setUp() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        LocalBluetoothManager btManager = Utils.getLocalBtManager(mContext);
        when(btManager.getEventManager()).thenReturn(mBtEventManager);
        when(btManager.getProfileManager()).thenReturn(mBtProfileManager);
        when(mBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mBroadcast.getAllBroadcastMetadata()).thenReturn(emptyList());
        mContext = ApplicationProvider.getApplicationContext();
        mFragment = new AudioStreamsQrCodeFragment();
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void getMetricsCategory_returnEnum() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(SettingsEnums.AUDIO_STREAM_QR_CODE);
    }

    @Test
    public void onCreateView_noProfile_noQrCode() {
        when(mBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(null);
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        View view = mFragment.getView();

        assertThat(view).isNotNull();
        ImageView qrCodeView = view.findViewById(R.id.qrcode_view);
        TextView passwordView = view.requireViewById(R.id.password);
        assertThat(qrCodeView).isNotNull();
        assertThat(qrCodeView.getDrawable()).isNull();
        assertThat(passwordView).isNotNull();
        assertThat(passwordView.getText().toString()).isEqualTo("");
    }

    @Test
    public void onCreateView_noMetadata_noQrCode() {
        List<BluetoothLeBroadcastMetadata> list = new ArrayList<>();
        when(mBroadcast.getAllBroadcastMetadata()).thenReturn(list);
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        View view = mFragment.getView();

        assertThat(view).isNotNull();
        ImageView qrCodeView = view.findViewById(R.id.qrcode_view);
        TextView passwordView = view.requireViewById(R.id.password);
        assertThat(qrCodeView).isNotNull();
        assertThat(qrCodeView.getDrawable()).isNull();
        assertThat(passwordView).isNotNull();
        assertThat(passwordView.getText().toString()).isEqualTo("");
    }

    @Test
    public void onCreateView_hasMetadata_hasQrCode() {
        var metadata =
                BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(VALID_METADATA);
        List<BluetoothLeBroadcastMetadata> list = new ArrayList<>();
        list.add(metadata);
        when(mBroadcast.getAllBroadcastMetadata()).thenReturn(list);
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        View view = mFragment.getView();

        assertThat(view).isNotNull();
        ImageView qrCodeView = view.findViewById(R.id.qrcode_view);
        TextView passwordView = view.requireViewById(R.id.password);
        assertThat(qrCodeView).isNotNull();
        assertThat(qrCodeView.getDrawable()).isNotNull();
        assertThat(passwordView).isNotNull();
        assertThat(passwordView.getText().toString())
                .isEqualTo(
                        mContext.getString(
                                R.string.audio_streams_qr_code_page_password,
                                new String(metadata.getBroadcastCode(), StandardCharsets.UTF_8)));
    }
}
