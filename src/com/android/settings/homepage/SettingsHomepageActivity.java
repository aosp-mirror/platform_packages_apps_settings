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

package com.android.settings.homepage;

import static android.provider.Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY;
import static android.provider.Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY;
import static android.provider.Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI;

import static com.android.settings.SettingsActivity.EXTRA_USER_HANDLE;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toolbar;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.window.embedding.SplitController;
import androidx.window.embedding.SplitRule;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsApplication;
import com.android.settings.accounts.AvatarViewMixin;
import com.android.settings.activityembedding.ActivityEmbeddingRulesController;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.settings.core.CategoryMixin;
import com.android.settings.core.FeatureFlags;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.PasswordUtils;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settingslib.Utils;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;

import java.net.URISyntaxException;
import java.util.Set;

/** Settings homepage activity */
public class SettingsHomepageActivity extends FragmentActivity implements
        CategoryMixin.CategoryHandler {

    private static final String TAG = "SettingsHomepageActivity";

    // Additional extra of Settings#ACTION_SETTINGS_LARGE_SCREEN_DEEP_LINK.
    // Put true value to the intent when startActivity for a deep link intent from this Activity.
    public static final String EXTRA_IS_FROM_SETTINGS_HOMEPAGE = "is_from_settings_homepage";

    // Additional extra of Settings#ACTION_SETTINGS_LARGE_SCREEN_DEEP_LINK.
    // Set & get Uri of the Intent separately to prevent failure of Intent#ParseUri.
    public static final String EXTRA_SETTINGS_LARGE_SCREEN_DEEP_LINK_INTENT_DATA =
            "settings_large_screen_deep_link_intent_data";

    static final int DEFAULT_HIGHLIGHT_MENU_KEY = R.string.menu_key_network;
    private static final long HOMEPAGE_LOADING_TIMEOUT_MS = 300;

    private TopLevelSettings mMainFragment;
    private View mHomepageView;
    private View mSuggestionView;
    private View mTwoPaneSuggestionView;
    private CategoryMixin mCategoryMixin;
    private Set<HomepageLoadedListener> mLoadedListeners;
    private SplitController mSplitController;
    private boolean mIsEmbeddingActivityEnabled;
    private boolean mIsTwoPane;
    // A regular layout shows icons on homepage, whereas a simplified layout doesn't.
    private boolean mIsRegularLayout = true;

    /** A listener receiving homepage loaded events. */
    public interface HomepageLoadedListener {
        /** Called when the homepage is loaded. */
        void onHomepageLoaded();
    }

    private interface FragmentCreator<T extends Fragment> {
        T create();

        /** To initialize after {@link #create} */
        default void init(Fragment fragment) {}
    }

    /**
     * Try to add a {@link HomepageLoadedListener}. If homepage is already loaded, the listener
     * will not be notified.
     *
     * @return Whether the listener is added.
     */
    public boolean addHomepageLoadedListener(HomepageLoadedListener listener) {
        if (mHomepageView == null) {
            return false;
        } else {
            if (!mLoadedListeners.contains(listener)) {
                mLoadedListeners.add(listener);
            }
            return true;
        }
    }

    /**
     * Shows the homepage and shows/hides the suggestion together. Only allows to be executed once
     * to avoid the flicker caused by the suggestion suddenly appearing/disappearing.
     */
    public void showHomepageWithSuggestion(boolean showSuggestion) {
        if (mHomepageView == null) {
            return;
        }
        Log.i(TAG, "showHomepageWithSuggestion: " + showSuggestion);
        final View homepageView = mHomepageView;
        mSuggestionView.setVisibility(showSuggestion ? View.VISIBLE : View.GONE);
        mTwoPaneSuggestionView.setVisibility(showSuggestion ? View.VISIBLE : View.GONE);
        mHomepageView = null;

        mLoadedListeners.forEach(listener -> listener.onHomepageLoaded());
        mLoadedListeners.clear();
        homepageView.setVisibility(View.VISIBLE);
    }

    /** Returns the main content fragment */
    public TopLevelSettings getMainFragment() {
        return mMainFragment;
    }

    @Override
    public CategoryMixin getCategoryMixin() {
        return mCategoryMixin;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsEmbeddingActivityEnabled = ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this);
        if (mIsEmbeddingActivityEnabled) {
            final UserManager um = getSystemService(UserManager.class);
            final UserInfo userInfo = um.getUserInfo(getUser().getIdentifier());
            if (userInfo.isManagedProfile()) {
                final Intent intent = new Intent(getIntent())
                        .setClass(this, DeepLinkHomepageActivityInternal.class)
                        .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        .putExtra(EXTRA_USER_HANDLE, getUser());
                intent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityAsUser(intent, um.getPrimaryUser().getUserHandle());
                finish();
                return;
            }
        }

        setupEdgeToEdge();
        setContentView(R.layout.settings_homepage_container);

        mSplitController = SplitController.getInstance();
        mIsTwoPane = mSplitController.isActivityEmbedded(this);

        updateAppBarMinHeight();
        initHomepageContainer();
        updateHomepageAppBar();
        updateHomepageBackground();
        mLoadedListeners = new ArraySet<>();

        initSearchBarView();

        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
        mCategoryMixin = new CategoryMixin(this);
        getLifecycle().addObserver(mCategoryMixin);

        final String highlightMenuKey = getHighlightMenuKey();
        // Only allow features on high ram devices.
        if (!getSystemService(ActivityManager.class).isLowRamDevice()) {
            initAvatarView();
            final boolean scrollNeeded = mIsEmbeddingActivityEnabled
                    && !TextUtils.equals(getString(DEFAULT_HIGHLIGHT_MENU_KEY), highlightMenuKey);
            showSuggestionFragment(scrollNeeded);
            if (FeatureFlagUtils.isEnabled(this, FeatureFlags.CONTEXTUAL_HOME)) {
                showFragment(() -> new ContextualCardsFragment(), R.id.contextual_cards_content);
                ((FrameLayout) findViewById(R.id.main_content))
                        .getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
            }
        }
        mMainFragment = showFragment(() -> {
            final TopLevelSettings fragment = new TopLevelSettings();
            fragment.getArguments().putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY,
                    highlightMenuKey);
            return fragment;
        }, R.id.main_content);

        // Launch the intent from deep link for large screen devices.
        launchDeepLinkIntentToRight();
        updateHomepagePaddings();
        updateSplitLayout();
    }

    @Override
    protected void onStart() {
        ((SettingsApplication) getApplication()).setHomeActivity(this);
        super.onStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // When it's large screen 2-pane and Settings app is in the background, receiving an Intent
        // will not recreate this activity. Update the intent for this case.
        setIntent(intent);
        reloadHighlightMenuKey();
        if (isFinishing()) {
            return;
        }

        if (ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this)
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0) {
            initSplitPairRules();
        }

        // Launch the intent from deep link for large screen devices.
        launchDeepLinkIntentToRight();
    }

    @VisibleForTesting
    void initSplitPairRules() {
        new ActivityEmbeddingRulesController(getApplicationContext()).initRules();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final boolean newTwoPaneState = mSplitController.isActivityEmbedded(this);
        if (mIsTwoPane != newTwoPaneState) {
            mIsTwoPane = newTwoPaneState;
            updateHomepageAppBar();
            updateHomepageBackground();
            updateHomepagePaddings();
        }
        updateSplitLayout();
    }

    private void updateSplitLayout() {
        if (!mIsEmbeddingActivityEnabled) {
            return;
        }

        if (mIsTwoPane) {
            if (mIsRegularLayout == ActivityEmbeddingUtils.isRegularHomepageLayout(this)) {
                // Layout unchanged
                return;
            }
        } else if (mIsRegularLayout) {
            // One pane mode with the regular layout, not needed to change
            return;
        }
        mIsRegularLayout = !mIsRegularLayout;

        // Update search title padding
        View searchTitle = findViewById(R.id.search_bar_title);
        if (searchTitle != null) {
            int paddingStart = getResources().getDimensionPixelSize(
                    mIsRegularLayout
                            ? R.dimen.search_bar_title_padding_start_regular_two_pane
                            : R.dimen.search_bar_title_padding_start);
            searchTitle.setPaddingRelative(paddingStart, 0, 0, 0);
        }
        // Notify fragments
        getSupportFragmentManager().getFragments().forEach(fragment -> {
            if (fragment instanceof SplitLayoutListener) {
                ((SplitLayoutListener) fragment).onSplitLayoutChanged(mIsRegularLayout);
            }
        });
    }

    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    // Apply the insets paddings to the view.
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

                    // Return CONSUMED if you don't want the window insets to keep being
                    // passed down to descendant views.
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    private void initSearchBarView() {
        final Toolbar toolbar = findViewById(R.id.search_action_bar);
        FeatureFactory.getFactory(this).getSearchFeatureProvider()
                .initSearchToolbar(this /* activity */, toolbar, SettingsEnums.SETTINGS_HOMEPAGE);

        if (mIsEmbeddingActivityEnabled) {
            final Toolbar toolbarTwoPaneVersion = findViewById(R.id.search_action_bar_two_pane);
            FeatureFactory.getFactory(this).getSearchFeatureProvider()
                    .initSearchToolbar(this /* activity */, toolbarTwoPaneVersion,
                            SettingsEnums.SETTINGS_HOMEPAGE);
        }
    }

    private void initAvatarView() {
        final ImageView avatarView = findViewById(R.id.account_avatar);
        final ImageView avatarTwoPaneView = findViewById(R.id.account_avatar_two_pane_version);
        if (AvatarViewMixin.isAvatarSupported(this)) {
            avatarView.setVisibility(View.VISIBLE);
            getLifecycle().addObserver(new AvatarViewMixin(this, avatarView));

            if (mIsEmbeddingActivityEnabled) {
                avatarTwoPaneView.setVisibility(View.VISIBLE);
                getLifecycle().addObserver(new AvatarViewMixin(this, avatarTwoPaneView));
            }
        }
    }

    private void updateHomepageBackground() {
        if (!mIsEmbeddingActivityEnabled) {
            return;
        }

        final Window window = getWindow();
        final int color = mIsTwoPane
                ? getColor(R.color.settings_two_pane_background_color)
                : Utils.getColorAttrDefaultColor(this, android.R.attr.colorBackground);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // Update status bar color
        window.setStatusBarColor(color);
        // Update content background.
        findViewById(android.R.id.content).setBackgroundColor(color);
    }

    private void showSuggestionFragment(boolean scrollNeeded) {
        final Class<? extends Fragment> fragmentClass = FeatureFactory.getFactory(this)
                .getSuggestionFeatureProvider(this).getContextualSuggestionFragment();
        if (fragmentClass == null) {
            return;
        }

        mSuggestionView = findViewById(R.id.suggestion_content);
        mTwoPaneSuggestionView = findViewById(R.id.two_pane_suggestion_content);
        mHomepageView = findViewById(R.id.settings_homepage_container);
        // Hide the homepage for preparing the suggestion. If scrolling is needed, the list views
        // should be initialized in the invisible homepage view to prevent a scroll flicker.
        mHomepageView.setVisibility(scrollNeeded ? View.INVISIBLE : View.GONE);
        // Schedule a timer to show the homepage and hide the suggestion on timeout.
        mHomepageView.postDelayed(() -> showHomepageWithSuggestion(false),
                HOMEPAGE_LOADING_TIMEOUT_MS);
        showFragment(new SuggestionFragCreator(fragmentClass, /* isTwoPaneLayout= */ false),
                R.id.suggestion_content);
        if (mIsEmbeddingActivityEnabled) {
            showFragment(new SuggestionFragCreator(fragmentClass, /* isTwoPaneLayout= */ true),
                    R.id.two_pane_suggestion_content);
        }
    }

    private <T extends Fragment> T showFragment(FragmentCreator<T> fragmentCreator, int id) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        T showFragment = (T) fragmentManager.findFragmentById(id);

        if (showFragment == null) {
            showFragment = fragmentCreator.create();
            fragmentCreator.init(showFragment);
            fragmentTransaction.add(id, showFragment);
        } else {
            fragmentCreator.init(showFragment);
            fragmentTransaction.show(showFragment);
        }
        fragmentTransaction.commit();
        return showFragment;
    }

    private void launchDeepLinkIntentToRight() {
        if (!mIsEmbeddingActivityEnabled) {
            return;
        }

        final Intent intent = getIntent();
        if (intent == null || !TextUtils.equals(intent.getAction(),
                ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)) {
            return;
        }

        if (!(this instanceof DeepLinkHomepageActivity
                || this instanceof DeepLinkHomepageActivityInternal)) {
            Log.e(TAG, "Not a deep link component");
            finish();
            return;
        }

        final String intentUriString = intent.getStringExtra(
                EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI);
        if (TextUtils.isEmpty(intentUriString)) {
            Log.e(TAG, "No EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI to deep link");
            finish();
            return;
        }

        final Intent targetIntent;
        try {
            targetIntent = Intent.parseUri(intentUriString, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to parse deep link intent: " + e);
            finish();
            return;
        }

        final ComponentName targetComponentName = targetIntent.resolveActivity(getPackageManager());
        if (targetComponentName == null) {
            Log.e(TAG, "No valid target for the deep link intent: " + targetIntent);
            finish();
            return;
        }

        ActivityInfo targetActivityInfo = null;
        try {
            targetActivityInfo = getPackageManager().getActivityInfo(targetComponentName,
                    /* flags= */ 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get target ActivityInfo: " + e);
            finish();
            return;
        }

        int callingUid = -1;
        try {
            callingUid = ActivityManager.getService().getLaunchedFromUid(getActivityToken());
        } catch (RemoteException re) {
            Log.e(TAG, "Not able to get callingUid: " + re);
            finish();
            return;
        }

        if (!hasPrivilegedAccess(callingUid, targetActivityInfo)) {
            if (!targetActivityInfo.exported) {
                Log.e(TAG, "Target Activity is not exported");
                finish();
                return;
            }

            if (!isCallingAppPermitted(targetActivityInfo.permission)) {
                Log.e(TAG, "Calling app must have the permission of deep link Activity");
                finish();
                return;
            }
        }

        targetIntent.setComponent(targetComponentName);

        // To prevent launchDeepLinkIntentToRight again for configuration change.
        intent.setAction(null);

        targetIntent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Sender of intent may want to send intent extra data to the destination of targetIntent.
        targetIntent.replaceExtras(intent);

        targetIntent.putExtra(EXTRA_IS_FROM_SETTINGS_HOMEPAGE, true);
        targetIntent.putExtra(SettingsActivity.EXTRA_IS_FROM_SLICE, false);

        targetIntent.setData(intent.getParcelableExtra(
                SettingsHomepageActivity.EXTRA_SETTINGS_LARGE_SCREEN_DEEP_LINK_INTENT_DATA));

        // Only allow FLAG_GRANT_READ/WRITE_URI_PERMISSION if calling app has the permission to
        // access specified Uri.
        int uriPermissionFlags = targetIntent.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (targetIntent.getData() != null
                && uriPermissionFlags != 0
                && checkUriPermission(targetIntent.getData(), /* pid= */ -1, callingUid,
                        uriPermissionFlags) == PackageManager.PERMISSION_DENIED) {
            Log.e(TAG, "Calling app must have the permission to access Uri and grant permission");
            finish();
            return;
        }

        // Set 2-pane pair rule for the deep link page.
        ActivityEmbeddingRulesController.registerTwoPanePairRule(this,
                new ComponentName(getApplicationContext(), getClass()),
                targetComponentName,
                targetIntent.getAction(),
                SplitRule.FINISH_ALWAYS,
                SplitRule.FINISH_ALWAYS,
                true /* clearTop */);
        ActivityEmbeddingRulesController.registerTwoPanePairRule(this,
                new ComponentName(getApplicationContext(), Settings.class),
                targetComponentName,
                targetIntent.getAction(),
                SplitRule.FINISH_ALWAYS,
                SplitRule.FINISH_ALWAYS,
                true /* clearTop */);

        final UserHandle user = intent.getParcelableExtra(EXTRA_USER_HANDLE, UserHandle.class);
        if (user != null) {
            startActivityAsUser(targetIntent, user);
        } else {
            startActivity(targetIntent);
        }
    }

    // Check if calling app has privileged access to launch Activity of activityInfo.
    private boolean hasPrivilegedAccess(int callingUid, ActivityInfo activityInfo) {
        if (TextUtils.equals(PasswordUtils.getCallingAppPackageName(getActivityToken()),
                    getPackageName())) {
            return true;
        }

        int targetUid = -1;
        try {
            targetUid = getPackageManager().getApplicationInfo(activityInfo.packageName,
                    /* flags= */ 0).uid;
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, "Not able to get targetUid: " + nnfe);
            return false;
        }

        // When activityInfo.exported is false, Activity still can be launched if applications have
        // the same user ID.
        if (UserHandle.isSameApp(callingUid, targetUid)) {
            return true;
        }

        // When activityInfo.exported is false, Activity still can be launched if calling app has
        // root or system privilege.
        int callingAppId = UserHandle.getAppId(callingUid);
        if (callingAppId == Process.ROOT_UID || callingAppId == Process.SYSTEM_UID) {
            return true;
        }

        return false;
    }

    @VisibleForTesting
    boolean isCallingAppPermitted(String permission) {
        return TextUtils.isEmpty(permission) || PasswordUtils.isCallingAppPermitted(
                this, getActivityToken(), permission);
    }

    private String getHighlightMenuKey() {
        final Intent intent = getIntent();
        if (intent != null && TextUtils.equals(intent.getAction(),
                ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)) {
            final String menuKey = intent.getStringExtra(
                    EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY);
            if (!TextUtils.isEmpty(menuKey)) {
                return maybeRemapMenuKey(menuKey);
            }
        }
        return getString(DEFAULT_HIGHLIGHT_MENU_KEY);
    }

    private String maybeRemapMenuKey(String menuKey) {
        boolean isPrivacyOrSecurityMenuKey =
                getString(R.string.menu_key_privacy).equals(menuKey)
                        || getString(R.string.menu_key_security).equals(menuKey);
        boolean isSafetyCenterMenuKey = getString(R.string.menu_key_safety_center).equals(menuKey);

        if (isPrivacyOrSecurityMenuKey && SafetyCenterManagerWrapper.get().isEnabled(this)) {
            return getString(R.string.menu_key_safety_center);
        }
        if (isSafetyCenterMenuKey && !SafetyCenterManagerWrapper.get().isEnabled(this)) {
            // We don't know if security or privacy, default to security as it is above.
            return getString(R.string.menu_key_security);
        }
        return menuKey;
    }

    @VisibleForTesting
    void reloadHighlightMenuKey() {
        mMainFragment.getArguments().putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY,
                getHighlightMenuKey());
        mMainFragment.reloadHighlightMenuKey();
    }

    private void initHomepageContainer() {
        final View view = findViewById(R.id.homepage_container);
        // Prevent inner RecyclerView gets focus and invokes scrolling.
        view.setFocusableInTouchMode(true);
        view.requestFocus();
    }

    private void updateHomepageAppBar() {
        if (!mIsEmbeddingActivityEnabled) {
            return;
        }
        updateAppBarMinHeight();
        if (mIsTwoPane) {
            findViewById(R.id.homepage_app_bar_regular_phone_view).setVisibility(View.GONE);
            findViewById(R.id.homepage_app_bar_two_pane_view).setVisibility(View.VISIBLE);
            findViewById(R.id.suggestion_container_two_pane).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.homepage_app_bar_regular_phone_view).setVisibility(View.VISIBLE);
            findViewById(R.id.homepage_app_bar_two_pane_view).setVisibility(View.GONE);
            findViewById(R.id.suggestion_container_two_pane).setVisibility(View.GONE);
        }
    }

    private void updateHomepagePaddings() {
        if (!mIsEmbeddingActivityEnabled) {
            return;
        }
        if (mIsTwoPane) {
            int padding = getResources().getDimensionPixelSize(
                    R.dimen.homepage_padding_horizontal_two_pane);
            mMainFragment.setPaddingHorizontal(padding);
        } else {
            mMainFragment.setPaddingHorizontal(0);
        }
        mMainFragment.updatePreferencePadding(mIsTwoPane);
    }

    private void updateAppBarMinHeight() {
        final int searchBarHeight = getResources().getDimensionPixelSize(R.dimen.search_bar_height);
        final int margin = getResources().getDimensionPixelSize(
                mIsEmbeddingActivityEnabled && mIsTwoPane
                        ? R.dimen.homepage_app_bar_padding_two_pane
                        : R.dimen.search_bar_margin);
        findViewById(R.id.app_bar_container).setMinimumHeight(searchBarHeight + margin * 2);
    }

    private static class SuggestionFragCreator implements FragmentCreator {

        private final Class<? extends Fragment> mClass;
        private final boolean mIsTwoPaneLayout;

        SuggestionFragCreator(Class<? extends Fragment> clazz, boolean isTwoPaneLayout) {
            mClass = clazz;
            mIsTwoPaneLayout = isTwoPaneLayout;
        }

        @Override
        public Fragment create() {
            try {
                Fragment fragment = mClass.getConstructor().newInstance();
                return fragment;
            } catch (Exception e) {
                Log.w(TAG, "Cannot show fragment", e);
            }
            return null;
        }

        @Override
        public void init(Fragment fragment) {
            if (fragment instanceof SplitLayoutListener) {
                ((SplitLayoutListener) fragment).setSplitLayoutSupported(mIsTwoPaneLayout);
            }
        }
    }
}
