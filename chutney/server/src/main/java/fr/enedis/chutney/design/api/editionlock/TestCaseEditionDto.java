/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.api.editionlock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableTestCaseEditionDto.class)
@Value.Style(jdkOnly = true)
public interface TestCaseEditionDto {

    @JsonProperty("testCaseId")
    String testCaseId();

    @JsonProperty("testCaseVersion")
    Integer testCaseVersion();

    @JsonProperty("editionStartDate")
    Instant editionStartDate();

    @JsonProperty("editionUser")
    String editionUser();

    @JsonCreator
    static TestCaseEditionDto of(
        @JsonProperty("testCaseId") String testCaseId,
        @JsonProperty("testCaseVersion") Integer testCaseVersion,
        @JsonProperty("editionStartDate") Instant editionStartDate,
        @JsonProperty("editionUser") String editionUser
    ) {
        return ImmutableTestCaseEditionDto.builder()
            .testCaseId(testCaseId)
            .testCaseVersion(testCaseVersion)
            .editionStartDate(editionStartDate)
            .editionUser(editionUser)
            .build();
    }
}
