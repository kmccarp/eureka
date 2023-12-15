package com.netflix.discovery.internal.util;

import com.netflix.archaius.api.Config;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author David Liu
 */
class InternalPrefixedConfigTest {

    @Test
    void prefixes() {
        Config configInstance = Mockito.mock(Config.class);

        InternalPrefixedConfig config = new InternalPrefixedConfig(configInstance);
        assertEquals("", config.getNamespace());

        config = new InternalPrefixedConfig(configInstance, "foo");
        assertEquals("foo.", config.getNamespace());

        config = new InternalPrefixedConfig(configInstance, "foo", "bar");
        assertEquals("foo.bar.", config.getNamespace());
    }
}
