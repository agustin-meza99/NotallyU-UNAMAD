package com.example.notally.data.remote

import com.example.notally.data.model.Subject
import com.example.notally.data.remote.firestore.RemoteSubject
import com.example.notally.data.remote.firestore.SubjectLimits
import com.example.notally.data.remote.firestore.toLocal
import com.example.notally.data.remote.firestore.toRemote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SubjectMapperTest {

    private fun subject(
        id: Long = 3L,
        name: String = "Historia del Peru",
        colorArgb: Long = 0xFF3A5A9E,
        professor: String? = "Dra. Rojas",
        credits: Int? = 4,
        createdAt: Long = 1_600_000_000_000L,
    ) = Subject(
        id = id,
        name = name,
        colorArgb = colorArgb,
        professor = professor,
        credits = credits,
        createdAt = createdAt,
    )

    @Test
    fun `subject round trips through remote`() {
        val local = subject()

        val remote = local.toRemote()
        val back = remote.toLocal(localId = local.id)

        assertEquals(local, back)
        assertEquals(local.name, remote.name)
        assertEquals(local.colorArgb, remote.colorArgb)
        assertEquals(local.createdAt, remote.createdAt)
        assertEquals(local.createdAt, remote.updatedAt)
        assertNull(remote.deletedAt)
    }

    @Test
    fun `nullable professor and credits survive as null`() {
        val local = subject(professor = null, credits = null)

        val remote = local.toRemote()
        val back = remote.toLocal(localId = local.id)

        assertNull(remote.professor)
        assertNull(remote.credits)
        assertNull(back.professor)
        assertNull(back.credits)
    }

    @Test
    fun `updatedAt can be supplied explicitly for last write wins`() {
        val local = subject()

        val remote = local.toRemote(updatedAtMs = 1_700_000_000_000L)

        assertEquals(1_700_000_000_000L, remote.updatedAt)
        assertEquals(local, remote.toLocal(localId = local.id))
    }

    @Test
    fun `name accepts 120 characters and rejects 121`() {
        assertEquals(120, subject(name = "a".repeat(120)).toRemote().name.length)
        assertThrows(IllegalArgumentException::class.java) {
            subject(name = "a".repeat(121)).toRemote()
        }
    }

    @Test
    fun `empty name is rejected because rules require at least one character`() {
        assertThrows(IllegalArgumentException::class.java) {
            subject(name = "").toRemote()
        }
    }

    @Test
    fun `professor accepts 120 characters and rejects 121`() {
        assertEquals(120, subject(professor = "p".repeat(120)).toRemote().professor!!.length)
        assertThrows(IllegalArgumentException::class.java) {
            subject(professor = "p".repeat(121)).toRemote()
        }
    }

    @Test
    fun `negative credits are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            subject(credits = -1).toRemote()
        }
    }

    @Test
    fun `zero credits are valid`() {
        assertEquals(0, subject(credits = 0).toRemote().credits)
    }

    @Test
    fun `tombstone cannot be mapped as a live local subject`() {
        val tombstone = RemoteSubject(
            name = "Borrada",
            colorArgb = 0xFF3A5A9E,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_610_000_000_000L,
            deletedAt = 1_620_000_000_000L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            tombstone.toLocal()
        }
    }

    @Test
    fun `remote with null createdAt falls back to the provided clock`() {
        val remote = RemoteSubject(
            name = "Sin fecha",
            colorArgb = 0xFF2F6A4E,
            createdAt = null,
        )

        val local = remote.toLocal(now = 77_000L)

        assertEquals(77_000L, local.createdAt)
    }

    @Test
    fun `invalid remote name and professor are rejected on the way in`() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteSubject(name = "", colorArgb = 0).toLocal()
        }
        assertThrows(IllegalArgumentException::class.java) {
            RemoteSubject(
                name = "Valida",
                colorArgb = 0,
                professor = "p".repeat(SubjectLimits.MAX_PROFESSOR + 1),
            ).toLocal()
        }
        assertThrows(IllegalArgumentException::class.java) {
            RemoteSubject(name = "Valida", colorArgb = 0, credits = -5).toLocal()
        }
    }
}
