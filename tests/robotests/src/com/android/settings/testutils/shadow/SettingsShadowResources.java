package com.android.settings.testutils.shadow;

import static android.util.TypedValue.TYPE_REFERENCE;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadow.api.Shadow.directlyOn;

import android.annotation.DimenRes;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.ArrayRes;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;

import com.android.settings.R;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.XmlResourceParserImpl;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.res.StyleData;
import org.robolectric.res.StyleResolver;
import org.robolectric.res.ThemeStyleSet;
import org.robolectric.shadows.ShadowAssetManager;
import org.robolectric.shadows.ShadowResources;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

/**
 * Shadow Resources and Theme classes to handle resource references that Robolectric shadows cannot
 * handle because they are too new or private.
 */
@Implements(value = Resources.class, inheritImplementationMethods = true)
public class SettingsShadowResources extends ShadowResources {

    @RealObject
    public Resources realResources;

    private static SparseArray<Object> sResourceOverrides = new SparseArray<>();

    public static void overrideResource(int id, Object value) {
        synchronized (sResourceOverrides) {
            sResourceOverrides.put(id, value);
        }
    }

    public static void overrideResource(String name, Object value) {
        final Resources res = application.getResources();
        final int resId = res.getIdentifier(name, null, null);
        if (resId == 0) {
            throw new Resources.NotFoundException("Cannot override \"" + name + "\"");
        }
        overrideResource(resId, value);
    }

    public static void reset() {
        synchronized (sResourceOverrides) {
            sResourceOverrides.clear();
        }
    }

    @Implementation
    public int getDimensionPixelSize(@DimenRes int id) throws NotFoundException {
        // Handle requests for private dimension resources,
        // TODO: Consider making a set of private dimension resource ids if this happens repeatedly.
        if (id == com.android.internal.R.dimen.preference_fragment_padding_bottom) {
            return 0;
        }
        return directlyOn(realResources, Resources.class).getDimensionPixelSize(id);
    }

    @Implementation
    public int getColor(@ColorRes int id, @Nullable Theme theme) throws NotFoundException {
        if (id == R.color.battery_icon_color_error) {
            return Color.WHITE;
        }
        return directlyOn(realResources, Resources.class).getColor(id, theme);
    }

    @Implementation
    public ColorStateList getColorStateList(@ColorRes int id, @Nullable Theme theme)
            throws NotFoundException {
        if (id == com.android.internal.R.color.text_color_primary) {
            return ColorStateList.valueOf(Color.WHITE);
        }
        return directlyOn(realResources, Resources.class).getColorStateList(id, theme);
    }

    /**
     * Deprecated because SDK 24+ uses
     * {@link SettingsShadowResourcesImpl#loadDrawable(Resources, TypedValue, int, int, Theme)}
     *
     * TODO: Delete when all tests have been migrated to sdk 26
     */
    @Deprecated
    @Implementation
    public Drawable loadDrawable(TypedValue value, int id, Theme theme)
            throws NotFoundException {
        // The drawable item in switchbar_background.xml refers to a very recent color attribute
        // that Robolectric isn't yet aware of.
        // TODO: Remove this once Robolectric is updated.
        if (id == R.drawable.switchbar_background) {
            return new ColorDrawable();
        } else if (id == R.drawable.ic_launcher_settings) {
            // ic_launcher_settings uses adaptive-icon, which is not supported by robolectric,
            // change it to a normal drawable.
            id = R.drawable.ic_settings_wireless;
        } else if (id == R.drawable.app_filter_spinner_background) {
            id = R.drawable.ic_expand_more_inverse;
        }
        return super.loadDrawable(value, id, theme);
    }

    @Implementation
    public int[] getIntArray(@ArrayRes int id) throws NotFoundException {
        // The Robolectric isn't aware of resources in settingslib, so we need to stub it here
        if (id == com.android.settings.R.array.batterymeter_bolt_points
                || id == com.android.settings.R.array.batterymeter_plus_points) {
            return new int[2];
        }

        final Object override;
        synchronized (sResourceOverrides) {
            override = sResourceOverrides.get(id);
        }
        if (override instanceof int[]) {
            return (int[]) override;
        }
        return directlyOn(realResources, Resources.class).getIntArray(id);
    }

    @Implementation
    public String getString(int id) {
        final Object override;
        synchronized (sResourceOverrides) {
            override = sResourceOverrides.get(id);
        }
        if (override instanceof String) {
            return (String) override;
        }
        return directlyOn(
                realResources, Resources.class, "getString", ClassParameter.from(int.class, id));
    }

    @Implementation
    public int getInteger(int id) {
        final Object override;
        synchronized (sResourceOverrides) {
            override = sResourceOverrides.get(id);
        }
        if (override instanceof Integer) {
            return (Integer) override;
        }
        return directlyOn(
                realResources, Resources.class, "getInteger", ClassParameter.from(int.class, id));
    }

    @Implementation
    public boolean getBoolean(int id) {
        final Object override;
        synchronized (sResourceOverrides) {
            override = sResourceOverrides.get(id);
        }
        if (override instanceof Boolean) {
            return (boolean) override;
        }
        return directlyOn(realResources, Resources.class, "getBoolean",
                ClassParameter.from(int.class, id));
    }

    @Implements(value = Theme.class, inheritImplementationMethods = true)
    public static class SettingsShadowTheme extends ShadowTheme {

        @RealObject
        Theme realTheme;

        private ShadowAssetManager mAssetManager = shadowOf(
                RuntimeEnvironment.application.getAssets());

        @Implementation
        public TypedArray obtainStyledAttributes(
                AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
            // Replace all private string references with a placeholder.
            if (set != null) {
                synchronized (set) {
                    for (int i = 0; i < set.getAttributeCount(); ++i) {
                        final String attributeValue = set.getAttributeValue(i);
                        final Node node = ReflectionHelpers.callInstanceMethod(
                                XmlResourceParserImpl.class, set, "getAttributeAt",
                                ReflectionHelpers.ClassParameter.from(int.class, i));
                        if (attributeValue.contains("attr/fingerprint_layout_theme")) {
                            // Workaround for https://github.com/robolectric/robolectric/issues/2641
                            node.setNodeValue("@style/FingerprintLayoutTheme");
                        } else if (attributeValue.startsWith("@*android:string")) {
                            node.setNodeValue("PLACEHOLDER");
                        }
                    }
                }
            }

            // Track down all styles and remove all inheritance from private styles.
            final Map<Long, Object /* NativeTheme */> appliedStylesList =
                    ReflectionHelpers.getField(mAssetManager, "nativeThemes");
            synchronized (appliedStylesList) {
                for (Long idx : appliedStylesList.keySet()) {
                    final ThemeStyleSet appliedStyles = ReflectionHelpers.getField(
                            appliedStylesList.get(idx), "themeStyleSet");
                    // The Object's below are actually ShadowAssetManager.OverlayedStyle.
                    // We can't use

                    // it here because it's private.
                    final List<Object /* OverlayedStyle */> overlayedStyles =
                            ReflectionHelpers.getField(appliedStyles, "styles");
                    for (Object appliedStyle : overlayedStyles) {
                        final StyleResolver styleResolver = ReflectionHelpers.getField(appliedStyle,
                                "style");
                        final List<StyleData> styleDatas =
                                ReflectionHelpers.getField(styleResolver, "styles");
                        for (StyleData styleData : styleDatas) {
                            if (styleData.getParent() != null &&
                                    styleData.getParent().startsWith("@*android:style")) {
                                ReflectionHelpers.setField(StyleData.class, styleData, "parent",
                                        null);
                            }
                        }
                    }

                }
            }
            return super.obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes);
        }

        @Implementation
        public synchronized boolean resolveAttribute(int resid, TypedValue outValue,
                boolean resolveRefs) {
            // The real Resources instance in Robolectric tests somehow fails to find the
            // preferenceTheme attribute in the layout. Let's do it ourselves.
            if (getResources().getResourceName(resid)
                    .equals("com.android.settings:attr/preferenceTheme")) {
                final int preferenceThemeResId =
                        getResources().getIdentifier(
                                "PreferenceTheme", "style", "com.android.settings");
                outValue.type = TYPE_REFERENCE;
                outValue.data = preferenceThemeResId;
                outValue.resourceId = preferenceThemeResId;
                return true;
            }
            return directlyOn(realTheme, Theme.class)
                    .resolveAttribute(resid, outValue, resolveRefs);
        }

        private Resources getResources() {
            return ReflectionHelpers.callInstanceMethod(ShadowTheme.class, this, "getResources");
        }
    }
}
