/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EventManagerService } from '@shared/event-manager.service';
import { Subject, Subscription, takeUntil } from 'rxjs';
import { TestCase, Authorization } from '@model';
import { ScenarioService, LoginService } from '@core/services';
import { CanDeactivatePage } from '@core/guards';
import { HjsonParserService } from '@shared/hjson-parser/hjson-parser.service';

@Component({
    selector: 'chutney-raw-edition',
    templateUrl: './raw-edition.component.html',
    styleUrls: ['./raw-edition.component.scss'],
    standalone: false
})
export class RawEditionComponent
    extends CanDeactivatePage
    implements OnInit, OnDestroy
{
    previousTestCase: TestCase;
    testCase: TestCase;
    modificationsSaved = false;
    errorMessage: any;
    modifiedContent = '';
    saveErrorMessage: string;
    defaultContent =
        `{
           givens: [
             {
               description: step description
               implementation:
               {
                 type: success
                 inputs: {
                 }
                 outputs: {
                 }
                 validations: {
                 }
               }
             }
           ]
           when: {}
           thens: []
        }`;

    private unsubscribeSub$: Subject<void> = new Subject();

    isAuthorizedToWriteScenario: boolean = false;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private scenarioService: ScenarioService,
        private hjsonParserService: HjsonParserService,
        private loginService: LoginService
    ) {
        super();
        this.testCase = new TestCase();
        this.previousTestCase = this.testCase.clone();
        this.isAuthorizedToWriteScenario = this.loginService.hasAuthorization(Authorization.SCENARIO_WRITE);
    }

    ngOnInit() {
        this.route.params
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe((params) => {
                const duplicate =
                    this.route.snapshot.queryParamMap.get('duplicate');
                if (duplicate) {
                    this.load(params['id'], true);
                } else {
                    this.load(params['id'], false);
                }
            });
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    canDeactivatePage(): boolean {
        return (
            this.modificationsSaved ||
            this.testCase.equals(this.previousTestCase)
        );
    }

    cancel() {
        if (this.testCase.id != null) {
            this.router.navigateByUrl('/scenario/' + this.testCase.id + '/executions');
        } else {
            this.router.navigateByUrl('/scenario');
        }
    }

    load(id, duplicate: boolean) {
        if (id != null) {
            this.scenarioService.findRawTestCase(id)
                .pipe(takeUntil(this.unsubscribeSub$))
                .subscribe({
                    next: (rawScenario) => {
                        this.testCase = rawScenario;
                        this.previousTestCase = this.testCase.clone();
                        this.checkParseError();

                        if (duplicate) {
                            this.previousTestCase.id = null;
                            this.testCase.id = null;
                            this.testCase.creationDate = null;
                            this.testCase.updateDate = null;
                            this.testCase.author = null;
                            this.testCase.title = '--COPY-- ' + this.testCase.title;
                            this.testCase.defaultDataset = null;
                            this.previousTestCase.title =
                                '--COPY-- ' + this.previousTestCase.title;
                        }
                    },
                    error: (error) => {
                        console.log(error);
                        this.errorMessage = error._body;
                    }
                });
        } else {
            this.testCase.title = 'scenario title';
            this.testCase.description = 'scenario description';
            this.testCase.content = this.defaultContent;
            this.modifiedContent = this.defaultContent;
            this.previousTestCase = this.testCase.clone();
        }
    }

    private checkParseError() {
        try {
            this.hjsonParserService.parse(this.modifiedContent);
            this.errorMessage = null;
        } catch (e) {
            this.errorMessage = e;
        }
    }

    saveScenario() {
        this.testCase.content = this.modifiedContent;
        this.scenarioService.createOrUpdateRawTestCase(this.testCase)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (response) => {
                    this.modificationsSaved = true;
                    this.router.navigateByUrl(
                        '/scenario/' + response + '/executions'
                    );
                },
                error: (error) => {
                    console.log(error);
                    if (error.error) {
                        this.saveErrorMessage = error.error;
                    }
                    this.errorMessage = error._body;
                }
            });
    }

    updateTags(event: string) {
        this.testCase.tags = event.split(',');
    }

    onScenarioContentChanged(data) {
        this.modifiedContent = data;
        this.checkParseError();
    }

    selectDataset(dataset: string) {
        this.testCase.defaultDataset = dataset;
    }

}
