/*
 * Created by khue.nguyen on 4/12/24, 10:06 AM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/12/24, 10:06 AM
 */

package com.nphausg.app.embeddedserver.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import kotlinx.serialization.json.Json

fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
}
