/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { DataSetService } from '@core/services';
import { Dataset } from '@model';
import { Subscription } from 'rxjs';


@Component({
    selector: 'chutney-dataset-selection',
    templateUrl: './dataset-selection.component.html',
    styleUrls: ['./dataset-selection.component.scss']
})
export class DatasetSelectionComponent implements OnInit, OnDestroy {

    @Input() selectedDatasetId: String;
    @Output() selectionEvent = new EventEmitter();

    datasets: Array<Dataset>;

    private datasetServiceSubscription: Subscription;

    constructor(private datasetService: DataSetService) {}

    ngOnInit(): void {
        this.datasetServiceSubscription = this.datasetService.findAll().subscribe((res: Array<Dataset>) => {
            this.datasets = res;
        });
    }

    ngOnDestroy(): void {
        this.datasetServiceSubscription?.unsubscribe();
    }

    changingValue(event: any) {
        this.selectionEvent.emit(event.target.value);
    }

}
