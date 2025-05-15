/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.mapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.enedis.chutney.scenario.api.raw.dto.ImmutableRawTestCaseDto;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotParsableException;
import org.junit.jupiter.api.Test;

public class RawTestCaseMapperTest {

    private final ImmutableRawTestCaseDto invalid_dto = ImmutableRawTestCaseDto.builder()
        .title("Test mapping")
        .scenario(" I am invalid\n {").build();

    @Test
    public void should_fail_on_parse_error() {
        assertThatThrownBy(() -> RawTestCaseMapper.fromDto(invalid_dto))
            .isInstanceOf(ScenarioNotParsableException.class);
    }

}
