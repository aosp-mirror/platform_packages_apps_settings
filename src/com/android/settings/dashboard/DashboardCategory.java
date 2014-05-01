/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class DashboardCategory implements Parcelable {

    /**
     * Default value for {@link com.android.settings.dashboard.DashboardCategory#id DashboardCategory.id}
     * indicating that no identifier value is set.  All other values (including those below -1)
     * are valid.
     */
    public static final long CAT_ID_UNDEFINED = -1;

    /**
     * Identifier for this tile, to correlate with a new list when
     * it is updated.  The default value is
     * {@link com.android.settings.dashboard.DashboardTile#TILE_ID_UNDEFINED}, meaning no id.
     * @attr ref android.R.styleable#PreferenceHeader_id
     */
    public long id = CAT_ID_UNDEFINED;

    /**
     * Resource ID of title of the category that is shown to the user.
     */
    public int titleRes;

    /**
     * Title of the category that is shown to the user.
     */
    public CharSequence title;

    /**
     * List of the category's children
     */
    public List<DashboardTile> tiles = new ArrayList<DashboardTile>();


    public DashboardCategory() {
        // Empty
    }

    public void addTile(DashboardTile tile) {
        tiles.add(tile);
    }

    public void addTile(int n, DashboardTile tile) {
        tiles.add(n, tile);
    }

    public void removeTile(DashboardTile tile) {
        tiles.remove(tile);
    }

    public void removeTile(int n) {
        tiles.remove(n);
    }

    public int getTilesCount() {
        return tiles.size();
    }

    public DashboardTile getTile(int n) {
        return tiles.get(n);
    }

    /**
     * Return the currently set title.  If {@link #titleRes} is set,
     * this resource is loaded from <var>res</var> and returned.  Otherwise
     * {@link #title} is returned.
     */
    public CharSequence getTitle(Resources res) {
        if (titleRes != 0) {
            return res.getText(titleRes);
        }
        return title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(titleRes);
        TextUtils.writeToParcel(title, dest, flags);

        final int count = tiles.size();
        dest.writeInt(count);

        for (int n = 0; n < count; n++) {
            DashboardTile tile = tiles.get(n);
            tile.writeToParcel(dest, flags);
        }
    }

    public void readFromParcel(Parcel in) {
        titleRes = in.readInt();
        title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);

        final int count = in.readInt();

        for (int n = 0; n < count; n++) {
            DashboardTile tile = DashboardTile.CREATOR.createFromParcel(in);
            tiles.add(tile);
        }
    }

    DashboardCategory(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<DashboardCategory> CREATOR = new Creator<DashboardCategory>() {
        public DashboardCategory createFromParcel(Parcel source) {
            return new DashboardCategory(source);
        }

        public DashboardCategory[] newArray(int size) {
            return new DashboardCategory[size];
        }
    };
}
