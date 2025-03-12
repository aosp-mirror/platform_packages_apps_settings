/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.LayoutRes;

import com.android.settings.R;

import com.google.android.setupdesign.GlifLayout;

public class TestUdfpsEnrollEnrollingView extends GlifLayout {
    public TestUdfpsEnrollEnrollingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onInflateTemplate(LayoutInflater inflater, @LayoutRes int template) {
        return super.onInflateTemplate(inflater, R.layout.biometrics_glif_compact);
    }
}
