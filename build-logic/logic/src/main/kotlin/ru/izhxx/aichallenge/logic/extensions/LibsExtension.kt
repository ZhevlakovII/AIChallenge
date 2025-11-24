package ru.izhxx.aichallenge.logic.extensions

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

/**
 * Получает доступ к Version Catalog.
 * @return VersionCatalog с идентификатором "libs"
 */
val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()