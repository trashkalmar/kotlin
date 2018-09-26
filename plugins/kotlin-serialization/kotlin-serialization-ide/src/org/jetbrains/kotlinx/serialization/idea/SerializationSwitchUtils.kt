/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.PlatformModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtElement

fun <T> getIfEnabledOn(clazz: ClassDescriptor, body: () -> T): T? {
    val sourceElement: KtElement = (clazz.findPsi() as? KtElement) ?: return null
    val module = sourceElement.getModuleInfo().findModule() ?: return null
    val facet = KotlinFacet.get(module) ?: return null
    val pluginClasspath = facet.configuration.settings.compilerArguments?.pluginClasspaths ?: return null
    if (pluginClasspath.none { it == KotlinSerializationImportHandler.PLUGIN_JPS_JAR }) return null
    return body()
}

fun runIfEnabledOn(clazz: ClassDescriptor, body: () -> Unit) { getIfEnabledOn<Unit>(clazz, body) }

private fun ModuleInfo.findModule(): Module? {
    return when (this) {
        is ModuleSourceInfo -> this
        is PlatformModuleInfo -> this.platformModule
        else -> null
    }?.module
}