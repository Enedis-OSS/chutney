/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';

import { Environment, Target, Authorization } from '@model';
import { EnvironmentService, LoginService } from '@core/services';
import { distinct, filterOnTextContent, match } from '@shared/tools';
import { Subscription } from 'rxjs';

@Component({
    selector: 'chutney-targets',
    templateUrl: './targets.component.html',
    styleUrls: ['./targets.component.scss'],
    standalone: false
})
export class TargetsComponent implements OnInit, OnDestroy {

    errorMessage: string = null;
    environments: Environment[] = [];
    targetsNames: string[] = [];
    targets: Target[] = [];

    environmentFilter: Environment;
    targetFilter = '';

    isAuthorizedToWriteTargets: boolean = false;

    private environmentServiceSubscription: Subscription = null;

    constructor(
        private environmentService: EnvironmentService,
        private loginService: LoginService
    ) {
            this.isAuthorizedToWriteTargets = this.loginService.hasAuthorization(Authorization.TARGET_WRITE);
    }

    ngOnInit() {
        this.loadTargets();
    }

    ngOnDestroy(): void {
        this.environmentServiceSubscription?.unsubscribe();
    }

    private loadTargets() {
        this.environmentServiceSubscription = this.environmentService.listTargets().subscribe({
            next: envs => {
                this.environments = envs;
                this.targets = envs.flatMap(env => env.targets).sort(this.targetSortFunction());
                this.targetsNames = distinct(this.targets.map(target => target.name));
            },
            error: error => this.errorMessage = error.error
        });
    }


    findTarget(targetName: string, environment: Environment): Target {
        return environment?.targets.find(target => target.name === targetName);
    }

    exist(targetName: string, environment: Environment): boolean {
        return !!this.findTarget(targetName, environment);
    }



    activeEnvironmentTab(targetName: string) : string{
        if (this.environmentFilter) {
            return this.environmentFilter.name;
        }
        return this.environments.find(env =>
            this.targetFilter ? this.matchEnv(env, targetName): this.exist(targetName, env)
        ).name;
    }

    private matchEnv(env: Environment, targetName) {
        return !!env.targets.find(target => target.name === targetName && this.match(target));
    }

    private filterByKeyword(targets: Target[]): Target[] {
        if (!!this.targetFilter) {
            return  targets.filter(target => this.match(target));
        }
        return targets;

    }

    filter(env: Environment = null) {
        if (env) {
            if (env.name === this.environmentFilter?.name) {
                this.environmentFilter = null;
                this.targets = this.environments.flatMap(e => e.targets);
            } else {
                this.environmentFilter = env;
                this.targets = env.targets;
            }
            this.targets.sort(this.targetSortFunction());
        }

       this.targetsNames = distinct(this.filterByKeyword(this.targets).map(target => target.name));
    }

    private match(target: Target): boolean{
        return match(target.name, this.targetFilter) || match(target.url, this.targetFilter) ||
            filterOnTextContent(target.properties, this.targetFilter, ['key', 'value'])?.length;
    }

    private targetSortFunction(): (a: Target, b: Target) => number {
        return (t1, t2) => t1.name.localeCompare(t2.name);
    }
}
