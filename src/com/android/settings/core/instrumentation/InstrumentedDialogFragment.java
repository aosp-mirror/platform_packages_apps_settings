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
package com.android.settings.core.instrumentation;

import android.content.Context;

import com.android.settings.DialogCreatable;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.core.lifecycle.ObservableDialogFragment;

public abstract class InstrumentedDialogFragment extends ObservableDialogFragment
        implements Instrumentable {

    protected final DialogCreatable mDialogCreatable;
    protected int mDialogId;
    protected MetricsFeatureProvider mMetricsFeatureProvider;

    public InstrumentedDialogFragment() {
        this(null /* parentFragment */, 0 /* dialogId */);
    }

    /**
     * Use this if the dialog is created via {@code DialogCreatable}
     */
    public InstrumentedDialogFragment(DialogCreatable dialogCreatable, int dialogId) {
        mDialogCreatable = dialogCreatable;
        mDialogId = dialogId;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory()
                .getMetricsFeatureProvider();
        mLifecycle.addObserver(new VisibilityLoggerMixin(getMetricsCategory(),
                mMetricsFeatureProvider));
        mLifecycle.onAttach(context);
    }
}
