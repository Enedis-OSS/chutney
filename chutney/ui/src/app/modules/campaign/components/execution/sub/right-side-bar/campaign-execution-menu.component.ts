/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { combineLatest, identity, Observable, of, Subject, timer } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';

import { BsModalService } from 'ngx-bootstrap/modal';
import { BsModalRef } from 'ngx-bootstrap/modal/bs-modal-ref.service';
import { FileSaverService } from 'ngx-filesaver';

import * as JSZip from 'jszip';

import { CampaignService, EnvironmentService, JiraPluginService, LoginService, ScenarioService } from '@core/services';
import { Authorization, Campaign, Dataset, ScenarioIndex, TestCase } from '@model';
import { EventManagerService } from '@shared';
import { MenuItem } from '@shared/components/layout/menuItem';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ScenarioExecuteModalComponent } from '@shared/components/execute-modal/scenario-execute-modal.component';

@Component({
    selector: 'chutney-campaign-execution-menu',
    templateUrl: './campaign-execution-menu.component.html',
    styleUrls: ['./campaign-execution-menu.component.scss'],
    standalone: false
})
export class CampaignExecutionMenuComponent implements OnInit, OnChanges, OnDestroy {

    @Input() canReplay: boolean;
    @Input() campaign: Campaign;
    rightMenuItems: MenuItem[] = [];

    private environments: Array<string> = [];
    private modalRef: BsModalRef;
    private unsubscribeSub$: Subject<void> = new Subject();

    executeLastMenuItem = {
        label: 'global.actions.execute.last',
        click: this.replay.bind(this),
        iconClass: 'fa fa-play',
        authorizations: [Authorization.CAMPAIGN_EXECUTE]
    };

    @ViewChild('delete_modal') deleteModal: TemplateRef<any>;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private campaignService: CampaignService,
        private scenarioService: ScenarioService,
        private environmentService: EnvironmentService,
        private fileSaverService: FileSaverService,
        private jiraLinkService: JiraPluginService,
        private loginService: LoginService,
        private modalService: BsModalService,
        private eventManagerService: EventManagerService,
        private ngbModalService: NgbModal) {
    }

    ngOnInit(): void {
        this.environments$()
            .pipe(
                takeUntil(this.unsubscribeSub$),
                tap(() => this.initRightMenu())
            )
            .subscribe(environments => {
                this.environments = environments;
            });
    }

    ngOnChanges(changes: SimpleChanges) {
        this.addReplayButtonIfNecessary()
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    private addReplayButtonIfNecessary() {
        if (!this.rightMenuItems?.find(item => item.label === 'global.actions.execute.last') && this.canReplay) {
            this.rightMenuItems.splice(0, 0, this.executeLastMenuItem);
        }
    }

    private replay() {
        this.eventManagerService.broadcast({ name: 'executeLast' });
    }

    private environments$(): Observable<string[]> {
        if (this.loginService.hasAuthorization(Authorization.CAMPAIGN_EXECUTE)) {
            return this.environmentService.names();
        }
        return of([]);
    }

    private executeCampaign() {
        const executeCallback = (env: string, dataset: Dataset) => {
            this.broadcastCatchError(this.campaignService.executeCampaign(this.campaign.id, env, dataset))
                .pipe(takeUntil(this.unsubscribeSub$))
                .subscribe();
            timer(1000).pipe(
                takeUntil(this.unsubscribeSub$),
                switchMap(() => of(this.eventManagerService.broadcast({ name: 'execute', env: env })))
            ).subscribe();
        }

        let modalSize: "lg" | "xl" = "lg"
        const changeModalSize = (size: "lg" | "xl") => {
            modalSize = size;
            modalRef.update({size: modalSize})
        }
        const modalRef = this.ngbModalService.open(ScenarioExecuteModalComponent, { centered: true, size: modalSize });
        modalRef.componentInstance.environments = this.environments;
        modalRef.componentInstance.executeCallback = executeCallback;
        modalRef.componentInstance.changeModalSize = changeModalSize;
    }

    private deleteCampaign() {
        combineLatest([
            this.broadcastCatchError(this.campaignService.delete(this.campaign.id)),
            this.broadcastCatchError(this.jiraLinkService.removeForCampaign(this.campaign.id))
        ])
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(() => this.router.navigateByUrl('/campaign'));
    }

    private exportCampaign() {
        combineLatest([
            this.broadcastCatchError(this.campaignService.find(this.campaign.id)),
            this.broadcastCatchError(this.campaignService.findAllScenarios(this.campaign.id))
        ])
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(([campaign, scenarios]) => this.createZip(campaign.title, scenarios));
    }

    private createZip(campaignTitle: string, scenarios: ScenarioIndex[]) {
        const $rawTestCases: Array<Observable<TestCase>> = [];

        for (const testCase of scenarios) {
            $rawTestCases.push(this.scenarioService.findRawTestCase(testCase.id));
        }

        combineLatest($rawTestCases)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(rawTestCases => {
                const zip = new JSZip();
                rawTestCases.forEach(testCase => {
                    const fileName = `${testCase.id}-${testCase.title}.chutney.hjson`;
                    zip.file(fileName, testCase.content);
                });

                zip.generateAsync({ type: 'blob' })
                    .then(blob => this.fileSaverService.save(blob, campaignTitle));
            });
    }

    private openDeleteModal() {
        this.modalRef = this.modalService.show(this.deleteModal, { class: 'modal-sm' });
    }

    confirmDelete(): void {
        this.modalRef.hide();
        this.deleteCampaign();
    }

    declineDelete(): void {
        this.modalRef.hide();
    }

    private initRightMenu() {
        const emptyCampaign = this.hasCampaignWithoutScenarios();
        this.rightMenuItems = [
            {
                label: emptyCampaign ? 'campaigns.execution.error.empty' : 'global.actions.execute',
                click: this.executeCampaign.bind(this),
                iconClass: 'fa fa-play',
                secondaryIconClass: 'fa-solid fa-gear fa-2xs',
                authorizations: [Authorization.CAMPAIGN_EXECUTE],
            },
            {
                label: 'global.actions.edit',
                link: `/campaign/${this.campaign.id}/edition`,
                iconClass: 'fa fa-pencil-alt',
                authorizations: [Authorization.CAMPAIGN_WRITE]
            },
            {
                label: 'global.actions.delete',
                click: this.openDeleteModal.bind(this),
                iconClass: 'fa fa-trash',
                authorizations: [Authorization.CAMPAIGN_WRITE]
            },
            {
                label: 'global.actions.export',
                click: this.exportCampaign.bind(this),
                iconClass: 'fa fa-file-code',
                authorizations: [Authorization.CAMPAIGN_WRITE]
            }
        ];
        this.addReplayButtonIfNecessary()
    }

    private broadcastError(errorMessage: string) {
        this.eventManagerService.broadcast({ name: 'error', msg: errorMessage });
    }

    private broadcastCatchError(obs: Observable<any>, errorHandler: (error: any) => string = identity): Observable<any> {
        return obs.pipe(
            catchError((err: HttpErrorResponse) => {
                this.broadcastError(errorHandler(err.error));
                throw err;
            })
        );
    }

    private hasCampaignWithoutScenarios(): boolean {
        return this.campaign.scenarios.length == 0;
    }
}
