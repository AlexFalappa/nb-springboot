/*
 * Copyright 2020 the original author or authors.
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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections4.ComparatorUtils;
import org.springframework.util.comparator.Comparators;

/**
 * Represents an artifact version of the form {@code MAJOR.MINOR.PATCH[-MODIFIER]} or {@code MAJOR.MINOR.PATCH[.MODIFIER]}.
 * <p>
 * The modifier is optional.
 * <p>
 * Objects of this class are ordered according to the semantic versioning progression. Modifiers are ordered alphabetically.
 *
 * @author Alessandro Falappa
 */
public class ArtifactVersion implements Comparable<ArtifactVersion> {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-.](.+))?$");
    private final int major;
    private final int minor;
    private final int patch;
    private final String modifier;

    public ArtifactVersion(int major, int minor, int patch) {
        this(major, minor, patch, null);
    }

    public ArtifactVersion(int major, int minor, int patch, String modifier) {
        if (major < 0) {
            throw new IllegalArgumentException("Negative major version");
        }
        if (minor < 0) {
            throw new IllegalArgumentException("Negative minor version");
        }
        if (patch < 0) {
            throw new IllegalArgumentException("Negative patch version");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.modifier = modifier != null ? modifier.toUpperCase() : null;
    }

    public static ArtifactVersion of(String versionString) {
        Matcher m = PATTERN.matcher(versionString);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid version string: " + versionString);
        } else {
            final int mj = Integer.parseInt(m.group(1));
            final int mn = Integer.parseInt(m.group(2));
            final int pa = Integer.parseInt(m.group(3));
            final String mod = m.group(4);
            return new ArtifactVersion(mj, mn, pa, mod);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major);
        sb.append('.').append(minor);
        sb.append('.').append(patch);
        if (modifier!=null) {
            sb.append('-').append(modifier);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + this.major;
        hash = 29 * hash + this.minor;
        hash = 29 * hash + this.patch;
        hash = 29 * hash + Objects.hashCode(this.modifier);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ArtifactVersion other = (ArtifactVersion) obj;
        if (this.major != other.major) {
            return false;
        }
        if (this.minor != other.minor) {
            return false;
        }
        if (this.patch != other.patch) {
            return false;
        }
        if (!Objects.equals(this.modifier, other.modifier)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(ArtifactVersion other) {
        int result;
        // compare version numbers
        if ((result = Integer.compare(major, other.major)) != 0) {
            return result;
        }
        if ((result = Integer.compare(minor, other.minor)) != 0) {
            return result;
        }
        if ((result = Integer.compare(patch, other.patch)) != 0) {
            return result;
        }
        // compare modifiers
        if (modifier == null) {
            if (other.modifier == null) {
                return result;
            } else {
                return 1;
            }
        } else {
            if (other.modifier == null) {
                return -1;
            } else {
                return modifier.compareTo(other.modifier);
            }
        }
    }
}
