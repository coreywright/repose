package org.openrepose.core.services.reporting.metrics;

import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.impl.MetricsServiceContext;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletContextEvent;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class MetricsServiceContextTest {
    public static class EnabledDisabled {
        protected MetricsServiceContext metricsServiceContext;

        protected ServiceRegistry serviceRegistry;
        protected ConfigurationService configurationService;
        protected MetricsService metricsService;
        protected HealthCheckService healthCheckService;
        protected HealthCheckServiceProxy healthCheckServiceProxy;
        protected ServletContextEvent sce;


        @Before
        public void setUp() {
            serviceRegistry = mock(ServiceRegistry.class);
            configurationService = mock(ConfigurationService.class);
            metricsService = mock(MetricsService.class);
            healthCheckService = mock(HealthCheckService.class);
            healthCheckServiceProxy = mock(HealthCheckServiceProxy.class);
            when(healthCheckService.register()).thenReturn(healthCheckServiceProxy);
            metricsServiceContext = new MetricsServiceContext(serviceRegistry, configurationService, metricsService, healthCheckService);
            sce = mock(ServletContextEvent.class);
        }

        @Test
        public void testMetricsServiceEnabled() {
            when(metricsService.isEnabled()).thenReturn(true);

            assertNotNull(metricsServiceContext.getService());
        }

        @Test
        public void testMetricsServiceDisabled() {
            when(metricsService.isEnabled()).thenReturn(false);

            assertNull(metricsServiceContext.getService());
        }

        @Test
        public void verifyRegisteredToHealthCheckService() {

            verify(healthCheckService, times(1)).register();
        }

        @Test
        public void verifyIssueReported() throws IOException {

            ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
            ConfigurationResource configurationResource = mock(ConfigurationResource.class);
            when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
            when(resourceResolver.resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
            when(configurationService.getResourceResolver().resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

            when(configurationResource.exists()).thenReturn(false);
            metricsServiceContext.contextInitialized(sce);
            verify(healthCheckServiceProxy, times(1)).reportIssue(any(String.class), any(String.class), any(Severity.class));
        }
    }
}
