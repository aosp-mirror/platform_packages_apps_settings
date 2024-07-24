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

package com.android.settings.homepage.contextualcards;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.lifecycle.LifecycleOwner;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardController;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardRenderer;
import com.android.settings.homepage.contextualcards.conditional.ConditionFooterContextualCardRenderer;
import com.android.settings.homepage.contextualcards.conditional.ConditionHeaderContextualCardRenderer;
import com.android.settings.homepage.contextualcards.legacysuggestion.LegacySuggestionContextualCardController;
import com.android.settings.homepage.contextualcards.legacysuggestion.LegacySuggestionContextualCardRenderer;
import com.android.settings.homepage.contextualcards.slices.SliceContextualCardController;
import com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer;

import java.util.Set;

/**
 * This is a fragment scoped singleton holding a set of {@link ContextualCardController} and
 * {@link ContextualCardRenderer}.
 */
public class ControllerRendererPool {

    private static final String TAG = "ControllerRendererPool";

    private final Set<ContextualCardController> mControllers;
    private final Set<ContextualCardRenderer> mRenderers;

    public ControllerRendererPool() {
        mControllers = new ArraySet<>();
        mRenderers = new ArraySet<>();
    }

    public <T extends ContextualCardController> T getController(Context context,
            @ContextualCard.CardType int cardType) {
        final Class<? extends ContextualCardController> clz =
                ContextualCardLookupTable.getCardControllerClass(cardType);
        for (ContextualCardController controller : mControllers) {
            if (controller.getClass().getName().equals(clz.getName())) {
                Log.d(TAG, "Controller is already there.");
                return (T) controller;
            }
        }

        final ContextualCardController controller = createCardController(context, clz);
        if (controller != null) {
            mControllers.add(controller);
        }
        return (T) controller;
    }

    @VisibleForTesting
    Set<ContextualCardController> getControllers() {
        return mControllers;
    }

    @VisibleForTesting
    Set<ContextualCardRenderer> getRenderers() {
        return mRenderers;
    }

    public ContextualCardRenderer getRendererByViewType(Context context,
            LifecycleOwner lifecycleOwner, int viewType) {
        final Class<? extends ContextualCardRenderer> clz =
                ContextualCardLookupTable.getCardRendererClassByViewType(viewType);
        return getRenderer(context, lifecycleOwner, clz);
    }

    private ContextualCardRenderer getRenderer(Context context, LifecycleOwner lifecycleOwner,
            @NonNull Class<? extends ContextualCardRenderer> clz) {
        for (ContextualCardRenderer renderer : mRenderers) {
            if (renderer.getClass() == clz) {
                Log.d(TAG, "Renderer is already there.");
                return renderer;
            }
        }

        final ContextualCardRenderer renderer = createCardRenderer(context, lifecycleOwner, clz);
        if (renderer != null) {
            mRenderers.add(renderer);
        }
        return renderer;
    }

    private ContextualCardController createCardController(Context context,
            Class<? extends ContextualCardController> clz) {
        if (ConditionContextualCardController.class == clz) {
            return new ConditionContextualCardController(context);
        } else if (SliceContextualCardController.class == clz) {
            return new SliceContextualCardController(context);
        } else if (LegacySuggestionContextualCardController.class == clz) {
            return new LegacySuggestionContextualCardController(context);
        }
        return null;
    }

    private ContextualCardRenderer createCardRenderer(Context context,
            LifecycleOwner lifecycleOwner, Class<?> clz) {
        if (ConditionContextualCardRenderer.class == clz) {
            return new ConditionContextualCardRenderer(context, this /* controllerRendererPool */);
        } else if (SliceContextualCardRenderer.class == clz) {
            return new SliceContextualCardRenderer(context, lifecycleOwner,
                    this /* controllerRendererPool */);
        } else if (LegacySuggestionContextualCardRenderer.class == clz) {
            return new LegacySuggestionContextualCardRenderer(context,
                    this /* controllerRendererPool */);
        } else if (ConditionFooterContextualCardRenderer.class == clz) {
            return new ConditionFooterContextualCardRenderer(context,
                    this /*controllerRendererPool*/);
        } else if (ConditionHeaderContextualCardRenderer.class == clz) {
            return new ConditionHeaderContextualCardRenderer(context,
                    this /*controllerRendererPool*/);
        }
        return null;
    }
}
