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

package com.android.settings.wifi.calling;

import android.content.Context;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Factory class to create disclaimer items list.
 */
@VisibleForTesting
public final class DisclaimerItemFactory {

    /**
     * Creates disclaimer items list.
     *
     * @param context The application context
     * @param subId The subscription id.
     * @return The {@link DisclaimerItem} list instance, if there are no items, return empty list.
     */
    public static List<DisclaimerItem> create(Context context, int subId) {
        List<DisclaimerItem> itemList = getDisclaimerItemList(context, subId);
        Iterator itr = itemList.iterator();
        while (itr.hasNext()) {
            DisclaimerItem item = (DisclaimerItem) itr.next();
            if (!item.shouldShow()) {
                itr.remove();
            }
        }
        return itemList;
    }

    private static List<DisclaimerItem> getDisclaimerItemList(Context context, int subId) {
        List<DisclaimerItem> itemList = new ArrayList<DisclaimerItem>();
        itemList.add(new LocationPolicyDisclaimer(context, subId));
        itemList.add(new EmergencyCallLimitationDisclaimer(context, subId));

        return itemList;
    }
}
