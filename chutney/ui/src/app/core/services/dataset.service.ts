/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@env/environment';
import { Dataset, KeyValue } from '@model';
import { HttpClient, HttpParams } from '@angular/common/http';

@Injectable({
    providedIn: 'root'
})
export class DataSetService {

    private resourceUrl = '/api/v1/datasets';

    constructor(private httpClient: HttpClient) {
    }

    findAll(usage: boolean = false): Observable<Array<Dataset>> {
        let params = new HttpParams();
        params = params.append('usage', usage ? 'true' : 'false');
        return this.httpClient.get<Array<Dataset>>(environment.backend + this.resourceUrl, { params })
            .pipe(map((res: Array<any>) => {
                res = res.map(dto => {
                    return new Dataset(
                        dto.name,
                        dto.description,
                        dto.tags,
                        dto.lastUpdated,
                        dto.uniqueValues,
                        dto.multipleValues,
                        dto.id,
                        dto.scenarioUsage,
                        dto.campaignUsage,
                        dto.scenarioInCampaignUsage,
                    )
                });

                return res;
            }));
    }

    findById(id: string): Observable<Dataset> {

        return this.httpClient.get<Dataset>(environment.backend + this.resourceUrl + '/' + id)
            .pipe(
                map(dto => this.fromDto(dto))
            );
    }

    save(dataset: Dataset, oldId?: string): Observable<Dataset> {
        DataSetService.cleanTags(dataset);
        if (dataset.id && dataset.id.length > 0) {
            return this.httpClient.put<Dataset>(environment.backend + this.resourceUrl, dataset, {params: {oldId}})
                .pipe(
                    map(dto => this.fromDto(dto))
                );
        } else {
            return this.httpClient.post<Dataset>(environment.backend + this.resourceUrl, dataset)
                .pipe(
                    map(dto => this.fromDto(dto))
                );
        }
    }

    delete(id: String): Observable<Object> {
        return this.httpClient.delete(environment.backend + this.resourceUrl + '/' + id);
    }

    private fromDto(dto: any): Dataset {
        return new Dataset(
            dto.name,
            dto.description,
            dto.tags,
            dto.lastUpdated,
            dto.uniqueValues.map(o => new KeyValue(o.key, o.value)),
            dto.multipleValues.map(l => l.map(o => new KeyValue(o.key, o.value))),
            dto.id);
    }

    private static cleanTags(dataset: Dataset) {
        if (dataset.tags != null && dataset.tags.length > 0) {
            dataset.tags = dataset.tags.map((tag) => tag.toLocaleUpperCase().trim())
                .reduce((filteredTags, tag) => {
                    if (filteredTags.indexOf(tag) < 0) {
                        filteredTags.push(tag);
                    }
                    return filteredTags;
                }, []);
        }
    }

}
