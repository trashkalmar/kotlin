/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.optimization.boxing.isPrimitiveBoxing
import org.jetbrains.kotlin.codegen.optimization.boxing.isPrimitiveUnboxing
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.removeAll
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodNode

object RedundantUnboxingEliminationMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        // move boxings closer to the primitives or unboxings
        val unboxings = methodNode.instructions.asSequence().filter { it.isPrimitiveUnboxing() }.toList()
        if (unboxings.isEmpty()) return
        val boxings = findSuccessors(methodNode, unboxings)
            .filter { (_, succs) -> succs.all { it.isPrimitiveBoxing() } }
            .values.flatten().toSet()
        val boxingSources = findSourceInstructions(internalClassName, methodNode, boxings, ignoreCopy = false)

        val movableBoxings = boxings.filter { boxing -> boxingSources[boxing]?.all { it != boxing.previous } ?: false }
        for (boxing in movableBoxings) {
            val sources = boxingSources[boxing] ?: continue
            for (source in sources) {
                methodNode.instructions.insert(source, boxing.clone())
            }
            methodNode.instructions.remove(boxing)
        }

        // remove unboxings immediately followed by boxings
        val toRemove = methodNode.instructions.asSequence()
            .filter { it.isPrimitiveUnboxing() && it.previous.opcode == Opcodes.CHECKCAST && it.next.isPrimitiveBoxing() }
            .flatMap { sequenceOf(it.previous, it, it.next) }.toList()

        methodNode.instructions.removeAll(toRemove)
    }
}