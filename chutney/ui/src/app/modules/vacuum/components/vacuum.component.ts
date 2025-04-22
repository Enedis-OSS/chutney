/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy } from '@angular/core';

import { VacuumService } from '@core/services';
import { BehaviorSubject, map, Observable, Subscription, switchMap } from 'rxjs';

@Component({
    selector: 'chutney-database-admin',
    templateUrl: './vacuum.component.html'
})
export class VacuumComponent implements OnDestroy {

    errorMessage: string;
    private dbSize$ = new BehaviorSubject<number>(0);
    dbSizeObs$: Observable<number>;
    vacuumReport: number[];
    vacuumRunning = false;

    private vacuumServiceSubscription: Subscription = null;

    constructor(private vacuumService: VacuumService) {
        this.vacuumReport = [];
        this.dbSizeObs$ = this.dbSize$.asObservable().pipe(
            switchMap(() => vacuumService.computeDatabaseSize()
                .pipe(
                    map((x) => x)
                )
            )
        );
    }

    ngOnDestroy(): void {
        this.vacuumServiceSubscription?.unsubscribe();
    }

    refreshDBSize() {
        this.dbSize$.next(0);
    }

    launchVacuum() {
        this.vacuumRunning = true;
        this.vacuumServiceSubscription = this.vacuumService.compactDatabase()
            .subscribe({
                next: (val: number[]) => {
                    this.vacuumReport = val;
                    this.refreshDBSize();
                },
                error: (error) => {
                    this.vacuumRunning = false;
                    this.errorMessage = ( error.error ? error.error : (error.message ? error.message : error) );
                },
                complete: () => this.vacuumRunning = false
            });
    }
}
