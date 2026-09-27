package org.interester.data;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyConfigTest {

    @Test
    void withCredentials() {
        ProxyConfig p = ProxyConfig.fromEnv(Map.of("HTTPS_PROXY", "http://me:p%40ss@localhost:55200")).orElseThrow();

        assertEquals(new ProxyConfig("localhost", 55200, "me", "p@ss"), p);
        assertTrue(p.authenticator().isPresent());
        assertFalse(p.toString().contains("p@ss"), "password must not be printed");
    }

    @Test
    void lowercaseNoSchemeOrNone() {
        ProxyConfig p = ProxyConfig.fromEnv(Map.of("https_proxy", "proxy.local:3128")).orElseThrow();

        assertEquals(new ProxyConfig("proxy.local", 3128, null, null), p);
        assertTrue(p.authenticator().isEmpty());
        assertEquals(Optional.empty(), ProxyConfig.fromEnv(Map.of()));
        assertEquals(Optional.empty(), ProxyConfig.fromEnv(Map.of("HTTPS_PROXY", " ")));
    }
}
