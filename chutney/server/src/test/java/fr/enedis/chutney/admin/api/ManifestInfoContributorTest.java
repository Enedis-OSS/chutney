/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.admin.api;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

public class ManifestInfoContributorTest {

    @Test
    public void read_basic_manifest() {
        Info.Builder infoBuilder = new Info.Builder();

        InfoContributor manifestInfoContributor = new ManifestInfoContributor();

        manifestInfoContributor.contribute(infoBuilder);

        Info info = infoBuilder.build();

        Assertions.assertThat(info.getDetails())
            .as("Manifest Info")
            .containsEntry("chutney-test-manifest", ImmutableMap.builder()
                .put("Manifest-Version", "1.0")
                .put("Implementation-Title", "chutney-test-manifest")
                .put("Implementation-Version", "1.0.0-SNAPSHOT")
                .put("Built-By", "toto ;)")
                .put("Specification-Title", "server")
                .put("Implementation-Vendor-Id", "fr.enedis.chutney")
                .put("Created-By", "Apache Maven 3.2.2")
                .put("Build-Jdk", "1.8.0_121")
                .put("Specification-Version", "1.0")
                .build()
            )
        ;
    }
}
