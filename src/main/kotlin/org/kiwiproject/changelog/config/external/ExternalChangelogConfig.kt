package org.kiwiproject.changelog.config.external

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls.AS_EMPTY

// The JsonSetter is necessary, otherwise null is the actual value when deserializing if there are no values in the YAML

data class ExternalChangelogConfig(
    @JsonSetter(nulls = AS_EMPTY) val categories: List<ExternalCategory> = listOf(),
    val stripVPrefixFromNextMilestone: Boolean = true,
    val useTagDateForRelease: Boolean = false
) {

    fun labelCategoryMap(): Map<String, String> {
        return categories
            .flatMap { category -> category.labels.map { label -> LabelToCategory(label, category.name) } }
            .associate { labelToCategory -> labelToCategory.label to labelToCategory.category }
    }

    private data class LabelToCategory(val label: String, val category: String)

    fun categoryEmojiMap(): Map<String, String?> =
        categories.associate { category -> category.name to category.emoji }

    fun categoryOrder(): List<String> = categories.map { it.name }

    fun defaultCategory(): String? {
        val categoryOrNull = categories.firstOrNull { it.isDefault }
        return categoryOrNull?.name
    }
}
