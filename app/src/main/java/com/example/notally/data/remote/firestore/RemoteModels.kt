package com.example.notally.data.remote.firestore

data class RemoteTask(
    val title: String,
    val priority: String,
    val status: String,
    val notes: String = "",
    val courseId: String? = null,
    val dueAt: Long? = null,
    val subtasks: List<RemoteSubtask> = emptyList(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val completedAt: Long? = null,
    val deletedAt: Long? = null,
)
data class RemoteCurso(
    val name: String,
    val professor: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)

data class RemoteSubtask(
    val id: String,
    val title: String,
    val isDone: Boolean,
    val position: Int,
)
