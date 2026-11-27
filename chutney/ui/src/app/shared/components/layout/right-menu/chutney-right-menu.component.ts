/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { MenuItem } from '@shared/components/layout/menuItem';
import { NgTemplateOutlet, NgClass } from '@angular/common';
import { HasAuthorizationDirective } from '../../../directives/hasAuthorization.directive';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'chutney-chutney-right-menu',
    templateUrl: './chutney-right-menu.component.html',
    styleUrls: ['./chutney-right-menu.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [NgTemplateOutlet,
        NgbDropdown,
        NgbDropdownToggle,
        NgClass,
        NgbDropdownMenu,
        NgbDropdownItem,
        RouterLink,
        TranslatePipe,
        HasAuthorizationDirective
    ]
})
export class ChutneyRightMenuComponent {

    @Input() menuItems: MenuItem [] = [];
    constructor() { }

    onItemClick(item: MenuItem) {
        if (item.click) {
            const option = item.options && item.options.length ? item.options[0].id : null;
            option ? item.click(option) : item.click();
        }
    }

    getItemLink(item: MenuItem): string[] | null {
        return item.link ? [item.link] : null;
    }
}
