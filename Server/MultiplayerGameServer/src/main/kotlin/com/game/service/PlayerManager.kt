package com.game.service

import com.game.config.GameConfig
import com.game.model.Player
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class PlayerManager {

    private val players = ConcurrentHashMap<UUID, Player>()

    private val lastMoveTime = ConcurrentHashMap<UUID, Long>()

    private val availableColors = listOf(
        "#FF0000",
        "#00FF00",
        "#0000FF",
        "#FFFF00",
        "#FF00FF",
        "#00FFFF"
    )

    private val spawnPositions = listOf(
        Pair(-5f, -2f),
        Pair(0f, -2f),
        Pair(5f, -2f),
        Pair(-5f, 2f),
        Pair(0f, 2f),
        Pair(5f, 2f)
    )

    @Synchronized
    fun addPlayer(name: String): Player {
        val usedColors = players.values.map { it.color }

        val colorIndex = availableColors.indexOfFirst {
            it !in usedColors
        }

        if (colorIndex == -1) {
            throw IllegalStateException("No free player colors")
        }

        val color = availableColors[colorIndex]
        val spawnPosition = spawnPositions[colorIndex]

        val player = Player(
            id = UUID.randomUUID(),
            name = name,
            color = color,
            x = spawnPosition.first,
            y = spawnPosition.second
        )

        players[player.id] = player

        return player
    }

    @Synchronized
    fun removePlayer(playerId: UUID) {
        players.remove(playerId)
        lastMoveTime.remove(playerId)
    }

    fun getPlayer(playerId: UUID): Player? {
        return players[playerId]
    }

    fun getAllPlayers(): List<Player> {
        return players.values.toList()
    }

    fun movePlayer(playerId: UUID, direction: String): Player? {
        val player = players[playerId] ?: return null

        val currentTime = System.currentTimeMillis()
        val previousMoveTime = lastMoveTime[playerId] ?: 0L

        if (currentTime - previousMoveTime < GameConfig.MOVE_COOLDOWN_MS) {
            return null
        }

        when (direction) {
            "UP" -> player.y += GameConfig.PLAYER_SPEED
            "DOWN" -> player.y -= GameConfig.PLAYER_SPEED
            "LEFT" -> player.x -= GameConfig.PLAYER_SPEED
            "RIGHT" -> player.x += GameConfig.PLAYER_SPEED
            else -> return null
        }

        player.x = player.x.coerceIn(
            GameConfig.MAP_MIN,
            GameConfig.MAP_MAX
        )

        player.y = player.y.coerceIn(
            GameConfig.MAP_MIN,
            GameConfig.MAP_MAX
        )

        lastMoveTime[playerId] = currentTime

        return player
    }
}