/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.shortcut;

import static com.google.common.base.Preconditions.checkArgument;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;

class Shortcuts {

    private static final String TAG = "Shortcuts";

    static final String SHORTCUT_ID_PREFIX = "component-shortcut-";
    static final Intent SHORTCUT_PROBE = new Intent(Intent.ACTION_MAIN)
            .addCategory("com.android.settings.SHORTCUT")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    static ShortcutInfo createShortcutInfo(Context context, ResolveInfo target) {
        checkArgument(target.activityInfo != null);
        String shortcutId = SHORTCUT_ID_PREFIX
                + target.activityInfo.getComponentName().flattenToShortString();

        return createShortcutInfo(context, shortcutId, target);
    }

    static ShortcutInfo createShortcutInfo(Context context, String id, ResolveInfo target) {
        Intent intent = new Intent(SHORTCUT_PROBE)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setClassName(target.activityInfo.packageName, target.activityInfo.name);
        if (ActivityEmbeddingUtils.isEmbeddingActivityEnabled(context)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        CharSequence label = target.loadLabel(context.getPackageManager());
        Icon maskableIcon = getMaskableIcon(context, target.activityInfo);

        return new ShortcutInfo.Builder(context, id)
                .setIntent(intent)
                .setShortLabel(label)
                .setIcon(maskableIcon)
                .build();
    }

    private static Icon getMaskableIcon(Context context, ActivityInfo activityInfo) {
        if (activityInfo.icon != 0 && activityInfo.applicationInfo != null) {
            return Icon.createWithAdaptiveBitmap(createIcon(
                    context,
                    activityInfo.applicationInfo, activityInfo.icon,
                    R.layout.shortcut_badge_maskable,
                    context.getResources().getDimensionPixelSize(R.dimen.shortcut_size_maskable)));
        } else {
            return Icon.createWithResource(context, R.drawable.ic_launcher_settings);
        }
    }

    static Bitmap createIcon(Context context, ApplicationInfo app, int resource, int layoutRes,
            int size) {
        final Context themedContext = new ContextThemeWrapper(context,
                android.R.style.Theme_Material);
        final View view = LayoutInflater.from(themedContext).inflate(layoutRes, null);
        final int spec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
        view.measure(spec, spec);
        final Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        Drawable iconDrawable;
        try {
            iconDrawable = context.getPackageManager().getResourcesForApplication(app)
                    .getDrawable(resource, themedContext.getTheme());
            if (iconDrawable instanceof LayerDrawable) {
                iconDrawable = ((LayerDrawable) iconDrawable).getDrawable(1);
            }
            ((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(iconDrawable);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Cannot load icon from app " + app + ", returning a default icon");
            Icon icon = Icon.createWithResource(context, R.drawable.ic_launcher_settings);
            ((ImageView) view.findViewById(android.R.id.icon)).setImageIcon(icon);
        }

        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        return bitmap;
    }
}
