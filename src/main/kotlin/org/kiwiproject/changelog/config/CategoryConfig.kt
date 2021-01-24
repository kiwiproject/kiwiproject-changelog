package org.kiwiproject.changelog.config

data class CategoryConfig(val defaultCategory: String = "Assorted",
                          val labelToCategoryMapping: Map<String, String>?,
                          val alwaysIncludePRsFrom: List<String>?,
                          val categoryOrder: List<String>?)
