/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

interface JwtTokenPayload {
    id: string,
    name: string,
    firstname: string,
    lastname: string,
    mail: string,
    authorizations: string[],
    sub: string,
    iat: number,
    exp: number,
    iss: string,
    aud: string,
    azp: string,
    nonce: string,
    amr: string,
}

export class JwtService {

    public static decodeToken(token: string): JwtTokenPayload {
        if (!token) {
            return null;
        }
        try {
            return this.parseJwt(token)
        } catch (error) {
            console.error('Error while decoding token', error);
            return null;
        }
    }

    private static parseJwt (token: string) {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    }
}
