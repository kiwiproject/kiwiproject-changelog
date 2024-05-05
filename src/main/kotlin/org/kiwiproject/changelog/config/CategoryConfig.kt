package org.kiwiproject.changelog.config

// TODO Probably should remove the default for defaultCategory; should also make args non-nullable
data class CategoryConfig(val defaultCategory: String = "Assorted",
                          val labelToCategoryMapping: Map<String, String>?,
                          val alwaysIncludePRsFrom: List<String>?,
                          val categoryOrder: List<String>?)
