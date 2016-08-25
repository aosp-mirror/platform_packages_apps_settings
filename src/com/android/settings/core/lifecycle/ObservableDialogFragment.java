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

import android.app.DialogFragment;

/**
 * {@link DialogFragment} that has hooks to observe fragment lifecycle events.
 */
public class ObservableDialogFragment extends DialogFragment {

    protected final Lifecycle mLifecycle = new Lifecycle();

    @Override
    public void onStart() {
        mLifecycle.onStart();
        super.onStart();
    }

    @Override
    public void onResume() {
        mLifecycle.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        mLifecycle.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        mLifecycle.onStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mLifecycle.onDestroy();
        super.onDestroy();
    }

}
