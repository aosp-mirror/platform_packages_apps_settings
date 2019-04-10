package com.android.settings.testutils.shadow;

import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.shadow.api.Shadow.directlyOn;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.util.SparseArray;

import androidx.annotation.ArrayRes;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowResources;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Shadow Resources and Theme classes to handle resource references that Robolectric shadows cannot
 * handle because they are too new or private.
 */
@Implements(value = Resources.class)
public class SettingsShadowResources extends ShadowResources {

    @RealObject
    public Resources realResources;

    private static SparseArray<Object> sResourceOverrides = new SparseArray<>();

    public static void overrideResource(int id, Object value) {
        sResourceOverrides.put(id, value);
    }

    public static void overrideResource(String name, Object value) {
        final Resources res = application.getResources();
        final int resId = res.getIdentifier(name, null, null);
        if (resId == 0) {
            throw new Resources.NotFoundException("Cannot override \"" + name + "\"");
        }
        overrideResource(resId, value);
    }

    @Resetter
    public static void reset() {
        sResourceOverrides.clear();
    }

    @Implementation
    protected int[] getIntArray(@ArrayRes int id) throws NotFoundException {
        final Object override = sResourceOverrides.get(id);
        if (override instanceof int[]) {
            return (int[]) override;
        }
        return directlyOn(realResources, Resources.class).getIntArray(id);
    }

    @Implementation
    protected String getString(int id) {
        final Object override = sResourceOverrides.get(id);
        if (override instanceof String) {
            return (String) override;
        }
        return directlyOn(
                realResources, Resources.class, "getString", ClassParameter.from(int.class, id));
    }

    @Implementation
    protected int getInteger(int id) {
        final Object override = sResourceOverrides.get(id);
        if (override instanceof Integer) {
            return (Integer) override;
        }
        return directlyOn(
                realResources, Resources.class, "getInteger", ClassParameter.from(int.class, id));
    }

    @Implementation
    protected boolean getBoolean(int id) {
        final Object override = sResourceOverrides.get(id);
        if (override instanceof Boolean) {
            return (boolean) override;
        }
        return directlyOn(realResources, Resources.class, "getBoolean",
                ClassParameter.from(int.class, id));
    }
}
