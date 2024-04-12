/*
 * Created by khue.nguyen on 4/12/24, 10:06 AM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/12/24, 10:06 AM
 */

package com.nphausg.app.embeddedserver.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.websocket.WebSockets

fun Application.configureWebSockets() {
    install(WebSockets) {

    }
}
