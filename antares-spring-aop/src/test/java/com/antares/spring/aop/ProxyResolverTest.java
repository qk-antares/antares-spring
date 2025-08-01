package com.antares.spring.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class ProxyResolverTest {
    @Test
    public void testProxyResolver() {
        OriginBean origin = new OriginBean();
        origin.name = "Bob";

        assertEquals("Hello, Bob.", origin.hello());

        // create proxy:
        OriginBean proxy = new ProxyResolver().createProxy(origin, new PoliteInvocationHandler());

        // Proxy类名,类似OriginBean$ByteBuddy$9hQwRy3T:
        System.out.println(proxy.getClass().getName());

        // proxy class, not origin class:
        assertNotSame(OriginBean.class, proxy.getClass());
        // proxy.name is null:
        assertNull(proxy.name);

        // 带@Polite:
        assertEquals("Hello, Bob!", proxy.hello());
        // 不带@Polite:
        assertEquals("Morning, Bob.", proxy.morning());
    }
}
