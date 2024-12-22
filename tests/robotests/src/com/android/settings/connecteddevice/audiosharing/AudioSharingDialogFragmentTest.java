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
import static org.mockito.Mockito.verify;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.flags.Flags;

import org.junit.After;
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
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_DEVICE_NAME3 = "test3";
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM1 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME1, /* groupId= */ 1, /* isActive= */ false);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM2 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME2, /* groupId= */ 2, /* isActive= */ false);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM3 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME3, /* groupId= */ 3, /* isActive= */ false);
    private static final AudioSharingDialogFragment.DialogEventListener EMPTY_EVENT_LISTENER =
            new AudioSharingDialogFragment.DialogEventListener() {
            };
    private static final Pair<Integer, Object> TEST_EVENT_DATA = Pair.create(1, 1);
    private static final Pair<Integer, Object>[] TEST_EVENT_DATA_LIST =
            new Pair[] {TEST_EVENT_DATA};

    private Fragment mParent;
    private FakeFeatureFactory mFeatureFactory;

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
        AudioSharingDialogFragment fragment = new AudioSharingDialogFragment();
        assertThat(fragment.getMetricsCategory())
                .isEqualTo(SettingsEnums.DIALOG_AUDIO_SHARING_ADD_DEVICE);
    }

    @Test
    public void onCreateDialog_flagOff_dialogNotExist() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingDialogFragment.show(
                mParent, new ArrayList<>(), EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_unattachedFragment_dialogNotExist() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingDialogFragment.show(
                new Fragment(), new ArrayList<>(), EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_flagOn_noExtraConnectedDevice() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingDialogFragment.show(
                mParent, new ArrayList<>(), EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        TextView description = dialog.findViewById(R.id.description_text);
        assertThat(description).isNotNull();
        ImageView image = dialog.findViewById(R.id.description_image);
        assertThat(image).isNotNull();
        Button positiveBtn = dialog.findViewById(R.id.positive_btn);
        assertThat(positiveBtn).isNotNull();
        Button negativeBtn = dialog.findViewById(R.id.negative_btn);
        assertThat(negativeBtn).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(description.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(description.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_dialog_connect_device_content));
        assertThat(image.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(positiveBtn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(positiveBtn.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_pair_button_label));
        assertThat(negativeBtn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(negativeBtn.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_qrcode_button_label));
    }

    @Test
    public void onCreateDialog_noExtraConnectedDevice_pairNewDevice() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AtomicBoolean isPairBtnClicked = new AtomicBoolean(false);
        AudioSharingDialogFragment.show(
                mParent,
                new ArrayList<>(),
                new AudioSharingDialogFragment.DialogEventListener() {
                    @Override
                    public void onPositiveClick() {
                        isPairBtnClicked.set(true);
                    }
                },
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        Button pairBtn = dialog.findViewById(R.id.positive_btn);
        assertThat(pairBtn).isNotNull();
        pairBtn.performClick();
        shadowMainLooper().idle();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
        assertThat(isPairBtnClicked.get()).isTrue();
        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    public void onCreateDialog_noExtraConnectedDevice_showQRCode() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AtomicBoolean isQrCodeBtnClicked = new AtomicBoolean(false);
        AudioSharingDialogFragment.show(
                mParent,
                new ArrayList<>(),
                new AudioSharingDialogFragment.DialogEventListener() {
                    @Override
                    public void onCancelClick() {
                        isQrCodeBtnClicked.set(true);
                    }
                },
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        Button qrCodeBtn = dialog.findViewById(R.id.negative_btn);
        assertThat(qrCodeBtn).isNotNull();
        qrCodeBtn.performClick();
        shadowMainLooper().idle();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
        assertThat(isQrCodeBtnClicked.get()).isTrue();
        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    public void onCreateDialog_flagOn_singleExtraConnectedDevice() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        AudioSharingDialogFragment.show(mParent, list, EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        TextView title = dialog.findViewById(R.id.title_text);
        assertThat(title).isNotNull();
        TextView description = dialog.findViewById(R.id.description_text);
        assertThat(description).isNotNull();
        ImageView image = dialog.findViewById(R.id.description_image);
        assertThat(image).isNotNull();
        Button positiveBtn = dialog.findViewById(R.id.positive_btn);
        assertThat(positiveBtn).isNotNull();
        Button negativeBtn = dialog.findViewById(R.id.negative_btn);
        assertThat(negativeBtn).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(title.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_share_with_dialog_title, TEST_DEVICE_NAME1));
        assertThat(description.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(description.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_dialog_share_content));
        assertThat(image.getVisibility()).isEqualTo(View.GONE);
        assertThat(positiveBtn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(positiveBtn.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_share_button_label));
        assertThat(negativeBtn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(negativeBtn.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_no_thanks_button_label));
    }

    @Test
    public void onCreateDialog_singleExtraConnectedDevice_dialogDismiss() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        AudioSharingDialogFragment.show(mParent, list, EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(R.id.negative_btn);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();

        assertThat(dialog.isShowing()).isFalse();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
    }

    @Test
    public void onCreateDialog_singleExtraConnectedDevice_shareClicked() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        AtomicBoolean isShareBtnClicked = new AtomicBoolean(false);
        AudioSharingDialogFragment.show(
                mParent,
                list,
                new AudioSharingDialogFragment.DialogEventListener() {
                    @Override
                    public void onItemClick(@NonNull AudioSharingDeviceItem item) {
                        isShareBtnClicked.set(true);
                    }
                },
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(R.id.positive_btn);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();

        assertThat(dialog.isShowing()).isFalse();
        assertThat(isShareBtnClicked.get()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
    }

    @Test
    public void onCreateDialog_flagOn_multipleExtraConnectedDevice() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        list.add(TEST_DEVICE_ITEM2);
        list.add(TEST_DEVICE_ITEM3);
        AudioSharingDialogFragment.show(mParent, list, EMPTY_EVENT_LISTENER, TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        TextView description = dialog.findViewById(R.id.description_text);
        assertThat(description).isNotNull();
        ImageView image = dialog.findViewById(R.id.description_image);
        assertThat(image).isNotNull();
        Button shareBtn = dialog.findViewById(R.id.positive_btn);
        assertThat(shareBtn).isNotNull();
        Button cancelBtn = dialog.findViewById(R.id.negative_btn);
        assertThat(cancelBtn).isNotNull();
        RecyclerView recyclerView = dialog.findViewById(R.id.device_btn_list);
        assertThat(recyclerView).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(description.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(description.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_dialog_share_more_content));
        assertThat(image.getVisibility()).isEqualTo(View.GONE);
        assertThat(shareBtn.getVisibility()).isEqualTo(View.GONE);
        assertThat(cancelBtn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(cancelBtn.getText().toString())
                .isEqualTo(mParent.getString(com.android.settings.R.string.cancel));
        assertThat(recyclerView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(recyclerView.getAdapter().getItemCount()).isEqualTo(3);
    }

    @Test
    public void onCreateDialog_multipleExtraConnectedDevice_dialogDismiss() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        list.add(TEST_DEVICE_ITEM2);
        list.add(TEST_DEVICE_ITEM3);
        AtomicBoolean isCancelBtnClicked = new AtomicBoolean(false);
        AudioSharingDialogFragment.show(
                mParent,
                list,
                new AudioSharingDialogFragment.DialogEventListener() {
                    @Override
                    public void onCancelClick() {
                        isCancelBtnClicked.set(true);
                    }
                },
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(R.id.negative_btn);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();

        assertThat(dialog.isShowing()).isFalse();
        assertThat(isCancelBtnClicked.get()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
    }
}
