package com.awd.r2cloud.data.repository

import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import com.awd.r2cloud.data.local.EncryptedStorage
import com.awd.r2cloud.data.remote.CloudflareApi
import com.awd.r2cloud.data.remote.ManagedDomainRequest
import com.awd.r2cloud.data.remote.R2ClientFactory
import com.awd.r2cloud.domain.model.Bucket
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.domain.model.R2Credentials
import com.awd.r2cloud.domain.repository.R2Repository
import com.awd.r2cloud.security.EncryptionManager
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class R2RepositoryImpl @Inject constructor(
    private val clientFactory: R2ClientFactory,
    private val encryptedStorage: EncryptedStorage,
    private val cloudflareApi: CloudflareApi,
    private val encryptionManager: EncryptionManager
) : R2Repository {

    override suspend fun validateCredentials(credentials: R2Credentials): Boolean {
        return try {
            val client = clientFactory.createClient(credentials)
            client.listBuckets()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getBuckets(): Result<List<Bucket>> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val response = client.listBuckets()
            val buckets = response.buckets?.map { 
                Bucket(
                    name = it.name ?: "",
                    creationDate = it.creationDate?.toString() ?: ""
                )
            } ?: emptyList()
            Result.success(buckets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createBucket(name: String, region: String?): Result<Unit> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val request = CreateBucketRequest {
                bucket = name
                region?.let {
                    createBucketConfiguration {
                        locationConstraint = BucketLocationConstraint.fromValue(it)
                    }
                }
            }
            client.createBucket(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBucket(name: String): Result<Unit> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val request = DeleteBucketRequest {
                bucket = name
            }
            client.deleteBucket(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listObjects(bucketName: String, prefix: String?): Result<List<FileItem>> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val request = ListObjectsV2Request {
                bucket = bucketName
                this.prefix = prefix
                delimiter = "/"
            }
            val response = client.listObjectsV2(request)

            val folders = response.commonPrefixes?.map {
                FileItem(
                    key = it.prefix ?: "",
                    name = it.prefix?.removeSuffix("/")?.substringAfterLast("/") ?: it.prefix ?: "",
                    isFolder = true
                )
            } ?: emptyList()

            val files = response.contents?.filter { it.key != prefix }?.map {
                val isImage = it.key?.lowercase()?.let { key ->
                    key.endsWith(".jpg") || key.endsWith(".jpeg") || key.endsWith(".png") || key.endsWith(".webp") || key.endsWith(".gif") || key.endsWith(".bmp")
                } ?: false

                val thumbnailUrl = if (isImage) {
                    val getRequest = GetObjectRequest {
                        bucket = bucketName
                        key = it.key
                    }
                    client.presignGetObject(getRequest, 3600.seconds).url.toString()
                } else null

                FileItem(
                    key = it.key ?: "",
                    name = it.key?.substringAfterLast("/") ?: "",
                    size = it.size,
                    lastModified = it.lastModified?.toString(),
                    isFolder = false,
                    thumbnailUrl = thumbnailUrl,
                    bucketName = bucketName
                )
            } ?: emptyList()

            Result.success(folders + files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listObjectsRecursive(bucketName: String): Result<List<FileItem>> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val allFiles = mutableListOf<FileItem>()
            var continuationToken: String? = null
            
            do {
                val request = ListObjectsV2Request {
                    bucket = bucketName
                    this.continuationToken = continuationToken
                }
                val response = client.listObjectsV2(request)
                
                response.contents?.forEach { s3Object ->
                    val isImage = s3Object.key?.lowercase()?.let { key ->
                        key.endsWith(".jpg") || key.endsWith(".jpeg") || key.endsWith(".png") || key.endsWith(".webp") || key.endsWith(".gif") || key.endsWith(".bmp")
                    } ?: false

                    val thumbnailUrl = if (isImage) {
                        val getRequest = GetObjectRequest {
                            bucket = bucketName
                            key = s3Object.key
                        }
                        client.presignGetObject(getRequest, 3600.seconds).url.toString()
                    } else null

                    allFiles.add(
                        FileItem(
                            key = s3Object.key ?: "",
                            name = s3Object.key?.substringAfterLast("/") ?: "",
                            size = s3Object.size,
                            lastModified = s3Object.lastModified?.toString(),
                            isFolder = false,
                            thumbnailUrl = thumbnailUrl,
                            bucketName = bucketName
                        )
                    )
                }
                continuationToken = response.nextContinuationToken
            } while (continuationToken != null)

            Result.success(allFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(
        bucketName: String, 
        key: String, 
        fileBytes: ByteArray,
        onProgress: ((Int) -> Unit)?
    ): Result<Unit> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            
            val finalBytes = if (credentials.encryptionKey != null) {
                encryptionManager.encrypt(fileBytes, credentials.encryptionKey)
            } else fileBytes

            if (finalBytes.size > 5 * 1024 * 1024) { // Use Multipart for > 5MB
                uploadMultipart(client, bucketName, key, finalBytes, onProgress)
            } else {
                val request = PutObjectRequest {
                    bucket = bucketName
                    this.key = key
                    body = ByteStream.fromBytes(finalBytes)
                }
                client.putObject(request)
                onProgress?.invoke(100)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadMultipart(
        client: aws.sdk.kotlin.services.s3.S3Client, 
        bucketName: String, 
        key: String, 
        data: ByteArray,
        onProgress: ((Int) -> Unit)?
    ) {
        val createResponse = client.createMultipartUpload(CreateMultipartUploadRequest {
            bucket = bucketName
            this.key = key
        })
        val uploadId = createResponse.uploadId ?: throw Exception("Failed to get upload ID")

        val partSize = 5 * 1024 * 1024
        val parts = mutableListOf<CompletedPart>()
        
        var offset = 0
        var partNumber = 1
        while (offset < data.size) {
            val end = minOf(offset + partSize, data.size)
            val chunk = data.sliceArray(offset until end)
            
            val uploadPartResponse = client.uploadPart(UploadPartRequest {
                bucket = bucketName
                this.key = key
                this.uploadId = uploadId
                this.partNumber = partNumber
                body = ByteStream.fromBytes(chunk)
            })
            
            parts.add(CompletedPart {
                eTag = uploadPartResponse.eTag
                this.partNumber = partNumber
            })
            
            offset += partSize
            partNumber++
            
            val progress = ((offset.toFloat() / data.size.toFloat()) * 100).toInt().coerceAtMost(99)
            onProgress?.invoke(progress)
        }

        client.completeMultipartUpload(CompleteMultipartUploadRequest {
            bucket = bucketName
            this.key = key
            this.uploadId = uploadId
            multipartUpload = CompletedMultipartUpload {
                this.parts = parts
            }
        })
        onProgress?.invoke(100)
    }

    override suspend fun deleteObject(bucketName: String, key: String): Result<Unit> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val request = DeleteObjectRequest {
                bucket = bucketName
                this.key = key
            }
            client.deleteObject(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameObject(bucketName: String, oldKey: String, newKey: String): Result<Unit> {
        return copyObject(bucketName, oldKey, bucketName, newKey).onSuccess {
            deleteObject(bucketName, oldKey)
        }
    }

    override suspend fun copyObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String): Result<Unit> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val request = CopyObjectRequest {
                bucket = destBucket
                key = destKey
                copySource = "$sourceBucket/$sourceKey"
            }
            client.copyObject(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String): Result<Unit> {
        return copyObject(sourceBucket, sourceKey, destBucket, destKey).onSuccess {
            deleteObject(sourceBucket, sourceKey)
        }
    }

    override suspend fun getRecentFiles(limit: Int): Result<List<FileItem>> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val buckets = getBuckets().getOrThrow()
            val allFiles = mutableListOf<FileItem>()
            
            buckets.forEach { bucket ->
                listObjectsRecursive(bucket.name).onSuccess { files ->
                    allFiles.addAll(files.filter { !it.isFolder })
                }
            }
            
            val recent = allFiles.sortedByDescending { it.lastModified ?: "" }
                .take(limit)
            
            Result.success(recent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getObject(
        bucketName: String, 
        key: String,
        onProgress: ((Int) -> Unit)?
    ): Result<ByteArray> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val request = GetObjectRequest {
                bucket = bucketName
                this.key = key
            }
            client.getObject(request) { response ->
                val body = response.body ?: throw Exception("Empty body")
                onProgress?.invoke(50)
                val bytes = body.toByteArray()
                
                val finalBytes = if (credentials.encryptionKey != null) {
                    try {
                        encryptionManager.decrypt(bytes, credentials.encryptionKey)
                    } catch (e: Exception) {
                        bytes
                    }
                } else bytes
                
                onProgress?.invoke(100)
                Result.success(finalBytes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generatePresignedUrl(bucketName: String, key: String): Result<String> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            val request = GetObjectRequest {
                bucket = bucketName
                this.key = key
            }
            val presignedRequest = client.presignGetObject(request, 3600.seconds)
            Result.success(presignedRequest.url.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAccountUsage(): Result<com.awd.r2cloud.data.remote.R2UsageResult> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        val token = credentials.apiToken ?: return Result.failure(Exception("No API token provided"))
        
        return try {
            val response = cloudflareApi.getAccountUsage(
                accountId = credentials.accountId,
                auth = "Bearer $token"
            )
            if (response.success && response.result != null) {
                Result.success(response.result)
            } else {
                Result.failure(Exception(response.errors.firstOrNull()?.message ?: "Unknown API error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNativeUsage(bucketName: String): Result<Long> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        val token = credentials.apiToken ?: return Result.failure(Exception("No API token provided"))
        
        return try {
            val response = cloudflareApi.getBucketUsage(
                accountId = credentials.accountId,
                bucketName = bucketName,
                auth = "Bearer $token"
            )
            if (response.success && response.result != null) {
                Result.success(response.result.payloadSize)
            } else {
                Result.failure(Exception(response.errors.firstOrNull()?.message ?: "Unknown API error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getManagedDomain(bucketName: String): Result<Pair<Boolean, String?>> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        val token = credentials.apiToken ?: return Result.failure(Exception("No API token provided"))
        
        return try {
            val response = cloudflareApi.getManagedDomain(
                accountId = credentials.accountId,
                bucketName = bucketName,
                auth = "Bearer $token"
            )
            if (response.success && response.result != null) {
                Result.success(Pair(response.result.enabled, response.result.domain))
            } else {
                Result.failure(Exception(response.errors.firstOrNull()?.message ?: "Unknown API error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun togglePublicAccess(bucketName: String, enabled: Boolean): Result<Boolean> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        val token = credentials.apiToken ?: return Result.failure(Exception("No API token provided"))
        
        return try {
            val response = cloudflareApi.updateManagedDomain(
                accountId = credentials.accountId,
                bucketName = bucketName,
                auth = "Bearer $token",
                request = ManagedDomainRequest(enabled = enabled)
            )
            if (response.success && response.result != null) {
                Result.success(response.result.enabled)
            } else {
                Result.failure(Exception(response.errors.firstOrNull()?.message ?: "Unknown API error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkBucketHealth(bucketName: String): Result<Boolean> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        return try {
            val client = clientFactory.createClient(credentials)
            client.headBucket(HeadBucketRequest { bucket = bucketName })
            Result.success(true)
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    // Account Management
    override fun saveAccount(credentials: R2Credentials) {
        encryptedStorage.saveAccount(credentials)
    }

    override fun getAllAccounts(): List<R2Credentials> {
        return encryptedStorage.getAllAccounts()
    }

    override fun getActiveAccount(): R2Credentials? {
        return encryptedStorage.getActiveAccount()
    }

    override fun setActiveAccount(accountId: String) {
        encryptedStorage.setActiveAccount(accountId)
    }

    override fun removeAccount(accountId: String) {
        encryptedStorage.removeAccount(accountId)
    }

    override fun clearAll() {
        encryptedStorage.clearAll()
    }

    override fun isAppLockEnabled(): Boolean {
        return encryptedStorage.isAppLockEnabled()
    }

    override fun setAppLockEnabled(enabled: Boolean) {
        encryptedStorage.setAppLockEnabled(enabled)
    }

    override fun getBackupFolders(): List<com.awd.r2cloud.domain.model.BackupFolder> {
        return encryptedStorage.getBackupFolders()
    }

    override fun saveBackupFolder(folder: com.awd.r2cloud.domain.model.BackupFolder) {
        encryptedStorage.saveBackupFolder(folder)
    }

    override fun removeBackupFolder(uri: String) {
        encryptedStorage.removeBackupFolder(uri)
    }

    override fun getStarredFiles(): Set<String> {
        return encryptedStorage.getStarredFiles()
    }

    override fun toggleStarred(bucketName: String, fileKey: String) {
        encryptedStorage.toggleStarred(bucketName, fileKey)
    }

    override suspend fun getAllStarredFiles(): Result<List<com.awd.r2cloud.domain.model.FileItem>> {
        val credentials = getActiveAccount() ?: return Result.failure(Exception("No credentials found"))
        val starredKeys = encryptedStorage.getStarredFiles()
        if (starredKeys.isEmpty()) return Result.success(emptyList())

        return try {
            val buckets = getBuckets().getOrThrow()
            val starredFiles = mutableListOf<com.awd.r2cloud.domain.model.FileItem>()
            val client = clientFactory.createClient(credentials)
            
            buckets.forEach { bucket ->
                val bucketPrefix = "${bucket.name}|"
                val keysInThisBucket = starredKeys.filter { it.startsWith(bucketPrefix) }
                    .map { it.removePrefix(bucketPrefix) }
                
                if (keysInThisBucket.isNotEmpty()) {
                    val request = ListObjectsV2Request {
                        this.bucket = bucket.name
                    }
                    val response = client.listObjectsV2(request)
                    
                    response.contents?.filter { keysInThisBucket.contains(it.key) }?.forEach { s3Object ->
                        val isImage = s3Object.key?.lowercase()?.let { key ->
                            key.endsWith(".jpg") || key.endsWith(".jpeg") || key.endsWith(".png") || key.endsWith(".webp") || key.endsWith(".gif") || key.endsWith(".bmp")
                        } ?: false

                        val thumbnailUrl = if (isImage) {
                            val getRequest = GetObjectRequest {
                                this.bucket = bucket.name
                                key = s3Object.key
                            }
                            client.presignGetObject(getRequest, 3600.seconds).url.toString()
                        } else null

                        starredFiles.add(
                            FileItem(
                                key = s3Object.key ?: "",
                                name = s3Object.key?.substringAfterLast("/") ?: "",
                                size = s3Object.size,
                                lastModified = s3Object.lastModified?.toString(),
                                isFolder = false,
                                thumbnailUrl = thumbnailUrl,
                                bucketName = bucket.name
                            )
                        )
                    }
                }
            }
            Result.success(starredFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
