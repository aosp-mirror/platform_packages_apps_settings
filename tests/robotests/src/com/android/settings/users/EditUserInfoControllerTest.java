/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(RobolectricTestRunner.class)
public class EditUserInfoControllerTest {
    private static final int MAX_USER_NAME_LENGTH = 100;

    @Mock
    private Fragment mFragment;
    @Mock
    private Drawable mCurrentIcon;

    private boolean mCanChangePhoto;

    private FragmentActivity mActivity;
    private TestEditUserInfoController mController;

    public class TestEditUserInfoController extends EditUserInfoController {
        private EditUserPhotoController mPhotoController;

        private EditUserPhotoController getPhotoController() {
            return mPhotoController;
        }

        @Override
        protected EditUserPhotoController createEditUserPhotoController(Fragment fragment,
                ImageView userPhotoView, Drawable drawable) {
            mPhotoController = mock(EditUserPhotoController.class, Answers.RETURNS_DEEP_STUBS);
            return mPhotoController;
        }

        @Override
        boolean canChangePhoto(Context context, UserInfo user) {
            return mCanChangePhoto;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(ActivityController.of(new FragmentActivity()).get());
        when(mFragment.getActivity()).thenReturn(mActivity);
        mController = new TestEditUserInfoController();
        mCanChangePhoto = true;
    }

    @Test
    public void photoControllerOnActivityResult_whenWaiting_isCalled() {
        mController.createDialog(mFragment, mCurrentIcon, "test user",
                "title", null,
                android.os.Process.myUserHandle(), null);
        mController.startingActivityForResult();
        Intent resultData = new Intent();
        mController.onActivityResult(0, 0, resultData);
        EditUserPhotoController photoController = mController.getPhotoController();
        assertThat(photoController).isNotNull();
        verify(photoController).onActivityResult(eq(0), eq(0), same(resultData));
    }

    @Test
    @Config(shadows = ShadowAlertDialogCompat.class)
    public void userNameView_inputLongName_shouldBeConstrained() {
        // generate a string of 200 'A's
        final String longName = Stream.generate(
                () -> String.valueOf('A')).limit(200).collect(Collectors.joining());
        final AlertDialog dialog = (AlertDialog) mController.createDialog(mFragment, mCurrentIcon,
                "test user", "title", null,
                android.os.Process.myUserHandle(), null);
        final EditText userName = ShadowAlertDialogCompat.shadowOf(dialog).getView()
                .findViewById(R.id.user_name);

        userName.setText(longName);

        assertThat(userName.getText().length()).isEqualTo(MAX_USER_NAME_LENGTH);
    }

    @Test
    public void onDialogCompleteCallback_isCalled_whenCancelled() {
        EditUserInfoController.OnContentChangedCallback contentChangeCallback = mock(
                EditUserInfoController.OnContentChangedCallback.class);

        EditUserInfoController.OnDialogCompleteCallback dialogCompleteCallback = mock(
                EditUserInfoController.OnDialogCompleteCallback.class);

        AlertDialog dialog = (AlertDialog) mController.createDialog(
                mFragment, mCurrentIcon, "test",
                "title", contentChangeCallback,
                android.os.Process.myUserHandle(),
                dialogCompleteCallback);

        dialog.show();
        dialog.cancel();

        verify(contentChangeCallback, times(0))
                .onLabelChanged(any(), any());
        verify(contentChangeCallback, times(0))
                .onPhotoChanged(any(), any());
        verify(dialogCompleteCallback, times(0)).onPositive();
        verify(dialogCompleteCallback, times(1)).onNegativeOrCancel();
    }

    @Test
    public void onDialogCompleteCallback_isCalled_whenPositiveClicked() {
        EditUserInfoController.OnContentChangedCallback contentChangeCallback = mock(
                EditUserInfoController.OnContentChangedCallback.class);

        EditUserInfoController.OnDialogCompleteCallback dialogCompleteCallback = mock(
                EditUserInfoController.OnDialogCompleteCallback.class);

        AlertDialog dialog = (AlertDialog) mController.createDialog(
                mFragment, mCurrentIcon, "test",
                "title", contentChangeCallback,
                android.os.Process.myUserHandle(),
                dialogCompleteCallback);

        // No change to the photo.
        when(mController.getPhotoController().getNewUserPhotoDrawable()).thenReturn(mCurrentIcon);

        dialog.show();
        dialog.getButton(Dialog.BUTTON_POSITIVE).performClick();

        verify(contentChangeCallback, times(0))
                .onLabelChanged(any(), any());
        verify(contentChangeCallback, times(0))
                .onPhotoChanged(any(), any());
        verify(dialogCompleteCallback, times(1)).onPositive();
        verify(dialogCompleteCallback, times(0)).onNegativeOrCancel();
    }

    @Test
    public void onDialogCompleteCallback_isCalled_whenNegativeClicked() {
        EditUserInfoController.OnContentChangedCallback contentChangeCallback = mock(
                EditUserInfoController.OnContentChangedCallback.class);

        EditUserInfoController.OnDialogCompleteCallback dialogCompleteCallback = mock(
                EditUserInfoController.OnDialogCompleteCallback.class);

        AlertDialog dialog = (AlertDialog) mController.createDialog(
                mFragment, mCurrentIcon, "test",
                "title", contentChangeCallback,
                android.os.Process.myUserHandle(),
                dialogCompleteCallback);

        dialog.show();
        dialog.getButton(Dialog.BUTTON_NEGATIVE).performClick();

        verify(contentChangeCallback, times(0))
                .onLabelChanged(any(), any());
        verify(contentChangeCallback, times(0))
                .onPhotoChanged(any(), any());
        verify(dialogCompleteCallback, times(0)).onPositive();
        verify(dialogCompleteCallback, times(1)).onNegativeOrCancel();
    }

    @Test
    public void onContentChangedCallback_isCalled_whenLabelChanges() {
        EditUserInfoController.OnContentChangedCallback contentChangeCallback = mock(
                EditUserInfoController.OnContentChangedCallback.class);

        EditUserInfoController.OnDialogCompleteCallback dialogCompleteCallback = mock(
                EditUserInfoController.OnDialogCompleteCallback.class);

        AlertDialog dialog = (AlertDialog) mController.createDialog(
                mFragment, mCurrentIcon, "test",
                "title", contentChangeCallback,
                android.os.Process.myUserHandle(),
                dialogCompleteCallback);

        // No change to the photo.
        when(mController.getPhotoController().getNewUserPhotoDrawable()).thenReturn(mCurrentIcon);

        dialog.show();
        String expectedNewName = "new test user";
        EditText editText = (EditText) dialog.findViewById(R.id.user_name);
        editText.setText(expectedNewName);

        dialog.getButton(Dialog.BUTTON_POSITIVE).performClick();

        verify(contentChangeCallback, times(1))
                .onLabelChanged(any(), eq(expectedNewName));
        verify(contentChangeCallback, times(0))
                .onPhotoChanged(any(), any());
        verify(dialogCompleteCallback, times(1)).onPositive();
        verify(dialogCompleteCallback, times(0)).onNegativeOrCancel();
    }

    @Test
    public void onContentChangedCallback_isCalled_whenPhotoChanges() {
        EditUserInfoController.OnContentChangedCallback contentChangeCallback = mock(
                EditUserInfoController.OnContentChangedCallback.class);

        EditUserInfoController.OnDialogCompleteCallback dialogCompleteCallback = mock(
                EditUserInfoController.OnDialogCompleteCallback.class);

        AlertDialog dialog = (AlertDialog) mController.createDialog(
                mFragment, mCurrentIcon, "test",
                "title", contentChangeCallback,
                android.os.Process.myUserHandle(),
                dialogCompleteCallback);

        // A different drawable.
        Drawable newPhoto = mock(Drawable.class);
        when(mController.getPhotoController().getNewUserPhotoDrawable()).thenReturn(newPhoto);

        dialog.show();
        dialog.getButton(Dialog.BUTTON_POSITIVE).performClick();

        verify(contentChangeCallback, times(0))
                .onLabelChanged(any(), any());
        verify(contentChangeCallback, times(1))
                .onPhotoChanged(any(), eq(newPhoto));
        verify(dialogCompleteCallback, times(1)).onPositive();
        verify(dialogCompleteCallback, times(0)).onNegativeOrCancel();
    }

    @Test
    public void createDialog_canNotChangePhoto_nullPhotoController() {
        mCanChangePhoto = false;

        mController.createDialog(
                mFragment, mCurrentIcon, "test",
                "title", null,
                android.os.Process.myUserHandle(),
                null);

        assertThat(mController.mPhotoController).isNull();
    }
}
