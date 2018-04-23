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

package com.android.settings.notification;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_ZEN_SHOW_CUSTOM;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ZenModeRestrictNotificationsSettings extends ZenModeSettingsBase implements Indexable {

    protected static final int APP_MENU_SHOW_CUSTOM = 1;
    protected boolean mShowMenuSelected;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, APP_MENU_SHOW_CUSTOM, 0, R.string.zen_mode_restrict_notifications_enable_custom)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == APP_MENU_SHOW_CUSTOM) {
            final FeatureFactory featureFactory = FeatureFactory.getFactory(mContext);
            MetricsFeatureProvider metrics = featureFactory.getMetricsFeatureProvider();

            mShowMenuSelected = !mShowMenuSelected;

            ZenModeVisEffectsCustomPreferenceController custom =
                    use(ZenModeVisEffectsCustomPreferenceController.class);
            custom.setShownByMenu(mShowMenuSelected);

            if (mShowMenuSelected) {
                custom.select();
                metrics.action(mContext, ACTION_ZEN_SHOW_CUSTOM, true);
            } else {
                metrics.action(mContext, ACTION_ZEN_SHOW_CUSTOM, false);
            }

            return true;
        }
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mShowMenuSelected && !use(ZenModeVisEffectsCustomPreferenceController.class)
                .areCustomOptionsSelected()) {
            menu.findItem(APP_MENU_SHOW_CUSTOM)
                    .setTitle(R.string.zen_mode_restrict_notifications_disable_custom);
        } else {
            menu.findItem(APP_MENU_SHOW_CUSTOM)
                    .setTitle(R.string.zen_mode_restrict_notifications_enable_custom);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeVisEffectsNonePreferenceController(
                context, lifecycle, "zen_mute_notifications"));
        controllers.add(new ZenModeVisEffectsAllPreferenceController(
                context, lifecycle, "zen_hide_notifications"));
        controllers.add(new ZenModeVisEffectsCustomPreferenceController(
                context, lifecycle, "zen_custom"));
        controllers.add(new ZenFooterPreferenceController(context, lifecycle,
                FooterPreference.KEY_FOOTER));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_restrict_notifications_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_ZEN_NOTIFICATIONS;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.zen_mode_restrict_notifications_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }

            @Override
            public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
                return buildPreferenceControllers(context, null);
            }
        };
}
