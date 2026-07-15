/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { AccessToken, CreatedAccessToken } from "@core/model/token.model";
import { TokenService } from "@core/services/token.service";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { Subject, takeUntil } from "rxjs";
import { TokenCreationComponent } from "../creation/tokens-creation.component";
import { TokenDeleteComponent } from "../delete/tokens-delete.component";

@Component({
    selector: 'chutney-tokens',
    templateUrl: './tokens.component.html',
    styleUrls: ['./tokens.component.scss'],
    standalone: false
})
export class TokenListComponent implements OnInit, OnDestroy {

    createdAccessToken:CreatedAccessToken;
    displayCreatedToken: boolean;

    constructor(
        private tokenService: TokenService,
        private ngbModalService: NgbModal
        ) {
    }

    tokens: Array<AccessToken> = [];

    private unsubscribeSub$: Subject<void> = new Subject();

    ngOnInit() {
        this.initTokens();
    }

    initTokens() {
        this.tokenService.getTokensForUser()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.tokens = res;
                },
                error: (error) => console.log(error)
            });
    }

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    createToken() {
        const modalRef = this.ngbModalService.open(TokenCreationComponent, { centered: true, size: 'sm' });

        modalRef.result.then(
            (createdAccessToken) => {
                this.createdAccessToken = createdAccessToken;
                this.displayCreatedToken = true;
                this.tokens.unshift(createdAccessToken)
            });
    }

    deleteToken(id: string) {
        const modalRef = this.ngbModalService.open(TokenDeleteComponent, { centered: true, size: 'sm' });
        modalRef.componentInstance.id = id;

        modalRef.result.then(() => {
            this.displayCreatedToken = false;
            this.initTokens();
        })
    }

    copyToClipboard() {
        navigator.clipboard.writeText(this.createdAccessToken.token);
    }
}
