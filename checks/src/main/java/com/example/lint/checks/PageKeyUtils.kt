package com.example.lint.checks

import com.google.gson.JsonParser
import java.io.File
import java.io.IOException

object PageKeyUtils {
    fun writeMap(projectDir: File, key: String, value: String) {
        try {
            var f = getFile(projectDir)
            if (f == null) {
                f = File(projectDir.parentFile, PageKeyDetector.PAGE_KEY_FILE_NAME)
            }
            val s = "\"$key\":\"$value\",\n"
            f.appendText(s)
            println("writeMap: ${f.path} - $s")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun inflateEventIdJson(projectDir: File): Map<String, MutableList<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        val jsonObject = JsonParser.parseString(readJson(projectDir)).asJsonObject
        val set = jsonObject.entrySet()
        for (it in set) {
            if (it.key != null && it.value != null) {
                if (map[it.key] == null) {
                    map[it.key] = mutableListOf()
                }
                map[it.key]?.add(it.value.toString())
            }
        }
        return map
    }

    private fun readJson(projectDir: File): String {
        var builder = StringBuilder()
        builder.append("{")
        try {
            val f = getFile(projectDir)
            val bf = f?.bufferedReader()
            var line: String?
            while (bf?.readLine().also { line = it } != null) {
                builder.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        builder = StringBuilder(builder.removeSuffix(","))
        builder.append("}")
        return builder.toString()
    }

    private fun getFile(projectDir: File): File? {
        var f = File(projectDir.parentFile, PageKeyDetector.PAGE_KEY_FILE_NAME)
        println("getFile projectDir.parentFile: ${projectDir.parentFile.path}")
        if (f.exists()) {
            return f
        }
        f = File(projectDir, LogDetector.EVENT_ID_FILE_NAME)
        println("getFile projectDir: ${projectDir.path}")
        if (f.exists()) {
            return f
        }
        val codeQualifiedFile = findCodeQuality(projectDir) ?: return null
        println("getFile codeQualifiedFile: ${codeQualifiedFile.path}")
        f = File(codeQualifiedFile, LogDetector.EVENT_ID_FILE_NAME)
        if (f.exists()) {
            return f
        }
        return null
    }

    private fun findCodeQuality(projectDir: File): File? {
        if (projectDir.parent != null) {
            val parent = projectDir.parentFile
            val file = parent.listFiles()?.firstOrNull {
                it.name == ".codequality" && it.isDirectory
            }
            return file ?: findCodeQuality(parent)
        }
        return null
    }
}
