/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.ArraySet;

import androidx.appcompat.app.AppCompatActivity;

import com.android.settings.core.CategoryMixin.CategoryListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class CategoryMixinTest {
    private ActivityController<TestActivity> mActivityController;

    @Before
    public void setUp() {
        mActivityController = Robolectric.buildActivity(TestActivity.class);
    }

    @Test
    public void resumeActivity_shouldRegisterReceiver() {
        mActivityController.setup();

        final TestActivity activity = mActivityController.get();
        assertThat(activity.getRegisteredReceivers()).isNotEmpty();
    }

    @Test
    public void pauseActivity_shouldUnregisterReceiver() {
        mActivityController.setup().pause();

        final TestActivity activity = mActivityController.get();
        assertThat(activity.getRegisteredReceivers()).isEmpty();
    }

    @Test
    public void onCategoriesChanged_listenerAdded_shouldNotifyChanged() {
        mActivityController.setup().pause();
        final CategoryMixin categoryMixin = mActivityController.get().getCategoryMixin();
        final CategoryListener listener = mock(CategoryListener.class);
        categoryMixin.addCategoryListener(listener);

        categoryMixin.onCategoriesChanged(new ArraySet<>());

        verify(listener).onCategoriesChanged(anySet());
    }

    static class TestActivity extends AppCompatActivity implements CategoryMixin.CategoryHandler {

        private CategoryMixin mCategoryMixin;
        private List<BroadcastReceiver> mRegisteredReceivers = new ArrayList<>();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(androidx.appcompat.R.style.Theme_AppCompat);
            mCategoryMixin = new CategoryMixin(this);
            getLifecycle().addObserver(mCategoryMixin);
        }

        @Override
        public CategoryMixin getCategoryMixin() {
            return mCategoryMixin;
        }

        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
            mRegisteredReceivers.add(receiver);
            return super.registerReceiver(receiver, filter);
        }

        @Override
        public void unregisterReceiver(BroadcastReceiver receiver) {
            mRegisteredReceivers.remove(receiver);
            super.unregisterReceiver(receiver);
        }

        List<BroadcastReceiver> getRegisteredReceivers() {
            return mRegisteredReceivers;
        }
    }
}
