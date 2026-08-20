package com.awd.r2cloud.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.*

@Serializable
data class R2UsageResponse(
    val result: R2UsageResult?,
    val success: Boolean,
    val errors: List<CloudflareError>
)

@Serializable
data class R2UsageResult(
    val payloadSize: Long,
    val objectCount: Int,
    val classAOperations: Long,
    val classBOperations: Long
)

@Serializable
data class ManagedDomainResponse(
    val result: ManagedDomainResult?,
    val success: Boolean,
    val errors: List<CloudflareError>
)

@Serializable
data class ManagedDomainResult(
    val domain: String?,
    val enabled: Boolean
)

@Serializable
data class ManagedDomainRequest(
    val enabled: Boolean
)

@Serializable
data class CloudflareError(
    val code: Int,
    val message: String
)

interface CloudflareApi {
    @GET("accounts/{account_id}/r2/usage")
    suspend fun getAccountUsage(
        @Path("account_id") accountId: String,
        @Header("Authorization") auth: String
    ): R2UsageResponse

    @GET("accounts/{account_id}/r2/buckets/{bucket_name}/usage")
    suspend fun getBucketUsage(
        @Path("account_id") accountId: String,
        @Path("bucket_name") bucketName: String,
        @Header("Authorization") auth: String
    ): R2UsageResponse

    @GET("accounts/{account_id}/r2/buckets/{bucket_name}/domains/managed")
    suspend fun getManagedDomain(
        @Path("account_id") accountId: String,
        @Path("bucket_name") bucketName: String,
        @Header("Authorization") auth: String
    ): ManagedDomainResponse

    @PUT("accounts/{account_id}/r2/buckets/{bucket_name}/domains/managed")
    suspend fun updateManagedDomain(
        @Path("account_id") accountId: String,
        @Path("bucket_name") bucketName: String,
        @Header("Authorization") auth: String,
        @Body request: ManagedDomainRequest
    ): ManagedDomainResponse
}
