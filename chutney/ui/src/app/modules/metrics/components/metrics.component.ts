/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Metric } from '@core/model/metric.model';
import { PrometheusService } from '@core/services/prometheus.service';
import { NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { filterOnTextContent } from '@shared/tools';
import { interval, Subscription } from 'rxjs';

@Component({
    selector: 'chutney-metrics',
    templateUrl: './metrics.component.html',
    styleUrls: ['./metrics.component.scss'],
    standalone: false
})
export class MetricsComponent implements OnInit, OnDestroy {

    metrics: Metric[] = [];
    chutneyMetrics: Metric[] = [];

    filtredMetrics: Metric[] = [];
    filtredChutneyMetrics: Metric[] = [];
    textFilter = '';

    activeTab = 'chutneyMetrics';
    autoRefresh = false;

    private refreshSubscribe: Subscription;
    private prometheusServiceSubscription: Subscription;

    constructor(
        private prometheusService: PrometheusService) {
    }

    ngOnInit(): void {
        this.refreshMetrics();
    }

    ngOnDestroy(): void {
        this.refreshSubscribe?.unsubscribe();
        this.prometheusServiceSubscription?.unsubscribe();
    }

    onRefreshSwitchChange() {
        this.autoRefresh = !this.autoRefresh;
        if (this.autoRefresh) {
            this.refreshSubscribe = interval(10000)
                .subscribe(() => { this.refreshMetrics() });
        } else {
            this.refreshSubscribe.unsubscribe();
        }
    }

    onTabChange(changeEvent: NgbNavChangeEvent) {
        this.activeTab = changeEvent.nextId;
        this.updateTextFilter(this.textFilter);
    }

    updateTextFilter(text: string) {
        this.textFilter = text;
        if (this.activeTab === 'chutneyMetrics') {
            this.filtredChutneyMetrics = filterOnTextContent(this.chutneyMetrics, this.textFilter, ['name', 'tags']);
        } else {
            this.filtredMetrics = filterOnTextContent(this.metrics, this.textFilter, ['name', 'tags']);
        }
    }

    refreshMetrics() {
        const chutneyMetricPattern = '^scenario|^campaign';
        this.prometheusServiceSubscription = this.prometheusService.getMetrics()
            .subscribe(result => {
                this.metrics = result;
                this.chutneyMetrics = this.metrics.filter(metric => metric.name.match(chutneyMetricPattern));
                this.metrics = this.metrics.filter(metric => !metric.name.match(chutneyMetricPattern));
                this.updateTextFilter(this.textFilter);
            });
    }
}
