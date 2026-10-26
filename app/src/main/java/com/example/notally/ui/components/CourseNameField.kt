package com.example.notally.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.notally.R
import com.example.notally.data.repository.ResultadoCurso
import com.example.notally.ui.theme.FormaNotally

/**
 * Campo de nombre de curso compartido por la hoja de Cursos y la hoja de curso
 * del editor. Solo pinta el campo: el botón y su estado de envío los decide
 * cada flujo.
 */
@Composable
internal fun CourseNameField(
    valor: String,
    alCambiarValor: (String) -> Unit,
    resultado: ResultadoCurso?,
    alLimpiarError: () -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = {
            alCambiarValor(it)
            if (resultado != null) alLimpiarError()
        },
        label = { Text(stringResource(R.string.course_name)) },
        singleLine = true,
        isError = resultado != null && !resultado.esExito,
        supportingText = when (resultado) {
            ResultadoCurso.Duplicado -> {
                { Text(stringResource(R.string.course_name_duplicate)) }
            }

            ResultadoCurso.Error -> {
                { Text(stringResource(R.string.course_save_error)) }
            }

            else -> null
        },
        shape = FormaNotally.s,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
