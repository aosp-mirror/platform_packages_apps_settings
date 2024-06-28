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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;

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

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialogCompat.class,
            ShadowBluetoothAdapter.class,
        })
public class AudioSharingStopDialogFragmentTest {

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_DEVICE_NAME3 = "test3";
    private static final int TEST_DEVICE_GROUP_ID1 = 1;
    private static final int TEST_DEVICE_GROUP_ID2 = 2;
    private static final int TEST_DEVICE_GROUP_ID3 = 3;
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM2 =
            new AudioSharingDeviceItem(
                    TEST_DEVICE_NAME2, TEST_DEVICE_GROUP_ID2, /* isActive= */ false);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM3 =
            new AudioSharingDeviceItem(
                    TEST_DEVICE_NAME3, TEST_DEVICE_GROUP_ID3, /* isActive= */ false);
    private static final AudioSharingStopDialogFragment.DialogEventListener EMPTY_EVENT_LISTENER =
            () -> {};
    private static final Pair<Integer, Object> TEST_EVENT_DATA = Pair.create(1, 1);
    private static final Pair<Integer, Object>[] TEST_EVENT_DATA_LIST =
            new Pair[] {TEST_EVENT_DATA};

    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice2;
    private FakeFeatureFactory mFeatureFactory;
    private Fragment mParent;
    private AudioSharingStopDialogFragment mFragment;

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
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME2);
        when(mCachedDevice2.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        mFragment = new AudioSharingStopDialogFragment();
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
                .isEqualTo(SettingsEnums.DIALOG_STOP_AUDIO_SHARING);
    }

    @Test
    public void onCreateDialog_flagOff_dialogNotExist() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(),
                mCachedDevice1,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_unattachedFragment_dialogNotExist() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingStopDialogFragment.show(
                new Fragment(),
                ImmutableList.of(TEST_DEVICE_ITEM2),
                mCachedDevice1,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_oneDeviceInSharing_showDialogWithCorrectMessage() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(TEST_DEVICE_ITEM2),
                mCachedDevice1,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView view = dialog.findViewById(R.id.description_text);
        assertThat(view).isNotNull();
        assertThat(view.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_stop_dialog_content, TEST_DEVICE_NAME2));
    }

    @Test
    public void onCreateDialog_twoDeviceInSharing_showDialogWithCorrectMessage() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(TEST_DEVICE_ITEM2, TEST_DEVICE_ITEM3),
                mCachedDevice1,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView view = dialog.findViewById(R.id.description_text);
        assertThat(view).isNotNull();
        assertThat(view.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_stop_dialog_with_two_content,
                                TEST_DEVICE_NAME2,
                                TEST_DEVICE_NAME3));
    }

    @Test
    public void onCreateDialog_dialogIsShowingForSameDevice_updateDialog() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(),
                mCachedDevice1,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView view = dialog.findViewById(R.id.description_text);
        assertThat(view).isNotNull();
        assertThat(view.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_stop_dialog_with_more_content));

        // Update the content
        AtomicBoolean isStopBtnClicked = new AtomicBoolean(false);
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(),
                mCachedDevice1,
                () -> isStopBtnClicked.set(true),
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider, times(0))
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS),
                        eq(SettingsEnums.DIALOG_STOP_AUDIO_SHARING));

        View btnView = dialog.findViewById(android.R.id.button1);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isFalse();
        assertThat(isStopBtnClicked.get()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
    }

    @Test
    public void onCreateDialog_dialogIsShowingForNewDevice_showNewDialog() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(),
                mCachedDevice1,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView view = dialog.findViewById(R.id.description_text);
        assertThat(view).isNotNull();
        assertThat(view.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_stop_dialog_with_more_content));
        TextView title = dialog.findViewById(R.id.title_text);
        assertThat(title).isNotNull();
        assertThat(title.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_stop_dialog_title, TEST_DEVICE_NAME1));

        // Show new dialog
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(),
                mCachedDevice2,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS),
                        eq(SettingsEnums.DIALOG_STOP_AUDIO_SHARING));

        view = dialog.findViewById(R.id.description_text);
        assertThat(view).isNotNull();
        assertThat(view.getText().toString())
                .isEqualTo(mParent.getString(R.string.audio_sharing_stop_dialog_with_more_content));
        title = dialog.findViewById(R.id.title_text);
        assertThat(title).isNotNull();
        assertThat(title.getText().toString())
                .isEqualTo(
                        mParent.getString(
                                R.string.audio_sharing_stop_dialog_title, TEST_DEVICE_NAME2));
    }

    @Test
    public void onCreateDialog_clickCancel_dialogDismiss() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(),
                mCachedDevice1,
                EMPTY_EVENT_LISTENER,
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(android.R.id.button2);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isFalse();
        verify(mFeatureFactory.metricsFeatureProvider, times(0))
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS),
                        eq(SettingsEnums.DIALOG_STOP_AUDIO_SHARING));
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
    }

    @Test
    public void onCreateDialog_clickShare_callbackTriggered() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AtomicBoolean isStopBtnClicked = new AtomicBoolean(false);
        AudioSharingStopDialogFragment.show(
                mParent,
                ImmutableList.of(),
                mCachedDevice1,
                () -> isStopBtnClicked.set(true),
                TEST_EVENT_DATA_LIST);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(android.R.id.button1);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isFalse();
        assertThat(isStopBtnClicked.get()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider, times(0))
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS),
                        eq(SettingsEnums.DIALOG_STOP_AUDIO_SHARING));
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED),
                        eq(TEST_EVENT_DATA));
    }
}
