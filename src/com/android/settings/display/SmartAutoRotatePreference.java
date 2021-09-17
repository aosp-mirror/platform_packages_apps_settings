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

package com.android.settings.display;

import android.content.Context;
import android.util.AttributeSet;

import com.android.internal.view.RotationPolicy;
import com.android.settingslib.PrimarySwitchPreference;

/**
 * component for the display settings auto rotate toggle
 */
public class SmartAutoRotatePreference extends PrimarySwitchPreference {

    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;

    public SmartAutoRotatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    setChecked(!RotationPolicy.isRotationLocked(getContext()));
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(getContext(),
                mRotationPolicyListener);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (mRotationPolicyListener != null) {
            RotationPolicy.unregisterRotationPolicyListener(getContext(),
                    mRotationPolicyListener);
        }
    }
}
