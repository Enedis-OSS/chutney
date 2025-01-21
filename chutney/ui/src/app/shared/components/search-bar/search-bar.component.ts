/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { TypeaheadMatch } from 'ngx-bootstrap/typeahead';
import { Hit } from '@core/model/search/hit.model';
import { SearchService } from '@core/services/search.service';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
@Component({
    selector: 'chutney-search-bar',
    templateUrl: './search-bar.component.html',
    styleUrl: './search-bar.component.scss'
})
export class SearchBarComponent {

    keyword: string;
    isSearchExpanded = false;
    searchResults: Hit[] = [];

    isMacOS = false;

    private searchSubject = new Subject<string>();

    constructor(private searchService: SearchService,
        private router: Router) {
        this.isMacOS = navigator.platform.toUpperCase().includes('MAC');
        this.setupSearch();
    }

    private setupSearch(): void {
        this.searchSubject.pipe(
            debounceTime(200),
            distinctUntilChanged(),
            switchMap(keyword => {
                if (!keyword.trim()) {
                    this.searchResults = [];
                    return EMPTY;
                }
                return this.searchService.search(keyword);
            })
        ).subscribe((results: Hit[]) => {
            this.searchResults = results;
        });
    }

    onSearch() {
        this.searchSubject.next(this.keyword); // Déclenche la recherche avec le mot-clé actuel
    }

    // Méthode pour regrouper les résultats par catégorie (what)
    get categorizedResults() {
        return this.searchResults.reduce((acc: { [key: string]: Hit[] }, hit: Hit) => {
            if (!acc[hit.what]) {
                acc[hit.what] = [];
            }
            acc[hit.what].push(hit);
            return acc;
        }, {});
    }

    onSelectSearchHit(hit: TypeaheadMatch<Hit>) {
        this.keyword = null;
        this.router.navigate([hit.item.what, hit.item.id]);
    }

    @ViewChild('searchInput') searchInput!: ElementRef;
    expandSearch() {
        this.isSearchExpanded = true;
        setTimeout(() => {
            this.searchInput.nativeElement.focus();
        }, 0);
    }

    closeSearch() {
        this.isSearchExpanded = false;
    }

    @HostListener('window:keydown', ['$event'])
    handleKeydown(event: KeyboardEvent) {
        const isCmdKey = this.isMacOS ? event.metaKey : event.ctrlKey;

        if (isCmdKey && event.key === 'k') {
            event.preventDefault();
            this.expandSearch();
        }
        if (event.key === 'Escape') {
            this.closeSearch();
        }
    }

    getTagColor(tag: string): string {
        // Generate a hash value from the tag
        const hash = [...tag].reduce((acc, char) => acc + char.charCodeAt(0), 0);

        // Generate a distinct hue based on the hash
        const hue = hash * 137 % 360; // 137 is used to distribute colors evenly

        // Return an HSL color with fixed saturation & lightness for readability
        return `hsl(${hue}, 50%, 60%)`;
    }

    sanitizeMarkTags(input: string): string {
        return input.replace(/<\/?mark>/g, '');
    }

    // Get search results for a given Hit
    getSearchResults(hit: Hit) {
        return hit.search('<mark>');
    }
}
