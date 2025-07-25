/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, HostListener, OnInit } from '@angular/core';
import { LoginService } from '@core/services';
import { LayoutOptions } from '@core/layout/layout-options.service';
import { MenuItem } from '@shared/components/layout/menuItem';
import { allMenuItems } from '@shared/components/layout/left-menu/chutney-left-menu.items';
import { FeatureService } from '@core/feature/feature.service';

@Component({
    selector: 'chutney-chutney-left-menu',
    templateUrl: './chutney-left-menu.component.html',
    styleUrls: ['./chutney-left-menu.component.scss'],
    standalone: false
})
export class ChutneyLeftMenuComponent implements OnInit {
    public menuItems = allMenuItems;
    private newInnerWidth: number;
    private innerWidth: number;

    constructor(public layoutOptions: LayoutOptions,
                private loginService: LoginService,
                private featureService: FeatureService) {
    }

    ngOnInit(): void {
        setTimeout(() => {
            this.innerWidth = window.innerWidth;
            if (this.innerWidth < 1200) {
                this.layoutOptions.toggleSidebar = true;
            }
        });
    }

    canViewMenuGroup(item: MenuItem): boolean {
        return !!item.children.find(subItem => this.canViewMenuItem(subItem));

    }

    canViewMenuItem(item: MenuItem): boolean {
        return this.loginService.hasAuthorization(item.authorizations) && this.featureService.active(item.feature);
    }

    toggleSidebar() {
        this.layoutOptions.toggleSidebar = !this.layoutOptions.toggleSidebar;
    }

    @HostListener('window:resize', ['$event'])
    onResize(event) {
        this.newInnerWidth = event.target.innerWidth;

        if (this.newInnerWidth < 1200) {
            this.layoutOptions.toggleSidebar = true;
        } else {
            this.layoutOptions.toggleSidebar = false;
        }

    }
}
