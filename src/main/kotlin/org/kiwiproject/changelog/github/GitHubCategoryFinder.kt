package org.kiwiproject.changelog.github

import org.kiwiproject.changelog.config.CategoryConfig

class GitHubCategoryFinder(categoryConfig: CategoryConfig) {

    private val defaultCategory = categoryConfig.defaultCategory
    private val labelMapping = categoryConfig.labelToCategoryMapping

    fun findCategory(labels: List<String>) : String {
        if (labels.isEmpty()) {
            return defaultCategory
        }

        val foundLabel = labels.intersect(labelMapping.keys).firstOrNull()
        if (foundLabel == null) {
            println("⚠️  WARN: Using default category ($defaultCategory) b/c no mapping found for label(s): $labels")
            return defaultCategory
        }

        return labelMapping[foundLabel] ?: error("matchingLabel should never be null here")
    }
}
