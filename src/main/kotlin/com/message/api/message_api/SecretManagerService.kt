package com.message.api.message_api

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import org.springframework.stereotype.Service

@Service
class SecretManagerService {

    fun loadSecret(projectId: String, secretName: String, version: String = "latest"): String {
        try {
            SecretManagerServiceClient.create().use { client ->
                return getSecret(client, projectId, secretName, version)
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to load secret from Secret Manager. " +
                        "Ensure the secret '$secretName' exists in project '$projectId' and the service account has access.",
                e
            )
        }
    }

    fun loadSecrets(projectId: String, secretid: String): Map<String, String>  {

        val secretValues = mutableMapOf<String, String>()

        val secrets = mapOf("DATABASE_PASSWORD" to secretid)

        secrets.forEach { (secretName, secretId) ->
            try {
                val value = loadSecret(
                    projectId,
                    secretId
                )
                secretValues[secretName] = value
            } catch (e: Exception) {

                throw e
            }
        }
        return secretValues
    }

    private fun getSecret(
        client: SecretManagerServiceClient,
        projectId: String,
        secretId: String,
        version: String = "latest"
    ): String
    {

        val secretVersionName = SecretVersionName.of(projectId, secretId, version)
        val request = AccessSecretVersionRequest.newBuilder()
            .setName(secretVersionName.toString()).build()

        val response = client.accessSecretVersion(request)
        return response.payload.data.toStringUtf8()
    }


}

