package com.netflix.discovery;

import jakarta.inject.Provider;

import com.netflix.discovery.shared.transport.jersey.Jersey1DiscoveryClientOptionalArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netflix.appinfo.HealthCheckCallback;
import com.netflix.appinfo.HealthCheckHandler;

/**
 * @author Matt Nelson
 */
class Jersey1DiscoveryClientOptionalArgsTest {
    
    private Jersey1DiscoveryClientOptionalArgs args;

    @BeforeEach
    void before() {
        args = new Jersey1DiscoveryClientOptionalArgs();
    }

    @Test
    void healthCheckCallbackGuiceProvider() {
        args.setHealthCheckCallbackProvider(new GuiceProvider<HealthCheckCallback>());
    }

    @Test
    void healthCheckCallbackJavaxProvider() {
        args.setHealthCheckCallbackProvider(new JavaxProvider<HealthCheckCallback>());
    }

    @Test
    void healthCheckHandlerGuiceProvider() {
        args.setHealthCheckHandlerProvider(new GuiceProvider<HealthCheckHandler>());
    }

    @Test
    void healthCheckHandlerJavaxProvider() {
        args.setHealthCheckHandlerProvider(new JavaxProvider<HealthCheckHandler>());
    }
    
    private class JavaxProvider<T> implements Provider<T> {
        @Override
        public T get() {
            return null;
        }
    }
    
    private class GuiceProvider<T> implements com.google.inject.Provider<T> {
        
        @Override
        public T get() {
            return null;
        }
    }
}
