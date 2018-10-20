package com.DevDashboard.demo;

import static org.junit.Assert.*;
import org.junit.Test;

public class SpringControllerTest {

    @Test
    public void testGetMetrics() {
        SpringController sc = new SpringController();      
        assertTrue(sc.getCodeCoverage(/*Developer ID*/) == /*Code Coverage*/);
        assertTrue(sc.getPrs(/*Developer ID*/) == /*Number of PRs*/);
        assertTrue(sc.getCriticalIssues(/*Developer ID*/)==/*Number of Critical Issues*/);
    }

}
