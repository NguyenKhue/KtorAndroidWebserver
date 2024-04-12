/*
 * Created by khue.nguyen on 4/12/24, 10:14 AM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/12/24, 10:14 AM
 */

package com.nphausg.app.embeddedserver.plugins

import android.content.Context
import io.ktor.server.application.Application

fun Application.module(applicationContext: Context) {
    configureCors()
    configureContentNegotiation()
    configureWebSockets()
    configureWebServerRouting(applicationContext)
    configureWebSocketsRouting()
}