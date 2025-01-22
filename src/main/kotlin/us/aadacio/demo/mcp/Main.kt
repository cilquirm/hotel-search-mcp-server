package us.aadacio.demo.mcp

import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.util.collections.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.SSEServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.github.oshai.kotlinlogging.KotlinLogging


import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.*

val logger = KotlinLogging.logger { } 

fun configureServer() : Server =
  Server(
    Implementation("Hotel Search MCP Server", "0.1.0"),
    ServerOptions(
      ServerCapabilities(
        prompts = ServerCapabilities.Prompts(true),
        resources = ServerCapabilities.Resources(true, true),
        tools = ServerCapabilities.Tools(true)
      )
    )
  ).apply {
    addPrompt(
      name = "Hotel Search",
      description = "Search for a great hotel deal",
      arguments = listOf(
        PromptArgument ("city", "Name of the city", true),
        PromptArgument("startDate", "The start date of the stay", true),
        PromptArgument("endDate", "The end date of the stay", true)
      )
    ) { req ->
    GetPromptResult(
      "Description for ${req.name}",
      listOf(
        PromptMessage(
          Role.user,
          TextContent("Search for a hotel in [[city]]${req.arguments?.get("city")} between ${req.arguments?.get("startDate")} and ${req.arguments?.get("endDate")}")
      )
    )
    )
  }
  addTool("hotel_search",
          "Search a hotel using Priceline",
          Tool.Input(
            properties = buildJsonObject {
              put("startDate", buildJsonObject {
                put("type", "string")
                put("description", "The date you want to check in")
              } )
              
              put("endDate", buildJsonObject {
                put("type", "string")
                put("description", "The date you want to check out")
              } )

              put("hotelType", buildJsonObject {
                put("enum", buildJsonArray {
                  add("luxury")
                  add("mid-range")
                  add("budget")
                })
                put("default", "mid-range")
              })

                                         },
            required = listOf("startDate", "endDate")
          )
  ) { ctr ->

    logger.info { "Received query of ${ctr.arguments["startDate"]} to ${ctr.arguments["endDate"]} with a hotel type of ${ctr.arguments["hotelType"]}" }
    CallToolResult(listOf(
      TextContent("The Best Hotel Anywhere"),
      TextContent("The SuperBest Hotel in San Diego"),
      TextContent("A mediocre hotel in Texas")
    ))
  }
  addTool( "flight_search",
    "Search for a flight using Priceline",
    Tool.Input(
      properties = buildJsonObject {
        put("location", buildJsonObject { put("type", "string") } )
      },
      required = listOf("location")
    )
  ) { req -> CallToolResult(listOf(TextContent("Let's go to ${req.arguments["location"]} "))) }

}

fun main() {

  println("starting embedded server")

  // val def = CompletableDeferred<Unit>()
  runBlocking {
    val servers = ConcurrentMap<String, Server>()
    embeddedServer(CIO, host = "0.0.0.0", port = 9090) {
      install(SSE)
      routing {
        sse("/sse") {
          println("in the sse path")
          val transport = SSEServerTransport("/message", this)
          val server = configureServer()

          servers[transport.sessionId] = server

          server.onCloseCallback = {
            servers.remove(transport.sessionId)
          }

          server.connect(transport)
        }
        post("/message") {
          println("received message")
          val sessionId = call.request.queryParameters["sessionId"]!!
          val transport = servers[sessionId]?.transport as? SSEServerTransport
          if (transport == null) {
            call.respond(status = HttpStatusCode.NotFound, message = "Session not found")
            return@post
          }

          transport.handlePostMessage(call)
        }
      }
    }.start(wait = true)
  }

} 
