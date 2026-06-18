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
import { Subject, takeUntil } from "rxjs";

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

    constructor(private environmentService: EnvironmentService,
        private formBuilder: FormBuilder) {}

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
        this.importedEnvironment.name = name;

        this.environmentService.create(this.importedEnvironment)
                    .pipe(takeUntil(this.unsubscribeSub$))
                    .subscribe({
                        next: () => {
                            this.activeModal.close(this.importedEnvironment);
                        },
                        error: (error) => {
                            console.log(error);
                        }
                    });
    }

}
