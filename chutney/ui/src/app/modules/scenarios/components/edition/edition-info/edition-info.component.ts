/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, HostListener, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';

import { EditionService } from '@core/services';
import { TestCaseEdition } from '@model';
import { lastValueFrom, Subject, takeUntil } from 'rxjs';

@Component({
    selector: 'chutney-edition-info',
    templateUrl: './edition-info.component.html',
    styleUrls: ['./edition-info.component.scss']
})
export class EditionInfoComponent implements OnChanges, OnDestroy {
    @Input() testCase;

    edition: TestCaseEdition;
    editions: Array<TestCaseEdition> = [];

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(private editionService: EditionService) {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (this.testCase && this.testCase.id) {
            const id = this.testCase.id;
            this.editionService.editTestCase(id)
                .pipe(takeUntil(this.unsubscribeSub$))
                .subscribe({
                    next: edition => {
                        this.edition = edition;
                        this.editionService.findAllTestCaseEditions(id)
                            .pipe(takeUntil(this.unsubscribeSub$))
                            .subscribe(
                                editions => { this.editions = editions.filter(e => e.editionUser != edition.editionUser); }
                            );
                    },
                    error: error => {
                        console.log(error);
                    }
                });
        }
    }

    @HostListener('window:beforeunload')
    async ngOnDestroy() {
        if (this.testCase.id != null) {
            await lastValueFrom(this.editionService.endTestCaseEdition(this.testCase.id));
        }
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }
}
