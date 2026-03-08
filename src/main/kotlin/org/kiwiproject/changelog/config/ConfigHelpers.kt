package org.kiwiproject.changelog.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.google.common.annotations.VisibleForTesting
import org.kiwiproject.yaml.RuntimeYamlException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.kiwiproject.changelog.config.external.ExternalChangelogConfig
import org.kiwiproject.json.LoggingDeserializationProblemHandler
import org.kiwiproject.yaml.YamlHelper
import java.io.File

private val LOG = KotlinLogging.logger {}

private const val ASSORTED = "Assorted"
private const val CONFIG_FILE_NAME = ".kiwi-changelog.yml"

internal object ConfigHelpers {

    fun mappingsToMap(mappings: List<String>): Map<String, String> {
        return mappings
            .asSequence()
            .map { opt -> opt.split(":") }
            .associate { parts -> parts[0] to parts[1] }
    }

    fun externalConfig(
        currentDirectory: File,
        userHomeDirectory: File,
        configFile: String?,
        ignoreConfigFiles: Boolean
    ): ExternalChangelogConfig {
        val externalConfigYaml = externalConfigYaml(
            currentDirectory,
            userHomeDirectory,
            configFile,
            ignoreConfigFiles
        )
        return externalConfig(externalConfigYaml)
    }

    private fun externalConfigYaml(
        currentDirectory: File,
        userHomeDirectory: File,
        configFile: String?,
        ignoreConfigFiles: Boolean
    ): String? {
        val configYaml: String? = configFile?.let { File(it).readText() }
        if (configYaml != null) {
            return configYaml
        }

        if (ignoreConfigFiles) {
            LOG.debug { "No explicit config file, and option to ignore standard config file locations was specified" }
            return null
        }

        val wellKnownConfig1 = File(currentDirectory, CONFIG_FILE_NAME)
        val wellKnownConfig2 = File(currentDirectory.parent, CONFIG_FILE_NAME)
        val wellKnownConfig3 = File(userHomeDirectory, CONFIG_FILE_NAME)

        LOG.debug {
            "Checking for external configuration in locations: $wellKnownConfig1, $wellKnownConfig2, $wellKnownConfig3"
        }

        return listOf(wellKnownConfig1, wellKnownConfig2, wellKnownConfig3)
            .firstOrNull { it.exists() }
            ?.readText()
    }

    fun externalConfig(externalConfigYaml: String?): ExternalChangelogConfig {
        return when (externalConfigYaml) {
            null -> ExternalChangelogConfig()
            else -> {
                val yamlFactory = YAMLFactory().disable(YAMLGenerator.Feature.SPLIT_LINES)
                val mapper = ObjectMapper(yamlFactory)
                val collectingHandler = CollectingDeserializationProblemHandler()
                // Jackson prepends each handler to an internal linked list, so the last handler
                // added is the first one called. CollectingDeserializationProblemHandler must be
                // called first (returns false to continue the chain) so that
                // LoggingDeserializationProblemHandler is also called (returns true to handle it).
                mapper.addHandler(LoggingDeserializationProblemHandler())
                mapper.addHandler(collectingHandler)
                try {
                    val config = YamlHelper(mapper).toObject(externalConfigYaml, ExternalChangelogConfig::class.java)
                    collectingHandler.problems.forEach { problem -> println("⚠️  $problem") }
                    config
                } catch (e: RuntimeYamlException) {
                    val cause = e.cause
                    if (cause is InvalidFormatException) {
                        println("⚠️  ${invalidFormatMessage(cause)}")
                    }
                    throw e
                }
            }
        }
    }

    @VisibleForTesting
    internal fun invalidFormatMessage(cause: InvalidFormatException): String {
        val location = if (cause.location != null) " (line ${cause.location.lineNr}, column ${cause.location.columnNr})" else ""
        return "Invalid value in configuration file$location: ${cause.originalMessage}"
    }

    fun buildCategoryConfig(
        labelToCategoryMappings: List<String>,
        categoryToEmojiMappings: List<String>,
        categoryOrder: List<String>,
        defaultCategory: String?,
        externalConfig: ExternalChangelogConfig
    ): CategoryConfig {
        // Merge label-to-category CLI arguments and external configuration, preferring CLI arguments
        val cliLabelToCategoryMappings = mappingsToMap(labelToCategoryMappings)
        val mergedLabelToCategoryMappings = externalConfig.labelCategoryMap() + cliLabelToCategoryMappings

        // Merge category-to-emoji CLI arguments and external configuration, preferring CLI arguments
        val cliCategoryToEmojiMappings = mappingsToMap(categoryToEmojiMappings)
        val mergedCategoryToEmojiMappings = externalConfig.categoryEmojiMap() + cliCategoryToEmojiMappings

        // Compute final order for categories. CLI order takes precedence.
        val finalCategoryOrder = LinkedHashSet(categoryOrder.toList() + externalConfig.categoryOrder()).toList()

        // Get the default category, preferring CLI to external config, and falling back if neither provided
        val finalDefaultCategory = defaultCategory ?: externalConfig.defaultCategory() ?: ASSORTED

        return CategoryConfig(
            finalDefaultCategory,
            mergedLabelToCategoryMappings,
            finalCategoryOrder,
            mergedCategoryToEmojiMappings
        )
    }
}

private class CollectingDeserializationProblemHandler : DeserializationProblemHandler() {

    val problems = mutableListOf<String>()

    override fun handleUnknownProperty(
        ctxt: DeserializationContext,
        p: JsonParser,
        deserializer: JsonDeserializer<*>,
        beanOrClass: Any,
        propertyName: String
    ): Boolean {
        problems.add("Unknown property '$propertyName' in configuration file")
        return false  // let LoggingDeserializationProblemHandler handle it next
    }
}
