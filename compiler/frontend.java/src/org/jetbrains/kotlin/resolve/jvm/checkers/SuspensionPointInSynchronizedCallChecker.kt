/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.isTopLevelInPackage
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.checkers.findEnclosingSuspendFunction
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.source.getPsi

class SuspensionPointInSynchronizedCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor
        if (descriptor !is FunctionDescriptor || !descriptor.isSuspend) return

        val enclosingSuspendFunction = findEnclosingSuspendFunction(context) ?: return
        val enclosingSuspendFunctionSource = enclosingSuspendFunction.source.getPsi() ?: return

        // Search for `synchronized` call
        var psi = reportOn
        var insideLambda = false
        while (psi != enclosingSuspendFunctionSource) {
            if (psi is KtCallExpression) {
                val call = context.trace[BindingContext.CALL, psi.calleeExpression] ?: continue
                val resolved = context.trace[BindingContext.RESOLVED_CALL, call] ?: continue
                if (resolved.resultingDescriptor.isTopLevelInPackage("synchronized", "kotlin") && insideLambda) {
                    context.trace.report(ErrorsJvm.SUSPENSION_POINT_INSIDE_SYNCHRONIZED.on(reportOn, resolvedCall.resultingDescriptor))
                    break
                }
            }
            if (psi is KtLambdaExpression) {
                insideLambda = true
            }
            psi = psi.parent
        }
    }
}