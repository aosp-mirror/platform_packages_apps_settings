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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class EditUserInfoControllerTest {
    @Mock
    private Fragment mFragment;
    @Mock
    private LayoutInflater mInflater;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private View mDialogContent;
    @Mock
    private EditText mUserName;
    @Mock
    private ImageView mPhotoView;
    @Mock
    private Drawable mCurrentIcon;

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
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(ActivityController.of(new FragmentActivity()).get());
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getLayoutInflater()).thenReturn(mInflater);
        when(mInflater.inflate(eq(R.layout.edit_user_info_dialog_content), any())).thenReturn(
                mDialogContent);
        when(mDialogContent.findViewById(eq(R.id.user_name))).thenReturn(mUserName);
        when(mDialogContent.findViewById(eq(R.id.user_photo))).thenReturn(mPhotoView);
        when(mPhotoView.getContext()).thenReturn((Context) mActivity);
        mController = new TestEditUserInfoController();
    }

    @Test
    public void photoControllerOnActivityResult_whenWaiting_isCalled() {
        mController.createDialog(mFragment, mCurrentIcon, "test user",
                R.string.profile_info_settings_title, null, android.os.Process.myUserHandle());
        mController.startingActivityForResult();
        Intent resultData = new Intent();
        mController.onActivityResult(0, 0, resultData);
        EditUserPhotoController photoController = mController.getPhotoController();
        assertThat(photoController).isNotNull();
        verify(photoController).onActivityResult(eq(0), eq(0), same(resultData));
    }
}
