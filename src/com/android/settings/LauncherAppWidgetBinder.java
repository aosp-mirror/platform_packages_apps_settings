/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ComponentName;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;

public class LauncherAppWidgetBinder extends Activity {
    private static final String TAG = "LauncherAppWidgetBinder";
    private static final boolean LOGD = true;
    
    static final String AUTHORITY = "com.android.launcher.settings";
    static final String TABLE_FAVORITES = "favorites";
    
    static final String EXTRA_BIND_SOURCES = "com.android.launcher.settings.bindsources";
    static final String EXTRA_BIND_TARGETS = "com.android.launcher.settings.bindtargets";

    static final String EXTRA_APPWIDGET_BITMAPS = "com.android.camera.appwidgetbitmaps";
    
    /**
     * {@link ContentProvider} constants pulled over from Launcher
     */
    static final class LauncherProvider implements BaseColumns {
        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_FAVORITES);

        static final String ITEM_TYPE = "itemType";
        static final String APPWIDGET_ID = "appWidgetId";
        static final String ICON = "icon";

        static final int ITEM_TYPE_APPWIDGET = 4;
        static final int ITEM_TYPE_WIDGET_CLOCK = 1000;
        static final int ITEM_TYPE_WIDGET_SEARCH = 1001;
        static final int ITEM_TYPE_WIDGET_PHOTO_FRAME = 1002;
    }
    
    static final String[] BIND_PROJECTION = new String[] {
        LauncherProvider._ID,
        LauncherProvider.ITEM_TYPE,
        LauncherProvider.APPWIDGET_ID,
        LauncherProvider.ICON,
    };
    
    static final int INDEX_ID = 0;
    static final int INDEX_ITEM_TYPE = 1;
    static final int INDEX_APPWIDGET_ID = 2;
    static final int INDEX_ICON = 3;
    
    static final ComponentName BIND_PHOTO_APPWIDGET = new ComponentName("com.android.camera",
            "com.android.camera.PhotoAppWidgetBind");

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        finish();

        // This helper reaches into the Launcher database and binds any unlinked
        // widgets. If will remove any items that can't be bound successfully.
        // We protect this binder at the manifest level by asserting the caller
        // has the Launcher WRITE_SETTINGS permission.
        
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        
        int[] bindSources = null;
        ArrayList<ComponentName> bindTargets = null;
        Exception exception = null;

        try {
            bindSources = extras.getIntArray(EXTRA_BIND_SOURCES);
            bindTargets = intent.getParcelableArrayListExtra(EXTRA_BIND_TARGETS);
        } catch (ClassCastException ex) {
            exception = ex;
        }
        
        if (exception != null || bindSources == null || bindTargets == null ||
                bindSources.length != bindTargets.size()) {
            Log.w(TAG, "Problem reading incoming bind request, or invalid request", exception);
            return;
        }
        
        final String selectWhere = buildOrWhereString(LauncherProvider.ITEM_TYPE, bindSources);
        
        final ContentResolver resolver = getContentResolver();
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        
        boolean foundPhotoAppWidgets = false;
        final ArrayList<Integer> photoAppWidgetIds = new ArrayList<Integer>();
        final ArrayList<Bitmap> photoBitmaps = new ArrayList<Bitmap>();
        
        Cursor c = null;
        
        try {
            c = resolver.query(LauncherProvider.CONTENT_URI,
                    BIND_PROJECTION, selectWhere, null, null);
            
            if (LOGD) Log.d(TAG, "found bind cursor count="+c.getCount());
            
            final ContentValues values = new ContentValues();
            while (c != null && c.moveToNext()) {
                long favoriteId = c.getLong(INDEX_ID);
                int itemType = c.getInt(INDEX_ITEM_TYPE);
                int appWidgetId = c.getInt(INDEX_APPWIDGET_ID);
                byte[] iconData = c.getBlob(INDEX_ICON);
                
                // Find the binding target for this type
                ComponentName targetAppWidget = null;
                for (int i = 0; i < bindSources.length; i++) {
                    if (bindSources[i] == itemType) {
                        targetAppWidget = bindTargets.get(i);
                        break;
                    }
                }
                
                if (LOGD) Log.d(TAG, "found matching targetAppWidget="+targetAppWidget.toString()+" for favoriteId="+favoriteId);
                
                boolean bindSuccess = false;
                try {
                    appWidgetManager.bindAppWidgetId(appWidgetId, targetAppWidget);
                    bindSuccess = true;
                } catch (RuntimeException ex) {
                    Log.w(TAG, "Problem binding widget", ex);
                }
                
                // Handle special case of photo widget by loading bitmap and
                // preparing for later binding
                if (bindSuccess && iconData != null &&
                        itemType == LauncherProvider.ITEM_TYPE_WIDGET_PHOTO_FRAME) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
                    
                    photoAppWidgetIds.add(appWidgetId);
                    photoBitmaps.add(bitmap);
                    foundPhotoAppWidgets = true;
                }

                if (LOGD) Log.d(TAG, "after finished, success="+bindSuccess);

                // Depending on success, update launcher or remove item
                Uri favoritesUri = ContentUris.withAppendedId(LauncherProvider.CONTENT_URI, favoriteId);
                if (bindSuccess) {
                    values.clear();
                    values.put(LauncherProvider.ITEM_TYPE, LauncherProvider.ITEM_TYPE_APPWIDGET);
                    values.putNull(LauncherProvider.ICON);
                    resolver.update(favoritesUri, values, null, null);
                } else {
                    resolver.delete(favoritesUri, null, null);
                }
                    
            }
        } catch (SQLException ex) {
            Log.w(TAG, "Problem while binding appWidgetIds for Launcher", ex);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        
        if (foundPhotoAppWidgets) {
            // Convert appWidgetIds into int[]
            final int N = photoAppWidgetIds.size();
            final int[] photoAppWidgetIdsArray = new int[N];
            for (int i = 0; i < N; i++) {
                photoAppWidgetIdsArray[i] = photoAppWidgetIds.get(i);
            }
            
            // Launch intent over to handle bitmap binding, but we don't need to
            // wait around for the result.
            final Intent bindIntent = new Intent();
            bindIntent.setComponent(BIND_PHOTO_APPWIDGET);
            
            final Bundle bindExtras = new Bundle();
            bindExtras.putIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS, photoAppWidgetIdsArray);
            bindExtras.putParcelableArrayList(EXTRA_APPWIDGET_BITMAPS, photoBitmaps);
            bindIntent.putExtras(bindExtras);
            
            startActivity(bindIntent);
        }
        
        if (LOGD) Log.d(TAG, "completely finished with binding for Launcher");
    }
    
    /**
     * Build a query string that will match any row where the column matches
     * anything in the values list.
     */
    static String buildOrWhereString(String column, int[] values) {
        StringBuilder selectWhere = new StringBuilder();
        for (int i = values.length - 1; i >= 0; i--) {
            selectWhere.append(column).append("=").append(values[i]);
            if (i > 0) {
                selectWhere.append(" OR ");
            }
        }
        return selectWhere.toString();
    }
    
}
