package com.example.tennisscore.domain

/** Derives the next server from the first server and the scored match. */
object ServeOrder {
    fun currentServer(score: MatchScore, firstServer: Player): Player {
        val completedGames = score.sets.sumOf { it.you + it.opponent }
        val gameServer = if (completedGames % 2 == 0) firstServer else firstServer.other()
        if (!score.isTieBreak && !score.isMatchTieBreak) return gameServer

        val playedTieBreakPoints = score.youPoints + score.opponentPoints
        return if (playedTieBreakPoints > 0 && (playedTieBreakPoints - 1) / 2 % 2 == 0) {
            gameServer.other()
        } else {
            gameServer
        }
    }

    private fun Player.other(): Player =
        if (this == Player.YOU) Player.OPPONENT else Player.YOU
}
