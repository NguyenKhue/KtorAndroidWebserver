/*
 * Created by nphau on 4/10/24, 7:04 PM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/10/24, 7:04 PM
 */

package com.nphausg.app.embeddedserver

import android.content.Context
import com.nphausg.app.embeddedserver.plugins.module
import com.nphausg.app.embeddedserver.utils.NetworkUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.network.tls.certificates.DEFAULT_CA_PRINCIPAL
import io.ktor.network.tls.certificates.DEFAULT_PRINCIPAL
import io.ktor.network.tls.certificates.KeyType
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.network.tls.extensions.HashAlgorithm
import io.ktor.network.tls.extensions.SignatureAlgorithm
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
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

object EmbeddedServer {

    private const val PORT = 6868
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private lateinit var applicationContext: Context

    private val server by lazy {
        val keyStoreFile = File(applicationContext.filesDir, "keystore.jks")

        val keyStore = buildKeyStore {
            certificate("sampleAlias") {
                password = "foobar"
                domains = listOf(NetworkUtils.getLocalIpAddress() ?: "127.0.0.1", "127.0.0.1",  "0.0.0.0", "localhost")
                subject = X500Principal("CN=EPaper, OU=Kotlin, O=Samsung, C=VN")
            }
        }


        keyStore.saveToFile(keyStoreFile, "123456")

        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            connector {
                port = PORT
            }
            sslConnector(
                keyStore = keyStore,
                keyAlias = "sampleAlias",
                keyStorePassword = { "123456".toCharArray() },
                privateKeyPassword = { "foobar".toCharArray() }) {
                port = 8443
                keyStorePath = keyStoreFile

            }
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