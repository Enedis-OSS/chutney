/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { AccessToken, CreatedAccessToken } from "@core/model/token.model";
import { environment } from "@env/environment";
import { map, Observable } from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class TokenService {

    private resourceUrl = '/api/v1/accesstokens';

    constructor(private httpClient: HttpClient) {
    }

    createToken(token: AccessToken): Observable<CreatedAccessToken> {
        return this.httpClient.post<string>(environment.backend + this.resourceUrl, token,
        ).pipe(
            map((result:any) => new CreatedAccessToken(result.note, result.token, result.expiresAt)
        ));
    }

    getTokensForUser(): Observable<Array<AccessToken>> {
        return this.httpClient.get<Array<AccessToken>>(
            environment.backend + this.resourceUrl).pipe(map((res: Array<AccessToken>) => res));
    }
}