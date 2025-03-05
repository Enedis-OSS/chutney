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
        const payload = token.split('.')[1];
        try {
            const user = JSON.parse(atob(payload));
            return user
        } catch (error) {
            console.error('Error while decoding token', error);
            return null;
        }
    }
}
