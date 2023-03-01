package com.example.lint.checks

import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EventIdGsonUtils {

    fun inflateEventIdJson(projectDir: File): Map<String?, String?> {
        val gson = Gson()
        val mapType: Type = object : TypeToken<Map<String?, String?>?>() {}.type
        return gson.fromJson<Map<String?, String?>?>(readJson(projectDir), mapType)?.apply {  } ?: mutableMapOf()
    }

    private fun readJson(projectDir: File): String {
        val builder = StringBuilder()
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
        return builder.toString()
    }


    private fun getFile(projectDir: File): File? {
        var f = File(projectDir.parentFile, LogDetector.EVENT_ID_FILE_NAME)
        if (f.exists()) {
            return f
        }
        f = File(projectDir, LogDetector.EVENT_ID_FILE_NAME)
        if (f.exists()) {
            return f
        }
        val codeQualifiedFile = findCodeQuality(projectDir) ?: return null
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