/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { ChangeDetectionStrategy, Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { Observable, Subject, of, shareReplay } from 'rxjs';
import { Hit } from '@core/model/search/hit.model';
import { SearchService } from '@core/services/search.service';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, map, switchMap } from 'rxjs/operators';

@Component({
    selector: 'chutney-search-bar',
    templateUrl: './search-bar.component.html',
    styleUrl: './search-bar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchBarComponent {

    keyword: string = '';
    isSearchExpanded = false;
    isMacOS = false;

    private searchSubject = new Subject<string>();

    constructor(
        private searchService: SearchService,
        private router: Router
    ) {
        this.isMacOS = navigator.platform.toUpperCase().includes('MAC');
    }

    onSearch() {
        this.searchSubject.next(this.keyword);
    }

    categorized$: Observable<Record<string, Hit[]>> = this.searchSubject.pipe(
        debounceTime(200),
        distinctUntilChanged(),
        switchMap(keyword => {
            const term = keyword.trim();
            if (!term) {
                return of([] as Hit[]);
            }
            return this.searchService.search(term);
        }),
        map(results => results.map(hit => {
            const tagColors: Record<string, string> = {};
            hit.tags.forEach(tag => tagColors[tag] = this.getTagColor(tag));
            hit.tagColors = tagColors;
            hit.matches = hit.search('<mark>');
            return hit;
        })),
        map(results => this.groupBy(results)),
        shareReplay(1)
    );

    private groupBy(results: Hit[]): Record<string, Hit[]> {
        return results.reduce((acc, hit) => {
            (acc[hit.what] = acc[hit.what] || []).push(hit);
            return acc;
        }, {} as Record<string, Hit[]>);
    }

    navigateToDetail(event: MouseEvent, item: any) {
        if (event.ctrlKey || event.metaKey || event.button === 1) {
            const baseHref = document.getElementsByTagName('base')[0].href;
            const urlTree = this.router.createUrlTree([item.what, this.sanitizeMark(item.id)]);
            const serializedUrl = this.router.serializeUrl(urlTree);

            const fullUrl = `${baseHref}#${serializedUrl}`;
            window.open(fullUrl, '_blank');
        } else {
            this.router.navigate([item.what, this.sanitizeMark(item.id)]);
        }

        if (!(event.ctrlKey || event.metaKey || event.button === 1)) {
            setTimeout(() => {
                this.isSearchExpanded = false;
            }, 200);
        }
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
        tag = this.sanitizeMark(tag);

        // Generate a hash value from the tag
        const hash = [...tag].reduce((acc, char) => acc + char.charCodeAt(0), 0);

        // Generate a distinct hue based on the hash
        const hue = hash * 137 % 360; // 137 is used to distribute colors evenly

        // Return an HSL color with fixed saturation & lightness for readability
        return `hsl(${hue}, 50%, 60%)`;
    }

    sanitizeMark(input: string): string {
        return input.replace(/<\/?mark>/g, '');
    }

    trackByCategory(_: number, pair: { key: string; value: Hit[] }) {
        return pair.key;
    }
}
