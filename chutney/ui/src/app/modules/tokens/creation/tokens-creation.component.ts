/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup, Validators, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { AccessToken } from "@core/model/token.model";
import { TokenService } from "@core/services/token.service";
import { NgbActiveModal, NgbDatepickerConfig } from "@ng-bootstrap/ng-bootstrap";
import { NgbDate } from '@ng-bootstrap/ng-bootstrap';
import { Subject, takeUntil } from "rxjs";
import { NgbInputDatepicker } from "@ng-bootstrap/ng-bootstrap/datepicker";
import { TranslatePipe } from "@ngx-translate/core";

@Component({
    selector: 'chutney-tokens-creation',
    templateUrl: './tokens-creation.component.html',
    styleUrls: ['./tokens-creation.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [FormsModule,
        ReactiveFormsModule,
        NgbInputDatepicker,
        TranslatePipe
    ]
})
export class TokenCreationComponent implements OnInit {

    activeModal = inject(NgbActiveModal);

    tokenForm: UntypedFormGroup;

    submitted: boolean;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(
        private tokenService: TokenService,
        private formBuilder: UntypedFormBuilder,
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

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
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
            new Date(Date.UTC(expirationDate.year, expirationDate.month - 1, expirationDate.day, 0, 0, 0, 0)) : null;

        const token: AccessToken = new AccessToken('', note, date)

        this.tokenService.createToken(token)
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
