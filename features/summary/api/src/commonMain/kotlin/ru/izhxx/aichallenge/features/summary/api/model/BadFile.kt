package ru.izhxx.aichallenge.features.summary.api.model

/**
 * Nothing to do
 * TODO (asjdboasdhaisjda)
 */
data class BadFile(
    val i: Long,
    val n: String,
    val dsc: String? = "ASDUHAOHISDOISAOIDUsad",
    val ennn: MyEnumLol
) {
    enum class MyEnumLol {
        NOTHING
    }
}
