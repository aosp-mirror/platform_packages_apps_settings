package com.android.settings.testutils.shadow;

import android.annotation.DimenRes;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.res.StyleData;
import org.robolectric.res.StyleResolver;
import org.robolectric.res.builder.XmlResourceParserImpl;
import org.robolectric.shadows.ShadowAssetManager;
import org.robolectric.shadows.ShadowResources;
import org.robolectric.util.ReflectionHelpers;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

import static android.util.TypedValue.TYPE_REFERENCE;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.internal.Shadow.directlyOn;

/**
 * Shadow Resources and Theme classes to handle resource references that Robolectric shadows cannot
 * handle because they are too new or private.
 */
@Implements(Resources.class)
public class SettingsShadowResources extends ShadowResources {

    @RealObject Resources realResources;

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
    public Drawable loadDrawable(TypedValue value, int id, Theme theme)
            throws NotFoundException {
        // The drawable item in switchbar_background.xml refers to a very recent color attribute
        // that Robolectric isn't yet aware of.
        // TODO: Remove this once Robolectric is updated.
        if (id == com.android.settings.R.drawable.switchbar_background) {
            return new ColorDrawable();
        }
        return super.loadDrawable(value, id, theme);
    }

    @Implements(Theme.class)
    public static class SettingsShadowTheme extends ShadowTheme {

        @RealObject
        Theme realTheme;

        @Implementation
        public TypedArray obtainStyledAttributes(
                AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
            // Replace all private string references with a placeholder.
            if (set != null) {
                for (int i = 0; i < set.getAttributeCount(); ++i) {
                    if (set.getAttributeValue(i).startsWith("@*android:string")) {
                        Node node = ReflectionHelpers.callInstanceMethod(
                                XmlResourceParserImpl.class, set, "getAttributeAt",
                                ReflectionHelpers.ClassParameter.from(int.class, i));
                        node.setNodeValue("PLACEHOLDER");
                    }
                }
            }

            // Track down all styles and remove all inheritance from private styles.
            ShadowAssetManager assetManager = shadowOf(RuntimeEnvironment.application.getAssets());
            // The Object's below are actually ShadowAssetManager.OverlayedStyle. We can't use it
            // here because it's package private.
            Map<Long, List<Object>> appliedStylesList =
                    ReflectionHelpers.getField(assetManager, "appliedStyles");
            for (Long idx : appliedStylesList.keySet()) {
                List<Object> appliedStyles = appliedStylesList.get(idx);
                int i = 1;
                for (Object appliedStyle : appliedStyles) {
                    StyleResolver styleResolver = ReflectionHelpers.getField(appliedStyle, "style");
                    List<StyleData> styleDatas =
                            ReflectionHelpers.getField(styleResolver, "styles");
                    for (StyleData styleData : styleDatas) {
                        if (styleData.getParent() != null &&
                                styleData.getParent().startsWith("@*android:style")) {
                            ReflectionHelpers.setField(StyleData.class, styleData, "parent", null);
                        }
                    }
                }

            }
            return super.obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes);
        }

        @Implementation
        public boolean resolveAttribute(int resid, TypedValue outValue, boolean resolveRefs) {
            // The real Resources instance in Robolectric tests somehow fails to find the
            // preferenceTheme attribute in the layout. Let's do it ourselves.
            if (getResources().getResourceName(resid)
                    .equals("com.android.settings:attr/preferenceTheme")) {
                int preferenceThemeResId =
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
    }
}
