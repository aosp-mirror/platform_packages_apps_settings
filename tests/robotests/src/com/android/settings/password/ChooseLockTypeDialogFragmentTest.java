/*
 * Copyright (C) 2017 Google Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.password.ChooseLockTypeDialogFragment.OnLockTypeSelectedListener;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowEventLogWriter.class,
                ShadowUserManager.class,
                ShadowUtils.class
        })
public class ChooseLockTypeDialogFragmentTest {
    private Context mContext;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = new TestFragment();
        FragmentTestUtil.startFragment(mFragment);
    }

    @Test
    public void testThatDialog_IsShown() {
        AlertDialog latestDialog = startLockFragment();
        assertNotNull(latestDialog);
        ShadowDialog shadowDialog = Shadows.shadowOf(latestDialog);
        // verify that we are looking at the expected dialog.
        assertEquals(shadowDialog.getTitle(),
                mContext.getString(R.string.setup_lock_settings_options_dialog_title));
    }

    @Test
    public void testThat_OnClickListener_IsCalled() {
        mFragment.mDelegate = mock(OnLockTypeSelectedListener.class);
        AlertDialog lockDialog = startLockFragment();
        ShadowAlertDialog shadowAlertDialog = Shadows.shadowOf(lockDialog);
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
        return ShadowAlertDialog.getLatestAlertDialog();
    }


    public static class TestFragment extends Fragment
            implements OnLockTypeSelectedListener{
        OnLockTypeSelectedListener mDelegate;
        @Override
        public void onLockTypeSelected(ScreenLockType lock) {
            if (mDelegate != null) {
                mDelegate.onLockTypeSelected(lock);
            }
        }
    }
}
