package com.android.settings.testutils.shadow;

import static org.robolectric.shadow.api.Shadow.directlyOn;

import com.android.internal.util.XmlUtils;

import org.robolectric.Robolectric;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

@Implements(XmlUtils.class)
public class ShadowXmlUtils {

    @Implementation
    public static final int convertValueToInt(CharSequence charSeq, int defaultValue) {
        final Class<?> xmlUtilsClass = ReflectionHelpers.loadClass(
                Robolectric.class.getClassLoader(), "com.android.internal.util.XmlUtils");
        try {
            return directlyOn(xmlUtilsClass, "convertValueToInt",
                    ClassParameter.from(CharSequence.class, charSeq),
                    ClassParameter.from(int.class, new Integer(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
