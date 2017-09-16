package com.android.settings.testutils.shadow;

import com.android.settings.wrapper.KeyValueListParserWrapper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Shadow for {@link KeyValueListParserWrapperImpl} so we could implement
 * {@link #getBoolean(String, boolean)} that doesn't support in the current
 * robolectric
 */
@Implements(KeyValueListParserWrapper.class)
public class ShadowKeyValueListParserWrapperImpl {

    @Implementation
    public boolean getBoolean(String key, boolean defaultValue) {
        return defaultValue;
    }
}
