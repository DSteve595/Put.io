class PutioSecrets(
    val keystorePath: String? = null,
    val keystorePassword: String? = null,
    val keyAlias: String? = null,
    val keyPassword: String? = null,

    val putioClientId: String,
    val putioApiKey: String
)
