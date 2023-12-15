package com.netflix.discovery.converters.wrappers;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author David Liu
 */
class CodecWrappersTest {

    private static String testWrapperName = "FOO_WRAPPER";

    @Test
    void registerNewWrapper() {
        assertNull(CodecWrappers.getEncoder(testWrapperName));
        assertNull(CodecWrappers.getDecoder(testWrapperName));

        CodecWrappers.registerWrapper(new TestWrapper());

        assertNotNull(CodecWrappers.getEncoder(testWrapperName));
        assertNotNull(CodecWrappers.getDecoder(testWrapperName));
    }

    private final class TestWrapper implements CodecWrapper {

        @Override
        public <T> T decode(String textValue, Class<T> type) throws IOException {
            return null;
        }

        @Override
        public <T> T decode(InputStream inputStream, Class<T> type) throws IOException {
            return null;
        }

        @Override
        public <T> String encode(T object) throws IOException {
            return null;
        }

        @Override
        public <T> void encode(T object, OutputStream outputStream) throws IOException {

        }

        @Override
        public String codecName() {
            return testWrapperName;
        }

        @Override
        public boolean support(MediaType mediaType) {
            return false;
        }
    }
}
