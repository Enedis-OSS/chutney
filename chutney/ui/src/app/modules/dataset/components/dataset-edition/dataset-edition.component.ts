/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import {
    AbstractControl,
    FormArray,
    FormBuilder,
    FormControl,
    FormGroup,
    ValidationErrors,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { of, Subject, takeUntil } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { delay } from '@shared/tools';
import { CanDeactivatePage } from '@core/guards';
import { DataSetService } from '@core/services';
import { Dataset, KeyValue } from '@model';

type Log = { level: 'danger' | 'info', content: string }

@Component({
    selector: 'chutney-dataset-edition',
    templateUrl: './dataset-edition.component.html',
    styleUrls: ['./dataset-edition.component.scss'],
    standalone: false
})
export class DatasetEditionComponent extends CanDeactivatePage implements OnInit, OnDestroy, AfterViewInit {

    private readonly emptyDataset = new Dataset('', '', [], new Date(), [], []);
    dataset: Dataset;
    activeTab = 'keyValue';
    datasetForm: FormGroup;
    message: Log;
    private modificationsSaved = false;
    private unsubscribeSub$: Subject<void> = new Subject();
    private savedMessage: string;

    errorDuplicateHeaderMessage: string
    @ViewChild('dataSetName') dataSetName: ElementRef;

    constructor(private dataSetService: DataSetService,
                private router: Router,
                private route: ActivatedRoute,
                private translate: TranslateService,
                private formBuilder: FormBuilder,
                private location: Location) {
        super();
    }

    ngOnInit(): void {
        this.route.params
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe((params) => {
                this.initForm(params['id']);
            });

        this.initTranslation();

    }

    ngAfterViewInit(): void {
        if (this.dataset && !this.dataset.id) {
            this.dataSetNameFocus();
        }
    }

    private initTranslation() {
        this.savedMessage = this.translate.instant('global.actions.done.saved');
        this.errorDuplicateHeaderMessage = this.translate.instant('dataset.edition.datatable.validations.duplicatedHeader');
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    save() {
        this.modificationsSaved = false;
        const dataset = this.formToDataset();
        if (this.datasetForm.invalid) {
            this.notify({level: 'danger', content: this.translate.instant('dataset.error.invalid')});
            return;
        }
        const obs$ = this.dataset.id ? this.dataSetService.update(this.dataset.id, dataset) : this.dataSetService.create(dataset);
        obs$.pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.notify({level: 'info', content: this.savedMessage});
                    this.modificationsSaved = true;
                    this.router.navigateByUrl('/dataset/' + res.id + '/edition');
                },
                error: (error) => {
                    this.notify({level: 'danger', content: error.error});

                }
            });
    }

    notify(message: Log) {
        (async () => {
            this.message = message;
            await delay(5000);
            this.message = null;
        })();
    }

    canDeactivatePage(): boolean {
        return this.modificationsSaved || this.formToDataset().equals(this.dataset);
    }

    cancel() {
        this.location.back();
    }

    selectTab(tab: string) {
        this.activeTab = tab;
    }

    deleteDataset() {
        this.dataSetService.delete(this.dataset.id)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: () => {
                    this.modificationsSaved = true;
                    this.router.navigateByUrl('/dataset');
                },
                error: (error) => console.log(error)
            });
    }

    asFormArray(keyValues: AbstractControl): FormArray {
        return keyValues as FormArray;
    }

    private formToDataset() {
        const {name, description, tags, keyValues, multiKeyValues} = this.datasetForm.value;
        return new Dataset(
            name,
            description,
            tags,
            new Date(),
            keyValues,
            multiKeyValues,
            this.dataset.id ? this.dataset.id : null
        );
    }

    private dataSetNameFocus(): void {
        this.dataSetName.nativeElement.focus();
    }

    private initForm(datasetId: string) {
        const dataset$ = !datasetId ? of(this.emptyDataset) : this.dataSetService.findById(datasetId)
        dataset$.pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(
                dataset => {
                    this.dataset = dataset;
                    this.datasetForm = this.formBuilder.group({
                        name: new FormControl(dataset.name, [Validators.required]),
                        description: new FormControl(dataset.description),
                        tags: new FormControl(dataset.tags.join(',')),
                        keyValues: this.toFormArray(dataset.uniqueValues, this.duplicatedKeysValidator()),
                        multiKeyValues: new FormArray(dataset.multipleValues.map(row => this.toFormArray(row)), [this.duplicatedHeadersValidator()])
                    });
                }
            );

    }

    private toFormArray(keyValues: Array<KeyValue>, validator?: ValidatorFn): FormArray {
        const formArray = new FormArray(keyValues.map(kv => new FormGroup({
            key: new FormControl(kv.key),
            value: new FormControl(kv.value)
        })));
        if (validator) {
            formArray.addValidators(validator);
        }
        return formArray;
    }


    private duplicatedHeadersValidator(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const datatable = (control as FormArray).value as KeyValue[][];
            const hasDuplicates = this.hasDuplicatedHeaders(datatable);
            return hasDuplicates ? { duplicatedHeader: true } : null;
        };
    }

    private duplicatedKeysValidator(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const keyValues = (control as FormArray).value as KeyValue[];
            const hasDuplicates = this.hasDuplicatedKeys(keyValues);
            return hasDuplicates ? { duplicatedKey: true } : null;
        };
    }


    private hasDuplicatedHeaders(multiKeyValues: Array<Array<KeyValue>>): boolean {
        if (!multiKeyValues || multiKeyValues.length === 0) return false;
        return this.hasDuplicatedKeys(multiKeyValues[0]);
    }

    private hasDuplicatedKeys(keyValues: Array<KeyValue>): boolean {
        if (!keyValues || keyValues.length === 0) return false;
        const keys = keyValues.map(kv => kv.key);
        const headerCount = keys.reduce<Record<string, number>>((acc, header) => {
            acc[header] = (acc[header] || 0) + 1;
            return acc;
        }, {});
        return Object.values(headerCount).some(count => count > 1);
    }
}
