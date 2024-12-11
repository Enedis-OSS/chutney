/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { TypeaheadMatch } from 'ngx-bootstrap/typeahead';
import { Hit } from '@core/model/search/hit.model';
import { SearchService } from '@core/services/search.service';
import { Router } from '@angular/router';

@Component({
  selector: 'chutney-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.scss'
})
export class SearchBarComponent {

    keyword: string;

    constructor(private searchService: SearchService,
        private router: Router) {
    }

    search(): Observable<any> {
        if (!this.keyword) {
            return EMPTY;
        }
        return this.searchService.search(this.keyword);
    }

    onSelectSearchHit(hit: TypeaheadMatch<Hit>) {
        this.keyword = null;
        this.router.navigate([hit.item.what, hit.item.id]);
    }
}
