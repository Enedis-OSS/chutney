/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, inject, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { EnvironmentService } from "@core/services";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { ValidationService } from "src/app/molecules/validation/validation.service";

@Component({
    selector: 'chutney-environment-import',
    templateUrl: './environment-import.component.html',
    standalone: false,
})
export class EnvironmentImportComponent implements OnInit, OnDestroy {

    activeModal = inject(NgbActiveModal);

    private unsubscribeSub$: Subject<void> = new Subject();

    importForm: FormGroup;

    submitted: boolean;

    errorMessage: string;
    nameValidationMessage: string;

    importedFile:File;

    constructor(private environmentService: EnvironmentService,
        public validationService: ValidationService,
        private formBuilder: FormBuilder,
        private translateService: TranslateService) {}

    ngOnInit() {
        this.importForm = this.formBuilder.group({
            name: ['', Validators.required],
        });
    }

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    select(file: File) {
        this.importedFile = file;
    }

    import() {
        this.submitted = true;
        const formValue = this.importForm.value;

        if (this.importForm.invalid) {
            return;
        }

        const name = formValue['name'];
        var isNewEnvironmentInvalid = !this.validationService.isValidEnvName(name);
        if ( isNewEnvironmentInvalid) {
            this.nameValidationMessage = this.translateService.instant('global.rules.env.name');
            this.importForm.setErrors(Validators.pattern)
            return;
        } else {
            this.nameValidationMessage = null;
        }

        this.environmentService.import(name, this.importedFile)
                    .pipe(takeUntil(this.unsubscribeSub$))
                    .subscribe({
                        next: (environment) => {
                            this.activeModal.close(environment);
                        },
                        error: (error) => {
                            this.errorMessage = error.error
                        }
                    });
    }

}
