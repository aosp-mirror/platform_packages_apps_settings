/**
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.core;

import static android.text.Layout.HYPHENATION_FREQUENCY_NORMAL_FAST;

import android.annotation.LayoutRes;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.text.LineBreakConfig;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.SubSettings;
import com.android.settings.core.CategoryMixin.CategoryHandler;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;
import com.android.settingslib.transition.SettingsTransitionHelper.TransitionType;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.resources.TextAppearanceConfig;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.transition.TransitionHelper;
import com.google.android.setupdesign.util.ThemeHelper;

/** Base activity for Settings pages */
public class SettingsBaseActivity extends FragmentActivity implements CategoryHandler {

    /**
     * What type of page transition should be apply.
     */
    public static final String EXTRA_PAGE_TRANSITION_TYPE = "page_transition_type";

    protected static final boolean DEBUG_TIMING = false;
    private static final String TAG = "SettingsBaseActivity";
    private static final int DEFAULT_REQUEST = -1;
    private static final float TOOLBAR_LINE_SPACING_MULTIPLIER = 1.1f;

    protected CategoryMixin mCategoryMixin;
    protected CollapsingToolbarLayout mCollapsingToolbarLayout;
    protected AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;

    @Override
    public CategoryMixin getCategoryMixin() {
        return mCategoryMixin;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final boolean isAnySetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
        if (isAnySetupWizard) {
            TransitionHelper.applyForwardTransition(this);
            TransitionHelper.applyBackwardTransition(this);
        }
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        if (isLockTaskModePinned() && !isSettingsRunOnTop()) {
            Log.w(TAG, "Devices lock task mode pinned.");
            finish();
        }
        final long startTime = System.currentTimeMillis();
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
        TextAppearanceConfig.setShouldLoadFontSynchronously(true);

        mCategoryMixin = new CategoryMixin(this);
        getLifecycle().addObserver(mCategoryMixin);

        final TypedArray theme = getTheme().obtainStyledAttributes(android.R.styleable.Theme);
        if (!theme.getBoolean(android.R.styleable.Theme_windowNoTitle, false)) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        // Apply SetupWizard light theme during setup flow. This is for SubSettings pages.
        if (isAnySetupWizard && this instanceof SubSettings) {
            setTheme(SetupWizardUtils.getTheme(this, getIntent()));
            setTheme(R.style.SettingsPreferenceTheme_SetupWizard);
            ThemeHelper.trySetDynamicColor(this);
        }

        if (isToolbarEnabled() && !isAnySetupWizard) {
            super.setContentView(
                    com.android.settingslib.collapsingtoolbar.R.layout.collapsing_toolbar_base_layout);
            mCollapsingToolbarLayout =
                    findViewById(com.android.settingslib.collapsingtoolbar.R.id.collapsing_toolbar);
            mAppBarLayout = findViewById(R.id.app_bar);
            if (mCollapsingToolbarLayout != null) {
                mCollapsingToolbarLayout.setLineSpacingMultiplier(TOOLBAR_LINE_SPACING_MULTIPLIER);
                mCollapsingToolbarLayout.setHyphenationFrequency(HYPHENATION_FREQUENCY_NORMAL_FAST);
                mCollapsingToolbarLayout.setStaticLayoutBuilderConfigurer(builder ->
                        builder.setLineBreakConfig(
                                new LineBreakConfig.Builder()
                                        .setLineBreakWordStyle(
                                                LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE)
                                        .build()));
            }
            autoSetCollapsingToolbarLayoutScrolling();
        } else {
            super.setContentView(R.layout.settings_base_layout);
        }

        // This is to hide the toolbar from those pages which don't need a toolbar originally.
        final Toolbar toolbar = findViewById(R.id.action_bar);
        if (!isToolbarEnabled() || isAnySetupWizard) {
            toolbar.setVisibility(View.GONE);
            return;
        }
        setActionBar(toolbar);

        if (DEBUG_TIMING) {
            Log.d(TAG, "onCreate took " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    @Override
    public void setActionBar(@androidx.annotation.Nullable Toolbar toolbar) {
        super.setActionBar(toolbar);

        mToolbar = toolbar;
    }

    @Override
    public boolean onNavigateUp() {
        if (!super.onNavigateUp()) {
            finishAfterTransition();
        }
        return true;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode,
            @androidx.annotation.Nullable Bundle options) {
        final int transitionType = getTransitionType(intent);
        super.startActivityForResult(intent, requestCode, options);
        if (transitionType == TransitionType.TRANSITION_SLIDE) {
            overridePendingTransition(
                    com.google.android.setupdesign.R.anim.sud_slide_next_in,
                    com.google.android.setupdesign.R.anim.sud_slide_next_out);
        } else if (transitionType == TransitionType.TRANSITION_FADE) {
            overridePendingTransition(
                    android.R.anim.fade_in, com.google.android.setupdesign.R.anim.sud_stay);
        }
    }

    @Override
    protected void onPause() {
        // For accessibility activities launched from setup wizard.
        if (getTransitionType(getIntent()) == TransitionType.TRANSITION_FADE) {
            overridePendingTransition(
                    com.google.android.setupdesign.R.anim.sud_stay, android.R.anim.fade_out);
        }
        super.onPause();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        final ViewGroup parent = findViewById(R.id.content_frame);
        if (parent != null) {
            parent.removeAllViews();
        }
        LayoutInflater.from(this).inflate(layoutResID, parent);
    }

    @Override
    public void setContentView(View view) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view, params);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (mCollapsingToolbarLayout != null) {
            mCollapsingToolbarLayout.setTitle(title);
        }
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(getText(titleId));
        if (mCollapsingToolbarLayout != null) {
            mCollapsingToolbarLayout.setTitle(getText(titleId));
        }
    }

    /**
     * SubSetting page should show a toolbar by default. If the page wouldn't show a toolbar,
     * override this method and return false value.
     *
     * @return ture by default
     */
    protected boolean isToolbarEnabled() {
        return true;
    }

    private boolean isLockTaskModePinned() {
        final ActivityManager activityManager =
                getApplicationContext().getSystemService(ActivityManager.class);
        return activityManager.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_PINNED;
    }

    private boolean isSettingsRunOnTop() {
        final ActivityManager activityManager =
                getApplicationContext().getSystemService(ActivityManager.class);
        final String taskPkgName = activityManager.getRunningTasks(1 /* maxNum */)
                .get(0 /* index */).baseActivity.getPackageName();
        return TextUtils.equals(getPackageName(), taskPkgName);
    }

    /**
     * @return whether or not the enabled state actually changed.
     */
    public boolean setTileEnabled(ComponentName component, boolean enabled) {
        final PackageManager pm = getPackageManager();
        int state = pm.getComponentEnabledSetting(component);
        boolean isEnabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        if (isEnabled != enabled || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            if (enabled) {
                mCategoryMixin.removeFromDenylist(component);
            } else {
                mCategoryMixin.addToDenylist(component);
            }
            pm.setComponentEnabledSetting(component, enabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            return true;
        }
        return false;
    }

    private void autoSetCollapsingToolbarLayoutScrolling() {
        if (mAppBarLayout == null) {
            return;
        }
        final CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();
        final AppBarLayout.Behavior behavior = new AppBarLayout.Behavior();
        behavior.setDragCallback(
                new AppBarLayout.Behavior.DragCallback() {
                    @Override
                    public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                        // Header can be scrolling while device in landscape mode.
                        return appBarLayout.getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_LANDSCAPE;
                    }
                });
        params.setBehavior(behavior);
    }

    private int getTransitionType(Intent intent) {
        if (intent == null) {
            return TransitionType.TRANSITION_NONE;
        }
        return intent.getIntExtra(EXTRA_PAGE_TRANSITION_TYPE, TransitionType.TRANSITION_NONE);
    }
}
