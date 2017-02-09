/*
 * Copyright 2016 Alessandro Falappa.
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
import java.util.logging.Logger;

import org.openide.util.NbPreferences;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_INITIALIZR_TIMEOUT;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_INITIALIZR_URL;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.REST_USER_AGENT;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

/**
 * Helper class managing and centralizing connection to the Spring Initializr service.
 *
 * @author Alessandro Falappa
 */
public class InitializrService {

    private static final Logger logger = Logger.getLogger(InitializrService.class.getName());
    private final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    private final RestTemplate rt = new RestTemplate(requestFactory);

    public JsonNode getMetadata() throws Exception {
        // set connection timeouts
        timeoutFromPrefs();
        // prepare request
        final String serviceUrl = NbPreferences.forModule(InitializrService.class).get(PREF_INITIALIZR_URL, "http://start.spring.io");
        RequestEntity<Void> req = RequestEntity
                .get(new URI(serviceUrl))
                .accept(APPLICATION_JSON)
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
            final JsonNode json = mapper.readTree(respEntity.getBody());
            logger.log(INFO, "Retrieved Spring Initializr service metadata. Took {0} msec", System.currentTimeMillis() - start);
            if (logger.isLoggable(FINE)) {
                logger.fine(mapper.writeValueAsString(json));
            }
            return json;
        } else {
            // log status code
            final String errMessage = String.format("Spring initializr service connection problem. HTTP status code: %s", statusCode
                    .toString());
            logger.severe(errMessage);
            // throw exception in order to set error message
            throw new RuntimeException(errMessage);
        }
    }

    public InputStream getProject(String bootVersion, String mvnGroup, String mvnArtifact, String mvnVersion, String mvnName, String mvnDesc,
            String packaging, String pkg, String lang, String javaVersion, String deps) throws Exception {
        // set connection timeouts
        timeoutFromPrefs();
        // prepare parameterized url
        final String serviceUrl = NbPreferences.forModule(InitializrService.class).get(PREF_INITIALIZR_URL, "http://start.spring.io");
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
            final String errMessage = String.format("Spring initializr service connection problem. HTTP status code: %s", statusCode
                    .toString());
            logger.severe(errMessage);
            // throw exception in order to set error message
            throw new RuntimeException(errMessage);
        }
    }

    private void timeoutFromPrefs() {
        final int serviceTimeoutMillis = 1000 * NbPreferences.forModule(InitializrService.class).getInt(PREF_INITIALIZR_TIMEOUT, 30);
        requestFactory.setConnectTimeout(serviceTimeoutMillis);
        requestFactory.setReadTimeout(serviceTimeoutMillis);
    }

}
