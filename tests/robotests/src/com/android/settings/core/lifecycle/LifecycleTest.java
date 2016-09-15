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
package com.android.settings.core.lifecycle;

import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.core.lifecycle.events.OnAttach;
import com.android.settings.core.lifecycle.events.OnDestroy;
import com.android.settings.core.lifecycle.events.OnPause;
import com.android.settings.core.lifecycle.events.OnResume;
import com.android.settings.core.lifecycle.events.OnStart;
import com.android.settings.core.lifecycle.events.OnStop;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;
import org.robolectric.util.FragmentController;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LifecycleTest {

    public static class TestDialogFragment extends ObservableDialogFragment {

        final TestObserver mFragObserver;

        public TestDialogFragment() {
            mFragObserver = new TestObserver();
            mLifecycle.addObserver(mFragObserver);
        }
    }

    public static class TestActivity extends ObservableActivity {

        final TestObserver mActObserver;

        public TestActivity() {
            mActObserver = new TestObserver();
            getLifecycle().addObserver(mActObserver);
        }

    }

    public static class TestObserver implements LifecycleObserver, OnAttach, OnStart, OnResume,
            OnPause, OnStop, OnDestroy {

        boolean mOnAttachObserved;
        boolean mOnAttachHasContext;
        boolean mOnStartObserved;
        boolean mOnResumeObserved;
        boolean mOnPauseObserved;
        boolean mOnStopObserved;
        boolean mOnDestroyObserved;

        @Override
        public void onAttach(Context context) {
            mOnAttachObserved = true;
            mOnAttachHasContext = context != null;
        }

        @Override
        public void onStart() {
            mOnStartObserved = true;
        }

        @Override
        public void onPause() {
            mOnPauseObserved = true;
        }

        @Override
        public void onResume() {
            mOnResumeObserved = true;
        }

        @Override
        public void onStop() {
            mOnStopObserved = true;
        }

        @Override
        public void onDestroy() {
            mOnDestroyObserved = true;
        }
    }

    @Test
    public void runThroughActivityLifecycles_shouldObserveEverything() {
        ActivityController<TestActivity> ac = Robolectric.buildActivity(TestActivity.class);
        TestActivity activity = ac.get();

        ac.start();
        assertTrue(activity.mActObserver.mOnStartObserved);
        ac.resume();
        assertTrue(activity.mActObserver.mOnResumeObserved);
        ac.pause();
        assertTrue(activity.mActObserver.mOnPauseObserved);
        ac.stop();
        assertTrue(activity.mActObserver.mOnStopObserved);
        ac.destroy();
        assertTrue(activity.mActObserver.mOnDestroyObserved);
    }

    @Test
    public void runThroughFragmentLifecycles_shouldObserveEverything() {
        FragmentController<TestDialogFragment> fragmentController =
                Robolectric.buildFragment(TestDialogFragment.class);
        TestDialogFragment fragment = fragmentController.get();

        fragmentController.attach().create().start().resume().pause().stop().destroy();

        assertTrue(fragment.mFragObserver.mOnAttachObserved);
        assertTrue(fragment.mFragObserver.mOnAttachHasContext);
        assertTrue(fragment.mFragObserver.mOnStartObserved);
        assertTrue(fragment.mFragObserver.mOnResumeObserved);
        assertTrue(fragment.mFragObserver.mOnPauseObserved);
        assertTrue(fragment.mFragObserver.mOnStopObserved);
        assertTrue(fragment.mFragObserver.mOnDestroyObserved);
    }
}
