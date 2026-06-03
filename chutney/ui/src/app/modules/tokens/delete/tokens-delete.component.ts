/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, inject, Input, OnDestroy } from "@angular/core";
import { TokenService } from "@core/services/token.service";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { Subject, takeUntil } from "rxjs";

@Component({
    selector: 'chutney-tokens-delete',
    templateUrl: './tokens-delete.component.html',
    styleUrls: ['./tokens-delete.component.scss'],
    standalone: false
})
export class TokenDeleteComponent implements OnDestroy {

    activeModal = inject(NgbActiveModal);

    @Input() id: string;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(
        private tokenService: TokenService,
    ) {
    }

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    deleteToken() {
        this.tokenService.deleteToken(this.id)
                    .pipe(takeUntil(this.unsubscribeSub$))
                    .subscribe({
                        next: (response) => {
                            this.activeModal.close(response);
                        },
                        error: (error) => {
                            console.log(error);
                        }
                    });
    }
}
