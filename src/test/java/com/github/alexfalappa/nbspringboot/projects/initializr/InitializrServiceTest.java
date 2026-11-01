package com.github.alexfalappa.nbspringboot.projects.initializr;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class InitializrServiceTest {
    private HttpServer server;
    private InitializrService service;
    private final AtomicInteger metadataCalls = new AtomicInteger();
    private final AtomicInteger dependencyCalls = new AtomicInteger();

    @Before public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/dependencies")) dependencyCalls.incrementAndGet();
            if (path.equals("/")) metadataCalls.incrementAndGet();
            byte[] body = (path.equals("/starter.zip") ? "PK-project" : "{\"ok\":true}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", path.equals("/starter.zip") ? "application/octet-stream" : "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (java.io.OutputStream out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
        service = new InitializrService("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @After public void stopServer() { if (server != null) server.stop(0); }

    @Test public void cacheCanBeClearedAndReused() throws Exception {
        service.getMetadata(); service.getMetadata();
        service.getDependencies("3.5.7"); service.getDependencies("3.5.7");
        assertEquals(1, metadataCalls.get());
        assertEquals(1, dependencyCalls.get());
        service.clearCachedValues();
        service.getMetadata(); service.getDependencies("3.5.7");
        assertEquals(2, metadataCalls.get());
        assertEquals(2, dependencyCalls.get());
    }

    @Test public void downloadsModernProject() throws Exception {
        try (java.io.InputStream in = service.getProject("4.0.0", "com.example", "demo", "1.0", "Demo", "José & equipo", "jar", "com.example", "java", "21", "web")) {
            assertEquals("PK-project", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsIncompatibleJavaBeforeCallingService() throws Exception {
        service.getProject("4.0.0", "com.example", "demo", "1.0", "Demo", "Demo", "jar", "com.example", "java", "8", "web");
    }
}
