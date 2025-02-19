/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

@Injectable({
  providedIn: 'root'
})
export class VacuumService {

  private adminUrl = '/api/v1/admin/database';

  constructor(private http: HttpClient) { }

  compactDatabase(): Observable<number[]> {
    return this.http.post<number[]>(environment.backend + this.adminUrl + '/compact', null);
  }

  computeDatabaseSize(): Observable<number> {
    return this.http.get<number>(environment.backend + this.adminUrl + '/size');
  }
}
