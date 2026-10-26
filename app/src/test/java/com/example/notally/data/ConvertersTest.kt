package com.example.notally.data

import com.example.notally.data.model.Estado
import com.example.notally.data.model.ModoTema
import com.example.notally.data.model.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConvertersTest {

    private val convertidores = Converters()

    @Test
    fun `la prioridad va y vuelve por su texto persistido en ingles`() {
        Prioridad.entries.forEach { prioridad ->
            val guardado = convertidores.prioridadATexto(prioridad)
            assertEquals(prioridad.aTexto(), guardado)
            assertEquals(prioridad, convertidores.textoAPrioridad(guardado))
        }
        assertEquals("HIGH", convertidores.prioridadATexto(Prioridad.ALTA))
        assertEquals("LOW", convertidores.prioridadATexto(Prioridad.BAJA))
        assertThrows(IllegalArgumentException::class.java) {
            convertidores.textoAPrioridad("URGENT")
        }
    }

    @Test
    fun `el estado va y vuelve por su texto persistido en ingles`() {
        Estado.entries.forEach { estado ->
            val guardado = convertidores.estadoATexto(estado)
            assertEquals(estado.aTexto(), guardado)
            assertEquals(estado, convertidores.textoAEstado(guardado))
        }
        assertEquals("DONE", convertidores.estadoATexto(Estado.COMPLETADA))
        assertEquals("IN_PROGRESS", convertidores.estadoATexto(Estado.EN_CURSO))
        assertThrows(IllegalArgumentException::class.java) {
            convertidores.textoAEstado("BLOCKED")
        }
    }

    @Test
    fun `el modo de tema va y vuelve por su texto persistido en ingles`() {
        ModoTema.entries.forEach { modo ->
            val guardado = convertidores.modoTemaATexto(modo)
            assertEquals(modo.aTexto(), guardado)
            assertEquals(modo, convertidores.textoAModoTema(guardado))
        }
        assertEquals("SYSTEM", convertidores.modoTemaATexto(ModoTema.SISTEMA))
        assertEquals("DARK", convertidores.modoTemaATexto(ModoTema.OSCURO))
        assertThrows(IllegalArgumentException::class.java) {
            convertidores.textoAModoTema("AUTO")
        }
    }

    @Test
    fun `los valores persistidos coinciden con los nombres usados en SQL`() {
        assertEquals("DONE", convertidores.estadoATexto(Estado.COMPLETADA))
        assertEquals("PENDING", convertidores.estadoATexto(Estado.PENDIENTE))
        assertEquals("HIGH", convertidores.prioridadATexto(Prioridad.ALTA))
        assertEquals("MEDIUM", convertidores.prioridadATexto(Prioridad.MEDIA))
    }
}
