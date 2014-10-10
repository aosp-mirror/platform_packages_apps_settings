/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.graphics.ColorFilter;
import android.util.DisplayMetrics;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Displays a list of all activities matching the incoming
 * {@link Intent#EXTRA_INTENT} query, along with any injected items.
 */
public class ActivityPicker extends AlertActivity implements
        DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    
    /**
     * Adapter of items that are displayed in this dialog.
     */
    private PickAdapter mAdapter;
    
    /**
     * Base {@link Intent} used when building list.
     */
    private Intent mBaseIntent;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        
        // Read base intent from extras, otherwise assume default
        Parcelable parcel = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (parcel instanceof Intent) {
            mBaseIntent = (Intent) parcel;
        } else {
            mBaseIntent = new Intent(Intent.ACTION_MAIN, null);
            mBaseIntent.addCategory(Intent.CATEGORY_DEFAULT);
        }

        // Create dialog parameters
        AlertController.AlertParams params = mAlertParams;
        params.mOnClickListener = this;
        params.mOnCancelListener = this;
        
        // Use custom title if provided, otherwise default window title
        if (intent.hasExtra(Intent.EXTRA_TITLE)) {
            params.mTitle = intent.getStringExtra(Intent.EXTRA_TITLE);
        } else {
            params.mTitle = getTitle();
        }
        
        // Build list adapter of pickable items
        List<PickAdapter.Item> items = getItems();
        mAdapter = new PickAdapter(this, items);
        params.mAdapter = mAdapter;

        setupAlert();
    }
    
    /**
     * Handle clicking of dialog item by passing back
     * {@link #getIntentForPosition(int)} in {@link #setResult(int, Intent)}.
     */
    public void onClick(DialogInterface dialog, int which) {
        Intent intent = getIntentForPosition(which);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
    
    /**
     * Handle canceled dialog by passing back {@link Activity#RESULT_CANCELED}.
     */
    public void onCancel(DialogInterface dialog) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    /**
     * Build the specific {@link Intent} for a given list position. Convenience
     * method that calls through to {@link PickAdapter.Item#getIntent(Intent)}.
     */
    protected Intent getIntentForPosition(int position) {
        PickAdapter.Item item = (PickAdapter.Item) mAdapter.getItem(position);
        return item.getIntent(mBaseIntent);
    }

    /**
     * Build and return list of items to be shown in dialog. Default
     * implementation mixes activities matching {@link #mBaseIntent} from
     * {@link #putIntentItems(Intent, List)} with any injected items from
     * {@link Intent#EXTRA_SHORTCUT_NAME}. Override this method in subclasses to
     * change the items shown.
     */
    protected List<PickAdapter.Item> getItems() {
        PackageManager packageManager = getPackageManager();
        List<PickAdapter.Item> items = new ArrayList<PickAdapter.Item>();
        
        // Add any injected pick items
        final Intent intent = getIntent();
        ArrayList<String> labels =
            intent.getStringArrayListExtra(Intent.EXTRA_SHORTCUT_NAME);
        ArrayList<ShortcutIconResource> icons =
            intent.getParcelableArrayListExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
        
        if (labels != null && icons != null && labels.size() == icons.size()) {
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                Drawable icon = null;
                
                try {
                    // Try loading icon from requested package
                    ShortcutIconResource iconResource = icons.get(i);
                    Resources res = packageManager.getResourcesForApplication(
                            iconResource.packageName);
                    icon = res.getDrawable(res.getIdentifier(
                            iconResource.resourceName, null, null), null);
                } catch (NameNotFoundException e) {
                    // Ignore
                }
                
                items.add(new PickAdapter.Item(this, label, icon));
            }
        }

        // Add any intent items if base was given
        if (mBaseIntent != null) {
            putIntentItems(mBaseIntent, items);
        }
        
        return items;
    }

    /**
     * Fill the given list with any activities matching the base {@link Intent}. 
     */
    protected void putIntentItems(Intent baseIntent, List<PickAdapter.Item> items) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(baseIntent,
                0 /* no flags */);
        Collections.sort(list, new ResolveInfo.DisplayNameComparator(packageManager));
        
        final int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            ResolveInfo resolveInfo = list.get(i);
            items.add(new PickAdapter.Item(this, packageManager, resolveInfo));
        }
    }
    
    /**
     * Adapter which shows the set of activities that can be performed for a
     * given {@link Intent}.
     */
    protected static class PickAdapter extends BaseAdapter {
        
        /**
         * Item that appears in a {@link PickAdapter} list.
         */
        public static class Item implements AppWidgetLoader.LabelledItem {
            protected static IconResizer sResizer;
            
            protected IconResizer getResizer(Context context) {
                if (sResizer == null) {
                    final Resources resources = context.getResources();
                    int size = (int) resources.getDimension(android.R.dimen.app_icon_size);
                    sResizer = new IconResizer(size, size, resources.getDisplayMetrics());
                }
                return sResizer;
            }
            
            CharSequence label;
            Drawable icon;
            String packageName;
            String className;
            Bundle extras;
            
            /**
             * Create a list item from given label and icon.
             */
            Item(Context context, CharSequence label, Drawable icon) {
                this.label = label;
                this.icon = getResizer(context).createIconThumbnail(icon);
            }

            /**
             * Create a list item and fill it with details from the given
             * {@link ResolveInfo} object.
             */
            Item(Context context, PackageManager pm, ResolveInfo resolveInfo) {
                label = resolveInfo.loadLabel(pm);
                if (label == null && resolveInfo.activityInfo != null) {
                    label = resolveInfo.activityInfo.name;
                }

                icon = getResizer(context).createIconThumbnail(resolveInfo.loadIcon(pm));
                packageName = resolveInfo.activityInfo.applicationInfo.packageName;
                className = resolveInfo.activityInfo.name;
            }

            /**
             * Build the {@link Intent} described by this item. If this item
             * can't create a valid {@link android.content.ComponentName}, it will return
             * {@link Intent#ACTION_CREATE_SHORTCUT} filled with the item label.
             */
            Intent getIntent(Intent baseIntent) {
                Intent intent = new Intent(baseIntent);
                if (packageName != null && className != null) {
                    // Valid package and class, so fill details as normal intent
                    intent.setClassName(packageName, className);
                    if (extras != null) {
                        intent.putExtras(extras);
                    }
                } else {
                    // No valid package or class, so treat as shortcut with label
                    intent.setAction(Intent.ACTION_CREATE_SHORTCUT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
                }
                return intent;
            }

            public CharSequence getLabel() {
                return label;
            }
        }
        
        private final LayoutInflater mInflater;
        private final List<Item> mItems;
        
        /**
         * Create an adapter for the given items.
         */
        public PickAdapter(Context context, List<Item> items) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mItems = items;
        }

        /**
         * {@inheritDoc}
         */
        public int getCount() {
            return mItems.size();
        }

        /**
         * {@inheritDoc}
         */
        public Object getItem(int position) {
            return mItems.get(position);
        }

        /**
         * {@inheritDoc}
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.pick_item, parent, false);
            }
            
            Item item = (Item) getItem(position);
            TextView textView = (TextView) convertView;
            textView.setText(item.label);
            textView.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null);
            
            return convertView;
        }
    }
        
    /**
     * Utility class to resize icons to match default icon size. Code is mostly
     * borrowed from Launcher.
     */
    private static class IconResizer {
        private final int mIconWidth;
        private final int mIconHeight;

        private final DisplayMetrics mMetrics;
        private final Rect mOldBounds = new Rect();
        private final Canvas mCanvas = new Canvas();
        
        public IconResizer(int width, int height, DisplayMetrics metrics) {
            mCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                    Paint.FILTER_BITMAP_FLAG));

            mMetrics = metrics;
            mIconWidth = width;
            mIconHeight = height; 
        }

        /**
         * Returns a Drawable representing the thumbnail of the specified Drawable.
         * The size of the thumbnail is defined by the dimension
         * android.R.dimen.launcher_application_icon_size.
         *
         * This method is not thread-safe and should be invoked on the UI thread only.
         *
         * @param icon The icon to get a thumbnail of.
         *
         * @return A thumbnail for the specified icon or the icon itself if the
         *         thumbnail could not be created. 
         */
        public Drawable createIconThumbnail(Drawable icon) {
            int width = mIconWidth;
            int height = mIconHeight;

            if (icon == null) {
                return new EmptyDrawable(width, height);
            }
            
            try {
                if (icon instanceof PaintDrawable) {
                    PaintDrawable painter = (PaintDrawable) icon;
                    painter.setIntrinsicWidth(width);
                    painter.setIntrinsicHeight(height);
                } else if (icon instanceof BitmapDrawable) {
                    // Ensure the bitmap has a density.
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                        bitmapDrawable.setTargetDensity(mMetrics);
                    }
                }
                int iconWidth = icon.getIntrinsicWidth();
                int iconHeight = icon.getIntrinsicHeight();
    
                if (iconWidth > 0 && iconHeight > 0) {
                    if (width < iconWidth || height < iconHeight) {
                        final float ratio = (float) iconWidth / iconHeight;
    
                        if (iconWidth > iconHeight) {
                            height = (int) (width / ratio);
                        } else if (iconHeight > iconWidth) {
                            width = (int) (height * ratio);
                        }
    
                        final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
                                    Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                        final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                        final Canvas canvas = mCanvas;
                        canvas.setBitmap(thumb);
                        // Copy the old bounds to restore them later
                        // If we were to do oldBounds = icon.getBounds(),
                        // the call to setBounds() that follows would
                        // change the same instance and we would lose the
                        // old bounds
                        mOldBounds.set(icon.getBounds());
                        final int x = (mIconWidth - width) / 2;
                        final int y = (mIconHeight - height) / 2;
                        icon.setBounds(x, y, x + width, y + height);
                        icon.draw(canvas);
                        icon.setBounds(mOldBounds);
                        //noinspection deprecation
                        icon = new BitmapDrawable(thumb);
                        ((BitmapDrawable) icon).setTargetDensity(mMetrics);
                        canvas.setBitmap(null);
                    } else if (iconWidth < width && iconHeight < height) {
                        final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                        final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                        final Canvas canvas = mCanvas;
                        canvas.setBitmap(thumb);
                        mOldBounds.set(icon.getBounds());
                        final int x = (width - iconWidth) / 2;
                        final int y = (height - iconHeight) / 2;
                        icon.setBounds(x, y, x + iconWidth, y + iconHeight);
                        icon.draw(canvas);
                        icon.setBounds(mOldBounds);
                        //noinspection deprecation
                        icon = new BitmapDrawable(thumb);
                        ((BitmapDrawable) icon).setTargetDensity(mMetrics);
                        canvas.setBitmap(null);
                    }
                }
    
            } catch (Throwable t) {
                icon = new EmptyDrawable(width, height);
            }

            return icon;
        }
    }

    private static class EmptyDrawable extends Drawable {
        private final int mWidth;
        private final int mHeight;

        EmptyDrawable(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mHeight;
        }

        @Override
        public int getMinimumWidth() {
            return mWidth;
        }

        @Override
        public int getMinimumHeight() {
            return mHeight;
        }

        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
