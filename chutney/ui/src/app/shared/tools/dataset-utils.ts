/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TranslateService } from '@ngx-translate/core';
import { Dataset } from '@model';
import { Injectable } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class DatasetUtils {

    constructor(private translateService: TranslateService) {}

    public getDatasetName(dataset?: Dataset) {
        if (!dataset) return '';
        return this.getExecutionDatasetName(dataset.id);
    }

    public getExecutionDatasetName(datasetId?: string) {
        if (datasetId) {
            if (Dataset.CUSTOM_ID === datasetId) {
                return  this.translateService.instant("dataset.customLabel");
            }
            return datasetId;
        }
        return ''
    }
}
