/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage;

import android.content.Context;
import android.util.Log;

import androidx.collection.ArraySet;

import com.android.settings.homepage.conditional.ConditionHomepageCardController;
import com.android.settings.homepage.conditional.ConditionHomepageCardRenderer;

import java.util.Set;

/**
 * This is a fragment scoped singleton holding a set of {@link HomepageCardController} and
 * {@link HomepageCardRenderer}.
 */
public class ControllerRendererPool {

    private static final String TAG = "ControllerRendererPool";

    private final Set<HomepageCardController> mControllers;
    private final Set<HomepageCardRenderer> mRenderers;

    public ControllerRendererPool() {
        mControllers = new ArraySet<>();
        mRenderers = new ArraySet<>();
    }

    public <T extends HomepageCardController> T getController(Context context,
            @HomepageCard.CardType int cardType) {
        final Class<? extends HomepageCardController> clz =
                HomepageCardLookupTable.getCardControllerClass(cardType);
        for (HomepageCardController controller : mControllers) {
            if (controller.getClass() == clz) {
                Log.d(TAG, "Controller is already there.");
                return (T) controller;
            }
        }

        final HomepageCardController controller = createCardController(context, clz);
        if (controller != null) {
            mControllers.add(controller);
        }
        return (T) controller;
    }

    public Set<HomepageCardController> getControllers() {
        return mControllers;
    }

    public HomepageCardRenderer getRenderer(Context context, @HomepageCard.CardType int cardType) {
        final Class<? extends HomepageCardRenderer> clz =
                HomepageCardLookupTable.getCardRendererClasses(cardType);
        for (HomepageCardRenderer renderer : mRenderers) {
            if (renderer.getClass() == clz) {
                Log.d(TAG, "Renderer is already there.");
                return renderer;
            }
        }

        final HomepageCardRenderer renderer = createCardRenderer(context, clz);
        if (renderer != null) {
            mRenderers.add(renderer);
        }
        return renderer;
    }

    private HomepageCardController createCardController(Context context,
            Class<? extends HomepageCardController> clz) {
        if (ConditionHomepageCardController.class == clz) {
            return new ConditionHomepageCardController(context);
        }
        return null;
    }

    private HomepageCardRenderer createCardRenderer(Context context, Class<?> clz) {
        if (ConditionHomepageCardRenderer.class == clz) {
            return new ConditionHomepageCardRenderer(context, this /*controllerRendererPool*/);
        }
        return null;
    }

}
