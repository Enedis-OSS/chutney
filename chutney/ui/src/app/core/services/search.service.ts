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
import { catchError, map, Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
    private readonly searchUri = '/api/search';

    constructor(private httpClient: HttpClient) {
    }

    search(keyword: string): Observable<Hit[]> {
      const encodedKeyword = encodeURIComponent(keyword);
        return this.httpClient.get<Hit[]>(environment.backend + `${this.searchUri}?keyword=${encodedKeyword}`).pipe(
          map(data => data.map(Hit.fromJson)),
          catchError(error => {
            console.error('Search API Error:', error);
            return of([]);
          })
        );
    }
}
