/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';

import { of, Subject, takeUntil } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { delay } from '@shared/tools';
import { CanDeactivatePage } from '@core/guards';
import { DataSetService } from '@core/services';
import { ValidationService } from '../../../../molecules/validation/validation.service';
import { Dataset, KeyValue } from '@model';

@Component({
    selector: 'chutney-dataset-edition',
    templateUrl: './dataset-edition.component.html',
    styleUrls: ['./dataset-edition.component.scss'],
    standalone: false
})
export class DatasetEditionComponent extends CanDeactivatePage implements OnInit, OnDestroy, AfterViewInit {

    dataset: Dataset = new Dataset('', '', [], new Date(), [], []);

    activeTab = 'keyValue';
    datasetForm: FormGroup;
    private unsubscribeSub$: Subject<void> = new Subject();
    private modificationsSaved = false;
    message;
    backendError;
    private savedMessage: string;
    errorDuplicateHeader = false;

    errorDuplicateHeaderMessage: string
    @ViewChild('dataSetName') dataSetName: ElementRef;

    constructor(private dataSetService: DataSetService,
                private router: Router,
                private route: ActivatedRoute,
                private validationService: ValidationService,
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
        this.dataSetNameFocus();
    }

    private initTranslation() {
        this.translate.get('global.actions.done.saved').subscribe((res: string) => {
            this.savedMessage = res;
        });
        this.translate.get('components.dataset.error.duplicatedHeader').subscribe((res: string) => {
            this.errorDuplicateHeaderMessage = res;
        });
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    isValid(): boolean {
        return this.validationService.isNotEmpty(this.datasetForm.value['name']);
    }

    save() {
        const dataset = this.formToDataset();
        this.errorDuplicateHeader = false;
        const obs$ = this.dataset.id ? this.dataSetService.update(this.dataset.id, dataset) : this.dataSetService.create(dataset);
        obs$.pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    //this.setCurrentDataSet(res);
                    this.notify(this.savedMessage, null);
                    this.modificationsSaved = true;
                    this.router.navigateByUrl('/dataset/' + res.id + '/edition');
                },
                error: (error) => {
                    if (error.status === 400) {
                        this.errorDuplicateHeader = true;
                        this.notify(this.errorDuplicateHeaderMessage + ':', error.error);
                        this.modificationsSaved = false;
                    } else {
                        this.notify(null, error.error);
                        this.modificationsSaved = false;
                    }
                }
            });
    }

    notify(message: string, backendError: string) {
        (async () => {
            this.message = message;
            this.backendError = backendError;
            await delay(5000);
            this.message = null;
            this.backendError = null;
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

    private formToDataset() {
        const name = this.datasetForm.value['name'] ;
        const desc = this.datasetForm.value['description'];
        const tags = this.datasetForm.value['tags'] ? this.datasetForm.value['tags'].split(',') : [];
        const date = new Date();

        const kv = this.datasetForm.controls['keyValues'] as FormArray;
        const keyValues = kv.value ? kv.value.map((p) => new KeyValue(p.key, p.value)) : [];

        const mkv = this.datasetForm.controls['multiKeyValues'] as FormArray;
        const multiKeyValues = mkv.value ? mkv.value.map(a => a.map((p) => new KeyValue(p.key, p.value))) : [];

        const id = this.dataset.id ? this.dataset.id : null;

        return new Dataset(
            name,
            desc,
            tags,
            date,// todo move to backend
            keyValues,
            multiKeyValues,
            id
        );
    }

    private dataSetNameFocus(): void {
        if (this.dataset.id == null || this.dataset.id.length === 0) {
            this.dataSetName.nativeElement.focus();
        }
    }

    private initForm(datasetId: string) {
        const dataset$ = datasetId === null ? of(new Dataset('', '', [], null, [], [])) :
            this.dataSetService.findById(datasetId).pipe(takeUntil(this.unsubscribeSub$))
        dataset$.subscribe(
                dataset => {
                    this.datasetForm = this.formBuilder.group({
                        name: new FormControl(dataset.name, [Validators.required]),
                        description: new FormControl(dataset.description),
                        tags: new FormControl(dataset.tags.join(',')),
                        keyValues: new FormControl(dataset.uniqueValues),
                        multiKeyValues: new FormControl(dataset.multipleValues)
                    });
                    this.dataset = dataset;
                }
            );

    }
}
