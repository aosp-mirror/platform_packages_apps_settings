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
package com.android.settings.dashboard;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView.ViewHolder;
import com.android.settingslib.drawer.Tile;

public class DashboardItemAnimator extends DefaultItemAnimator {

    @Override
    public boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY,
            int toX, int toY) {
        final Object tag = oldHolder.itemView.getTag();
        if (tag instanceof Tile && oldHolder == newHolder) {
            // When this view has other move animation running, skip this value to avoid
            // animations interrupt each other.
            if (!isRunning()) {
                fromX += ViewCompat.getTranslationX(oldHolder.itemView);
                fromY += ViewCompat.getTranslationY(oldHolder.itemView);
            }

            if (fromX == toX && fromY == toY) {
                dispatchMoveFinished(oldHolder);
                return false;
            }
        }
        return super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);
    }
}
