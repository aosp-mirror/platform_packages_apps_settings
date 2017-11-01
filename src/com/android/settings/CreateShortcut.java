/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.LauncherActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.ListView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.Settings.TetherSettingsActivity;
import com.android.settings.overlay.FeatureFactory;

import java.util.ArrayList;
import java.util.List;

public class CreateShortcut extends LauncherActivity {

    @VisibleForTesting
    static final String SHORTCUT_ID_PREFIX = "component-shortcut-";

    @Override
    protected Intent getTargetIntent() {
        return getBaseIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final ListItem item = itemForPosition(position);
        logCreateShortcut(item.resolveInfo);
        setResult(RESULT_OK, createResultIntent(intentForPosition(position),
                item.resolveInfo, item.label));
        finish();
    }

    protected Intent createResultIntent(Intent shortcutIntent, ResolveInfo resolveInfo,
            CharSequence label) {
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        ShortcutManager sm = getSystemService(ShortcutManager.class);
        ActivityInfo activityInfo = resolveInfo.activityInfo;

        Icon maskableIcon = activityInfo.icon != 0 ? Icon.createWithAdaptiveBitmap(
                createIcon(activityInfo.icon,
                        R.layout.shortcut_badge_maskable,
                        getResources().getDimensionPixelSize(R.dimen.shortcut_size_maskable))) :
                Icon.createWithResource(this, R.drawable.ic_launcher_settings);
        String shortcutId = SHORTCUT_ID_PREFIX +
                shortcutIntent.getComponent().flattenToShortString();
        ShortcutInfo info = new ShortcutInfo.Builder(this, shortcutId)
                .setShortLabel(label)
                .setIntent(shortcutIntent)
                .setIcon(maskableIcon)
                .build();
        Intent intent = sm.createShortcutResultIntent(info);
        if (intent == null) {
            intent = new Intent();
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher_settings));
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);

        if (activityInfo.icon != 0) {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, createIcon(activityInfo.icon,
                R.layout.shortcut_badge,
                getResources().getDimensionPixelSize(R.dimen.shortcut_size)));
        }
        return intent;
    }

    private void logCreateShortcut(ResolveInfo info) {
        if (info == null || info.activityInfo == null) {
            return;
        }
        FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(
                this, MetricsProto.MetricsEvent.ACTION_SETTINGS_CREATE_SHORTCUT,
                info.activityInfo.name);
    }

    private Bitmap createIcon(int resource, int layoutRes, int size) {
        Context context = new ContextThemeWrapper(this, android.R.style.Theme_Material);
        View view = LayoutInflater.from(context).inflate(layoutRes, null);
        ((ImageView) view.findViewById(android.R.id.icon)).setImageResource(resource);

        int spec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        view.measure(spec, spec);
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        return bitmap;
    }

    @Override
    protected boolean onEvaluateShowIcons() {
        return false;
    }

    @Override
    protected void onSetContentView() {
        setContentView(R.layout.activity_list);
    }

    /**
     * Perform query on package manager for list items.  The default
     * implementation queries for activities.
     */
    protected List<ResolveInfo> onQueryPackageManager(Intent queryIntent) {
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(queryIntent,
                PackageManager.GET_META_DATA);
        final ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (activities == null) return null;
        for (int i = activities.size() - 1; i >= 0; i--) {
            ResolveInfo info = activities.get(i);
            if (info.activityInfo.name.endsWith(TetherSettingsActivity.class.getSimpleName())) {
                if (!cm.isTetheringSupported()) {
                    activities.remove(i);
                }
            }
        }
        return activities;
    }

    @VisibleForTesting
    static Intent getBaseIntent() {
        return new Intent(Intent.ACTION_MAIN).addCategory("com.android.settings.SHORTCUT");
    }

    public static class ShortcutsUpdateTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        public ShortcutsUpdateTask(Context context) {
            mContext = context;
        }

        @Override
        public Void doInBackground(Void... params) {
            ShortcutManager sm = mContext.getSystemService(ShortcutManager.class);
            PackageManager pm = mContext.getPackageManager();

            List<ShortcutInfo> updates = new ArrayList<>();
            for (ShortcutInfo info : sm.getPinnedShortcuts()) {
                if (!info.getId().startsWith(SHORTCUT_ID_PREFIX)) {
                    continue;
                }
                ComponentName cn = ComponentName.unflattenFromString(
                        info.getId().substring(SHORTCUT_ID_PREFIX.length()));
                ResolveInfo ri = pm.resolveActivity(getBaseIntent().setComponent(cn), 0);
                if (ri == null) {
                    continue;
                }
                updates.add(new ShortcutInfo.Builder(mContext, info.getId())
                        .setShortLabel(ri.loadLabel(pm)).build());
            }
            if (!updates.isEmpty()) {
                sm.updateShortcuts(updates);
            }
            return null;
        }
    }
}
