package com.example.notally.data.aula

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AulaParserTest {

    private val cursosJson = """
        [
          {
            "id": "a249fa6d-e1f9-fbb1-84f2-5e6bb66d8e83",
            "codeCourse": "SI311",
            "sectionId": "bb3c76cb-da07-4f42-3514-08df02e00f8a",
            "codeSection": "A",
            "name": "BIG DATA",
            "type": "OBLIGATORIO",
            "area": "Tecnología",
            "deparment": "INGENIERÍA DE SISTEMAS E INFORMÁTICA",
            "faculty": "INGENIERIA"
          },
          {
            "id": "2d947368-bfae-4e2d-e405-08d904e9c6bb",
            "codeCourse": "SI513",
            "sectionId": "21c23dda-4b37-4610-353b-08df02e00f8a",
            "codeSection": "B",
            "name": "APLICACIONES MÓVILES II",
            "type": "ELECTIVO",
            "area": "Ciencias",
            "deparment": "INGENIERÍA DE SISTEMAS E INFORMÁTICA",
            "faculty": "INGENIERIA"
          }
        ]
    """.trimIndent()

    private val tareasJson = """
        [
          {
            "id": "636ee12e-f7a5-4dc5-2637-08df1577cf4b",
            "title": "La ética del uso de los datos",
            "attempts": 2,
            "isGroup": false,
            "unidad": "UNIDAD 1",
            "description": "<div>Si tuvieras acceso a grandes volúmenes de datos&nbsp; de usuarios,</div><div><br></div><div>¿Qué límites &eacute;ticos establecerías?</div>",
            "dateBegin": "18/09/2026 07:34",
            "dateEnd": "18/09/2026 13:00",
            "isEnded": true
          }
        ]
    """.trimIndent()

    @Test
    fun `parsea cursos con sectionId como identificador externo`() {
        val cursos = AulaParser.parsearCursos(cursosJson)

        assertEquals(2, cursos.size)
        assertEquals("BIG DATA", cursos[0].nombre)
        assertEquals("bb3c76cb-da07-4f42-3514-08df02e00f8a", cursos[0].sectionId)
        assertEquals("SI311", cursos[0].codigoCurso)
        assertEquals("A", cursos[0].codigoSeccion)
        assertEquals("OBLIGATORIO", cursos[0].tipo)
        assertEquals("Tecnología", cursos[0].area)
        assertEquals("INGENIERÍA DE SISTEMAS E INFORMÁTICA", cursos[0].departamento)
        assertEquals("SI513", cursos[1].codigoCurso)
    }

    @Test
    fun `acepta el envoltorio status data del servidor`() {
        val envuelto = """{"status":true,"data":$cursosJson}"""

        val cursos = AulaParser.parsearCursos(envuelto)

        assertEquals(2, cursos.size)
        assertEquals("APLICACIONES MÓVILES II", cursos[1].nombre)
    }

    @Test
    fun `parsea tareas y limpia el HTML de la descripcion`() {
        val tareas = AulaParser.parsearTareas(tareasJson)

        assertEquals(1, tareas.size)
        val tarea = tareas.single()
        assertEquals("636ee12e-f7a5-4dc5-2637-08df1577cf4b", tarea.id)
        assertEquals("La ética del uso de los datos", tarea.titulo)
        assertEquals(
            "Si tuvieras acceso a grandes volúmenes de datos de usuarios, " +
                "¿Qué límites éticos establecerías?",
            tarea.descripcion,
        )
        assertEquals("UNIDAD 1", tarea.unidad)
        assertEquals(2, tarea.intentos)
        assertEquals(true, tarea.finalizada)
        assertEquals(false, tarea.grupal)
    }

    @Test
    fun `dateEnd con hora usa America Lima`() {
        val tareas = AulaParser.parsearTareas(tareasJson)

        assertEquals(
            Instant.parse("2026-09-18T18:00:00Z").toEpochMilli(),
            tareas.single().entregaEn,
        )
        assertEquals(
            Instant.parse("2026-09-18T12:34:00Z").toEpochMilli(),
            tareas.single().inicioEn,
        )
    }

    @Test
    fun `dateEnd sin hora usa las 2359 de Lima`() {
        val json = """
            [{"id":"t1","title":"Entrega","dateEnd":"20/09/2026"}]
        """.trimIndent()

        val tarea = AulaParser.parsearTareas(json).single()

        assertEquals(
            Instant.parse("2026-09-21T04:59:00Z").toEpochMilli(),
            tarea.entregaEn,
        )
    }

    @Test
    fun `isEnded se respeta y una fecha ilegible queda nula`() {
        val json = """
            [
              {"id":"t1","title":"Vencida","dateEnd":"18/09/2026 13:00","isEnded":true},
              {"id":"t2","title":"Rara","dateEnd":"sin fecha","isEnded":false}
            ]
        """.trimIndent()

        val tareas = AulaParser.parsearTareas(json)

        assertEquals(true, tareas[0].finalizada)
        assertEquals(false, tareas[1].finalizada)
        assertNull(tareas[1].entregaEn)
    }

    @Test
    fun `descripcion vacia y entidades numericas`() {
        assertEquals("", AulaParser.limpiarHtml(null))
        assertEquals("", AulaParser.limpiarHtml(""))
        assertEquals(
            "A & B <C> \"D\" 'E' — fin",
            AulaParser.limpiarHtml(
                "A &amp; B &lt;C&gt; &quot;D&quot; &#39;E&#39; &mdash; fin",
            ),
        )
        assertEquals(
            "Texto con acentos: áéíóú ñ",
            AulaParser.limpiarHtml("Texto con acentos: &aacute;&eacute;&iacute;&oacute;&uacute; &ntilde;"),
        )
    }

    @Test
    fun `json invalido lanza error tipado`() {
        assertThrows(AulaJsonException::class.java) {
            AulaParser.parsearCursos("no es json")
        }
        assertThrows(AulaJsonException::class.java) {
            AulaParser.parsearTareas("")
        }
    }

    @Test
    fun `array vacio devuelve lista vacia`() {
        assertEquals(emptyList<AulaCurso>(), AulaParser.parsearCursos("[]"))
        assertEquals(emptyList<AulaTarea>(), AulaParser.parsearTareas("[]"))
        assertEquals(
            emptyList<AulaCurso>(),
            AulaParser.parsearCursos("""{"status":false,"data":"[]"}"""),
        )
    }
}
