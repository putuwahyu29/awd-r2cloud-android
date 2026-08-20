package com.awd.r2cloud.domain.model

data class Bucket(
    val name: String,
    val creationDate: String,
    val size: Long = 0L,
    val isOnline: Boolean = true,
    val verifiedViaApi: Boolean = false,
    val isPublic: Boolean = false,
    val publicDomain: String? = null
)
