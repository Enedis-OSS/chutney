/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable, Injector } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { EMPTY, from, Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { LoginService } from '@core/services';
import { AlertService } from '@shared';

@Injectable({
    providedIn: 'root'
  })
export class ErrorInterceptor implements HttpInterceptor {

    private loginService: LoginService
    private sessionExpiredMessage: string = '';

    constructor(
        private router: Router,
        private alertService: AlertService,
        private translateService: TranslateService,
        private injector: Injector

    ) {
        this.initTranslation();
    }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
      if (!this.loginService) {
          this.loginService = this.injector.get(LoginService);
      }
    if (request.headers.get('no-intercept-error') === '') {
        const newHeaders = request.headers.delete('no-intercept-error')
        const newRequest = request.clone({ headers: newHeaders });
        return next.handle(newRequest);
    } else {
        return next.handle(request).pipe(
            catchError(
                (err: any) => {
                    if (err instanceof HttpErrorResponse) {
                        if (err.status === 401 || err.status === 403) {
                            if (this.loginService.isAuthenticated()) {
                                this.loginService.logout();
                                this.alertService.error(this.sessionExpiredMessage, { timeOut: 0, extendedTimeOut: 0, closeButton: true });
                                return EMPTY
                            } else {
                                const requestURL = this.router.url !== undefined ? this.router.url : '';
                                this.loginService.initLogin(requestURL)
                                return EMPTY
                            }
                        }
                    }
                    return throwError(err);
                }
            )
        );
    }
  }

  private initTranslation() {
    this.getTranslation();
    this.translateService.onLangChange.subscribe(() => {
        this.getTranslation();
    });
  }

  private getTranslation() {
    this.translateService.get('login.expired').subscribe((res: string) => {
        this.sessionExpiredMessage = res;
    });
  }
}
