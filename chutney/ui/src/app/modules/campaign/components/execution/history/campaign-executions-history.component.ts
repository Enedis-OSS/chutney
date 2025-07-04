/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { empty, EMPTY, forkJoin, Observable, of, Subject, Subscription, timer, zip } from 'rxjs';
import { delay, map, repeat, switchMap, takeUntil, tap } from 'rxjs/operators';
import { NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';

import { Campaign, CampaignReport } from '@model';
import { CampaignService, JiraPluginConfigurationService } from '@core/services';
import { EventManagerService } from '@shared';

@Component({
    selector: 'chutney-campaign-executions-history',
    templateUrl: './campaign-executions-history.component.html',
    styleUrls: ['./campaign-executions-history.component.scss'],
    standalone: false
})
export class CampaignExecutionsHistoryComponent implements OnInit, OnDestroy {

    static readonly LAST_ID: string = 'last';

    jiraUrl: string;

    errors: string[] = [];
    campaign: Campaign;
    campaignReports: CampaignReport[];
    activeTab: string = '0';
    tabs: CampaignReport[] = [];
    canReplay = false;

    private campaignId: number;
    private _executionsFilters: Params = {};
    private tabFilters: Params = {};

    private refreshSubscription: Subscription;
    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(private route: ActivatedRoute,
                private router: Router,
                private campaignService: CampaignService,
                private eventManagerService: EventManagerService,
                private jiraPluginConfigurationService: JiraPluginConfigurationService) {
    }

    ngOnInit(): void {
        this.loadJiraUrl();

        this.route.params.pipe(
            tap((params) => this.campaignId = params['id']),
            switchMap(() =>
                forkJoin({
                    executeEvent: this.eventManagerService.listen('execute', () => this.refreshCampaign()),
                    errorEvent: this.eventManagerService.listen('error', (event) => of(this.onMenuError(event))),
                    replayEvent: this.eventManagerService.listen('replay', () => this.refreshCampaign()),
                    executeLastEvent: this.eventManagerService.listen('executeLast', () => this.replay()),
                    campaigns: this.campaign$().pipe(switchMap(() => this.openTabs$()))
                })
            ),
            takeUntil(this.unsubscribeSub$)
        ).subscribe({
            error: (error) => this.errors.push(error.error)
        });
    }

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
        this.unsubscribeRefresh();
    }

    private campaign$(): Observable<Campaign> {
        return this.campaignService.find(this.campaignId).pipe(
            tap(campaign => this.campaign = campaign),
            tap(campaign => this.campaignReports = campaign.campaignExecutionReports.map(cer => new CampaignReport(cer))),
            tap(campaign => this.canReplay = campaign.campaignExecutionReports.length > 0),
            tap(() => this.refreshOpenTabs()),
            tap(() => this.checkForRefresh())
        );
    }

    private refreshOpenTabs(): void {
        for(var i=0; i<this.tabs.length; i++) {
            if (this.tabs[i].isRunning()) {
                this.tabs[i].refresh(this.campaignReports.find((cr) => cr.report.executionId === this.tabs[i].report.executionId));
            }
        }
    }

    private loadJiraUrl() {
        this.jiraPluginConfigurationService.getUrl()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(url => this.jiraUrl = url);
    }

    private replay(): Observable<any> {
        const lastReport = this.campaignReports[0]
        this.campaignService.executeCampaign(this.campaign.id, lastReport.report.executionEnvironment, lastReport.report.dataset)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe()
        return timer(1000).pipe(
            switchMap(() => this.refreshCampaign())
        );
    }

    private checkForRefresh() {
        if (this.campaignReports.find(c => c.isRunning())) {
            if (!this.isRefreshActive()) {
                this.refreshSubscription = this.campaign$().pipe(
                    delay(5000),
                    repeat()
                ).subscribe();
            }
        } else {
            this.unsubscribeRefresh();
        }
    }

    private openTabs$(): Observable<CampaignReport[]> {
        let executionsFilters: Params;
        return this.route.queryParams.pipe(
            tap(queryParams => executionsFilters = queryParams),
            switchMap(queryParams => this.openTabs(queryParams)),
            tap(() => this.executionsFilters = executionsFilters)
        );
    }

    private refreshCampaign(): Observable<Campaign>  {
        if (this.campaign.scenarios.length > 0) {
            return this.campaign$().pipe(
                tap(c => this.openReport({ execution: this.campaignReports[0], focus: true })
            ));
        } else {
            return EMPTY;
        }
    }

    private onMenuError(menuErrorEvent: any) {
        this.errors.push(menuErrorEvent.msg);
    }

    getActiveTab(): string {
        if(this.activeTab === CampaignExecutionsHistoryComponent.LAST_ID) {
            return this.campaign.campaignExecutionReports[0]?.executionId?.toString();
        }
        return this.activeTab;
    }

    onTabChange(changeEvent: NgbNavChangeEvent) {
        this.activeTab = changeEvent.nextId;
        this.tabFilters['active'] = changeEvent.nextId;
        this.updateQueryParams();
    }

    private updateQueryParams() {
        let queryParams = this.cleanParams({...this.executionsFilters, ...this.tabFilters});
        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: queryParams
        });
    }

    private cleanParams(params: Params) {
        Object.keys(params).forEach(key => {
            if (params[key] === null || params[key] === '' || params[key] === '0') {
                delete params[key];
            }
        });
        return params;
    }

    closeReport(event: MouseEvent, executionId: number) {
        event.preventDefault();
        event.stopImmediatePropagation();
        const openTabs = this.tabs.filter(exec => exec.report.executionId !== executionId);
        this.tabFilters['open'] = openTabs.map(exec => exec.report.executionId).toString();

        if (this.activeTab === executionId.toString() || !openTabs.length) {
            this.tabFilters['active'] = null;
        }
        this.updateQueryParams();
    }

    openReport(request: { execution: CampaignReport, focus: boolean }) {
        if (request.execution) {
            var tabs = this.tabs;
            if (!this.isOpened(request.execution.report.executionId.toString())) {
                tabs = tabs.concat(request.execution);
            }

            this.tabFilters['open'] = tabs.map(exec => exec.report.executionId).toString();
            this.tabFilters['active'] = request.focus ? request.execution.report.executionId : null;

            this.updateQueryParams();
        }
    }

    get executionsFilters(): Params {
        return this._executionsFilters;
    }

    set executionsFilters(value: Params) {
        const {open, active, ...executionsParams} = value;
        this._executionsFilters = executionsParams;
        this.updateQueryParams();
    }

    private isOpened(executionId: string): boolean {
        const lastExecId = this.lastExecutionId();
        const openTabs: string = this.tabFilters['open'];
        return !!openTabs && !!openTabs.split(',')
            .find(openId => {
                let id = openId;
                if (openId === CampaignExecutionsHistoryComponent.LAST_ID && executionId === lastExecId) {
                    id = lastExecId;
                }
                return executionId === id;
            });
    }

    private focusOnTab(executionId: string) {
        this.activeTab = executionId && this.isOpened(executionId) ? executionId : '0';
        this.tabFilters['active'] = this.activeTab;
    }

    private openTabs(queryParams: Params): Observable<CampaignReport[]> {
        if (!this.campaign?.campaignExecutionReports.length) {
            return EMPTY;
        }

        let openedExecutions$ = of([]);
        this.cleanQueryParams(queryParams);
        if (queryParams['open']) {
            let executions$: Observable<CampaignReport>[] = queryParams['open']
                .split(',')
                .map(id => this.campaignReports.find(cer => cer.report.executionId == id))
                .filter((campaignReport: CampaignReport) => campaignReport)
                .map((campaignReport: CampaignReport) => of(campaignReport))
            openedExecutions$ = zip(executions$);
        }

        return openedExecutions$.pipe(
            tap(executions => {
                this.tabs = executions;
                this.tabFilters['open'] = this.getOpenTabs(queryParams['open']);
                this.focusOnTab(queryParams['active']);
            })
        )
    }

    private getOpenTabs(opens: string) {
        return this.tabs.map((exec, i) => {
            if (opens.includes(CampaignExecutionsHistoryComponent.LAST_ID) && i === 0) {
                return CampaignExecutionsHistoryComponent.LAST_ID;
            }
            return exec.report.executionId;
        }).toString();
    }

    private cleanQueryParams(queryParams: Params) {
        let redirect = false;
        const lastExecutionId = this.lastExecutionId();
        if (queryParams['open']) {
            let openExecutionsIds: string[] = queryParams['open'].split(',');
            if (openExecutionsIds.includes(CampaignExecutionsHistoryComponent.LAST_ID) && openExecutionsIds.includes(lastExecutionId)) {
                openExecutionsIds = openExecutionsIds.filter(id => id !== lastExecutionId);
                this.tabFilters['open'] = openExecutionsIds;
                redirect = true;
            }
        }

        if (queryParams['active']) {
            let activeExecutionsIds: string[] = queryParams['active'].split(',');
            if (activeExecutionsIds.includes(CampaignExecutionsHistoryComponent.LAST_ID) && activeExecutionsIds.includes(lastExecutionId)) {
                activeExecutionsIds = activeExecutionsIds.filter(id => id !== lastExecutionId);
                this.tabFilters['active'] = activeExecutionsIds;
                redirect = true;
            }
        }

        redirect && this.updateQueryParams();
    }

    private lastExecutionId(): string | null {
        return this.campaign.campaignExecutionReports[0]?.executionId?.toString();
    }

    private isRefreshActive(): boolean {
        return this.refreshSubscription && !this.refreshSubscription.closed;
    }

    private unsubscribeRefresh() {
        if (this.isRefreshActive()) {
            this.refreshSubscription.unsubscribe();
        }
    }
}
