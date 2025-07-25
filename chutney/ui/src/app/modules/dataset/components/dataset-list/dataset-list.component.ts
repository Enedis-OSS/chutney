/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { newInstance } from '@shared/tools';
import { distinct, flatMap } from '@shared/tools/array-utils';
import { DataSetService } from '@core/services';
import { Authorization, Dataset } from '@model';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { DROPDOWN_SETTINGS } from '@core/model/dropdown-settings';
import { IDropdownSettings } from 'ng-multiselect-dropdown';

@Component({
    selector: 'chutney-dataset-list',
    templateUrl: './dataset-list.component.html',
    styleUrls: ['./dataset-list.component.scss'],
    standalone: false
})
export class DatasetListComponent implements OnInit, OnDestroy {

    readonly Object = Object;

    datasets: Array<Dataset> = [];

    preview: Dataset = null;

    dataSetFilter = '';
    itemList = [];
    selectedTags: string[] = [];
    selectedItem: any[];

    Authorization = Authorization;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(
        private router: Router,
        private dataSetService: DataSetService,
        private readonly route: ActivatedRoute,
        @Inject(DROPDOWN_SETTINGS) public dropdownSettings: IDropdownSettings
    ) {}

    ngOnInit(): void {
        this.dataSetService.findAll(true)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.datasets = res;
                    this.initTags();
                    this.applyUriState();
                },
                error: (error) => console.log(error)
            });
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    showPreview(dataset: Dataset) {
        if (this.preview == null || this.preview.id !== dataset.id) {
            this.dataSetService.findById(dataset.id)
                .pipe(takeUntil(this.unsubscribeSub$))
                .subscribe({
                    next: (res) => {
                        this.preview = res;
                    },
                    error: (error) => console.log(error)
                });
        } else {
            this.preview = null;
        }
    }

    private initTags() {
        const allTagsInDataset: string[] = distinct(flatMap(this.datasets, (sc) => sc.tags)).sort();
        let index = 0;
        this.itemList = allTagsInDataset.map(t => {
            index++;
            return { 'id': index, 'text': t };
        });
    }

    filterSearchChange(searchFilter: string) {
        this.dataSetFilter = searchFilter;
        this.applyFiltersToRoute();
    }

    onItemSelect(item: any) {
        this.selectedTags.push(item.text);
        this.selectedTags = newInstance(this.selectedTags);
        this.applyFiltersToRoute();
    }

    onItemDeSelect(item: any) {
        this.selectedTags.splice(this.selectedTags.indexOf(item.text), 1);
        this.selectedTags = newInstance(this.selectedTags);
        this.applyFiltersToRoute();
    }

    onItemDeSelectAll() {
        this.selectedTags = newInstance([]);
        this.applyFiltersToRoute();
    }

    applyFiltersToRoute() {
        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {
                text: this.dataSetFilter ? this.dataSetFilter : null,
                tags: this.selectedItem?.length ? this.selectedItem.map((i) => i.text).toString() : null
            }
        });
    }

    private applyUriState() {
        this.route.queryParams
            .pipe(
                takeUntil(this.unsubscribeSub$),
                map((params: Array<any>) => {
                    if (params['text']) {
                        this.dataSetFilter = params['text'];
                    }
                    if (params['tags']) {
                        const uriTag = params['tags'].split(',');
                        if (uriTag != null) {
                            this.selectedItem = this.itemList.filter((tagItem) => uriTag.includes(tagItem.text));
                            this.selectedTags = this.selectedItem.map((i) => i.text);
                            this.applyFiltersToRoute();
                        }
                    }
                }))
            .subscribe();
    }
}
