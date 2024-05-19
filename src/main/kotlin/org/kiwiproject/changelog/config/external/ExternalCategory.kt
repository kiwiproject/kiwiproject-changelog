package org.kiwiproject.changelog.config.external

import com.fasterxml.jackson.annotation.JsonProperty

data class ExternalCategory(
    @JsonProperty("name") val name: String,
    @JsonProperty("emoji") val emoji: String?,
    @JsonProperty("labels") val labels: List<String>,
    @JsonProperty("default") val isDefault: Boolean
)
