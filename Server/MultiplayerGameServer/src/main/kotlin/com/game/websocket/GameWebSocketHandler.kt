package com.game.websocket

import com.game.service.PlayerManager
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class GameWebSocketHandler(
    private val playerManager: PlayerManager,
    private val jsonMapper: JsonMapper
) : TextWebSocketHandler() {

    private val sessionPlayers = ConcurrentHashMap<String, UUID>()

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = session

        println("Client connected: ${session.id}")

        sendMessage(
            session,
            mapOf(
                "type" to "WELCOME",
                "message" to "Welcome to game server!"
            )
        )
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage
    ) {
        val request = try {
            jsonMapper.readValue(message.payload, Map::class.java)
        } catch (e: Exception) {
            sendError(session, "Invalid JSON")
            return
        }

        when (request["type"] as? String) {
            "JOIN" -> registerPlayer(session, request["name"] as? String)

            "MOVE" -> handleMove(
                session,
                request["direction"] as? String
            )

            "CHAT" -> handleChat(
                session,
                request["message"] as? String
            )

            else -> sendError(session, "Unknown message type")
        }
    }

    private fun registerPlayer(
        session: WebSocketSession,
        requestedName: String?
    ) {
        if (sessionPlayers.containsKey(session.id)) {
            sendError(session, "Player already registered")
            return
        }

        val name = requestedName?.trim()

        if (name == null || name.length !in 2..15) {
            sendError(session, "Name must contain 2-15 characters")
            return
        }

        val player = try {
            playerManager.addPlayer(name)
        } catch (e: IllegalStateException) {
            sendError(session, "No free player colors")
            return
        }

        sessionPlayers[session.id] = player.id

        println("Player joined: ${player.name}, ID: ${player.id}")

        sendMessage(
            session,
            mapOf(
                "type" to "JOINED",
                "id" to player.id.toString(),
                "name" to player.name,
                "color" to player.color,
                "x" to player.x,
                "y" to player.y
            )
        )
        sendPlayerList(session)
        broadcastToPlayers(
            mapOf(
                "type" to "PLAYER_JOINED",
                "player" to player
            ),
            excludedSessionId = session.id
        )
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: org.springframework.web.socket.CloseStatus
    ) {
        val playerId = sessionPlayers.remove(session.id)

        sessions.remove(session.id)

        if (playerId != null) {
            playerManager.removePlayer(playerId)

            println("Player disconnected: $playerId")

            broadcastToPlayers(
                mapOf(
                    "type" to "PLAYER_LEFT",
                    "id" to playerId.toString()
                )
            )
        }
        println("Client disconnected: ${session.id}")
    }

    private fun sendError(
        session: WebSocketSession,
        message: String
    ) {
        sendMessage(
            session,
            mapOf(
                "type" to "ERROR",
                "message" to message
            )
        )
    }

    private fun sendMessage(
        session: WebSocketSession,
        data: Map<String, Any>
    ) {
        val targetSession = sessions[session.id] ?: session

        if (!targetSession.isOpen) {
            return
        }

        try {
            val json = jsonMapper.writeValueAsString(data)
            targetSession.sendMessage(TextMessage(json))
        } catch (e: Exception) {
            println(
                "Failed to send message to ${session.id}: ${e.message}"
            )
        }
    }

    private fun sendPlayerList(session: WebSocketSession) {
        val players = playerManager.getAllPlayers()

        sendMessage(
            session,
            mapOf(
                "type" to "PLAYER_LIST",
                "players" to players
            )
        )
    }

    private fun broadcastToPlayers(
        data: Map<String, Any>,
        excludedSessionId: String? = null
    ) {
        sessionPlayers.keys.forEach { sessionId ->
            if (sessionId != excludedSessionId) {
                sessions[sessionId]?.let { session ->
                    sendMessage(session, data)
                }
            }
        }
    }

    private fun handleMove(session: WebSocketSession, direction: String?) {
        val playerId = sessionPlayers[session.id]

        if (playerId == null) {
            sendError(session, "Player is not registered")
            return
        }

        if (direction == null) {
            sendError(session, "Direction is required")
            return
        }

        if (direction !in setOf("UP", "DOWN", "LEFT", "RIGHT")) {
            sendError(session, "Invalid direction")
            return
        }

        val player = playerManager.movePlayer(playerId, direction)

        if (player == null) {
            return
        }

        broadcastToPlayers(
            mapOf(
                "type" to "PLAYER_MOVED",
                "id" to player.id.toString(),
                "x" to player.x,
                "y" to player.y
            )
        )
    }

    private fun handleChat(
        session: WebSocketSession,
        message: String?
    ) {
        val playerId = sessionPlayers[session.id]

        if (playerId == null) {
            sendError(session, "Player is not registered")
            return
        }

        val text = message?.trim()

        if (text.isNullOrEmpty()) {
            sendError(session, "Message cannot be empty")
            return
        }

        if (text.length > 200) {
            sendError(session, "Message is too long (max 200 characters)")
            return
        }

        val player = playerManager.getAllPlayers()
            .firstOrNull { it.id == playerId }

        if (player == null) {
            sendError(session, "Player not found")
            return
        }

        broadcastToPlayers(
            mapOf(
                "type" to "CHAT_MESSAGE",
                "id" to player.id.toString(),
                "name" to player.name,
                "message" to text
            )
        )
    }
}