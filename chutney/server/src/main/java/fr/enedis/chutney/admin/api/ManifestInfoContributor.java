/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.admin.api;


import static fr.enedis.chutney.tools.loader.ExtensionLoaders.Sources.classpath;

import fr.enedis.chutney.tools.loader.ExtensionLoader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class ManifestInfoContributor implements InfoContributor {
    @Override
    public void contribute(Builder builder) {
        ExtensionLoader.Builder
            .<String, String>withSource(classpath("META-INF/MANIFEST.MF"))
            .withMapper(Collections::singleton)
        .load()
        .stream()
        .map(ManifestInfoContributor::toMap)
        .filter(manifestMap -> manifestMap.getOrDefault("Implementation-Title", "").contains("chutney"))
        .forEach(manifestMap -> builder.withDetail(manifestMap.get("Implementation-Title"), manifestMap));
    }

    static Map<String, String> toMap(String manifestAsString) {
        Manifest manifest;
        try {
            manifest = new Manifest(new ByteArrayInputStream(manifestAsString.getBytes()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Manifest:\n" + manifestAsString);
        }

        return manifest.getMainAttributes()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> String.valueOf(entry.getKey()),
                entry -> String.valueOf(entry.getValue())
            ));
    }
}
