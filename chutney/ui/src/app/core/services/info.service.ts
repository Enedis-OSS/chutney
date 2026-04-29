/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '@env/environment';
import { Observable } from 'rxjs';

export const infosResourceUrl = '/api/v1/info'
@Injectable({
    providedIn: 'root'
})
export class InfoService {

    constructor(private http: HttpClient) {
    }

    public getVersion(): Observable<string> {
        return this.http.get(environment.backend + infosResourceUrl + '/build/version', {responseType: 'text'});
    }

    public getApplicationName(): Observable<string> {
        return this.http.get(environment.backend + infosResourceUrl + '/appname', {responseType: 'text'});
    }
}
