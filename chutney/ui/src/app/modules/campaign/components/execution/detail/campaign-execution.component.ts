/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { Observable, of, Subject, switchMap, timer } from 'rxjs';

import {
    Authorization,
    CampaignExecutionFullReport,
    CampaignReport,
    JiraScenario,
    JiraTestExecutionScenarios,
    ScenarioExecutionReportOutline,
    XrayStatus
} from '@core/model';
import { CampaignService, JiraPluginService } from '@core/services';
import { Params } from '@angular/router';
import { ExecutionStatus } from '@core/model/scenario/execution-status';
import { EventManagerService } from '@shared';
import { sortByAndOrder } from '@shared/tools';
import { CampaignReportService } from '@core/services/campaign-report.service';
import { DatasetUtils } from '@shared/tools/dataset-utils';
import { map, takeUntil, tap } from 'rxjs/operators';
import { ScenarioExecutionService } from 'src/app/core/services/scenario-execution.service';
import { ExecutionDataset } from '@core/model/scenario/execution.dataset';

@Component({
    selector: 'chutney-campaign-execution',
    templateUrl: './campaign-execution.component.html',
    styleUrls: ['./campaign-execution.component.scss'],
    standalone: false
})
export class CampaignExecutionComponent implements OnInit, OnDestroy, OnChanges {

    @Input() campaignId: number;
    @Input() report: CampaignReport;
    @Input() jiraUrl: string;

    Authorization = Authorization;
    ExecutionStatus = ExecutionStatus;

    datasetByScenarioExecutionId: Map<string, ExecutionDataset> = new Map<string, ExecutionDataset>()

    errors: string[] = [];
    jiraTestExecutionId: string;
    private jiraScenarios: JiraScenario[] = [];
    UNSUPPORTED = 'UNSUPPORTED';
    selectedStatusByJiraId: Map<string, string> = new Map();
    showMore: boolean[] = [];

    orderBy: string;
    reverseOrder: boolean;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(
        private jiraLinkService: JiraPluginService,
        private campaignService: CampaignService,
        private eventManagerService: EventManagerService,
        private campaignReportService: CampaignReportService,
        private scenarioExecutionService: ScenarioExecutionService,
        private datasetUtils: DatasetUtils
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.sortBy();
    }

    ngOnInit(): void {
        this.cleanJiraUrl();
        this.jiraTestExecutionScenarios$().pipe(
            takeUntil(this.unsubscribeSub$)
        ).subscribe(result => {
                this.jiraScenarios = result.jiraScenarios;
                this.jiraTestExecutionId = result.id;
            });
        this.report.report.scenarioExecutionReports.forEach((_report, index) => this.showMore[index] = false);
        this.fetchMissingInlineDatasetsForScenarioExecution()
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    private fetchMissingInlineDatasetsForScenarioExecution() {
        const scenarioExecutionReportWithoutDataset = this.report.report.scenarioExecutionReports.filter(scenarioExecutionReport => !scenarioExecutionReport.dataset);
        for (const execution of scenarioExecutionReportWithoutDataset) {
             this.scenarioExecutionService.findExecutionReport(execution.scenarioId, execution.executionId).pipe(
                takeUntil(this.unsubscribeSub$),
                map(scenarioExecution => scenarioExecution.dataset),
                tap(dataset => this.datasetByScenarioExecutionId.set(String(execution.executionId), dataset))
            ).subscribe()
        }
    }

    private cleanJiraUrl() {
        if (this.jiraUrl && this.jiraUrl.length == 0) {
            this.jiraUrl = null;
        }
    }

    protected getDataset(execution: ScenarioExecutionReportOutline) {
        if (!execution.dataset) {
            if (this.datasetByScenarioExecutionId && this.datasetByScenarioExecutionId.has(String(execution.executionId))) {
                return this.datasetUtils.getExecutionDatasetName(this.datasetByScenarioExecutionId.get(String(execution.executionId)).datasetId)
            }
            return ''
        }
        return this.datasetUtils.getDatasetName(execution.dataset)
    }

    private jiraTestExecutionScenarios$(): Observable<JiraTestExecutionScenarios> {
        if (this.jiraUrl != null) {
            return this.jiraLinkService.findTestExecScenariosByCampaignExecution(this.report.report.executionId);
        } else {
            return of(new JiraTestExecutionScenarios(null, []));
        }
    }

    xrayStatuses(): Array<string> {
        const keys = Object.keys(XrayStatus);
        return keys.slice();
    }

    selectedUpdateStatus(execution: ScenarioExecutionReportOutline, event: any) {
        this.selectedStatusByJiraId.set(this.jiraScenarioOf(execution).id, event.target.value);
    }

    isStatusUpdatable(execution: ScenarioExecutionReportOutline): boolean {
        const newStatus = this.selectedStatusByJiraId.get(this.jiraScenarioOf(execution)?.id);
        return newStatus === XrayStatus.PASS || newStatus === XrayStatus.FAIL;
    }

    updateStatus(execution: ScenarioExecutionReportOutline) {
        if (this.isStatusUpdatable(execution)) {
            const jiraScenario = this.jiraScenarioOf(execution);
            this.jiraLinkService.updateScenarioStatus(this.jiraTestExecutionId, jiraScenario, this.selectedStatusByJiraId.get(jiraScenario.id))
                .pipe(takeUntil(this.unsubscribeSub$))
                .subscribe({
                    error: (error) => {
                        console.log(error);
                    }
                });
        }
    }

    scenarioStatus(execution: ScenarioExecutionReportOutline): string {
        const status = this.jiraScenarioOf(execution)?.executionStatus;
        return status === XrayStatus.PASS || status === XrayStatus.FAIL ? status : this.UNSUPPORTED;
    }

    jiraLinkFrom(execution: ScenarioExecutionReportOutline) {
        const jiraScenario = this.jiraScenarioOf(execution);
        return jiraScenario ? this.jiraUrl + '/browse/' + jiraScenario.id : null;
    }

    // Same resolution as the server report to xray: dataset link first, scenario link otherwise
    private jiraScenarioOf(execution: ScenarioExecutionReportOutline): JiraScenario {
        const datasetId = execution.dataset?.id ?? this.datasetByScenarioExecutionId.get(String(execution.executionId))?.datasetId;
        const links = this.jiraScenarios.filter(s => s.chutneyId === execution.scenarioId);
        return links.find(s => s.datasetId && s.datasetId === datasetId) ?? links.find(s => !s.datasetId);
    }

    toQueryParams(scenarioExecutionReportOutline: ScenarioExecutionReportOutline): Params {
        let execId = scenarioExecutionReportOutline.executionId !== -1 ? scenarioExecutionReportOutline.executionId : 'last';
        return {
            active: execId,
            open: execId,
        }
    }

    replay() {
        this.campaignService.replayFailedScenario(this.report.report.executionId)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                error: (error) => this.eventManagerService.broadcast({ name: 'error', msg: error.error })
            });

        timer(1000).pipe(
            takeUntil(this.unsubscribeSub$),
            switchMap(() => of(this.eventManagerService.broadcast({ name: 'replay', executionId: this.report.report.executionId })))
        ).subscribe();
    }

    stop(force = false) {
        this.campaignService.stopExecution(this.campaignId, this.report.report.executionId, force)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                error: (error) => this.eventManagerService.broadcast({ name: 'error', msg: error.error })
            });
    }

    statusClass(scenarioReportOutline: ScenarioExecutionReportOutline): string {
        if (scenarioReportOutline.status === ExecutionStatus.SUCCESS) {
            return 'fa-solid fa-circle-check text-info';
        }
        if (scenarioReportOutline.status === ExecutionStatus.FAILURE) {
            return 'fa-solid fa-circle-xmark text-danger';
        }
        if (scenarioReportOutline.status === ExecutionStatus.RUNNING || scenarioReportOutline.status === ExecutionStatus.PAUSED) {
            return 'fa-solid fa-spinner fa-pulse text-warning';
        }
        if (scenarioReportOutline.status === ExecutionStatus.STOPPED) {
            return 'fa-solid fa-circle-stop text-warning';
        }
        if (scenarioReportOutline.status === ExecutionStatus.NOT_EXECUTED) {
            return 'fa-regular fa-circle text-warning';
        }
        if (scenarioReportOutline.status === ExecutionStatus.SKIPPED) {
            return 'fa-solid fa-circle-minus text-secondary';
        }
        return null;
    }

    sortBy(property?: string ) {
        if (this.orderBy === property) {
            this.reverseOrder = !this.reverseOrder;
        }
        this.orderBy = property ? property : this.orderBy;
        if(this.orderBy) {
            sortByAndOrder(
                this.report.report.scenarioExecutionReports,
                (i) => i[this.orderBy] == null ? '' : i[this.orderBy],
                this.reverseOrder
            );
        }
    }

    exportReport() {
        this.campaignService.findExecution(this.report.report.executionId)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (report: CampaignExecutionFullReport) => {
                    this.campaignReportService.toPDF(report).save('campaignExecutionReport.pdf');
                },
                error: error => {
                    this.errors.push(error.message);
                }
            });
    }
}
