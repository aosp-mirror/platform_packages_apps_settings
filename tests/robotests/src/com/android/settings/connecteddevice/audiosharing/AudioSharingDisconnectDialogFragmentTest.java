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

import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

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

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_DEVICE_NAME3 = "test3";
    private static final int TEST_GROUP_ID1 = 1;
    private static final int TEST_GROUP_ID2 = 2;
    private static final int TEST_GROUP_ID3 = 3;
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM1 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME1, TEST_GROUP_ID1, /* isActive= */ true);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM2 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME2, TEST_GROUP_ID2, /* isActive= */ false);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM3 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME3, TEST_GROUP_ID3, /* isActive= */ false);

    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice2;
    @Mock private BluetoothDevice mDevice3;

    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private CachedBluetoothDevice mCachedDevice3;
    private Fragment mParent;
    private AudioSharingDisconnectDialogFragment mFragment;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private ArrayList<AudioSharingDeviceItem> mDeviceItems = new ArrayList<>();

    @Before
    public void setUp() {
        AlertDialog latestAlertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        if (latestAlertDialog != null) {
            latestAlertDialog.dismiss();
            ShadowAlertDialogCompat.reset();
        }
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_GROUP_ID1);
        when(mCachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME2);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCachedDevice2.getGroupId()).thenReturn(TEST_GROUP_ID2);
        when(mCachedDevice3.getName()).thenReturn(TEST_DEVICE_NAME3);
        when(mCachedDevice3.getDevice()).thenReturn(mDevice3);
        when(mCachedDevice3.getGroupId()).thenReturn(TEST_GROUP_ID3);
        mFragment = new AudioSharingDisconnectDialogFragment();
        mParent = new Fragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        mDeviceItems.add(TEST_DEVICE_ITEM1);
        mDeviceItems.add(TEST_DEVICE_ITEM2);
        mFragment.show(mParent, mDeviceItems, mCachedDevice3, (item) -> {});
        shadowMainLooper().idle();
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOff_dialogNotExist() {
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        list.add(TEST_DEVICE_ITEM2);
        mFragment.show(mParent, list, mCachedDevice3, (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOn_dialogShowBtnForTwoDevices() {
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        RecyclerView view = rootView.findViewById(R.id.device_btn_list);
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_dialogIsShowingForSameGroup_updateDialog() {
        String prefix = "Disconnect ";
        AtomicBoolean isItemBtnClicked = new AtomicBoolean(false);
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        RecyclerView view = rootView.findViewById(R.id.device_btn_list);
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);
        Button btn1 =
                view.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.device_button);
        assertThat(btn1.getText().toString()).isEqualTo(prefix + TEST_DEVICE_NAME1);
        Button btn2 =
                view.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.device_button);
        assertThat(btn2.getText().toString()).isEqualTo(prefix + TEST_DEVICE_NAME2);
        btn1.performClick();
        assertThat(isItemBtnClicked.get()).isFalse();

        // Update dialog content with same group
        mFragment.show(mParent, mDeviceItems, mCachedDevice3, (item) -> isItemBtnClicked.set(true));
        shadowMainLooper().idle();
        dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        btn1 = view.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.device_button);
        btn1.performClick();
        assertThat(isItemBtnClicked.get()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_dialogIsShowingForNewGroup_updateDialog() {
        String prefix = "Disconnect ";
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        RecyclerView view = rootView.findViewById(R.id.device_btn_list);
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);

        // Update dialog content with new group
        ArrayList<AudioSharingDeviceItem> newDeviceItems = new ArrayList<>();
        newDeviceItems.add(TEST_DEVICE_ITEM2);
        newDeviceItems.add(TEST_DEVICE_ITEM3);
        mFragment.show(mParent, newDeviceItems, mCachedDevice1, (item) -> {});
        shadowMainLooper().idle();
        dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        rootView = shadowDialog.getView();
        view = rootView.findViewById(R.id.device_btn_list);
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);
        Button btn1 =
                view.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.device_button);
        assertThat(btn1.getText().toString()).isEqualTo(prefix + TEST_DEVICE_NAME2);
        Button btn2 =
                view.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.device_button);
        assertThat(btn2.getText().toString()).isEqualTo(prefix + TEST_DEVICE_NAME3);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_clickCancel_dialogDismiss() {
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        rootView.findViewById(R.id.cancel_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
    }
}
