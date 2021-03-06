package org.openrepose.core.filter;

import org.openrepose.core.services.event.PowerFilterEvent;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class PowerFilterEventTest {
    public static class WhenUsingPowerFilterEvent {
        @Test
        public void shouldBeUsable() {
            PowerFilterEvent filterEvent = PowerFilterEvent.POWER_FILTER_CONFIGURED;
            assertEquals(PowerFilterEvent.POWER_FILTER_CONFIGURED, filterEvent);
        }
    }
}
