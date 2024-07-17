package org.kiwiproject.changelog.extension

fun <K, V> Map<K, V>.doesNotContainKey(key: K): Boolean = !containsKey(key)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.getMap(key: String) =
    this[key] as Map<String, Any>

@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.getListOfMaps(key: String) =
    this[key] as List<Map<String, Any>>

fun Map<String, Any>.getString(key: String) = this[key] as String

fun Map<String, Any>.getInt(key: String) = this[key] as Int
