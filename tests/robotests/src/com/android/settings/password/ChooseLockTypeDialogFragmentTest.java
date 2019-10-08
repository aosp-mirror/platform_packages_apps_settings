/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.password.ChooseLockTypeDialogFragment.OnLockTypeSelectedListener;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class, ShadowLockPatternUtils.class})
public class ChooseLockTypeDialogFragmentTest {

    private Context mContext;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = new TestFragment();
        FragmentController.setupFragment(mFragment, FragmentActivity.class, 0 /* containerViewId */,
                null /* bundle */);
    }

    @Test
    public void testThatDialog_IsShown() {
        AlertDialog latestDialog = startLockFragment();
        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(latestDialog);

        assertThat(latestDialog).isNotNull();
        assertThat(latestDialog.isShowing()).isTrue();
        // verify that we are looking at the expected dialog.
        assertThat(shadowAlertDialog.getTitle()).isEqualTo(
                mContext.getString(R.string.setup_lock_settings_options_dialog_title));
    }

    @Test
    public void testThat_OnClickListener_IsCalled() {
        mFragment.mDelegate = mock(OnLockTypeSelectedListener.class);
        AlertDialog lockDialog = startLockFragment();
        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(lockDialog);

        shadowAlertDialog.clickOnItem(0);

        verify(mFragment.mDelegate, times(1)).onLockTypeSelected(any(ScreenLockType.class));
    }

    @Test
    public void testThat_OnClickListener_IsNotCalledWhenCancelled() {
        mFragment.mDelegate = mock(OnLockTypeSelectedListener.class);
        AlertDialog lockDialog = startLockFragment();

        lockDialog.dismiss();

        verify(mFragment.mDelegate, never()).onLockTypeSelected(any(ScreenLockType.class));
    }

    private AlertDialog startLockFragment() {
        ChooseLockTypeDialogFragment chooseLockTypeDialogFragment =
                ChooseLockTypeDialogFragment.newInstance(1234);
        chooseLockTypeDialogFragment.show(mFragment.getChildFragmentManager(), null);
        return ShadowAlertDialogCompat.getLatestAlertDialog();
    }

    public static class TestFragment extends Fragment implements OnLockTypeSelectedListener {

        private OnLockTypeSelectedListener mDelegate;

        @Override
        public void onLockTypeSelected(ScreenLockType lock) {
            if (mDelegate != null) {
                mDelegate.onLockTypeSelected(lock);
            }
        }
    }
}
