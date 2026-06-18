/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, inject, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { Environment } from "@core/model";
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

    @Input()
    importedEnvironment: Environment;

    private unsubscribeSub$: Subject<void> = new Subject();

    importForm: FormGroup;

    submitted: boolean;

    errorMessage: string;
    nameValidationMessage: string;

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
        this.importedEnvironment.name = name;

        this.environmentService.create(this.importedEnvironment)
                    .pipe(takeUntil(this.unsubscribeSub$))
                    .subscribe({
                        next: () => {
                            this.activeModal.close(this.importedEnvironment);
                        },
                        error: (error) => {
                            this.errorMessage = error.error
                        }
                    });
    }

}
