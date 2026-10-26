package com.example.notally.data.repository

import com.example.notally.data.local.SubjectDao
import com.example.notally.data.model.Subject
import com.example.notally.data.model.SubjectWithTaskCount
import kotlinx.coroutines.flow.Flow

class SubjectRepository(
    private val subjectDao: SubjectDao,
) {

    fun observeSubjects(): Flow<List<Subject>> = subjectDao.observeSubjects()

    fun observeSubjectsWithTaskCount(): Flow<List<SubjectWithTaskCount>> =
        subjectDao.observeSubjectsWithTaskCount()

    suspend fun getSubject(subjectId: Long): Subject? = subjectDao.getById(subjectId)

    suspend fun createSubject(
        name: String,
        colorArgb: Long,
        professor: String? = null,
        credits: Int? = null,
    ): Long = subjectDao.insert(
        Subject(
            name = name,
            colorArgb = colorArgb,
            professor = professor,
            credits = credits,
        ),
    )

    suspend fun updateSubject(subject: Subject) = subjectDao.update(subject)

    suspend fun deleteSubject(subjectId: Long) = subjectDao.deleteById(subjectId)
}
