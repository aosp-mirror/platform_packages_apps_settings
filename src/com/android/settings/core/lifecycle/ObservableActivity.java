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

import android.annotation.Nullable;
import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;

/**
 * {@link Activity} that has hooks to observe activity lifecycle events.
 */
public class ObservableActivity extends Activity {

    private final Lifecycle mLifecycle = new Lifecycle();

    protected Lifecycle getLifecycle() {
        return mLifecycle;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mLifecycle.onAttach(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState,
            @Nullable PersistableBundle persistentState) {
        mLifecycle.onAttach(this);
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onStart() {
        mLifecycle.onStart();
        super.onStart();
    }

    @Override
    protected void onResume() {
        mLifecycle.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mLifecycle.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mLifecycle.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mLifecycle.onDestroy();
        super.onDestroy();
    }
}
