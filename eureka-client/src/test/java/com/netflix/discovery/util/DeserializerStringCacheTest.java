package com.netflix.discovery.util;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.netflix.discovery.util.DeserializerStringCache.CacheScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeserializerStringCacheTest {

    @Test
    void uppercaseConversionWithLowercasePreset() throws IOException {
        DeserializationContext deserializationContext = mock(DeserializationContext.class);
        DeserializerStringCache deserializerStringCache = DeserializerStringCache.from(deserializationContext);

        String lowerCaseValue = deserializerStringCache.apply("value", CacheScope.APPLICATION_SCOPE);
        assertThat(lowerCaseValue, is("value"));

        JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.getTextCharacters()).thenReturn(new char[] {'v', 'a', 'l', 'u', 'e'});
        when(jsonParser.getTextLength()).thenReturn(5);

        String upperCaseValue = deserializerStringCache.apply(jsonParser, CacheScope.APPLICATION_SCOPE, () -> "VALUE");
        assertThat(upperCaseValue, is("VALUE"));
    }

    @Test
    void uppercaseConversionWithLongString() throws IOException {
        DeserializationContext deserializationContext = mock(DeserializationContext.class);
        DeserializerStringCache deserializerStringCache = DeserializerStringCache.from(deserializationContext);
        char[] lowercaseValue = new char[1024];
        Arrays.fill(lowercaseValue, 'a');

        JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.getText()).thenReturn(new String(lowercaseValue));
        when(jsonParser.getTextCharacters()).thenReturn(lowercaseValue);
        when(jsonParser.getTextOffset()).thenReturn(0);
        when(jsonParser.getTextLength()).thenReturn(lowercaseValue.length);

        String upperCaseValue = deserializerStringCache.apply(jsonParser, CacheScope.APPLICATION_SCOPE, () -> {
            try {
                return jsonParser.getText().toUpperCase();
            }
            catch(IOException ioe) {
                // not likely from mock above
                throw new IllegalStateException("mock threw unexpected exception", ioe);
            }
        });
        char[] expectedValueChars = new char[1024];
        Arrays.fill(expectedValueChars, 'A');
        String expectedValue = new String(expectedValueChars);
        assertThat(upperCaseValue, is(expectedValue));
    }
}