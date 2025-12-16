package ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_provider_config")
internal class ProviderConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val primaryKey: Long = 0L,
    val apiUrl: String,
    val model: String,
    val apiKey: String,
)
