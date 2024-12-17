/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { StepExecutionReport } from "./step-execution-report.model";

describe('StepExecutionReportTest', () => {
    it('should flatten one iteration for strategy', () => {
        var report = {
            name: 'root step to be flatten',
            steps: [
                {
                    name: 'Step with one iteration - <i> - ${#dk}',
                    steps: [
                        {
                            name: 'Step with one iteration - 0 - one'
                        }
                    ],
                    strategy: 'for'
                },
                {
                    name: 'Step with one iteration - <i> - ${#dk}',
                    steps: [
                        {
                            name: 'Step with one iteration - 0 - one',
                            steps: [
                                {
                                    name: 'Child with one iteration - 0 - <j> - ${#dkk}',
                                    steps: [
                                        {
                                            name: 'Child with one iteration - 0 - 0 - one'
                                        }
                                    ],
                                    strategy: 'for'
                                }
                            ]
                        }
                    ],
                    strategy: 'for'
                }
            ]
        } as StepExecutionReport;
        let initialReport = Object.assign({}, report)
        let cleanedReport = StepExecutionReport.cleanReport(report);
        expect(cleanedReport).toBe(report);
        expect(cleanedReport.name).toBe(initialReport.name);
        expect(cleanedReport.steps).toHaveSize(2);
        expect(cleanedReport.steps[0]).toBe(initialReport.steps[0].steps[0]);
        expect(cleanedReport.steps[1].name).toBe(initialReport.steps[1].steps[0].name);
        expect(cleanedReport.steps[1].steps[0]).toBe(initialReport.steps[1].steps[0].steps[0]);
    });
});
