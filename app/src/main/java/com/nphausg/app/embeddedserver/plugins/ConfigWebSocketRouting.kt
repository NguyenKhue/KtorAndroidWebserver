/*
 * Created by khue.nguyen on 4/12/24, 10:06 AM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/12/24, 10:06 AM
 */

package com.nphausg.app.embeddedserver.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send

fun Application.configureWebSocketsRouting() {
    routing {
        webSocket("/chat") {
            send("You are connected!")

            for(frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                send("You said: $receivedText")
            }
        }
    }
}
