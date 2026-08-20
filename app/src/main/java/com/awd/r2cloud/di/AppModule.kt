package com.awd.r2cloud.di

import com.awd.r2cloud.data.remote.CloudflareApi
import com.awd.r2cloud.data.repository.PreferenceRepositoryImpl
import com.awd.r2cloud.data.repository.R2RepositoryImpl
import com.awd.r2cloud.data.repository.TransferRepositoryImpl
import com.awd.r2cloud.domain.repository.PreferenceRepository
import com.awd.r2cloud.domain.repository.R2Repository
import com.awd.r2cloud.domain.repository.TransferRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideCloudflareApi(okHttpClient: OkHttpClient, json: Json): CloudflareApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.cloudflare.com/client/v4/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(CloudflareApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindR2Repository(
        r2RepositoryImpl: R2RepositoryImpl
    ): R2Repository

    @Binds
    @Singleton
    abstract fun bindTransferRepository(
        transferRepositoryImpl: TransferRepositoryImpl
    ): TransferRepository

    @Binds
    @Singleton
    abstract fun bindPreferenceRepository(
        preferenceRepositoryImpl: PreferenceRepositoryImpl
    ): PreferenceRepository
}
