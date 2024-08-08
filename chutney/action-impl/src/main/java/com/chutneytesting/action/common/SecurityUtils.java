/*
 * Copyright 2017-2024 Enedis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chutneytesting.action.common;

import com.chutneytesting.action.spi.injectable.Target;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static SSLContextBuilder buildSslContext(Target target) {
        try {
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            target.property("sslProtocol").ifPresent(sslContextBuilder::setProtocol);
            configureTrustStore(target, sslContextBuilder);
            configureKeyStore(target, sslContextBuilder);
            return sslContextBuilder;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static void configureKeyStore(Target target, SSLContextBuilder sslContextBuilder) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        Optional<String> keystore = target.keyStore();
        String keystorePassword = target.keyStorePassword().orElse("");
        String keyPassword = target.keyPassword().orElse(keystorePassword);
        if (keystore.isPresent()) {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(Paths.get(keystore.get()).toUri().toURL().openStream(), keystorePassword.toCharArray());
            sslContextBuilder.loadKeyMaterial(store, keyPassword.toCharArray());
        }
    }

    static void configureTrustStore(Target target, SSLContextBuilder sslContextBuilder) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        Optional<String> truststore = target.trustStore();
        String truststorePassword = target.trustStorePassword().orElse("");
        if (truststore.isPresent()) {
            KeyStore trustMaterial = KeyStore.getInstance(KeyStore.getDefaultType());
            trustMaterial.load(Paths.get(truststore.get()).toUri().toURL().openStream(), truststorePassword.toCharArray());
            sslContextBuilder.loadTrustMaterial(trustMaterial, new TrustSelfSignedStrategy());
        } else {
            sslContextBuilder.loadTrustMaterial(null, (chain, authType) -> true);
        }
    }
}
