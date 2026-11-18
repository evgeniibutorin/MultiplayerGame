package com.game

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameWebSocketIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    /**
     * 1. Создание WebSocket-подключения.
     * 2. Проверяем получение приветственного сообщения WELCOME.
     * 3. Отправление запрос JOIN с именем TestPlayer.
     * 4. Проверка получения сообщения JOINED,
     *    подтверждающего успешную регистрацию игрока.
     * 5. Проверка получения PLAYER_LIST со списком игроков.
     * 6. Проверка, что сервер зарегистрировал игрока
     *    с указанным именем TestPlayer.
     */
    @Test
    fun `should connect and register player`() {

        val messages = LinkedBlockingQueue<String>()

        val listener = object : WebSocket.Listener {

            private val buffer = StringBuilder()

            override fun onOpen(webSocket: WebSocket) {
                webSocket.request(1)
            }

            override fun onText(
                webSocket: WebSocket,
                data: CharSequence,
                last: Boolean
            ): CompletableFuture<*>? {

                buffer.append(data)

                if (last) {
                    messages.offer(buffer.toString())
                    buffer.setLength(0)
                }

                webSocket.request(1)

                return CompletableFuture.completedFuture(null)
            }
        }

        val client = HttpClient.newHttpClient()

        val webSocket = client.newWebSocketBuilder()
            .buildAsync(
                URI.create("ws://localhost:$port/game"),
                listener
            )
            .join()

        try {
            val welcome = messages.poll(5, TimeUnit.SECONDS)

            assertTrue(
                welcome?.contains("\"type\":\"WELCOME\"") == true,
                "Server should send WELCOME message"
            )

            webSocket.sendText(
                """{"type":"JOIN","name":"TestPlayer"}""",
                true
            ).join()

            val joined = messages.poll(5, TimeUnit.SECONDS)

            assertTrue(
                joined?.contains("\"type\":\"JOINED\"") == true,
                "Server should send JOINED message"
            )

            val playerList = messages.poll(5, TimeUnit.SECONDS)

            assertTrue(
                playerList?.contains("\"type\":\"PLAYER_LIST\"") == true,
                "Server should send PLAYER_LIST message"
            )

            assertTrue(
                joined.contains("TestPlayer"),
                "Registered player should have the requested name"
            )

        } finally {
            webSocket.sendClose(
                WebSocket.NORMAL_CLOSURE,
                "Test finished"
            ).join()
        }
    }


    private fun createListener(
        messages: LinkedBlockingQueue<String>
    ): WebSocket.Listener {

        return object : WebSocket.Listener {

            private val buffer = StringBuilder()

            override fun onOpen(webSocket: WebSocket) {
                webSocket.request(1)
            }

            override fun onText(
                webSocket: WebSocket,
                data: CharSequence,
                last: Boolean
            ): CompletableFuture<*>? {

                buffer.append(data)

                if (last) {
                    messages.offer(buffer.toString())
                    buffer.setLength(0)
                }

                webSocket.request(1)

                return CompletableFuture.completedFuture(null)
            }
        }
    }

    /**
     * 1. Подключает первого игрока Player1 к серверу.
     * 2. Проверяет получение WELCOME, JOINED и PLAYER_LIST.
     * 3. Подключает второго игрока Player2 к серверу.
     * 4. Проверяет получение WELCOME, JOINED и PLAYER_LIST
     *    вторым игроком.
     * 5. Проверяет, что первый игрок получает PLAYER_JOINED
     *    после подключения второго игрока.
     * 6. Отправляет команду MOVE от второго игрока
     *    для перемещения вправо.
     * 7. Проверяет, что оба игрока получают PLAYER_MOVED
     *    с обновлённой координатой X = 1.0.
     * 8. Отправляет сообщение Hello everyone! от второго игрока.
     * 9. Проверяет, что оба игрока получают CHAT_MESSAGE
     *    с отправленным текстом.
     */
    @Test
    fun `should synchronize two players movement and chat`() {

        val client = HttpClient.newHttpClient()

        val player1Messages = LinkedBlockingQueue<String>()
        val player2Messages = LinkedBlockingQueue<String>()

        var player1Socket: WebSocket? = null
        var player2Socket: WebSocket? = null

        try {
            player1Socket = client.newWebSocketBuilder()
                .buildAsync(
                    URI.create("ws://localhost:$port/game"),
                    createListener(player1Messages)
                )
                .join()

            awaitMessage(player1Messages, "WELCOME")

            player1Socket.sendText(
                """{"type":"JOIN","name":"Player1"}""",
                true
            ).join()

            awaitMessage(player1Messages, "JOINED")
            awaitMessage(player1Messages, "PLAYER_LIST")

            player2Socket = client.newWebSocketBuilder()
                .buildAsync(
                    URI.create("ws://localhost:$port/game"),
                    createListener(player2Messages)
                )
                .join()

            awaitMessage(player2Messages, "WELCOME")

            player2Socket.sendText(
                """{"type":"JOIN","name":"Player2"}""",
                true
            ).join()

            awaitMessage(player2Messages, "JOINED")
            awaitMessage(player2Messages, "PLAYER_LIST")

            val playerJoined = awaitMessage(
                player1Messages,
                "PLAYER_JOINED"
            )

            assertTrue(playerJoined.contains("Player2"))


            player2Socket.sendText(
                """{"type":"MOVE","direction":"RIGHT"}""",
                true
            ).join()

            val movementForPlayer1 = awaitMessage(
                player1Messages,
                "PLAYER_MOVED"
            )

            val movementForPlayer2 = awaitMessage(
                player2Messages,
                "PLAYER_MOVED"
            )

            assertTrue(movementForPlayer1.contains("\"x\":1.0"))
            assertTrue(movementForPlayer2.contains("\"x\":1.0"))


            player2Socket.sendText(
                """{"type":"CHAT","message":"Hello everyone!"}""",
                true
            ).join()

            val chatForPlayer1 = awaitMessage(
                player1Messages,
                "CHAT_MESSAGE"
            )

            val chatForPlayer2 = awaitMessage(
                player2Messages,
                "CHAT_MESSAGE"
            )

            assertTrue(chatForPlayer1.contains("Hello everyone!"))
            assertTrue(chatForPlayer2.contains("Hello everyone!"))

        } finally {

            player2Socket?.sendClose(
                WebSocket.NORMAL_CLOSURE,
                "Test finished"
            )?.join()

            player1Socket?.sendClose(
                WebSocket.NORMAL_CLOSURE,
                "Test finished"
            )?.join()
        }
    }

    private fun awaitMessage(
        messages: LinkedBlockingQueue<String>,
        expectedType: String
    ): String {

        val message = messages.poll(5, TimeUnit.SECONDS)

        assertTrue(
            message?.contains("\"type\":\"$expectedType\"") == true,
            "Expected $expectedType, received: $message"
        )

        return message!!
    }
}
