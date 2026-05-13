import { Component, OnDestroy, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { AccessToken } from "@core/model/token.model";
import { TokenService } from "@core/services/token.service";
import { Observable, Subject, takeUntil } from "rxjs";

@Component({
    selector: 'chutney-tokens',
    templateUrl: './tokens.component.html',
    styleUrls: ['./tokens.component.scss'],
    standalone: false
})
export class TokenListComponent implements OnInit, OnDestroy {

    constructor(
        private tokenService: TokenService,
        private router: Router
        ) {
    }

    tokens: Array<AccessToken> = [];

    private unsubscribeSub$: Subject<void> = new Subject();

    ngOnInit() {
        this.tokenService.getTokensForUser()
                    .pipe(takeUntil(this.unsubscribeSub$))
                    .subscribe({
                        next: (res) => {
                            this.tokens = res;
                            console.log("size " + this.tokens.length)
                        },
                        error: (error) => console.log(error)
                });
    }

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    createToken() {
        this.router.navigate(['/tokens', 'creation']);
    }
}
