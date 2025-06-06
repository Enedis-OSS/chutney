/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.execution;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetEntityMapper {

    private static ObjectMapper mapper = new ObjectMapper();
    private static Logger logger = LoggerFactory.getLogger(DatasetEntityMapper.class);

    private static List<Map<String, String>> datasetDatatableFromString(String datasetDatatable) {
        if (datasetDatatable == null) {
            return null;
        }
        try {
            return mapper.readValue(datasetDatatable, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Error while parsing dataset datatable", e);
            return emptyList();
        }
    }

    private static Map<String, String> datasetConstantsFromString(String datasetConstants) {
        if (datasetConstants == null) {
            return null;
        }
        try {
            return mapper.readValue(datasetConstants, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Error while parsing dataset constants", e);
            return emptyMap();
        }
    }

    public static String datasetDatatableToString(List<Map<String, String>> datatable) {
        if (datatable == null || datatable.isEmpty()) {
            return null;
        }
        try {
            return mapper.writeValueAsString(datatable);
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing dataset datatable to string", e);
            return null;
        }
    }

    public static String datasetConstantsToString(Map<String, String> constants) {
        if (constants == null || constants.isEmpty()) {
            return null;
        }
        try {
            return mapper.writeValueAsString(constants);
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing dataset constants to string", e);
            return null;
        }
    }

    public static DataSet getDataset(String datasetId, String datasetConstants, String datasetDatatable) {
        if (datasetId == null && (datasetConstants == null || datasetConstants.isEmpty()) && (datasetDatatable == null || datasetDatatable.isEmpty())) {
            return null;
        }
        return DataSet.builder()
            .withId(datasetId)
            .withName("")
            .withConstants(datasetConstantsFromString(datasetConstants))
            .withDatatable(datasetDatatableFromString(datasetDatatable))
            .build();
    }
}
