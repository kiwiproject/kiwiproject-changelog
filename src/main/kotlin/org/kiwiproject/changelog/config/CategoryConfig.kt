package org.kiwiproject.changelog.config

data class CategoryConfig(
    val defaultCategory: String,
    val labelToCategoryMapping: Map<String, String>,
    val categoryOrder: List<String>,
    val categoryToEmoji: Map<String, String?>
) {
    companion object {
        fun empty(): CategoryConfig {
            return CategoryConfig(
                defaultCategory = "Assorted",
                labelToCategoryMapping = mapOf(),
                categoryOrder = listOf(),
                categoryToEmoji = mapOf()
            )
        }
    }
}
