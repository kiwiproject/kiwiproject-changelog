package org.kiwiproject.changelog.extension

fun <K, V> Map<K, V>.doesNotContainKey(key: K): Boolean = !containsKey(key)
