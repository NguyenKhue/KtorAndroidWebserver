/*
 * Created by nphau on 4/10/24, 7:04 PM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/10/24, 7:04 PM
 */

package com.nphausg.app.embeddedserver

import android.content.Context
import android.util.Base64
import android.util.Log
import com.nphausg.app.embeddedserver.plugins.connections
import com.nphausg.app.embeddedserver.plugins.module
import com.nphausg.app.embeddedserver.utils.NetworkUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec


val crt = "-----BEGIN CERTIFICATE-----\n" +
        "MIID2TCCAsGgAwIBAgIUY34cAQzIs2ZFtMC9H8xkHkypw40wDQYJKoZIhvcNAQEL\n" +
        "BQAwfDELMAkGA1UEBhMCVk4xDDAKBgNVBAgMA0hDTTEMMAoGA1UEBwwDSENNMQsw\n" +
        "CQYDVQQKDAJTUzELMAkGA1UECwwCc2ExDzANBgNVBAMMBmVwYXBlcjEmMCQGCSqG\n" +
        "SIb3DQEJARYXMDkwMzIwMDFraHVuZ0BnbWFpbC5jb20wHhcNMjQwNDE1MTczNTIw\n" +
        "WhcNMjkwNDE0MTczNTIwWjB8MQswCQYDVQQGEwJWTjEMMAoGA1UECAwDSENNMQww\n" +
        "CgYDVQQHDANIQ00xCzAJBgNVBAoMAlNTMQswCQYDVQQLDAJzYTEPMA0GA1UEAwwG\n" +
        "ZXBhcGVyMSYwJAYJKoZIhvcNAQkBFhcwOTAzMjAwMWtodW5nQGdtYWlsLmNvbTCC\n" +
        "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJbVni9kt04YUxtUO1u+cbbh\n" +
        "/mYwbA9vVnHZRqp8bUCiTHR8OkVHo/ShiagUt2XfGyUVLUtCwoFdIoMRb5PBxY1X\n" +
        "1zshZI5zKWU6CrgF4fb2xWjvm0tqLcrviffMT51vnohUGuUO6rMsZHPbToXpOY3R\n" +
        "Hwh/7Z+NeeRPq8J6tMQM3Yq1y1SyyBlTogj6EWXPuud8M8BcvSNYQH26lQTsOJxo\n" +
        "4unISol1UOumFEylAUXZWPt8PYvyOXEcK3fQfsA8HHI/wmxB5EieEi8s2/fAAuam\n" +
        "6vuLERZcGeakcsfkwOkqFeGxzzyaC0Xt/NDxuTAyLMMNTr9R4dbvebcuKB9Ud+0C\n" +
        "AwEAAaNTMFEwHQYDVR0OBBYEFCryj4bm1Y3/8/CblVEls+mbHJXcMB8GA1UdIwQY\n" +
        "MBaAFCryj4bm1Y3/8/CblVEls+mbHJXcMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZI\n" +
        "hvcNAQELBQADggEBAF4b+1PwU42fWBFo+ACnWVd0HQq7V92kX44LCKdjaZM71XIt\n" +
        "vcptTg1aEolyVSH2j4Ni+3uxiOzXVxY2c75o0C1w/N5Y3jRBzqnMTiw6l7AUzbdw\n" +
        "eM6veos4UCw5l1ALhquuLzCT7acGYD0iWk6QRQpn5cGuTQlAAFNNMGzm2l3pxvCW\n" +
        "UWSM2uE665fhqDQET0lLs/FGmGK3V9DSIWo7LB/cr4cp0tFFyOk6Qm3yu+bm4jLF\n" +
        "kIJG9rhcxvl28Y20ZPZadsWbKLlVv413su8ThmnekM8yXDPxp8Eh504r7miamGf7\n" +
        "dF0faRatl0DlAITKv0NMjVe5/QRKnZXLB8MpLmU=\n" +
        "-----END CERTIFICATE-----"

val key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCW1Z4vZLdOGFMb\n" +
        "VDtbvnG24f5mMGwPb1Zx2UaqfG1Aokx0fDpFR6P0oYmoFLdl3xslFS1LQsKBXSKD\n" +
        "EW+TwcWNV9c7IWSOcyllOgq4BeH29sVo75tLai3K74n3zE+db56IVBrlDuqzLGRz\n" +
        "206F6TmN0R8If+2fjXnkT6vCerTEDN2KtctUssgZU6II+hFlz7rnfDPAXL0jWEB9\n" +
        "upUE7DicaOLpyEqJdVDrphRMpQFF2Vj7fD2L8jlxHCt30H7APBxyP8JsQeRInhIv\n" +
        "LNv3wALmpur7ixEWXBnmpHLH5MDpKhXhsc88mgtF7fzQ8bkwMizDDU6/UeHW73m3\n" +
        "LigfVHftAgMBAAECggEAF14GwJ7gywd3sI8vFvp9EXEXgWtVAlskUETx7q2SG2ZJ\n" +
        "Y+5u9Jaxrs9rzQ9QjkavJOKP/s3b5kCwfQ+xcnmdilUmcfGta2gp7JW+XV4D8Mew\n" +
        "M56TN26+o2bSwAH1/5TwQXyOIhhageGJum0qUBKeqPrRzPZMsOAxFw7EtD9gS9LZ\n" +
        "cQTckqK1FwYAtKpaEKOy29jYlUhx2cA1HqcLQr/tAPhIoAwRRII1yk/4qepi2wYA\n" +
        "oXD2Xm5dgxggod6ULvfavUiO0JjKF7IMbdH4Qra9DLQBdThFI6O14jkmYPLaXY61\n" +
        "Vo5cD98eUS74R/Yy+8S6MPwBg0r4scpW2ISEKGR2IQKBgQDPqpYaU/t2KA6hwoYg\n" +
        "d87JqMyScukSwxqDbxFBK0zxwj9pKAL0h53SmILRlD8WFQG9eCuXNxV2Xrc8bhF4\n" +
        "9qWk537v20emvMrFeIzsLrNR2AT8WLStJ5fw3MasbfoGrfh5HdXP01b5Pja+ryY3\n" +
        "SkbaQTjf842FbrqLprj8CL+PSQKBgQC58M7W9n9NDsSREEqSRpZS9iCA2T0fsVqW\n" +
        "OxwnwW/hV6VdAR3hYHvh9Uvhn4kfoER3S0yeyFMx04tXhqB8oi1OSb5S5x6IETR/\n" +
        "UlZ123svsnKtOYZGlepFYYkKM0H19BAHl1A+sWupGyyHDaL9UpMZRzOnhTsLbMt+\n" +
        "RJ3mwFvPhQKBgQCGWwasWAOMSZRV4cXngbwfSn+4jHHxOpuPx68xK7OngpaGEWYA\n" +
        "ETHxy8xvjetW/RZYIESLnA7du5/vkALr8R/wVfoRcxyjaugB5OG/+OL5o7puDXIv\n" +
        "yTsLkbtUWf72jV4B9mScBk7yCOdgbW9bPEok8Se79RZt6tr0eVSbc4mESQKBgCiQ\n" +
        "MP9SLPlJhHZFAI+imH6mtPaG7b+xOBrX8E938olNToTYjoUxQDVOBuzEmextUSJZ\n" +
        "KfDlsMiI5rgEZZRq6MlQaxW4179FSZeRBc2WQOxp2HyTtQhHAiF6oqO4BOa8BJcz\n" +
        "Wk0i9WKhy/f2cJ0k23RDRTCBbx0R8d6s52mEg0LlAoGACGUs7s7lnogvUAYGYOrm\n" +
        "ResVL586o79pY5WZQmlS2mUdvd7Hn5Jm4CGVHJ8jpH2fgXOkpTRkHo22ESeHrdde\n" +
        "wVG/6IhbHn9cFGhx49xYx/f7e3qY+JQS8VFu1jyHZ3Q6Zrg/sagV2tHKMB+WZG0D\n" +
        "3hbrIJ+qCX2GZbE04AXZbf4="

object EmbeddedServer {

    private const val PORT = 8001
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private lateinit var applicationContext: Context

    private val server by lazy {
        val keyStoreFile = File(applicationContext.filesDir, "keystore.jks")

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        val certificateFactory = CertificateFactory.getInstance("X509")
        val epaperCA =
            certificateFactory.generateCertificate(ByteArrayInputStream(crt.toByteArray()))
        keyStore.load(null, null)

        val encoded: ByteArray =
            Base64.decode(key.replace(System.lineSeparator(), ""), Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(encoded)

        val certChain = listOfNotNull(epaperCA).toTypedArray()
        val newPrivateKey = keyFactory.generatePrivate(keySpec)
        Log.d("EmbeddedServer", "newPrivateKey: $newPrivateKey")
        keyStore.setKeyEntry(
            "khue",
            newPrivateKey,
            "khue".toCharArray(),
            certChain,
        )

        keyStore.saveToFile(keyStoreFile, "123456")


        keyStore.getCertificateChain("khue").also {
            Log.d("EmbeddedServer", it.toList().toString())
        }

        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            connector {
                port = PORT
            }
            sslConnector(
                keyStore = keyStore,
                keyAlias = "khue",
                keyStorePassword = { "123456".toCharArray() },
                privateKeyPassword = { "khue".toCharArray() }) {
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
                connections.forEach {
                    it.session.close(CloseReason(1000, "close"))
                }
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

