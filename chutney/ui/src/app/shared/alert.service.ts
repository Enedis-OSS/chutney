/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type AlertType = 'success' | 'info' | 'error' | 'warning';

export interface AlertConfig {
  timeOut?: number;
  extendedTimeOut?: number;
  closeButton?: boolean;
}

export interface Alert {
  id: number;
  type: AlertType;
  title: string;
  message: string;
  closeButton: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  private nextId = 0;
  private readonly alertsSignal = signal<Alert[]>([]);

  readonly alerts = this.alertsSignal.asReadonly();

  constructor(
    private translateService: TranslateService) {}

  success(msg: string, config: AlertConfig = {}): void {
    this.show('success', msg, config);
  }

  info(msg: string, config: AlertConfig = {}): void {
    this.show('info', msg, config);
  }

  error(msg: string, config: AlertConfig = {}): void {
    this.show('error', msg, config);
  }

  warning(msg: string, config: AlertConfig = {}): void {
    this.show('warning', msg, config);
  }

  removeAll(): void {
    this.alertsSignal.set([]);
  }

  remove(id: number): void {
    this.alertsSignal.update(alerts =>
      alerts.filter(alert => alert.id !== id)
    );
  }

  private show(
    type: AlertType,
    message: string,
    config: AlertConfig
  ): void {
    const titles: Record<AlertType, string> = {
      success: 'alert.success',
      info: 'alert.info',
      error: 'alert.error',
      warning: 'alert.warning'
    };

    this.translateService.get(titles[type]).subscribe(title => {
        const alert: Alert = {
          id: ++this.nextId,
          type,
          title,
          message,
          closeButton: config.closeButton ?? false
        };

        this.alertsSignal.update(alerts => [...alerts, alert]);

        const timeOut = config.timeOut ?? 5000;

        if (timeOut > 0) {
          setTimeout(() => this.remove(alert.id), timeOut);
        }
    });
  }
}
