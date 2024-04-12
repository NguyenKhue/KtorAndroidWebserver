/*
 * Created by khue.nguyen on 4/12/24, 10:06 AM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/12/24, 10:06 AM
 */

package com.nphausg.app.embeddedserver.plugins

import android.content.Context
import android.net.Uri
import android.os.Build
import com.nphausg.app.embeddedserver.EmbeddedServer
import com.nphausg.app.embeddedserver.data.BaseResponse
import com.nphausg.app.embeddedserver.data.Database
import com.nphausg.app.embeddedserver.data.models.Cart
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun Application.configureWebServerRouting(applicationContext: Context) {
    routing {
        //  staticResources
        staticResources("/static", ""){
            default("index.html")
        }
        get("/") {
            EmbeddedServer.okText(call, "Hello!! You are here in ${Build.MODEL}")
        }
        get("/fruits") {
            EmbeddedServer.okText(call, Json.encodeToString(BaseResponse(Cart.sample()).also {
                println(it.data)
            }))
        }
        get("/fruits/{id}") {
            val id = call.parameters["id"]
            val fruit = Database.FRUITS.find { it.id == id }
            if (fruit != null) {
                EmbeddedServer.okText(call, Json.encodeToString(fruit))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        get("/download") {
            val file = File(applicationContext.cacheDir , "file.png")
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    "file.png"
                ).toString()
            )
            call.response.status(HttpStatusCode.OK)
            call.respondFile(file)
        }
    }
}
