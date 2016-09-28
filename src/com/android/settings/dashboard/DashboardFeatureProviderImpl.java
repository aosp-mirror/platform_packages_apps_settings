/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.content.Context;

import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.CategoryManager;
import com.android.settingslib.drawer.DashboardCategory;

import java.util.List;

/**
 * Impl for {@code DashboardFeatureProvider}.
 */
public class DashboardFeatureProviderImpl implements DashboardFeatureProvider {

    protected final Context mContext;

    private final CategoryManager mCategoryManager;

    public DashboardFeatureProviderImpl(Context context) {
        mContext = context;
        mCategoryManager = CategoryManager.get();
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public DashboardCategory getTilesForHomepage() {
        return mCategoryManager.getTilesByCategory(mContext, CategoryKey.CATEGORY_HOMEPAGE);
    }

    @Override
    public List<DashboardCategory> getAllCategories() {
        return mCategoryManager.getCategories(mContext);
    }
}
