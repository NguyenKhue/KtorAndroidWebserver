/*
 * Created by nphau on 4/10/24, 7:04 PM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/10/24, 7:04 PM
 */

package com.nphausg.app.embeddedserver

import android.content.Context
import android.util.Base64
import android.util.Log
import com.nphausg.app.embeddedserver.plugins.module
import com.nphausg.app.embeddedserver.utils.NetworkUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.network.tls.certificates.KeyType
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.network.tls.extensions.HashAlgorithm
import io.ktor.network.tls.extensions.SignatureAlgorithm
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import javax.security.auth.x500.X500Principal


public fun KeyStore.generateCertificate(
    file: File? = null,
    algorithm: String = "SHA1withRSA",
    keyAlias: String = "mykey",
    keyPassword: String = "changeit",
    jksPassword: String = keyPassword,
    keySizeInBits: Int = 1024,
    caKeyAlias: String = "mykey",
    caPassword: String = "changeit",
    keyType: KeyType = KeyType.Server
): KeyStore {
    val caCert = getCertificate(caKeyAlias)
    val caKeys = KeyPair(caCert.publicKey, getKey(caKeyAlias, caPassword.toCharArray()) as PrivateKey)

    val keyStore = buildKeyStore {
        certificate(keyAlias) {
            val (hashName, signName) = algorithm.split("with")
            this.hash = HashAlgorithm.valueOf(hashName)
            this.sign = SignatureAlgorithm.valueOf(signName)
            this.password = keyPassword
            this.keySizeInBits = keySizeInBits
            this.keyType = keyType
            this.subject = X500Principal("CN=EPaper, OU=Kotlin, O=Samsung, C=VN")
            this.domains = listOf("127.0.0.1", "localhost")
            signWith(
                issuerKeyPair = caKeys,
                issuerKeyCertificate = caCert,
                issuerName = X500Principal("CN=EPaper, OU=Kotlin, O=Samsung, C=VN"),
            )
        }
    }

    file?.let {
        keyStore.saveToFile(it, jksPassword)
    }
    return keyStore
}

val crt = "-----BEGIN CERTIFICATE-----\n" +
        "MIID1TCCAr2gAwIBAgIUcgjkUYhqJKliF2Rfxm+0tzM8dzEwDQYJKoZIhvcNAQEL\n" +
        "BQAwejELMAkGA1UEBhMCVk4xDDAKBgNVBAgMA0hDTTEMMAoGA1UEBwwDSENNMQsw\n" +
        "CQYDVQQKDAJTUzELMAkGA1UECwwCc2ExDTALBgNVBAMMBGtodWUxJjAkBgkqhkiG\n" +
        "9w0BCQEWFzA5MDMyMDAxa2h1bmdAZ21haWwuY29tMB4XDTI0MDQxNDE2NTY0OVoX\n" +
        "DTI5MDQxMzE2NTY0OVowejELMAkGA1UEBhMCVk4xDDAKBgNVBAgMA0hDTTEMMAoG\n" +
        "A1UEBwwDSENNMQswCQYDVQQKDAJTUzELMAkGA1UECwwCc2ExDTALBgNVBAMMBGto\n" +
        "dWUxJjAkBgkqhkiG9w0BCQEWFzA5MDMyMDAxa2h1bmdAZ21haWwuY29tMIIBIjAN\n" +
        "BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnvM3lvsxOQBjXAm/jkDFEG/KoDc/\n" +
        "7jfU2WTM96s9nYbp0QPq7FjcCMVKgXp2on7hpKFmqEeqXvP27Cv7MRqBu3+u2fO2\n" +
        "q1pkT6ZLHgmuqWbZr1NPliIul4SwwjaXtXfwy0p9zsh1gcRcqStaHF377iNM323r\n" +
        "Ti2tQjB2/yxZugaQICJHbv7xSgtdG5GMbzPLKiTabJgf2q8feJUD2LC2vm7vBpK0\n" +
        "PvtAj/CNkYAPjUBeyerp83XAPHilClLhRFMl3Jpm4Fa0AxzO/xZT4RhXbjXCPAWg\n" +
        "Yrcyy4j5ESxqobV0DgBKwH/s32JiiS8LdVHAQlcP/fBqBuIWWOVcQE7UuQIDAQAB\n" +
        "o1MwUTAdBgNVHQ4EFgQU85f9iye5T+oic2EHwI4CDZbZVLAwHwYDVR0jBBgwFoAU\n" +
        "85f9iye5T+oic2EHwI4CDZbZVLAwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0B\n" +
        "AQsFAAOCAQEASUdGaXzWh5JFGPEOB2iqE1Q+TiGevjg4YC97CpUibSPD6oUE9m56\n" +
        "7K+NL64y5kgHLFrlTxZmwGInRLjL+7+LqdlOaUJ0f6qjuVEZfMLqFgP/Gp7K4pNh\n" +
        "uq6et2lAl7zROOEODogTR+ukEiF7zTLG2ib/SZ5Kzrft+GIxzIP7vIkBZmDYtUCq\n" +
        "KoKq04orA5AmODvk4Nve3wpZfsYJd6nBqjQYTIUnCQUKk8Ua0Q8tMU0jkVYTkuJP\n" +
        "dNAJSZMWjsigzhH1b5GbkosxfSp6pOzRwySXjFE7OJcYyY2oNHLQPf1XDdJa9k9O\n" +
        "CmOHqf8gNBjSnMfxD6Nx8T4W+1OQmMrrtg==\n" +
        "-----END CERTIFICATE-----"

val key = "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\n" +
        "2oj3WVfe0iFf6rh82wI5yKOzqnhWTm4j"

object EmbeddedServer {

    private const val PORT = 6868
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private lateinit var applicationContext: Context

    private val server by lazy {
//        val keyStoreFile = File(applicationContext.filesDir, "keystore.jks")
//
//        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
//        val certificateFactory = CertificateFactory.getInstance("X509")
//        val epaperCA = certificateFactory.generateCertificate(ByteArrayInputStream(crt.toByteArray()))
//        keyStore.load(null, null)
//
//        val encoded: ByteArray = Base64.decode(key.replace(System.lineSeparator(), ""), Base64.DEFAULT)
//
//        val keyFactory = KeyFactory.getInstance("RSA")
//        val keySpec = X509EncodedKeySpec(encoded)
//
//        keyStore.setKeyEntry(alias = "khue", chain = epaperCA , key = keyFactory.generatePrivate(keySpec) as RSAPrivateKey, password = "khue".toCharArray())
//        keyStore.saveToFile(keyStoreFile, "123456")


//        val keyStore = buildKeyStore {
//            certificate("sampleAlias") {
//                password = "foobar"
//                domains = listOf(NetworkUtils.getLocalIpAddress() ?: "127.0.0.1", "127.0.0.1",  "0.0.0.0", "localhost")
//                subject = X500Principal("CN=EPaper, OU=Kotlin, O=Samsung, C=VN")
//            }
//        }

//        keyStore.getCertificateChain("khue").also {
//            Log.d("EmbeddedServer", it.toList().toString())
//        }

        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            connector {
                port = PORT
            }
//            sslConnector(
//                keyStore = keyStore,
//                keyAlias = "khue",
//                keyStorePassword = { "123456".toCharArray() },
//                privateKeyPassword = { "khue".toCharArray() }) {
//                port = 8443
//                keyStorePath = keyStoreFile
//            }
            module {
                module(applicationContext)
            }
        }

        embeddedServer(factory = Netty, environment = environment)
    }

    fun start(applicationContext: Context) {
        this.applicationContext = applicationContext
        ioScope.launch {
            server.start(wait = true)
            try {

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        ioScope.launch {
            try {
                server.stop(500, 1000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val host: String
        get() = String.format("%s:%d", NetworkUtils.getLocalIpAddress(), PORT)

    suspend fun okText(call: ApplicationCall, text: String) {
        call.respondText(
            text = text,
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }
}