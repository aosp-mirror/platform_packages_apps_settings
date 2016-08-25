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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.android.settings.TestConfig;
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

        final TestDialogFragment mFragment;
        final TestObserver mActObserver;

        public TestActivity() {
            mFragment = new TestDialogFragment();
            mActObserver = new TestObserver();
            getLifecycle().addObserver(mActObserver);
        }

        @Override
        public void onCreate(Bundle b) {
            super.onCreate(b);
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(mFragment, "tag");
            fragmentTransaction.commit();
        }
    }

    public static class TestObserver implements LifecycleObserver, OnStart, OnResume,
            OnPause, OnStop, OnDestroy {

        boolean mOnStartObserved;
        boolean mOnResumeObserved;
        boolean mOnPauseObserved;
        boolean mOnStopObserved;
        boolean mOnDestroyObserved;

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
    public void runThroughLifecycles_shouldObserveEverything() {
        ActivityController<TestActivity> ac = Robolectric.buildActivity(TestActivity.class);
        TestActivity activity = ac.get();

        ac.create().start();
        assertTrue(activity.mFragment.mFragObserver.mOnStartObserved);
        assertTrue(activity.mActObserver.mOnStartObserved);
        ac.resume();
        assertTrue(activity.mFragment.mFragObserver.mOnResumeObserved);
        assertTrue(activity.mActObserver.mOnResumeObserved);
        ac.pause();
        assertTrue(activity.mFragment.mFragObserver.mOnPauseObserved);
        assertTrue(activity.mActObserver.mOnPauseObserved);
        ac.stop();
        assertTrue(activity.mFragment.mFragObserver.mOnStopObserved);
        assertTrue(activity.mActObserver.mOnStopObserved);
        ac.destroy();
        assertTrue(activity.mFragment.mFragObserver.mOnDestroyObserved);
        assertTrue(activity.mActObserver.mOnDestroyObserved);
    }
}
