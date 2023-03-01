package com.example.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isConstructorCall
import java.io.File
import java.util.*


/**
 * 收集 PARAM_CLASS 构造函数中的id和当前类的映射，存入 PAGE_KEY_FILE_NAME 文件
 * 仅需要收集时使用，在项目根目录 gradle.properties 设置 lint.PageKeyDetector=true 时生效
 */
class PageKeyDetector: Detector(), Detector.UastScanner, SourceCodeScanner {

    companion object {
        const val PAGE_KEY_FILE_NAME = "log_page_key.json"
        const val PARAM_CLASS = "com.netease.gl.glbase.log.page.param.Param"
//        const val PARAM_CLASS = "com.android.example.Param"
        const val SWITCH_ON = "lint.PageKeyDetector=true"

        val ISSUE = Issue.create(
            "page_key_issue",  //唯一 ID
            "获取 page_key 对应的页面",  //简单描述
            "获取 page_key 对应的页面",  //详细描述
            Category.CORRECTNESS,  //问题种类（正确性、安全性等）
            6,
            Severity.WARNING,  //问题严重程度（忽略、警告、错误）
            Implementation(PageKeyDetector::class.java, Scope.JAVA_FILE_SCOPE) //实现，包括处理实例和作用域
        )

        var map: MutableMap<String, MutableList<String>> = mutableMapOf()
        var isSwitchOn = false
    }

    override fun beforeCheckRootProject(context: Context) {
        super.beforeCheckRootProject(context)
        isSwitchOn = initSwitch(context)
        println("PageKeyDetector beforeCheckEachProject isSwitchOn: $isSwitchOn")
        if (!isSwitchOn) {
            return
        }
        map = PageKeyUtils.inflateEventIdJson(context.project.dir).toMutableMap()
        println("PageKeyDetector beforeCheckEachProject map: $map")
    }

    private fun initSwitch(context: Context): Boolean {
        try {
            val path = context.mainProject.dir.parent + "/gradle.properties"
            val f = File(path)
            f.readLines().forEach {
                if (SWITCH_ON == it.replace(" ", "")) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun getApplicableUastTypes(): List<Class<out UElement?>> {
        val types: MutableList<Class<out UElement?>> = ArrayList()
        types.add(UCallExpression::class.java)
        return types
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (!isSwitchOn) {
                    return
                }
                if (!node.isConstructorCall()) {
                    return
                }
                println("PageKeyDetector createUastHandler")
                val classRef = node.classReference
                try {
                    if (classRef != null) {
                        val psiCls = PsiTypesUtil.getPsiClass(node.returnType)
                        if (context.evaluator.inheritsFrom(psiCls, PARAM_CLASS, true)) {
                            val pageKey = when (val obj = node.valueArguments[0]) {
                                is ULiteralExpression -> { // 直接传字符串
                                    obj.value.toString()
                                }
                                is UQualifiedReferenceExpression -> { // 传引用，取最后一个下划线右侧的内容
                                    obj.selector.asRenderString().split("_").last()
                                }
                                is USimpleNameReferenceExpression -> { // 传引用，取最后一个下划线右侧的内容
                                    obj.identifier.split("_").last()
                                }
                                else -> ""
                            }
                            if (map[pageKey] == null || map[pageKey]?.contains(context.file.nameWithoutExtension) != true) {
                                PageKeyUtils.writeMap(context.project.dir, pageKey, context.file.nameWithoutExtension)
                                if (map[pageKey] == null) {
                                    map[pageKey] = mutableListOf()
                                }
                                map[pageKey]!!.add(context.file.nameWithoutExtension)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}