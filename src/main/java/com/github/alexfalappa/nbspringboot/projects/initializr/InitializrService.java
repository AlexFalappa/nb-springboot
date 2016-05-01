/*
 * Copyright 2016 Alessandro Falappa <alex.falappa at gmail.com>.
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

import org.openide.util.NbPreferences;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.PREF_INITIALIZR_URL;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.REST_USER_AGENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

/**
 *
 * @author Alessandro Falappa <alex.falappa at gmail.com>
 */
public class InitializrService {

    private final RestTemplate rt = new RestTemplate();

    public JsonNode getMetadata() throws Exception {
        final String serviceUrl = NbPreferences.forModule(InitializrService.class).get(PREF_INITIALIZR_URL, "http://start.spring.io");
        RequestEntity<Void> req = RequestEntity
                .get(new URI(serviceUrl))
                .accept(APPLICATION_JSON)
                .header("User-Agent", REST_USER_AGENT)
                .build();
        ResponseEntity<String> respEntity = rt.exchange(req, String.class);
        final HttpStatus statusCode = respEntity.getStatusCode();
        if (statusCode == OK) {
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.readTree(respEntity.getBody());
        } else {
            // TODO log status code
            // throw exception in order to set error message
            throw new RuntimeException(String.format("Service malfunction. HTTP status code: %s", statusCode.toString()));
        }
    }

    public InputStream getProject(String bootVersion, String mvnGroup, String mvnArtifact, String mvnVersion, String mvnName, String mvnDesc, String packaging, String pkg, String lang, String javaVersion, String deps) throws Exception {
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
        System.out.println(uri.toString());
        RequestEntity<Void> req = RequestEntity
                .get(uri)
                .accept(APPLICATION_OCTET_STREAM)
                .header("User-Agent", REST_USER_AGENT)
                .build();
        ResponseEntity<byte[]> respEntity = rt.exchange(req, byte[].class);
        final HttpStatus statusCode = respEntity.getStatusCode();
        if (statusCode == OK) {
            return new ByteArrayInputStream(respEntity.getBody());
        } else {
            // TODO log status code
            // throw exception in order to set error message
            throw new RuntimeException(String.format("Service malfunction. HTTP status code: %s", statusCode.toString()));
        }
    }

}
