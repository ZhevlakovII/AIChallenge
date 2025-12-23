package ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.entity.ProviderConfigEntity

@Dao
internal interface ProviderConfigDao {

    @Query("SELECT * FROM llm_provider_config")
    suspend fun getConfig(): ProviderConfigEntity

    @Query("UPDATE llm_provider_config SET apiUrl = :url")
    suspend fun updateApiUrl(url: String)

    @Query("UPDATE llm_provider_config SET model = :model")
    suspend fun updateModel(model: String)

    @Query("UPDATE llm_provider_config SET apiKey = :key")
    suspend fun updateApiKey(key: String)

    @Upsert(entity = ProviderConfigEntity::class)
    suspend fun updateConfig(config: ProviderConfigEntity)
}
