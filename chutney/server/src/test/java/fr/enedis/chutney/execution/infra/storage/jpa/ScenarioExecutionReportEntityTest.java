/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.storage.jpa;

import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ScenarioExecutionReportEntityTest {

    @Test
    public void should_parse_dataset_from_scenario_execution_report() {
        // Given
        String report = """
                    {
                        "constants": {
                            "TITI": "TATA",
                            "TOTO": "TUTU"
                        },
                        "contextVariables": {},
                        "datatable": [
                            {
                                "VIVI": "VOVO",
                                "VUVU": "VAVA"
                            }
                        ],
                        "environment": "TOTO",
                        "executionId": 63,
                        "report": {
                            "duration": 0,
                            "errors": [],
                            "evaluatedInputs": {},
                            "information": [],
                            "name": "TOTO",
                            "startDate": "2024-09-11T09:27:34.869956Z",
                            "status": "SUCCESS",
                            "stepOutputs": {},
                            "steps": [
                                {
                                    "duration": 0,
                                    "errors": [],
                                    "evaluatedInputs": {},
                                    "information": [],
                                    "name": "step description",
                                    "startDate": "2024-09-11T09:27:34.870027Z",
                                    "status": "SUCCESS",
                                    "stepOutputs": {},
                                    "steps": [],
                                    "strategy": "sequential",
                                    "targetName": "",
                                    "targetUrl": "",
                                    "type": "success"
                                },
                                {
                                    "duration": 0,
                                    "errors": [],
                                    "evaluatedInputs": {},
                                    "information": [],
                                    "name": "",
                                    "startDate": "2024-09-11T09:27:34.870349Z",
                                    "status": "SUCCESS",
                                    "stepOutputs": {},
                                    "steps": [],
                                    "strategy": "sequential",
                                    "targetName": "",
                                    "targetUrl": "",
                                    "type": ""
                                }
                            ],
                            "strategy": "sequential",
                            "targetName": "",
                            "targetUrl": "",
                            "type": ""
                        },
                        "scenarioName": "TOTO",
                        "tags": [],
                        "user": "admin"
                    }
            """;
        String expectedDatasetId = "My dataset Id";
        ScenarioExecutionEntity scenarioExecution = new ScenarioExecutionEntity(null, "1", null, null, null, null, null, null, null, null, null, expectedDatasetId, null, null);
        ScenarioExecutionReportEntity scenarioExecutionReportEntity = new ScenarioExecutionReportEntity(scenarioExecution, report);


        // When
        DataSet dataset = scenarioExecutionReportEntity.getDatasetFromReport();

        // Then
        assertThat(dataset).isNotNull();
        assertThat(dataset.id).isEqualTo(expectedDatasetId);
        assertThat(dataset.constants).containsEntry("TITI", "TATA");
        assertThat(dataset.constants).containsEntry("TOTO", "TUTU");
        assertThat(dataset.datatable).hasSize(1);
        assertThat(dataset.datatable.getFirst()).containsEntry("VIVI", "VOVO");
        assertThat(dataset.datatable.getFirst()).containsEntry("VUVU", "VAVA");
    }

    @Test
    public void should_parse_scenario_execution_report_without_dataset() {
        // Given
        String report = """
                    {
                        "contextVariables": {},
                        "environment": "TOTO",
                        "executionId": 63,
                        "report": {
                            "duration": 0,
                            "errors": [],
                            "evaluatedInputs": {},
                            "information": [],
                            "name": "TOTO",
                            "startDate": "2024-09-11T09:27:34.869956Z",
                            "status": "SUCCESS",
                            "stepOutputs": {},
                            "steps": [
                                {
                                    "duration": 0,
                                    "errors": [],
                                    "evaluatedInputs": {},
                                    "information": [],
                                    "name": "step description",
                                    "startDate": "2024-09-11T09:27:34.870027Z",
                                    "status": "SUCCESS",
                                    "stepOutputs": {},
                                    "steps": [],
                                    "strategy": "sequential",
                                    "targetName": "",
                                    "targetUrl": "",
                                    "type": "success"
                                },
                                {
                                    "duration": 0,
                                    "errors": [],
                                    "evaluatedInputs": {},
                                    "information": [],
                                    "name": "",
                                    "startDate": "2024-09-11T09:27:34.870349Z",
                                    "status": "SUCCESS",
                                    "stepOutputs": {},
                                    "steps": [],
                                    "strategy": "sequential",
                                    "targetName": "",
                                    "targetUrl": "",
                                    "type": ""
                                }
                            ],
                            "strategy": "sequential",
                            "targetName": "",
                            "targetUrl": "",
                            "type": ""
                        },
                        "scenarioName": "TOTO",
                        "tags": [],
                        "user": "admin"
                    }
            """;
        ScenarioExecutionEntity scenarioExecution = new ScenarioExecutionEntity(null, "1", null, null, null, null, null, null, null, null, null, null, null, null);
        ScenarioExecutionReportEntity scenarioExecutionReportEntity = new ScenarioExecutionReportEntity(scenarioExecution, report);

        // When
        DataSet dataset = scenarioExecutionReportEntity.getDatasetFromReport();

        // Then
        assertThat(dataset).isNull();
    }
}
