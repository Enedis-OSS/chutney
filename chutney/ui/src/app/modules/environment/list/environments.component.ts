/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, DoCheck, OnDestroy, OnInit } from '@angular/core';
import { Environment, Authorization } from '@model';
import { ActivatedRoute } from '@angular/router';
import { EnvironmentService, LoginService } from '@core/services';
import { ValidationService } from '../../../molecules/validation/validation.service';
import { TranslateService } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';

@Component({
    selector: 'chutney-environments',
    templateUrl: './environments.component.html',
    styleUrls: ['./environments.component.scss'],
    standalone: false
})
export class EnvironmentsComponent implements OnInit, DoCheck, OnDestroy {

    editableEnvironments: Environment[] = [];
    environments: Environment[] = [];
    environment: Environment;
    editionIndex: number;
    errorMessage: string;
    nameValidationMessage: string;
    errorDeleteLastMessage: string;

    isAuthorizedToWriteEnvironments: boolean = false;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(private route: ActivatedRoute,
                private environmentService: EnvironmentService,
                public validationService: ValidationService,
                private loginService: LoginService,
                private translateService: TranslateService
    ) {
        this.translateService.get("environment.error.delete.last.env").subscribe(msg => this.errorDeleteLastMessage = msg)

        this.isAuthorizedToWriteEnvironments = this.loginService.hasAuthorization(Authorization.ENVIRONMENT_WRITE);
    }

    ngOnInit(): void {
        this.route.data.subscribe((data: { environments: Environment[] }) => {
            this.editableEnvironments = data.environments;
            this.environments = data.environments.map(env => ({...env}));
        });
    }

    ngDoCheck() {
        var isNewEnvironmentInvalid = this.environment && !this.validationService.isValidEnvName(this.environment.name);
        var isEditableEnvironmentInvalid = this.editionIndex >= 0 && !this.validationService.isValidEnvName(this.editableEnvironments[this.editionIndex]?.name);
        if ( isNewEnvironmentInvalid || isEditableEnvironmentInvalid) {
            this.nameValidationMessage = this.translateService.instant('global.rules.env.name');
        } else {
            this.nameValidationMessage = null;
        }
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    editing(index: number): boolean {
        return this.editionIndex === index;
    }

    save(index: number) {
        this.environmentService.update(this.environments[index].name, this.editableEnvironments[index])
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: () => {
                    this.environments[index] = {...this.editableEnvironments[index]};
                    this.sort();
                    this.editionIndex = null;
                },
                error: err => this.errorMessage = err.error
            });
    }

    enableAdd() {
        this.environment = new Environment('', '');
    }

    add() {
        this.environmentService.create(this.environment)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: () => {
                    this.editableEnvironments.push(this.environment);
                    this.environments.push(this.environment);
                    this.sort();
                    this.environment = null;
                },
                error: err => {
                    this.errorMessage = err.error
                }
            });
    }

    delete(name: string, index: number) {
        this.environmentService.delete(name)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: () => {
                    this.editableEnvironments.splice(index, 1);
                    this.environments.splice(index, 1);
                },
                error: err => {
                    if (err.status === 409) {
                        this.errorMessage = this.errorDeleteLastMessage;
                    } else {
                        this.errorMessage = err.error
                    }
                }
            });
    }

    export(env: Environment) {
        this.environmentService.export(env.name)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                error: err => this.errorMessage = err.error
            });
    }

    import(file: File) {
        this.environmentService.import(file)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: env => {
                    this.editableEnvironments.push(env);
                    this.environments.push(env);
                    this.sort();
                },
                error: err => this.errorMessage = err.error
            });
    }

    private sort() {
        this.environments.sort((e1, e2) => e1.name.toUpperCase() > e2.name.toUpperCase() ? 1 : -1)
        this.editableEnvironments.sort((e1, e2) => e1.name.toUpperCase() >= e2.name.toUpperCase() ? 1 : -1)
    }
}
