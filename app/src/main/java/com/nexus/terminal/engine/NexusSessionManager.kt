package com.nexus.terminal.engine

import java.util.UUID

data class NexusSession(
    val id: String,
    val name: String,
    val engine: NexusCommandEngine,
    val history: MutableList<String> = mutableListOf()
)

class NexusSessionManager(
    private val homePath: String
) {
    private val sessions = linkedMapOf<String, NexusSession>()

    var activeSessionId: String? = null
        private set

    fun createSession(name: String? = null): NexusSession {
        val id = UUID.randomUUID().toString().take(8)

        val session = NexusSession(
            id = id,
            name = name?.takeIf { it.isNotBlank() }
                ?: "session-${sessions.size + 1}",
            engine = NexusCommandEngine(homePath)
        )

        sessions[id] = session

        if (activeSessionId == null) {
            activeSessionId = id
        }

        return session
    }

    fun getActiveSession(): NexusSession? {
        return activeSessionId?.let { sessions[it] }
    }

    fun getSession(id: String): NexusSession? {
        return sessions[id]
    }

    fun listSessions(): List<NexusSession> {
        return sessions.values.toList()
    }

    fun switchSession(id: String): Boolean {
        if (!sessions.containsKey(id)) return false
        activeSessionId = id
        return true
    }

    fun closeSession(id: String): Boolean {
        if (!sessions.containsKey(id)) return false
        if (sessions.size <= 1) return false

        sessions.remove(id)

        if (activeSessionId == id) {
            activeSessionId = sessions.keys.first()
        }

        return true
    }

    fun closeActiveSession(): Boolean {
        val id = activeSessionId ?: return false
        return closeSession(id)
    }
}
