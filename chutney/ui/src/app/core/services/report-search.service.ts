/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Execution } from '@core/model';

@Injectable({
  providedIn: 'root'
})
export class ReportSearchService {

  private reportUrl = '/api/v1/execution/report';

  constructor(private http: HttpClient) { }

  getExecutionReportMatchQuery(query: string): Observable<Execution[]> {
    return this.http.get<Execution[]>(environment.backend + this.reportUrl + '/search', {params: {query: query}})
    .pipe(
      map((res: Execution[]) => {
          return res.map((execution) => Execution.deserialize(execution));
      })
    )
  }
}
