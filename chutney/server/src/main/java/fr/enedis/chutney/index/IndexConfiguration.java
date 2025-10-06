/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index;

import fr.enedis.chutney.index.domain.IndexRepository;
import fr.enedis.chutney.index.domain.IndexService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndexConfiguration {

    @Bean
    public IndexService indexService(List<IndexRepository<?>> indexRepositories) {
        return new IndexService(indexRepositories);
    }
}
