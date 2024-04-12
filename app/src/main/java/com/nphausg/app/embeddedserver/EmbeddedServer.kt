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
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object EmbeddedServer {

    private const val PORT = 6868
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private lateinit var applicationContext: Context

    private val server by lazy {
        embeddedServer(factory = Netty, port = PORT, module = {
            module(applicationContext)
        })
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