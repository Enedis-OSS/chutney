/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config.web;

import fr.enedis.chutney.dataset.api.DataSetDto;
import fr.enedis.chutney.dataset.api.ImmutableDataSetDto;
import fr.enedis.chutney.design.api.editionlock.ImmutableTestCaseEditionDto;
import fr.enedis.chutney.design.api.editionlock.TestCaseEditionDto;
import fr.enedis.chutney.design.api.plugins.linkifier.ImmutableLinkifierDto;
import fr.enedis.chutney.design.api.plugins.linkifier.LinkifierDto;
import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import fr.enedis.chutney.execution.api.ImmutableExecutionSummaryDto;
import fr.enedis.chutney.scenario.api.raw.dto.GwtScenarioDto;
import fr.enedis.chutney.scenario.api.raw.dto.GwtStepDto;
import fr.enedis.chutney.scenario.api.raw.dto.GwtStepImplementationDto;
import fr.enedis.chutney.scenario.api.raw.dto.GwtTestCaseDto;
import fr.enedis.chutney.scenario.api.raw.dto.GwtTestCaseMetadataDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtScenarioDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtStepDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtStepImplementationDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtTestCaseDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableGwtTestCaseMetadataDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableRawTestCaseDto;
import fr.enedis.chutney.scenario.api.raw.dto.ImmutableTestCaseIndexDto;
import fr.enedis.chutney.scenario.api.raw.dto.RawTestCaseDto;
import fr.enedis.chutney.scenario.api.raw.dto.TestCaseIndexDto;
import fr.enedis.chutney.server.core.domain.tools.ImmutablePaginatedDto;
import fr.enedis.chutney.server.core.domain.tools.ImmutablePaginationRequestParametersDto;
import fr.enedis.chutney.server.core.domain.tools.ImmutablePaginationRequestWrapperDto;
import fr.enedis.chutney.server.core.domain.tools.ImmutableSortRequestParametersDto;
import fr.enedis.chutney.server.core.domain.tools.PaginatedDto;
import fr.enedis.chutney.server.core.domain.tools.PaginationRequestParametersDto;
import fr.enedis.chutney.server.core.domain.tools.PaginationRequestWrapperDto;
import fr.enedis.chutney.server.core.domain.tools.SortRequestParametersDto;
import tools.jackson.databind.cfg.MapperBuilder;

/**
 * Applies Jackson annotations declared on Immutables interfaces to generated implementations.
 *
 * Jackson 3 does not inherit annotations from interfaces; mixins copy @JsonCreator, @JsonProperty,
 * and other Jackson annotations declared on the interface onto the Immutable* class.
 */
public final class ImmutablesJacksonMixins {

    private ImmutablesJacksonMixins() {
    }

    public static void register(MapperBuilder<?, ?> builder) {
        builder.addMixIn(ImmutableExecutionSummaryDto.class, ExecutionSummaryDto.class);
        builder.addMixIn(ImmutableRawTestCaseDto.class, RawTestCaseDto.class);
        builder.addMixIn(ImmutableGwtTestCaseDto.class, GwtTestCaseDto.class);
        builder.addMixIn(ImmutableGwtTestCaseMetadataDto.class, GwtTestCaseMetadataDto.class);
        builder.addMixIn(ImmutableTestCaseIndexDto.class, TestCaseIndexDto.class);
        builder.addMixIn(ImmutableGwtScenarioDto.class, GwtScenarioDto.class);
        builder.addMixIn(ImmutableGwtStepDto.class, GwtStepDto.class);
        builder.addMixIn(ImmutableGwtStepImplementationDto.class, GwtStepImplementationDto.class);
        builder.addMixIn(ImmutableTestCaseEditionDto.class, TestCaseEditionDto.class);
        builder.addMixIn(ImmutableDataSetDto.class, DataSetDto.class);
        builder.addMixIn(ImmutableLinkifierDto.class, LinkifierDto.class);
        builder.addMixIn(ImmutablePaginatedDto.class, PaginatedDto.class);
        builder.addMixIn(ImmutablePaginationRequestParametersDto.class, PaginationRequestParametersDto.class);
        builder.addMixIn(ImmutablePaginationRequestWrapperDto.class, PaginationRequestWrapperDto.class);
        builder.addMixIn(ImmutableSortRequestParametersDto.class, SortRequestParametersDto.class);
    }
}
