/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.alexfalappa.nbspringboot.projects.initializr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.LRUMap;
import org.openide.util.NbPreferences;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.alexfalappa.nbspringboot.PrefConstants;

import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_INITIALIZR_TIMEOUT;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_INITIALIZR_URL;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.REST_USER_AGENT;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

/**
 * Helper singleton class managing and centralizing connection to the Spring Initializr service.
 * <p>
 * Caches project generation and dependencies metadata.
 *
 * @author Alessandro Falappa
 */
public class InitializrService {

    private static final Logger logger = Logger.getLogger(InitializrService.class.getName());
    private final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    private final RestTemplate rt = new RestTemplate(requestFactory);
    private JsonNode metadata;
    private Map<String, JsonNode> dependencyMetaMap = new LRUMap<>(6);

    private InitializrService() {
    }

    public static InitializrService getInstance() {
        return InitializrServiceHolder.INSTANCE;
    }

    private static class InitializrServiceHolder {

        private static final InitializrService INSTANCE = new InitializrService();
    }

    public void clearCachedValues() {
        metadata = null;
        dependencyMetaMap = null;
    }

    public JsonNode getMetadata() throws Exception {
        if (metadata == null) {
            // set connection timeouts
            timeoutFromPrefs();
            // prepare request
            final String serviceUrl = NbPreferences.forModule(PrefConstants.class).get(PREF_INITIALIZR_URL,
                    PrefConstants.DEFAULT_INITIALIZR_URL);
            RequestEntity<Void> req = RequestEntity
                    .get(new URI(serviceUrl))
                    .accept(MediaType.valueOf("application/vnd.initializr.v2.2+json"))
                    .header("User-Agent", REST_USER_AGENT)
                    .build();
            // connect
            logger.log(INFO, "Getting Spring Initializr metadata from: {0}", serviceUrl);
            logger.log(INFO, "Asking metadata as: {0}", REST_USER_AGENT);
            long start = System.currentTimeMillis();
            ResponseEntity<String> respEntity = rt.exchange(req, String.class);
            // analyze response
            final HttpStatus statusCode = respEntity.getStatusCode();
            if (statusCode == OK) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                metadata = mapper.readTree(respEntity.getBody());
                logger.log(INFO, "Retrieved Spring Initializr service metadata. Took {0} msec",
                        System.currentTimeMillis() - start);
                if (logger.isLoggable(FINE)) {
                    logger.fine(mapper.writeValueAsString(metadata));
                }
            } else {
                // log status code
                final String errMessage = String.format("Spring initializr service connection problem. HTTP status code: %s",
                        statusCode.toString());
                logger.severe(errMessage);
                // throw exception in order to set error message
                throw new RuntimeException(errMessage);
            }
        }
        return metadata;
    }

    public JsonNode getDependencies(String bootVersion) throws Exception {
        if (!dependencyMetaMap.containsKey(bootVersion)) {
            // set connection timeouts
            timeoutFromPrefs();
            // prepare request
            final String serviceUrl = NbPreferences.forModule(PrefConstants.class).get(PREF_INITIALIZR_URL,
                    PrefConstants.DEFAULT_INITIALIZR_URL);
            UriTemplate template = new UriTemplate(serviceUrl.concat("/dependencies?bootVersion={bootVersion}"));
            RequestEntity<Void> req = RequestEntity
                    .get(template.expand(bootVersion))
                    .accept(MediaType.valueOf("application/vnd.initializr.v2.1+json"))
                    .header("User-Agent", REST_USER_AGENT)
                    .build();
            // connect
            logger.log(INFO, "Getting Spring Initializr dependencies metadata from: {0}", template);
            logger.log(INFO, "Asking metadata as: {0}", REST_USER_AGENT);
            long start = System.currentTimeMillis();
            ResponseEntity<String> respEntity = rt.exchange(req, String.class);
            // analyze response
            final HttpStatus statusCode = respEntity.getStatusCode();
            if (statusCode == OK) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                final JsonNode depMeta = mapper.readTree(respEntity.getBody());
                logger.log(INFO, "Retrieved Spring Initializr dependencies metadata for boot version {0}. Took {1} msec",
                        new Object[]{bootVersion, System.currentTimeMillis() - start});
                if (logger.isLoggable(FINE)) {
                    logger.fine(mapper.writeValueAsString(depMeta));
                }
                dependencyMetaMap.put(bootVersion, depMeta);
            } else {
                // log status code
                final String errMessage = String.format("Spring initializr service connection problem. HTTP status code: %s",
                        statusCode.toString());
                logger.severe(errMessage);
                // throw exception in order to set error message
                throw new RuntimeException(errMessage);
            }
        }
        return dependencyMetaMap.get(bootVersion);
    }

    public InputStream getProject(String bootVersion, String mvnGroup, String mvnArtifact, String mvnVersion, String mvnName,
            String mvnDesc, String packaging, String pkg, String lang, String javaVersion, String deps) throws Exception {
        // set connection timeouts
        timeoutFromPrefs();
        // prepare parameterized url
        final String serviceUrl = NbPreferences.forModule(PrefConstants.class).get(PREF_INITIALIZR_URL,
                PrefConstants.DEFAULT_INITIALIZR_URL);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serviceUrl.concat("/starter.zip"))
                .queryParam("type", "maven-project")
                .queryParam("bootVersion", bootVersion)
                .queryParam("groupId", mvnGroup)
                .queryParam("artifactId", mvnArtifact)
                .queryParam("version", mvnVersion)
                .queryParam("packaging", packaging)
                .queryParam("name", mvnName)
                .queryParam("description", mvnDesc)
                .queryParam("language", lang)
                .queryParam("javaVersion", javaVersion)
                .queryParam("packageName", pkg)
                .queryParam("dependencies", deps);
        final URI uri = builder.build().encode().toUri();
        // setup request object
        RequestEntity<Void> req = RequestEntity
                .get(uri)
                .accept(APPLICATION_OCTET_STREAM)
                .header("User-Agent", REST_USER_AGENT)
                .build();
        // connect
        logger.info("Getting Spring Initializr project");
        logger.log(INFO, "Service URL: {0}", uri.toString());
        long start = System.currentTimeMillis();
        ResponseEntity<byte[]> respEntity = rt.exchange(req, byte[].class);
        // analyze response outcome
        final HttpStatus statusCode = respEntity.getStatusCode();
        if (statusCode == OK) {
            final ByteArrayInputStream stream = new ByteArrayInputStream(respEntity.getBody());
            logger.log(INFO, "Retrieved archived project from Spring Initializr service. Took {0} msec",
                    System.currentTimeMillis() - start);
            return stream;
        } else {
            // log status code
            final String errMessage = String.format("Spring initializr service connection problem. HTTP status code: %s",
                    statusCode.toString());
            logger.severe(errMessage);
            // throw exception in order to set error message
            throw new RuntimeException(errMessage);
        }
    }

    private void timeoutFromPrefs() {
        final int serviceTimeoutMillis = 1000 * NbPreferences.forModule(PrefConstants.class).getInt(PREF_INITIALIZR_TIMEOUT, 30);
        requestFactory.setConnectTimeout(serviceTimeoutMillis);
        requestFactory.setReadTimeout(serviceTimeoutMillis);
    }

}
