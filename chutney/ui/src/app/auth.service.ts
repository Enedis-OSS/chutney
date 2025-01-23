import { Injectable } from '@angular/core';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private oauthService: OAuthService) {
    this.configureSingleSignOn();
  }

  private configureSingleSignOn() {
    const authConfig: AuthConfig = {
      issuer: 'http://localhost:3000',
      redirectUri: 'https://localhost:4200',
      clientId: 'my-client',
      dummyClientSecret: 'my-client-secret',
      scope: 'openid profile email',
      responseType: 'code',
      showDebugInformation: true,
    };

    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();
    this.oauthService.loadDiscoveryDocumentAndTryLogin();
  }

  login() {
    this.oauthService.initLoginFlow();
  }

  logout() {
    this.oauthService.logOut();
  }

  get isLoggedIn(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  get userProfile(): any {
    return this.oauthService.getIdentityClaims();
  }
}
