/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.step.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.enedis.chutney.tools.MyMixInForIgnoreType;
import org.jdom2.Element;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public class ReportObjectMapperConfiguration {

    private static final ObjectMapper reportObjectMapper = configureObjectMapper();

    public static ObjectMapper reportObjectMapper() {
        return reportObjectMapper;
    }

    private static ObjectMapper configureObjectMapper() {
        SimpleModule jdomElementModule = new SimpleModule();
        jdomElementModule.addSerializer(Element.class, new JDomElementSerializer());
        return JsonMapper.builder()
            .addMixIn(Resource.class, MyMixInForIgnoreType.class)
            .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_NULL))
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addModule(jdomElementModule)
            .findAndAddModules()
            .build();
    }
}
