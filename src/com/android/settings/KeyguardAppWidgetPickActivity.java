/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;

import java.util.List;

/**
 * Displays a list of {@link AppWidgetProviderInfo} widgets, along with any
 * injected special widgets specified through
 * {@link AppWidgetManager#EXTRA_CUSTOM_INFO} and
 * {@link AppWidgetManager#EXTRA_CUSTOM_EXTRAS}.
 * <p>
 * When an installed {@link AppWidgetProviderInfo} is selected, this activity
 * will bind it to the given {@link AppWidgetManager#EXTRA_APPWIDGET_ID},
 * otherwise it will return the requested extras.
 */
public class KeyguardAppWidgetPickActivity extends Activity
    implements GridView.OnItemClickListener,
        AppWidgetLoader.ItemConstructor<KeyguardAppWidgetPickActivity.Item> {
    private static final String TAG = "KeyguardAppWidgetPickActivity";
    private static final int REQUEST_PICK_APPWIDGET = 126;
    private static final int REQUEST_CREATE_APPWIDGET = 127;

    private AppWidgetLoader<Item> mAppWidgetLoader;
    private List<Item> mItems;
    private GridView mGridView;
    private AppWidgetManager mAppWidgetManager;
    private int mAppWidgetId;
    // Might make it possible to make this be false in future
    private boolean mAddingToKeyguard = true;
    private Intent mResultData;
    private LockPatternUtils mLockPatternUtils;
    private boolean mSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.keyguard_appwidget_pick_layout);
        super.onCreate(savedInstanceState);

        // Set default return data
        setResultData(RESULT_CANCELED, null);

        // Read the appWidgetId passed our direction, otherwise bail if not found
        final Intent intent = getIntent();
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        } else {
            finish();
        }

        mGridView = (GridView) findViewById(R.id.widget_list);
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetLoader = new AppWidgetLoader<Item>(this, mAppWidgetManager, this);
        mItems = mAppWidgetLoader.getItems(getIntent());
        AppWidgetAdapter adapter = new AppWidgetAdapter(this, mItems);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(this);

        mLockPatternUtils = new LockPatternUtils(this); // TEMP-- we want to delete this
    }

    /**
     * Convenience method for setting the result code and intent. This method
     * correctly injects the {@link AppWidgetManager#EXTRA_APPWIDGET_ID} that
     * most hosts expect returned.
     */
    void setResultData(int code, Intent intent) {
        Intent result = intent != null ? intent : new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        mResultData = result;
        setResult(code, result);
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

    /**
     * Item that appears in the AppWidget picker grid.
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
        Intent getIntent() {
            Intent intent = new Intent();
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

    @Override
    public Item createItem(Context context, AppWidgetProviderInfo info, Bundle extras) {
        CharSequence label = info.label;
        Drawable icon = null;

        if (info.icon != 0) {
            try {
                final Resources res = context.getResources();
                final int density = res.getDisplayMetrics().densityDpi;
                int iconDensity;
                switch (density) {
                    case DisplayMetrics.DENSITY_MEDIUM:
                        iconDensity = DisplayMetrics.DENSITY_LOW;
                    case DisplayMetrics.DENSITY_TV:
                        iconDensity = DisplayMetrics.DENSITY_MEDIUM;
                    case DisplayMetrics.DENSITY_HIGH:
                        iconDensity = DisplayMetrics.DENSITY_MEDIUM;
                    case DisplayMetrics.DENSITY_XHIGH:
                        iconDensity = DisplayMetrics.DENSITY_HIGH;
                    case DisplayMetrics.DENSITY_XXHIGH:
                        iconDensity = DisplayMetrics.DENSITY_XHIGH;
                    default:
                        // The density is some abnormal value.  Return some other
                        // abnormal value that is a reasonable scaling of it.
                        iconDensity = (int)((density*0.75f)+.5f);
                }
                Resources packageResources = getPackageManager().
                        getResourcesForApplication(info.provider.getPackageName());
                icon = packageResources.getDrawableForDensity(info.icon, iconDensity);
            } catch (NameNotFoundException e) {
                Log.w(TAG, "Can't load icon drawable 0x" + Integer.toHexString(info.icon)
                        + " for provider: " + info.provider);
            }
            if (icon == null) {
                Log.w(TAG, "Can't load icon drawable 0x" + Integer.toHexString(info.icon)
                        + " for provider: " + info.provider);
            }
        }

        Item item = new Item(context, label, icon);
        item.packageName = info.provider.getPackageName();
        item.className = info.provider.getClassName();
        item.extras = extras;
        return item;
    }

    protected static class AppWidgetAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final List<Item> mItems;

        /**
         * Create an adapter for the given items.
         */
        public AppWidgetAdapter(Context context, List<Item> items) {
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
                convertView = mInflater.inflate(R.layout.keyguard_appwidget_item, parent, false);
            }

            Item item = (Item) getItem(position);
            TextView textView = (TextView) convertView.findViewById(R.id.icon_and_label);
            textView.setText(item.label);
            textView.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null);

            return convertView;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Item item = mItems.get(position);
        Intent intent = item.getIntent();

        int result;
        if (item.extras != null) {
            // If these extras are present it's because this entry is custom.
            // Don't try to bind it, just pass it back to the app.
            result = RESULT_OK;
            setResultData(result, intent);
        } else {
            try {
                Bundle options = null;
                if (intent.getExtras() != null) {
                    options = intent.getExtras().getBundle(
                            AppWidgetManager.EXTRA_APPWIDGET_OPTIONS);
                }
                mAppWidgetManager.bindAppWidgetId(mAppWidgetId, intent.getComponent(), options);
                result = RESULT_OK;
            } catch (IllegalArgumentException e) {
                // This is thrown if they're already bound, or otherwise somehow
                // bogus.  Set the result to canceled, and exit.  The app *should*
                // clean up at this point.  We could pass the error along, but
                // it's not clear that that's useful -- the widget will simply not
                // appear.
                result = RESULT_CANCELED;
            }
            setResultData(result, null);
        }
        if (mAddingToKeyguard) {
            onActivityResult(REQUEST_PICK_APPWIDGET, result, mResultData);
        } else {
            finish();
        }
    }

    protected void onDestroy() {
        if (!mSuccess && mAddingToKeyguard &&
                mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetHost.deleteAppWidgetIdForSystem(mAppWidgetId);
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET) {
            int appWidgetId = (data == null) ? -1 : data.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if ((requestCode == REQUEST_PICK_APPWIDGET) &&
                resultCode == Activity.RESULT_OK) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                boolean defaultWidget =
                        data.getBooleanExtra(LockPatternUtils.EXTRA_DEFAULT_WIDGET, false);

                AppWidgetProviderInfo appWidget = null;
                if (!defaultWidget) {
                    appWidget = appWidgetManager.getAppWidgetInfo(appWidgetId);
                }

                if (!defaultWidget && appWidget.configure != null) {
                    // Launch over to configure widget, if needed
                    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                    intent.setComponent(appWidget.configure);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

                    startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET);
                } else {
                    // Otherwise just add it
                    if (defaultWidget) {
                        // If we selected "none", delete the allocated id
                        AppWidgetHost.deleteAppWidgetIdForSystem(appWidgetId);
                        data.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                LockPatternUtils.ID_DEFAULT_STATUS_WIDGET);
                    }
                    onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, data);
                }
            } else if (requestCode == REQUEST_CREATE_APPWIDGET && resultCode == Activity.RESULT_OK) {
                mSuccess = true;
                mLockPatternUtils.addAppWidget(appWidgetId, 0);
                finishDelayedAndShowLockScreen();
            } else {
                finishDelayedAndShowLockScreen();
            }
        }
    }

    private void finishDelayedAndShowLockScreen() {
        IBinder b = ServiceManager.getService(Context.WINDOW_SERVICE);
        IWindowManager iWm = IWindowManager.Stub.asInterface(b);
        try {
            iWm.lockNow(null);
        } catch (RemoteException e) {
        }

        // Change background to all black
        ViewGroup root = (ViewGroup) findViewById(R.id.layout_root);
        root.setBackgroundColor(0xFF000000);
        // Hide all children
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            root.getChildAt(i).setVisibility(View.INVISIBLE);
        }
        mGridView.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, 500);
    }

    void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Settings does not have the permission to launch " + intent, e);
        }
    }
}
