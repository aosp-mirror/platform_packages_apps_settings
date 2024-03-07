/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.security;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentContainerView;

public final class TestActivity extends FragmentActivity {

    static final int CONTAINER_VIEW_ID = 1234;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        FragmentContainerView contentView = new FragmentContainerView(this);
        contentView.setId(CONTAINER_VIEW_ID);
        setContentView(contentView);
    }
}
