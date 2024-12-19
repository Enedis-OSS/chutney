/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

export class StepExecutionReport {
    constructor(
        public duration: string,
        public status: string,
        public startDate: string,
        public information: string[],
        public errors: string[],
        public type: string,
        public strategy: string,
        public targetName: string,
        public targetUrl: string,
        public evaluatedInputs: Map<string, Object>,
        public steps: Array<StepExecutionReport>,
        public stepOutputs: Map<string, Object>,
        public name?: string,
        ) {}

    static cleanReport(stepReport: StepExecutionReport): StepExecutionReport {
        if (stepReport?.steps) {
            if (stepReport?.strategy === 'for' && stepReport.steps.length == 1) {
                return StepExecutionReport.cleanReport(stepReport.steps[0]);
            }
            stepReport.steps = stepReport.steps.map(substep => StepExecutionReport.cleanReport(substep));
        }
        return stepReport;
    }
}
