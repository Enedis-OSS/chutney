/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package util.infra;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(
    basePackages = {
        "fr.enedis.chutney.scenario.infra",
        "fr.enedis.chutney.campaign.infra",
        "fr.enedis.chutney.execution.infra.storage"
    },
    includeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.*JpaRepository$")}
)
@ComponentScan(
    basePackages = {
        "fr.enedis.chutney.campaign.infra",
        "fr.enedis.chutney.scenario.infra",
        "fr.enedis.chutney.execution.infra.storage"
    }
)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableJpa {
}
