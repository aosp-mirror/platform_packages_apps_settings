/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputManager;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.core.lifecycle.LifecycleObserver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InputAndGestureSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.INPUT_SERVICE)).thenReturn(mock(InputManager.class));
        mFragment = new TestFragment(mContext);
    }

    @Test
    public void testGetPreferenceScreenResId() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(R.xml.input_and_gesture);
    }

    @Test
    public void testGetPreferenceControllers_shouldRegisterLifecycleObservers() {
        final List<PreferenceController> controllers = mFragment.getPreferenceControllers(mContext);
        int lifecycleObserverCount = 0;
        for (PreferenceController controller : controllers) {
            if (controller instanceof LifecycleObserver) {
                lifecycleObserverCount++;
            }
        }
        verify(mFragment.getLifecycle(), times(lifecycleObserverCount))
                .addObserver(any(LifecycleObserver.class));
    }

    public static class TestFragment extends InputAndGestureSettings {

        private Lifecycle mLifecycle;
        private Context mContext;

        public TestFragment(Context context) {
            mContext = context;
            mLifecycle = mock(Lifecycle.class);
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        protected Lifecycle getLifecycle() {
            if (mLifecycle == null) {
                return super.getLifecycle();
            }
            return mLifecycle;
        }
    }
}
