/*
 * Created by nphau on 4/10/24, 7:04 PM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/10/24, 7:04 PM
 */

package com.nphausg.app.embeddedserver

import android.content.Context
import android.util.Log
import com.nphausg.app.embeddedserver.plugins.module
import com.nphausg.app.embeddedserver.utils.NetworkUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
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
import javax.security.auth.x500.X500Principal
import android.util.Base64
import io.ktor.network.tls.certificates.KeyType

// https://yggr.medium.com/how-to-generate-public-private-key-in-android-7f3e244c0fd8
// https://www.linkedin.com/pulse/generate-self-signed-x509-certificate-using-javakotlin-yogesh-bisht-7eelc
// https://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java
// https://gist.github.com/alessandroleite/fa3e763552bb8b409bfa
object EmbeddedServer {

    private const val PORT = 8001
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private lateinit var applicationContext: Context

    private val server by lazy {
        val keyStoreFile = File(applicationContext.filesDir, "keystore.jks")

        val keyStore = buildKeyStore {
            certificate("sampleAlias") {
                password = "foobar"
                domains = listOf(NetworkUtils.getLocalIpAddress() ?: "127.0.0.1", "127.0.0.1",  "0.0.0.0", "localhost")
                subject = X500Principal("CN=EPaper, OU=Kotlin, O=Samsung, C=VN")
                keySizeInBits = 4096
                daysValid = 10
                keyType = KeyType.Server
            }
        }


        keyStore.saveToFile(keyStoreFile, "123456")

        Log.d("EmbeddedServer", String(Base64.encode(keyStore.getCertificate("sampleAlias").encoded, Base64.DEFAULT)))
        Log.d("EmbeddedServer", keyStore.getCertificate("sampleAlias").toString())

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
                port = 8002
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