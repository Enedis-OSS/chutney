/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { authenticationConfigResourceUrl } from '@core/services/authentification-config.service';


@Injectable()
export class AuthInterceptor implements HttpInterceptor {

    constructor() {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler) {
        if (this.isProtectedUrl(req.url)) {
            const token = this.retrieveAccessToken();
            if (token) {
                const clonedReq = req.clone({
                    headers: req.headers.set('Authorization', `Bearer ${token}`),
                });
                return next.handle(clonedReq);
            }
        }
        return next.handle(req);
    }

    private retrieveAccessToken() {
        return localStorage.getItem('jwt') || sessionStorage.getItem('access_token');
    }

    private isApiRequest(url: string): boolean {
        return url.startsWith('/api/');
    }

    private isOpenApiRequest(url: string): boolean {
        const openApiUrls = [authenticationConfigResourceUrl];
        return openApiUrls.some(openApiUrl => url.startsWith(openApiUrl));
    }

    private isProtectedUrl(url: string): boolean {
        return this.isApiRequest(url) && !this.isOpenApiRequest(url);
    }
}


@Injectable()
export class TokenInterceptor implements HttpInterceptor {
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next.handle(req).pipe(
            tap((event) => {
                if (event instanceof HttpResponse) {
                    const token = event.headers.get('X-Custom-Token');
                    if (token) {
                        localStorage.setItem('jwt', token);
                    }
                }
            })
        );
    }
}

