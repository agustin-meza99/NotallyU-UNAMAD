package com.example.notally.data.remote

import com.example.notally.data.model.Curso
import com.example.notally.data.remote.firestore.RemoteCurso
import com.example.notally.data.remote.firestore.CursoLimits
import com.example.notally.data.remote.firestore.toLocal
import com.example.notally.data.remote.firestore.toRemote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CursoMapperTest {

    private fun curso(
        id: Long = 3L,
        nombre: String = "Historia del Peru",
        profesor: String? = "Dra. Rojas",
        creditos: Int? = 4,
        creadoEn: Long = 1_600_000_000_000L,
    ) = Curso(
        id = id,
        nombre = nombre,
        profesor = profesor,
        creditos = creditos,
        creadoEn = creadoEn,
    )

    @Test
    fun `el curso va y vuelve por el remoto`() {
        val local = curso()

        val remoto = local.toRemote()
        val vuelta = remoto.toLocal(localId = local.id)

        assertEquals(local, vuelta)
        assertEquals(local.nombre, remoto.name)
        assertEquals(local.creadoEn, remoto.createdAt)
        assertEquals(local.creadoEn, remoto.updatedAt)
        assertNull(remoto.deletedAt)
    }

    @Test
    fun `profesor y creditos nulos sobreviven como nulos`() {
        val local = curso(profesor = null, creditos = null)

        val remoto = local.toRemote()
        val vuelta = remoto.toLocal(localId = local.id)

        assertNull(remoto.professor)
        assertNull(remoto.credits)
        assertNull(vuelta.profesor)
        assertNull(vuelta.creditos)
    }

    @Test
    fun `updatedAt se puede pasar explicitamente para last write wins`() {
        val local = curso()

        val remoto = local.toRemote(updatedAtMs = 1_700_000_000_000L)

        assertEquals(1_700_000_000_000L, remoto.updatedAt)
        assertEquals(local, remoto.toLocal(localId = local.id))
    }

    @Test
    fun `el nombre acepta 120 caracteres y rechaza 121`() {
        assertEquals(120, curso(nombre = "a".repeat(120)).toRemote().name.length)
        assertThrows(IllegalArgumentException::class.java) {
            curso(nombre = "a".repeat(121)).toRemote()
        }
    }

    @Test
    fun `un nombre vacio se rechaza porque las reglas exigen al menos un caracter`() {
        assertThrows(IllegalArgumentException::class.java) {
            curso(nombre = "").toRemote()
        }
    }

    @Test
    fun `el profesor acepta 120 caracteres y rechaza 121`() {
        assertEquals(120, curso(profesor = "p".repeat(120)).toRemote().professor!!.length)
        assertThrows(IllegalArgumentException::class.java) {
            curso(profesor = "p".repeat(121)).toRemote()
        }
    }

    @Test
    fun `los creditos negativos se rechazan`() {
        assertThrows(IllegalArgumentException::class.java) {
            curso(creditos = -1).toRemote()
        }
    }

    @Test
    fun `cero creditos es valido`() {
        assertEquals(0, curso(creditos = 0).toRemote().credits)
    }

    @Test
    fun `un tombstone no se puede mapear como curso local viva`() {
        val tombstone = RemoteCurso(
            name = "Borrada",
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_610_000_000_000L,
            deletedAt = 1_620_000_000_000L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            tombstone.toLocal()
        }
    }

    @Test
    fun `un remoto sin createdAt usa el reloj recibido`() {
        val remoto = RemoteCurso(
            name = "Sin fecha",
            createdAt = null,
        )

        val local = remoto.toLocal(now = 77_000L)

        assertEquals(77_000L, local.creadoEn)
    }

    @Test
    fun `nombre y profesor invalidos se rechazan al entrar`() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteCurso(name = "").toLocal()
        }
        assertThrows(IllegalArgumentException::class.java) {
            RemoteCurso(
                name = "Valida",
                professor = "p".repeat(CursoLimits.MAX_PROFESSOR + 1),
            ).toLocal()
        }
        assertThrows(IllegalArgumentException::class.java) {
            RemoteCurso(name = "Valida", credits = -5).toLocal()
        }
    }
}
