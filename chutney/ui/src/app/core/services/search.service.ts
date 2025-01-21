/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Hit } from '@core/model/search/hit.model';
import { environment } from '@env/environment';
import { catchError, debounceTime, distinctUntilChanged, map, Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
    private searchUri = '/api/search';

    constructor(private httpClient: HttpClient) {
    }

    search(keyword: string): Observable<Hit[]> {
        return this.httpClient.get<Hit[]>(environment.backend + `${this.searchUri}?keyword=${keyword}`).pipe(
          map(data => data.map(hit => new Hit(hit.id, hit.title, hit.description, hit.content, hit.tags, hit.what))), 
          catchError(error => {
            console.error('Search API Error:', error);
            return of([]);
          })
        );
    }
}
