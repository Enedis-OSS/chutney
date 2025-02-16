/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.execution.domain;

import com.chutneytesting.scenario.domain.gwt.GwtScenario;
import com.chutneytesting.scenario.domain.gwt.GwtTestCase;
import com.chutneytesting.server.core.domain.execution.ExecutionRequest;
import com.chutneytesting.server.core.domain.execution.processor.TestCasePreProcessor;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

@Component
public class GwtDataSetPreProcessor implements TestCasePreProcessor<GwtTestCase> {

    private final GwtScenarioMarshaller marshaller;

    public GwtDataSetPreProcessor(GwtScenarioMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public GwtTestCase apply(ExecutionRequest executionRequest) {
        GwtTestCase testCase = (GwtTestCase) executionRequest.testCase;
        return GwtTestCase.builder()
            .withMetadata(testCase.metadata)
            .withScenario(testCase.scenario)
            .build();
    }
}
