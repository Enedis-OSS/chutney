/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Target } from '@model';
import { ValidationService } from '../../../molecules/validation/validation.service';
import { EnvironmentService } from '@core/services';
import { Observable, Subject, takeUntil, zip } from 'rxjs';

@Component({
    selector: 'chutney-target',
    templateUrl: './target.component.html',
    styleUrls: ['./target.component.scss']
})
export class TargetComponent implements OnInit, OnDestroy {
    targets: Target[] = [];
    existingEnvs: string[] = [];
    environmentsNames: string[];
    name: string = '';
    oldName: string = '';
    errorMessage: string;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(private route: ActivatedRoute,
                private router: Router,
                private environmentService: EnvironmentService,
                public validationService: ValidationService) {
    }

    ngOnInit(): void {
        this.route.data.subscribe((data: { targets: Target[], environmentsNames: string [] }) => {
            if (data.targets.length) {
                this.name = data.targets[0].name;
                this.oldName = this.name;
            }
            this.existingEnvs = data.targets.map(target => target.environment);
            this.environmentsNames = data.environmentsNames;
            const newTargets = this.environmentsNames.filter(name => !data.targets.map(target => target.environment).includes(name))
                .map(name => new Target(
                    this.name,
                    '',
                    null,
                    name
                ));
            this.targets = [...data.targets, ...newTargets].sort((a, b) => a.environment.localeCompare(b.environment));
        });
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    save() {
        const actions$: Observable<any> [] = this.targets
            .filter(target => this.validationService.isValidUrl(target.url))
            .map(target => ({...target, name: this.name}))
            .map(target => this.existOn(target.environment) ? this.environmentService.updateTarget(this.oldName, target) : this.environmentService.addTarget(target));

        zip([...actions$])
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: () => {
                    this.router.navigate(['targets']);
                },
                error: err => this.errorMessage = err.error
            });
    }

    deleteAll() {
        this.environmentService.deleteTarget(this.oldName)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(() => {
                this.router.navigate(['targets']);
            });
    }

    delete(index: number) {
        let environmentName = this.targets[index].environment;
        this.environmentService.deleteEnvironmentTarget(environmentName, this.oldName)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(() => {
                this.targets[index] = new Target(this.oldName, '', null, environmentName);
                this.existingEnvs = this.existingEnvs.filter(env => env !== environmentName);
            });
    }

    existOn(environment: string): boolean {
        return this.existingEnvs.includes(environment);
    }

    canSave() {
        let definedTargets = this.targets.filter(target => target.url);
        return this.name && definedTargets.length &&
            definedTargets.every(target => this.validationService.isValidUrlOrSpel(target.url));
    }

    export(item: Target) {
        this.environmentService.exportTargetOn(item.environment, item.name)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                    error: (error) => {
                        this.errorMessage = error.error;
                    }
            });
    }

    import(file: File, index: number) {
        const environment = this.targets[index].environment;
        this.environmentService.importTarget(file, environment)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: target => {
                    this.targets[index] = target;
                },
                error: err => this.errorMessage = err.error
            });
    }
}
