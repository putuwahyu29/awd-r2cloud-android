package com.awd.r2cloud.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class R2Credentials(
    val alias: String = "My R2 Account",
    val accountId: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val apiToken: String? = null,
    val encryptionKey: String? = null
)
