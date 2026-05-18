/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { AccessToken } from "@core/model/token.model";
import { TokenService } from "@core/services/token.service";
import { NgbDatepickerConfig } from "@ng-bootstrap/ng-bootstrap";
import { NgbDate } from '@ng-bootstrap/ng-bootstrap/datepicker/ngb-date';
import { Subject, takeUntil } from "rxjs";

@Component({
    selector: 'chutney-tokens-creation',
    templateUrl: './tokens-creation.component.html',
    styleUrls: ['./tokens-creation.component.scss'],
    standalone: false
})
export class TokenCreationComponent implements OnInit {

    tokenForm: FormGroup;

    submitted: boolean;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(
        private tokenService: TokenService,
        private formBuilder: FormBuilder,
        private router: Router,
        private configDate: NgbDatepickerConfig,
    ) {
        const currentDate = new Date();
        this.configDate.minDate = {
            year: currentDate.getFullYear(),
            month: currentDate.getMonth(),
            day: currentDate.getDate()
        };
    }

    ngOnInit() {
        this.tokenForm = this.formBuilder.group({
            note: ['', Validators.required],
            expirationDate: null,
        });
    }

    saveToken() {
        this.submitted = true;
        const formValue = this.tokenForm.value;

        if (this.tokenForm.invalid) {
            return;
        }

        const note = formValue['note'];
        const expirationDate: NgbDate = formValue['expirationDate'];
        const date = expirationDate != null ? 
            new Date(expirationDate.year, expirationDate.month, expirationDate.day, 0, 0, 0, 0) : null;

        const token: AccessToken = new AccessToken(note, date)
        
        this.tokenService.createToken(token)
                    .pipe(takeUntil(this.unsubscribeSub$))
                    .subscribe({
                        next: (response) => {
                            this.router.navigate(['/tokens', 'display'],
                                {
                                    state: { token: response }
                                });
                        },
                        error: (error) => {
                            console.log(error);
                        }
                    });
    }
}
