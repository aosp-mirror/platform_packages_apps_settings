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

import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class AudioSharingDialogFragmentTest {

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";

    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_DEVICE_NAME3 = "test3";
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM1 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME1, /* groupId= */ 1, /* isActive= */ false);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM2 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME2, /* groupId= */ 2, /* isActive= */ false);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM3 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME3, /* groupId= */ 3, /* isActive= */ false);

    private Fragment mParent;
    private AudioSharingDialogFragment mFragment;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    @Before
    public void setUp() {
        ShadowAlertDialogCompat.reset();
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mFragment = new AudioSharingDialogFragment();
        mParent = new Fragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOff_dialogNotExist() {
        mFragment.show(mParent, new ArrayList<>(), (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOn_noConnectedDevice() {
        mFragment.show(mParent, new ArrayList<>(), (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        TextView subtitle1 = rootView.findViewById(R.id.share_audio_subtitle1);
        ImageView guidance = rootView.findViewById(R.id.share_audio_guidance);
        Button shareBtn = rootView.findViewById(R.id.share_btn);
        assertThat(dialog.isShowing()).isTrue();
        assertThat(subtitle1.getVisibility()).isEqualTo(View.GONE);
        assertThat(guidance.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(shareBtn.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_noConnectedDevice_dialogDismiss() {
        mFragment.show(mParent, new ArrayList<>(), (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        dialog.findViewById(android.R.id.button2).performClick();
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOn_singleConnectedDevice() {
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        mFragment.show(mParent, list, (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        TextView subtitle1 = rootView.findViewById(R.id.share_audio_subtitle1);
        ImageView guidance = rootView.findViewById(R.id.share_audio_guidance);
        Button shareBtn = rootView.findViewById(R.id.share_btn);
        assertThat(dialog.isShowing()).isTrue();
        assertThat(subtitle1.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subtitle1.getText().toString()).isEqualTo(TEST_DEVICE_NAME1);
        assertThat(guidance.getVisibility()).isEqualTo(View.GONE);
        assertThat(shareBtn.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_singleConnectedDevice_dialogDismiss() {
        mFragment.show(mParent, new ArrayList<>(), (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        rootView.findViewById(R.id.cancel_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_singleConnectedDevice_shareClicked() {
        AtomicBoolean isShareBtnClicked = new AtomicBoolean(false);
        mFragment.show(
                mParent,
                new ArrayList<>(),
                (item) -> {
                    isShareBtnClicked.set(true);
                });
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        rootView.findViewById(R.id.share_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
        assertThat(isShareBtnClicked.get()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOn_multipleConnectedDevice() {
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        list.add(TEST_DEVICE_ITEM2);
        list.add(TEST_DEVICE_ITEM3);
        mFragment.show(mParent, list, (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        TextView subtitle1 = rootView.findViewById(R.id.share_audio_subtitle1);
        ImageView guidance = rootView.findViewById(R.id.share_audio_guidance);
        Button shareBtn = rootView.findViewById(R.id.share_btn);
        RecyclerView recyclerView = rootView.findViewById(R.id.btn_list);
        assertThat(dialog.isShowing()).isTrue();
        assertThat(subtitle1.getVisibility()).isEqualTo(View.GONE);
        assertThat(guidance.getVisibility()).isEqualTo(View.GONE);
        assertThat(shareBtn.getVisibility()).isEqualTo(View.GONE);
        assertThat(recyclerView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(recyclerView.getAdapter().getItemCount()).isEqualTo(3);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_multipleConnectedDevice_dialogDismiss() {
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        list.add(TEST_DEVICE_ITEM2);
        list.add(TEST_DEVICE_ITEM3);
        mFragment.show(mParent, list, (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        rootView.findViewById(R.id.cancel_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
    }
}
