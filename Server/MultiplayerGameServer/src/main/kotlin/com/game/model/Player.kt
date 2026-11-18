package com.game.model

import java.util.UUID

data class Player(
    val id: UUID,
    val name: String,
    val color: String,
    var x: Float,
    var y: Float
)