package com.yoshicash.websocketprinter.utils

object EnvironmentConfig {

    private const val ENVIRONMENT = "prod" // dev, qa, prod
    const val YOSHI_DASHBOARD_DEV_URL = "http://dashboard.dev-yoshi.com.s3-website-us-east-1.amazonaws.com"
    const val YOSHI_DASHBOARD_QA_URL = "https://dashboard.qa-yoshicash.com"
    const val YOSHI_DASHBOARD_PROD_URL = "https://dashboard.yoshicash.com"

    fun getDashboardURl()  = when (ENVIRONMENT) {
        "dev" -> YOSHI_DASHBOARD_DEV_URL
        "qa" -> YOSHI_DASHBOARD_QA_URL
        "prod" -> YOSHI_DASHBOARD_PROD_URL
        else -> YOSHI_DASHBOARD_DEV_URL
    }


}