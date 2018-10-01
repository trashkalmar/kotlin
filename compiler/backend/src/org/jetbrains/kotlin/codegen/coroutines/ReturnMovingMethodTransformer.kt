/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal class ReturnMovingMethodTransformer(private val suspensionPoints: List<SuspensionPoint>) : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val areturns = methodNode.instructions.asSequence().filter { it.opcode == Opcodes.ARETURN }.toList()
        val sources = findSourceInstructions(internalClassName, methodNode, areturns, ignoreCopy = false)
            .filter { it.value.size > 1 }.values.flatten().toSet().filter { insn -> suspensionPoints.none { insn in it } }
        for (source in sources) {
            methodNode.instructions.insert(source, InsnNode(Opcodes.ARETURN))
        }
    }
}

private operator fun SuspensionPoint.contains(insn: AbstractInsnNode): Boolean {
    var current = suspensionCallBegin
    while (current != suspensionCallEnd) {
        if (current == insn) {
            return true
        }
        current = current.next
    }
    return false
}