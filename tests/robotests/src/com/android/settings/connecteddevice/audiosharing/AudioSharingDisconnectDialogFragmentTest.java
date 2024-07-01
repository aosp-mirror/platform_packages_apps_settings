/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.flags.Flags;

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
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialogCompat.class,
            ShadowBluetoothAdapter.class,
        })
public class AudioSharingDisconnectDialogFragmentTest {

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_DEVICE_NAME3 = "test3";
    private static final int TEST_GROUP_ID1 = 1;
    private static final int TEST_GROUP_ID2 = 2;
    private static final int TEST_GROUP_ID3 = 3;
    private static final String TEST_ADDRESS1 = "XX:11";
    private static final String TEST_ADDRESS3 = "XX:33";
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM1 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME1, TEST_GROUP_ID1, /* isActive= */ true);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM2 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME2, TEST_GROUP_ID2, /* isActive= */ false);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM3 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME3, TEST_GROUP_ID3, /* isActive= */ false);
    private static final AudioSharingDisconnectDialogFragment.DialogEventListener
            EMPTY_EVENT_LISTENER = (AudioSharingDeviceItem item) -> {};
    private static final Pair<Integer, Object> TEST_EVENT_DATA = Pair.create(1, 1);
    private static final Pair<Integer, Object>[] TEST_EVENT_DATA_LIST =
            new Pair[] {TEST_EVENT_DATA};

    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice3;
    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice3;
    private FakeFeatureFactory mFeatureFactory;
    private Fragment mParent;
    private AudioSharingDisconnectDialogFragment mFragment;
    private ArrayList<AudioSharingDeviceItem> mDeviceItems = new ArrayList<>();

    @Before
    public void setUp() {
        ShadowAlertDialogCompat.reset();
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mDevice1.getAnonymizedAddress()).thenReturn(TEST_ADDRESS1);
        when(mDevice3.getAnonymizedAddress()).thenReturn(TEST_ADDRESS3);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_GROUP_ID1);
        when(mCachedDevice3.getName()).thenReturn(TEST_DEVICE_NAME3);
        when(mCachedDevice3.getDevice()).thenReturn(mDevice3);
        when(mCachedDevice3.getGroupId()).thenReturn(TEST_GROUP_ID3);
        mFragment = new AudioSharingDisconnectDialogFragment();
        mParent = new Fragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
    }

    @After
    public void tearDown() {
        ShadowAlertDialogCompat.reset();
    }

    @Test
    public void getMetricsCategory_correctValue() {
        assertThat(mFragment.getMetricsCategory())
                .isEqualTo(SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE);
    }

    @Test
    public void onCreateDialog_flagOff_dialogNotExist() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mDeviceItems = new ArrayList<>();
        mDeviceItems.add(TEST_DEVICE_ITEM1);
        mDeviceItems.add(TEST_DEVICE_ITEM2);
        AudioSharingDisconnectDialogFragment.show(
                mParent, mDeviceItems, mCachedDevice3, EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_unattachedFragment_dialogNotExist() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mDeviceItems = new ArrayList<>();
        mDeviceItems.add(TEST_DEVICE_ITEM1);
        mDeviceItems.add(TEST_DEVICE_ITEM2);
        AudioSharingDisconnectDialogFragment.show(
                new Fragment(),
                mDeviceItems,
                mCachedDevice3,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_flagOn_dialogShowBtnForTwoDevices() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mDeviceItems = new ArrayList<>();
        mDeviceItems.add(TEST_DEVICE_ITEM1);
        mDeviceItems.add(TEST_DEVICE_ITEM2);
        AudioSharingDisconnectDialogFragment.show(
                mParent, mDeviceItems, mCachedDevice3, EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        RecyclerView view = dialog.findViewById(R.id.device_btn_list);
        assertThat(view).isNotNull();
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);
    }

    @Test
    public void onCreateDialog_dialogIsShowingForSameGroup_updateDialog() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mDeviceItems = new ArrayList<>();
        mDeviceItems.add(TEST_DEVICE_ITEM1);
        mDeviceItems.add(TEST_DEVICE_ITEM2);
        AudioSharingDisconnectDialogFragment.show(
                mParent, mDeviceItems, mCachedDevice3, EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        RecyclerView view = dialog.findViewById(R.id.device_btn_list);
        assertThat(view).isNotNull();
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);
        Button btn1 =
                view.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.device_button);
        assertThat(btn1.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_disconnect_device_button_label,
                                TEST_DEVICE_NAME1));
        Button btn2 =
                view.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.device_button);
        assertThat(btn2.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_disconnect_device_button_label,
                                TEST_DEVICE_NAME2));

        // Update dialog content for device with same group
        AtomicBoolean isItemBtnClicked = new AtomicBoolean(false);
        AudioSharingDisconnectDialogFragment.show(
                mParent,
                mDeviceItems,
                mCachedDevice3,
                (AudioSharingDeviceItem item) -> isItemBtnClicked.set(true),
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider, times(0))
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS),
                        eq(SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE));

        btn1 = view.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.device_button);
        btn1.performClick();
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isFalse();
        assertThat(isItemBtnClicked.get()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
    }

    @Test
    public void onCreateDialog_dialogIsShowingForNewGroup_showNewDialog() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mDeviceItems = new ArrayList<>();
        mDeviceItems.add(TEST_DEVICE_ITEM1);
        mDeviceItems.add(TEST_DEVICE_ITEM2);
        AudioSharingDisconnectDialogFragment.show(
                mParent, mDeviceItems, mCachedDevice3, EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        RecyclerView view = dialog.findViewById(R.id.device_btn_list);
        assertThat(view).isNotNull();
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);

        // Show new dialog for device with new group
        ArrayList<AudioSharingDeviceItem> newDeviceItems = new ArrayList<>();
        newDeviceItems.add(TEST_DEVICE_ITEM2);
        newDeviceItems.add(TEST_DEVICE_ITEM3);
        AudioSharingDisconnectDialogFragment.show(
                mParent,
                newDeviceItems,
                mCachedDevice1,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS),
                        eq(SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE));

        view = dialog.findViewById(R.id.device_btn_list);
        assertThat(view).isNotNull();
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);
        Button btn1 =
                view.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.device_button);
        assertThat(btn1.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_disconnect_device_button_label,
                                TEST_DEVICE_NAME2));
        Button btn2 =
                view.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.device_button);
        assertThat(btn2.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_disconnect_device_button_label,
                                TEST_DEVICE_NAME3));
    }

    @Test
    public void onCreateDialog_clickCancel_dialogDismiss() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mDeviceItems = new ArrayList<>();
        mDeviceItems.add(TEST_DEVICE_ITEM1);
        mDeviceItems.add(TEST_DEVICE_ITEM2);
        AudioSharingDisconnectDialogFragment.show(
                mParent, mDeviceItems, mCachedDevice3, EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        View btnView = dialog.findViewById(R.id.negative_btn);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();

        assertThat(dialog.isShowing()).isFalse();
        verify(mFeatureFactory.metricsFeatureProvider, times(0))
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS),
                        eq(SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE));
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
    }
}
