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
package com.android.settings.applications.intentpicker;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * This {@link AndroidViewModel} provides supported link wrapper data
 * between multiple fragments.
 */
public class SupportedLinkViewModel extends AndroidViewModel {
    private List<SupportedLinkWrapper> mSupportedLinkWrapperList;

    public SupportedLinkViewModel(Application application) {
        super(application);
    }

    /** Clear the list buffer of the {@link SupportedLinkWrapper}. */
    public void clearSupportedLinkWrapperList() {
        mSupportedLinkWrapperList = new ArrayList<>();
    }

    /** Set the list buffer of the {@link SupportedLinkWrapper}. */
    public void setSupportedLinkWrapperList(List<SupportedLinkWrapper> wrapperList) {
        mSupportedLinkWrapperList = wrapperList;
    }

    /** Get the list buffer of the {@link SupportedLinkWrapper}. */
    public List<SupportedLinkWrapper> getSupportedLinkWrapperList() {
        return mSupportedLinkWrapperList;
    }
}
