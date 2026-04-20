package com.vivek.unosimple.multiplayer

import com.vivek.unosimple.engine.Rules
import com.vivek.unosimple.engine.models.GameAction
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Integration coverage for the multiplayer sync layer. These tests drive
 * multiple clients against the same authoritative sim and assert that:
 *
 * - Every client observes the same state after each action.
 * - Clients can only submit on their own turn.
 * - Clients can only submit actions for themselves.
 * - A full game can be played through the sync interface end-to-end with
 *   a winner emerging on shared state.
 */
class InProcessSyncServiceTest {

    @Test
    fun bothClientsSeeTheSameInitialState() = runTest {
        val shared = InProcessSyncService.Shared(random = Random(1))
        val alice = shared.clientFor("p1")
        val bob = shared.clientFor("p2")

        alice.startRound(
            seats = listOf(PlayerSeat("p1", "Alice"), PlayerSeat("p2", "Bob")),
            handSize = 5,
        )

        val aliceState = alice.state.value
        val bobState = bob.state.value
        assertNotNull(aliceState)
        assertEquals(aliceState, bobState)
        assertEquals(2, aliceState.players.size)
    }

    @Test
    fun rejectsActionFromNonActingClient() = runTest {
        val shared = InProcessSyncService.Shared(random = Random(2))
        val alice = shared.clientFor("p1")
        val bob = shared.clientFor("p2")
        alice.startRound(listOf(PlayerSeat("p1", "A"), PlayerSeat("p2", "B")))

        // p1 is the starter, so bob should not be able to play.
        val anyCardOfBob = bob.state.value!!.players[1].hand.first()
        val result = bob.submit(
            GameAction.PlayCard(playerId = "p2", card = anyCardOfBob),
        )
        assertIs<SubmitResult.NotMyTurn>(result)
    }

    @Test
    fun rejectsSpoofedActionForAnotherPlayer() = runTest {
        val shared = InProcessSyncService.Shared(random = Random(3))
        val alice = shared.clientFor("p1")
        alice.startRound(listOf(PlayerSeat("p1", "A"), PlayerSeat("p2", "B")))

        // Alice tries to submit an action on behalf of p2 — authority should reject.
        val bobsCard = alice.state.value!!.players[1].hand.first()
        val result = alice.submit(
            GameAction.PlayCard(playerId = "p2", card = bobsCard),
        )
        assertIs<SubmitResult.Rejected>(result)
    }

    @Test
    fun acceptedPlayPropagatesToAllClients() = runTest {
        val shared = InProcessSyncService.Shared(random = Random(4))
        val alice = shared.clientFor("p1")
        val bob = shared.clientFor("p2")
        alice.startRound(listOf(PlayerSeat("p1", "A"), PlayerSeat("p2", "B")))

        // Find a legal play for p1.
        val currentState = alice.state.value!!
        val legalCard = Rules.legalPlaysFor(currentState, currentState.players[0].hand).firstOrNull()
        // If no legal play (rare with handSize=7), draw instead.
        val result = if (legalCard != null) {
            alice.submit(
                GameAction.PlayCard(
                    playerId = "p1",
                    card = legalCard,
                    chosenColor = if (legalCard.color == null) {
                        com.vivek.unosimple.engine.models.CardColor.RED
                    } else null,
                ),
            )
        } else {
            alice.submit(GameAction.DrawCard(playerId = "p1"))
        }
        assertIs<SubmitResult.Accepted>(result)

        // State on bob must match alice (same object reference via StateFlow).
        assertEquals(alice.state.value, bob.state.value)
        // Turn must have advanced.
        assertEquals("p2", bob.state.value!!.currentPlayer.id)
    }

    @Test
    fun fullGameViaSyncProducesAWinner() = runTest {
        val shared = InProcessSyncService.Shared(random = Random(42))
        val clientP1 = shared.clientFor("p1")
        val clientP2 = shared.clientFor("p2")
        val clientP3 = shared.clientFor("p3")
        val clients = mapOf("p1" to clientP1, "p2" to clientP2, "p3" to clientP3)

        clientP1.startRound(
            listOf(
                PlayerSeat("p1", "A"),
                PlayerSeat("p2", "B"),
                PlayerSeat("p3", "C"),
            ),
        )

        // Simple driver: whoever's turn it is picks a random legal play or
        // draws. Confirms the sync layer supports a full game end-to-end.
        val rng = Random(123)
        var steps = 0
        while (clientP1.state.value?.isRoundOver == false && steps < 2000) {
            val s = clientP1.state.value!!
            val actor = s.currentPlayer
            val legal = Rules.legalPlaysFor(s, actor.hand)
            val action = if (legal.isEmpty()) {
                GameAction.DrawCard(playerId = actor.id)
            } else {
                val card = legal.random(rng)
                GameAction.PlayCard(
                    playerId = actor.id,
                    card = card,
                    chosenColor = if (card.color == null) {
                        com.vivek.unosimple.engine.models.CardColor.entries.random(rng)
                    } else null,
                    declareUno = actor.hand.size == 2,
                )
            }
            val result = clients.getValue(actor.id).submit(action)
            assertIs<SubmitResult.Accepted>(result, "step=$steps, action=$action")
            steps++
        }

        val final = clientP1.state.value!!
        assertTrue(final.isRoundOver, "Game should have ended within step cap; steps=$steps")
        assertNotNull(final.winnerId)
        // All clients converged on the same final state.
        assertEquals(final, clientP2.state.value)
        assertEquals(final, clientP3.state.value)
    }

    @Test
    fun notConnectedBeforeRoundStarts() = runTest {
        val shared = InProcessSyncService.Shared(random = Random(5))
        val alice = shared.clientFor("p1")
        val result = alice.submit(GameAction.DrawCard(playerId = "p1"))
        assertIs<SubmitResult.NotConnected>(result)
    }
}
