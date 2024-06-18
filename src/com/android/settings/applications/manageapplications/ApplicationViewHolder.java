/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.manageapplications;

import static com.android.settings.applications.manageapplications.ManageApplications.ApplicationsAdapter;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_CLONED_APPS;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_NONE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.spaprivileged.template.app.AppListItemModelKt;
import com.android.settingslib.spaprivileged.template.app.AppListPageKt;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;

/**
 * @deprecated Will be removed, use {@link AppListItemModelKt} {@link AppListPageKt} instead.
 */
@Deprecated(forRemoval = true)
public class ApplicationViewHolder extends RecyclerView.ViewHolder {

    @VisibleForTesting
    final TextView mAppName;
    @VisibleForTesting
    final TextView mSummary;
    @VisibleForTesting
    final TextView mDisabled;
    @VisibleForTesting
    final ViewGroup mWidgetContainer;
    @VisibleForTesting
    final CompoundButton mSwitch;
    final ImageView mAddIcon;
    final ProgressBar mProgressBar;

    private final ImageView mAppIcon;

    ApplicationViewHolder(View itemView) {
        super(itemView);
        mAppName = itemView.findViewById(android.R.id.title);
        mAppIcon = itemView.findViewById(android.R.id.icon);
        mSummary = itemView.findViewById(android.R.id.summary);
        mDisabled = itemView.findViewById(com.android.settingslib.widget.preference.app.R.id.appendix);
        mSwitch = itemView.findViewById(com.android.settingslib.R.id.switchWidget);
        mWidgetContainer = itemView.findViewById(android.R.id.widget_frame);
        mAddIcon = itemView.findViewById(R.id.add_preference_widget);
        mProgressBar = itemView.findViewById(R.id.progressBar_cyclic);
    }

    static View newView(ViewGroup parent) {
        return newView(parent, false /* twoTarget */, LIST_TYPE_NONE /* listType */);
    }

    static View newView(ViewGroup parent, boolean twoTarget, int listType) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(com.android.settingslib.widget.preference.app.R.layout.preference_app, parent, false);
        ViewGroup widgetFrame = view.findViewById(android.R.id.widget_frame);
        if (twoTarget) {
            if (widgetFrame != null) {
                if (listType == LIST_TYPE_CLONED_APPS) {
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.preference_widget_add_progressbar, widgetFrame, true);
                } else {
                    LayoutInflater.from(parent.getContext()).inflate(
                            com.android.settingslib.R.layout.preference_widget_primary_switch,
                            widgetFrame, true);
                }
                View divider = LayoutInflater.from(parent.getContext()).inflate(
                        com.android.settingslib.widget.preference.twotarget.R.layout.preference_two_target_divider,
                        view, false);
                // second to last, before widget frame
                view.addView(divider, view.getChildCount() - 1);
            }
        } else if (widgetFrame != null) {
            widgetFrame.setVisibility(View.GONE);
        }
        return view;
    }

    static View newHeader(ViewGroup parent, int resText) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(com.android.settingslib.widget.preference.app.R.layout.preference_app_header,
                        parent, false);
        TextView textView = view.findViewById(R.id.apps_top_intro_text);
        textView.setText(resText);
        return view;
    }

    static View newHeaderWithAnimation(Context context, ViewGroup parent, int resIntroText,
            int resAnimation, int resAppListText) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.preference_app_header_animation, parent, false);
        TextView introText = view.findViewById(R.id.apps_top_intro_text);
        introText.setText(resIntroText);

        LottieAnimationView illustrationLottie = view.findViewById(R.id.illustration_lottie);
        illustrationLottie.setAnimation(resAnimation);
        illustrationLottie.setVisibility(View.VISIBLE);
        LottieColorUtils.applyDynamicColors(context, illustrationLottie);

        TextView appListText = view.findViewById(R.id.app_list_text);
        appListText.setText(resAppListText);

        return view;
    }

    void setSummary(CharSequence summary) {
        mSummary.setText(summary);
        updateSummaryVisibility();
    }

    void setSummary(@StringRes int summary) {
        mSummary.setText(summary);
        updateSummaryVisibility();
    }

    private void updateSummaryVisibility() {
        // Hide an empty summary and then title will be vertically centered.
        mSummary.setVisibility(TextUtils.isEmpty(mSummary.getText()) ? View.GONE : View.VISIBLE);
    }

    void setEnabled(boolean isEnabled) {
        itemView.setEnabled(isEnabled);
    }

    void setTitle(CharSequence title, CharSequence contentDescription) {
        if (title == null) {
            return;
        }
        mAppName.setText(title);

        if (TextUtils.isEmpty(contentDescription)) {
            return;
        }
        mAppName.setContentDescription(contentDescription);
    }

    void setIcon(Drawable icon) {
        if (icon == null) {
            return;
        }
        mAppIcon.setImageDrawable(icon);
    }

    void updateDisableView(ApplicationInfo info) {
        if ((info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
            mDisabled.setVisibility(View.VISIBLE);
            mDisabled.setText(R.string.not_installed);
        } else if (!info.enabled || info.enabledSetting
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
            mDisabled.setVisibility(View.VISIBLE);
            mDisabled.setText(com.android.settingslib.R.string.disabled);
        } else {
            mDisabled.setVisibility(View.GONE);
        }
    }

    void updateSizeText(AppEntry entry, CharSequence invalidSizeStr, int whichSize) {
        if (ManageApplications.DEBUG) {
            Log.d(ManageApplications.TAG, "updateSizeText of "
                    + entry.label + " " + entry + ": " + entry.sizeStr);
        }
        if (entry.sizeStr != null) {
            switch (whichSize) {
                case ManageApplications.SIZE_INTERNAL:
                    setSummary(entry.internalSizeStr);
                    break;
                case ManageApplications.SIZE_EXTERNAL:
                    setSummary(entry.externalSizeStr);
                    break;
                default:
                    setSummary(entry.sizeStr);
                    break;
            }
        } else if (entry.size == ApplicationsState.SIZE_INVALID) {
            setSummary(invalidSizeStr);
        }
    }

    void updateSwitch(CompoundButton.OnCheckedChangeListener listener, boolean enabled,
            boolean checked) {
        if (mSwitch != null && mWidgetContainer != null) {
            mWidgetContainer.setFocusable(false);
            mWidgetContainer.setClickable(false);
            mSwitch.setFocusable(true);
            mSwitch.setClickable(true);
            mSwitch.setOnCheckedChangeListener(listener);
            mSwitch.setChecked(checked);
            mSwitch.setEnabled(enabled);
        }
    }

    void updateAppCloneWidget(Context context, View.OnClickListener onClickListener,
            AppEntry entry) {
        if (mAddIcon != null) {
            if (!entry.isClonedProfile()) {
                mAddIcon.setBackground(context.getDrawable(R.drawable.ic_add_24dp));
            } else {
                mAddIcon.setBackground(context.getDrawable(R.drawable.ic_trash_can));
                setSummary(R.string.cloned_app_created_summary);
            }
            mAddIcon.setOnClickListener(onClickListener);
        }
    }

    View.OnClickListener appCloneOnClickListener(AppEntry entry,
            ApplicationsAdapter adapter, FragmentActivity manageApplicationsActivity) {
        Context context = manageApplicationsActivity.getApplicationContext();
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloneBackend cloneBackend = CloneBackend.getInstance(context);
                final MetricsFeatureProvider metricsFeatureProvider =
                        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

                String packageName = entry.info.packageName;

                if (mWidgetContainer != null) {
                    if (!entry.isClonedProfile()) {
                        metricsFeatureProvider.action(context,
                                SettingsEnums.ACTION_CREATE_CLONE_APP);
                        mAddIcon.setVisibility(View.INVISIBLE);
                        mProgressBar.setVisibility(View.VISIBLE);
                        setSummary(R.string.cloned_app_creation_summary);

                        // todo(b/262352524): To figure out a way to prevent memory leak
                        //  without making this static.
                        new AsyncTask<Void, Void, Integer>(){

                            @Override
                            protected Integer doInBackground(Void... unused) {
                                return cloneBackend.installCloneApp(packageName);
                            }

                            @Override
                            protected void onPostExecute(Integer res) {
                                mProgressBar.setVisibility(View.INVISIBLE);
                                mAddIcon.setVisibility(View.VISIBLE);

                                if (res != CloneBackend.SUCCESS) {
                                    setSummary(null);
                                    return;
                                }

                                // Refresh the page to reflect newly created cloned app.
                                adapter.rebuild();
                            }
                        }.execute();

                    } else if (entry.isClonedProfile()) {
                        metricsFeatureProvider.action(context,
                                SettingsEnums.ACTION_DELETE_CLONE_APP);
                        cloneBackend.uninstallClonedApp(packageName, /*allUsers*/ false,
                                manageApplicationsActivity);
                    }
                }
            }
        };
    }
}
