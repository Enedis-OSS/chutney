/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { forkJoin } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  private successTitle: string = '';
  private infoTitle: string = '';
  private errorTitle: string = '';
  private warningTitle: string = '';

  constructor(
    private toastr: ToastrService,
    private translateService: TranslateService
  ) {
    this.initTranslation();
  }

  success(msg: string, config = {}) {
    this.toastr.success(msg, this.successTitle, config);
  }

  info(msg: string, config = {}) {
    this.toastr.info(msg, this.infoTitle, config);
  }

  error(msg: string, config = {}) {
    this.toastr.error(msg, this.errorTitle, config);
  }

  warning(msg: string, config = {}) {
    this.toastr.warning(msg, this.warningTitle, config);
  }

  removeAll() {
    this.toastr.clear();
  }

  private initTranslation() {
    this.getTranslation();
    this.translateService.onLangChange.subscribe(() => {
        this.getTranslation();
    });
  }

  private getTranslation() {
    forkJoin({
        success: this.translateService.get('alert.success'),
        info: this.translateService.get('alert.info'),
        error: this.translateService.get('alert.error'),
        warn: this.translateService.get('alert.warning')
    }).subscribe(res => {
        this.successTitle = res.success;
        this.infoTitle = res.info;
        this.errorTitle = res.error;
        this.warningTitle = res.warn;
    });
  }
}
