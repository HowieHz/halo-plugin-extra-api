package top.howiehz.halo.plugin.extra.api.service.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@ExtendWith(MockitoExtension.class)
class AbstractPluginConfigSupplierTest {
    @Mock
    private ReactiveSettingFetcher fetcher;

    private TestConfigSupplier supplier;

    @BeforeEach
    void setUp() {
        supplier = new TestConfigSupplier(fetcher);
    }

    @Test
    void shouldReturnFetchedConfig() {
        TestConfig config = new TestConfig();
        config.setEnabled(true);
        when(fetcher.fetch("test", TestConfig.class)).thenReturn(Mono.just(config));

        TestConfig result = supplier.get().block();

        assertTrue(result.isEnabled());
        verify(fetcher, times(1)).fetch("test", TestConfig.class);
    }

    @Test
    void shouldFallbackToFailSafeConfigWhenFetcherIsEmpty() {
        when(fetcher.fetch("test", TestConfig.class)).thenReturn(Mono.empty());

        TestConfig result = supplier.get().block();

        assertFalse(result.isEnabled());
        assertEquals("default", result.getName());
    }

    @Test
    void shouldNormalizeFetchedConfig() {
        TestConfig config = new TestConfig();
        when(fetcher.fetch("test", TestConfig.class)).thenReturn(Mono.just(config));

        TestConfig result = supplier.get().block();

        assertEquals("normalized", result.getName());
    }

    @Test
    void shouldFallbackToFailSafeConfigWhenFetcherFails() {
        when(fetcher.fetch("test", TestConfig.class))
            .thenReturn(Mono.error(new RuntimeException("boom")));

        TestConfig result = supplier.get().block();

        assertFalse(result.isEnabled());
        assertEquals("default", result.getName());
    }

    private static class TestConfigSupplier extends AbstractPluginConfigSupplier<TestConfig> {
        TestConfigSupplier(ReactiveSettingFetcher fetcher) {
            super(fetcher);
        }

        @Override
        protected String configKey() {
            return "test";
        }

        @Override
        protected Class<TestConfig> configType() {
            return TestConfig.class;
        }

        @Override
        protected TestConfig fallbackConfig() {
            TestConfig config = new TestConfig();
            config.setName("default");
            return config;
        }

        @Override
        protected TestConfig normalizeConfig(TestConfig config) {
            TestConfig normalized = super.normalizeConfig(config);
            if (normalized.getName() == null) {
                normalized.setName("normalized");
            }
            return normalized;
        }
    }

    private static class TestConfig {
        private boolean enabled;
        private String name;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
