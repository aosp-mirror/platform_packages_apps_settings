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
package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothNameDialogFragmentTest {

    private TestBluetoothNameDialogFragment mBluetoothNameDialogFragment;
    private TextView mTextView;

    private static final String NAME_FOR_TEST = "test_device_name";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mBluetoothNameDialogFragment = new TestBluetoothNameDialogFragment();
        mTextView = new TextView(RuntimeEnvironment.application);
    }

    @Test
    public void onEditorAction_dialogNull_shouldNotCrash() {
        mBluetoothNameDialogFragment.mAlertDialog = null;

        // Should not crash
        assertThat(
                mBluetoothNameDialogFragment.onEditorAction(mTextView, EditorInfo.IME_ACTION_DONE,
                        null)).isTrue();
    }

    @Test
    public void onEditorAction_ImeNull_setsDeviceName() {


        mTextView.setText(NAME_FOR_TEST);
        assertThat(
                mBluetoothNameDialogFragment.onEditorAction(mTextView, EditorInfo.IME_NULL,
                        null)).isTrue();
        assertThat(mBluetoothNameDialogFragment.getDeviceName()).isEqualTo(NAME_FOR_TEST);
    }

    /**
     * Test fragment for {@link BluetoothNameDialogFragment} to test common methods
     */
    public static class TestBluetoothNameDialogFragment extends BluetoothNameDialogFragment {

        private String mName;

        @Override
        protected int getDialogTitle() {
            return 0;
        }

        @Override
        protected String getDeviceName() {
            return mName;
        }

        @Override
        protected void setDeviceName(String deviceName) {
            mName = deviceName;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }
}
