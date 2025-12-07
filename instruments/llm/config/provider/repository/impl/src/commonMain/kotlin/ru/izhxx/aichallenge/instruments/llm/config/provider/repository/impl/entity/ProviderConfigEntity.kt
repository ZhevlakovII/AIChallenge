package ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_provider_config")
internal class ProviderConfigEntity(
    @PrimaryKey
    internal val primaryKey: Long = 0L,
    val apiUrl: String,
    val model: String,
    val apiKey: String,
)
