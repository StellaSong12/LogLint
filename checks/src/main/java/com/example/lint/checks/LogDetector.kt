package com.example.lint.checks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import java.util.*

/**
 * 检查埋点事件在 EVENT_ID_FILE_NAME 中是否有定义，没有则提示
 */
class LogDetector : Detector(), Detector.UastScanner {

    companion object {
        const val EVENT_ID_FILE_NAME = "log_event_id.json"
        const val EVENT_TRACKER_CLASS = "com.netease.gl.servicelog.interfaces.IEventTracker"
//        const val EVENT_TRACKER_CLASS = "com.android.example.EventTracker"

        val ISSUE = Issue.create(
            "event_name_issue",  //唯一 ID
            "埋点需要在 $EVENT_ID_FILE_NAME 文件声明行为",  //简单描述
            "埋点需要在 $EVENT_ID_FILE_NAME 文件声明行为",  //详细描述
            Category.CORRECTNESS,  //问题种类（正确性、安全性等）
            6,
            Severity.WARNING,  //问题严重程度（忽略、警告、错误）
            Implementation(LogDetector::class.java, Scope.JAVA_FILE_SCOPE) //实现，包括处理实例和作用域
        )

        var map: MutableMap<String?, String?>? = mutableMapOf()
    }

    override fun beforeCheckRootProject(context: Context) {
        super.beforeCheckRootProject(context)
        map = EventIdGsonUtils.inflateEventIdJson(context.project.dir).toMutableMap()
        println("LogDetector beforeCheckEachProject map: $map")
    }

    override fun getApplicableUastTypes(): List<Class<out UElement?>> {
        val types: MutableList<Class<out UElement?>> = ArrayList()
        types.add(UCallExpression::class.java)
        return types
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("trackEventWithAttributes", "trackEvent", "trackEventForHttp")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val isMemberInClass = context.evaluator.isMemberInClass(method, EVENT_TRACKER_CLASS)
        val isMemberInSubClassOf = context.evaluator.isMemberInSubClassOf(method, EVENT_TRACKER_CLASS, true)
        if (isMemberInClass || isMemberInSubClassOf) {
            val obj = node.valueArguments[0]
            val eventId = when (obj) {
                is ULiteralExpression -> { // 直接传字符串
                    obj.value
                }
                is UQualifiedReferenceExpression -> { // 传引用，切换为小写比较
                    obj.selector.asRenderString().lowercase(Locale.ROOT)
                }
                else -> ""
            }
            if (map != null && map!![eventId] == null) {
                context.report(ISSUE, node, context.getLocation(node), "埋点需要在 $EVENT_ID_FILE_NAME 文件声明行为")
            }
        }
    }
}
