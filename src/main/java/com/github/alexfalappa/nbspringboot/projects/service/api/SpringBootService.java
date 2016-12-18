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
package com.github.alexfalappa.nbspringboot.projects.service.api;

import java.util.List;

import org.netbeans.api.java.classpath.ClassPath;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Service API for implementing support of Spring Boot related functionalities.
 * <p>
 * configuration properties metadata.
 *
 * @author Alessandro Falappa
 */
public interface SpringBootService {

    void init();

    boolean cfgPropsCompletionEnabled();

    public ItemHint getHintMetadata(String propertyName);

    public List<ItemHint.ValueHint> queryHintMetadata(String propertyName, String filter);

    public List<ItemMetadata> getPropertyMetadata(String propertyName);

    public List<ItemMetadata> queryPropertyMetadata(String filter);

    public ClassPath getManagedClassPath();
}
