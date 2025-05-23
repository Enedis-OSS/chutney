/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.api.editionlock;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableTestCaseEditionDto.class)
@JsonDeserialize(as = ImmutableTestCaseEditionDto.class)
@Value.Style(jdkOnly = true)
public interface TestCaseEditionDto {

    String testCaseId();

    Integer testCaseVersion();

    Instant editionStartDate();

    String editionUser();
}
