/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.domain;

import fr.enedis.chutney.design.domain.editionlock.TestCaseEditions;
import fr.enedis.chutney.design.domain.editionlock.TestCaseEditionsService;
import fr.enedis.chutney.scenario.infra.TestCaseRepositoryAggregator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EditionConfiguration {

    @Bean
    TestCaseEditionsService testCaseEditionsService(TestCaseEditions testCaseEditions, TestCaseRepositoryAggregator testCaseRepository) {
        return new TestCaseEditionsService(testCaseEditions, testCaseRepository);
    }

}
