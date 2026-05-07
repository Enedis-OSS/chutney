import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { AccessToken } from "@core/model/token.model";
import { environment } from "@env/environment";
import { Observable } from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class TokenService {

    private resourceUrl = '/api/v1/accesstokens';

    constructor(private httpClient: HttpClient) {
    }

    createToken(token: AccessToken): Observable<string> {
        return this.httpClient.post<string>(environment.backend + this.resourceUrl, token,
            { responseType: 'text' as 'json' });
    }
}