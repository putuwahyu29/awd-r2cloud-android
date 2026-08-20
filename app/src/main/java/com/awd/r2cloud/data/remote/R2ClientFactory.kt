package com.awd.r2cloud.data.remote

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import com.awd.r2cloud.domain.model.R2Credentials
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class R2ClientFactory @Inject constructor() {
    fun createClient(credentials: R2Credentials): S3Client {
        return S3Client {
            region = "auto"
            endpointUrl = Url.parse("https://${credentials.accountId}.r2.cloudflarestorage.com")
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = credentials.accessKeyId
                secretAccessKey = credentials.secretAccessKey
            }
        }
    }
}
