/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, TemplateRef, ViewChild } from '@angular/core';

import { Authorization, ScenarioIndex, TestCase } from '@model';
import { JiraPluginService, LoginService, ScenarioService } from '@core/services';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Observable, of, Subject, switchMap, takeUntil, tap } from 'rxjs';
import { FileSaverService } from 'ngx-filesaver';
import { BsModalService } from 'ngx-bootstrap/modal';
import { BsModalRef } from 'ngx-bootstrap/modal/bs-modal-ref.service';
import { EventManagerService } from '@shared';
import { MenuItem } from '@shared/components/layout/menuItem';
import { EnvironmentService } from '@core/services/environment.service';
import { ScenarioExecuteModalComponent } from '@shared/components/execute-modal/scenario-execute-modal.component';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'chutney-scenario-execution-menu',
    templateUrl: './scenario-execution-menu.component.html',
    styleUrls: ['./scenario-execution-menu.component.scss'],
    standalone: false
})
export class ScenarioExecutionMenuComponent implements OnInit, OnChanges, OnDestroy {

    testCaseId: string;

    environments: Array<string>;
    testCaseMetadata: ScenarioIndex;

    @Input() canReplay: boolean;

    @ViewChild('delete_modal') deleteModal: TemplateRef<any>;

    Authorization = Authorization;
    modalRef: BsModalRef;
    rightMenuItems: MenuItem[] = [];

    private unsubscribeSub$: Subject<void> = new Subject();

    executeLastMenuItem = {
        label: 'global.actions.execute.last',
        click: this.replay.bind(this),
        iconClass: 'fa fa-play',
        authorizations: [Authorization.SCENARIO_EXECUTE]
    };
    private executionModal: NgbModalRef;

    constructor(private environmentService: EnvironmentService,
        private fileSaverService: FileSaverService,
        private jiraLinkService: JiraPluginService,
        private router: Router,
        private scenarioService: ScenarioService,
        private loginService: LoginService,
        private modalService: BsModalService,
        private ngbModalService: NgbModal,
        private route: ActivatedRoute,
        private eventManagerService: EventManagerService) {
    }

    ngOnInit(): void {
        this.route.params
            .pipe(
                tap(params => this.testCaseId = params['id']),
                tap(() => this.initRightMenu()),
                switchMap(() =>
                    forkJoin({
                        scenarioMetadata: this.scenarioService.findScenarioMetadata(this.testCaseId).pipe(
                            takeUntil(this.unsubscribeSub$),
                            tap((scenarioMetadata) => this.testCaseMetadata = scenarioMetadata)
                        ),
                        environment: this.getEnvironments().pipe(
                            takeUntil(this.unsubscribeSub$),
                            tap((environments) => this.environments = environments)
                        )
                    })
                )
            ).subscribe();
    }

    ngOnChanges(changes: SimpleChanges) {
        this.addReplayButtonIfNecessary();
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
        this.executionModal && this.executionModal.close();
    }

    private addReplayButtonIfNecessary() {
        if (!this.rightMenuItems?.find(item => item.label === 'global.actions.execute.last') && this.canReplay) {
            this.rightMenuItems?.splice(0, 0, this.executeLastMenuItem);
        }
    }

    replay() {
        this.eventManagerService.broadcast({ name: 'executeLast' });
    }

    executeScenario() {
        const executeCallback = (env: string, dataset: string) => {
            this.eventManagerService.broadcast({ name: 'execute', env: env, dataset: dataset});
        }
        this.executionModal = this.ngbModalService.open(ScenarioExecuteModalComponent, { centered: true, size: 'lg' });
        this.executionModal.componentInstance.environments = this.environments;
        this.executionModal.componentInstance.executeCallback = executeCallback;
    }

    deleteScenario(id: string) {
        let delete$ = this.scenarioService.delete(id);

        delete$
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(() => {
                this.removeJiraLink(id);
                this.router.navigateByUrl('/scenario')
                    .then(null);
            });
    }

    duplicateScenario() {
        this.router.navigateByUrl('/scenario/' + this.testCaseId + '/raw-edition' + '?duplicate=true');
    }

    exportScenario() {
        const fileName = `${this.testCaseId}-${this.testCaseMetadata.title}.chutney.hjson`;
        this.scenarioService.findRawTestCase(this.testCaseId)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe((testCase: TestCase) => {
                this.fileSaverService.saveText(testCase.content, fileName);
            });
    }

    openModal() {
        this.modalRef = this.modalService.show(this.deleteModal, { class: 'modal-sm' });
    }

    confirm(): void {
        this.modalRef.hide();
        this.deleteScenario(this.testCaseId);
    }

    decline(): void {
        this.modalRef.hide();
    }


    private removeJiraLink(id: string) {
        this.jiraLinkService.removeForScenario(id)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                error: (error) => console.log(error)
            });
    }

    private initRightMenu() {
        this.rightMenuItems = [
            {
                label: 'global.actions.execute',
                click: this.executeScenario.bind(this),
                iconClass: 'fa fa-play',
                secondaryIconClass: 'fa-solid fa-gear fa-2xs',
                authorizations: [Authorization.SCENARIO_EXECUTE]
            },
            {
                label: 'global.actions.edit',
                link: '/scenario/' + this.testCaseId + '/raw-edition',
                iconClass: 'fa fa-pencil-alt',
                authorizations: [Authorization.SCENARIO_WRITE]
            },
            {
                label: 'global.actions.delete',
                click: this.openModal.bind(this),
                iconClass: 'fa fa-trash',
                authorizations: [Authorization.SCENARIO_WRITE]
            },
            {
                label: 'global.actions.clone',
                click: this.duplicateScenario.bind(this),
                iconClass: 'fa fa-clone',
                authorizations: [Authorization.SCENARIO_WRITE]
            },
            {
                label: 'global.actions.export',
                click: this.exportScenario.bind(this),
                iconClass: 'fa fa-file-code'
            }
        ];
        this.addReplayButtonIfNecessary();
    }

    private getEnvironments(): Observable<Array<string>> {
        if (this.loginService.hasAuthorization(Authorization.SCENARIO_EXECUTE)) {
            return this.environmentService.names();
        }
        return of([])
    }

}
