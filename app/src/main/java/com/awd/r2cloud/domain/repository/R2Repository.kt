package com.awd.r2cloud.domain.repository

import com.awd.r2cloud.domain.model.Bucket
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.domain.model.R2Credentials

interface R2Repository {
    suspend fun validateCredentials(credentials: R2Credentials): Boolean
    suspend fun getBuckets(): Result<List<Bucket>>
    suspend fun createBucket(name: String, region: String? = null): Result<Unit>
    suspend fun deleteBucket(name: String): Result<Unit>
    suspend fun listObjects(bucketName: String, prefix: String?): Result<List<FileItem>>
    suspend fun listObjectsRecursive(bucketName: String): Result<List<FileItem>>
    suspend fun uploadFile(
        bucketName: String, 
        key: String, 
        fileBytes: ByteArray,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Unit>
    suspend fun deleteObject(bucketName: String, key: String): Result<Unit>
    suspend fun renameObject(bucketName: String, oldKey: String, newKey: String): Result<Unit>
    suspend fun copyObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String): Result<Unit>
    suspend fun moveObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String): Result<Unit>
    suspend fun getRecentFiles(limit: Int = 10): Result<List<FileItem>>
    suspend fun getObject(
        bucketName: String, 
        key: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<ByteArray>
    suspend fun generatePresignedUrl(bucketName: String, key: String): Result<String>
    suspend fun getAccountUsage(): Result<com.awd.r2cloud.data.remote.R2UsageResult>
    suspend fun getNativeUsage(bucketName: String): Result<Long>
    suspend fun checkBucketHealth(bucketName: String): Result<Boolean>
    suspend fun getManagedDomain(bucketName: String): Result<Pair<Boolean, String?>>
    suspend fun togglePublicAccess(bucketName: String, enabled: Boolean): Result<Boolean>
    
    // Account Management
    fun saveAccount(credentials: R2Credentials)
    fun getAllAccounts(): List<R2Credentials>
    fun getActiveAccount(): R2Credentials?
    fun setActiveAccount(accountId: String)
    fun removeAccount(accountId: String)
    fun clearAll()
    fun isAppLockEnabled(): Boolean
    fun setAppLockEnabled(enabled: Boolean)
    
    // Backup Management
    fun getBackupFolders(): List<com.awd.r2cloud.domain.model.BackupFolder>
    fun saveBackupFolder(folder: com.awd.r2cloud.domain.model.BackupFolder)
    fun removeBackupFolder(uri: String)
    
    // Starred Management
    fun getStarredFiles(): Set<String>
    fun toggleStarred(bucketName: String, fileKey: String)
    suspend fun getAllStarredFiles(): Result<List<com.awd.r2cloud.domain.model.FileItem>>
}
