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
import { JiraPluginConfiguration } from '@core/model/jira-plugin-configuration.model';

@Injectable({
    providedIn: 'root'
})
export class JiraPluginConfigurationService {

    private url = '/api/ui/jira/v1/configuration';

    constructor(private http: HttpClient) {
    }

    public get(): Observable<JiraPluginConfiguration> {
        return this.http.get<JiraPluginConfiguration>(environment.backend + this.url);
    }

    public getUrl(): Observable<string> {
        return this.http.get(environment.backend + this.url + '/url', {responseType: 'text'});
    }

    public save(configuration: JiraPluginConfiguration): Observable<String> {
        return this.http.post(environment.backend + this.url, configuration, {responseType: 'text'});
    }

    public delete(): Observable<Object> {
        return this.http.delete(environment.backend + this.url);
    }
}
