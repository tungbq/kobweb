package com.varabyte.kobweb.cli.create.freemarker

import com.varabyte.kobweb.cli.common.KobwebException
import com.varabyte.kobweb.cli.common.processing
import com.varabyte.kobweb.cli.common.queryUser
import com.varabyte.kobweb.cli.common.wildcardToRegex
import com.varabyte.kobweb.cli.create.Instruction
import com.varabyte.kobweb.cli.create.freemarker.methods.*
import com.varabyte.konsole.runtime.KonsoleApp
import freemarker.cache.NullCacheStorage
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import freemarker.template.TemplateMethodModelEx
import java.io.File
import java.io.FileWriter
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

private fun String.process(cfg: Configuration, model: Map<String, Any>): String {
    val reader = StringReader(this)
    val writer = StringWriter()
    Template("unused", reader, cfg).process(model, writer)
    return writer.buffer.toString()
}

class FreemarkerState(private val src: Path, private val dest: Path, projectFolder: String) {
    private val model = mutableMapOf<String, Any>(
        "projectFolder" to projectFolder,

        "isNotEmpty" to IsNotEmptyMethod(),
        "isPackage" to IsPackageMethod(),

        "fileToName" to FileToNameMethod(),
        "fileToPackage" to FileToPackageMethod(),
        "packageToPath" to PackageToPathMethod(),
    )

    // See also: https://freemarker.apache.org/docs/pgui_quickstart_all.html
    private val cfg = Configuration(Configuration.VERSION_2_3_31).apply {
        setDirectoryForTemplateLoading(src.toFile())
        // Kobweb doesn't serve templates - it just runs through files once. No need to cache.
        cacheStorage = NullCacheStorage()
        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
        wrapUncheckedExceptions = true
        fallbackOnNullLoopVariable = false
    }

    fun execute(app: KonsoleApp, instructions: List<Instruction>) {
        app.apply {
            for (inst in instructions) {
                val useInstruction = inst.condition?.process(cfg, model)?.toBoolean() ?: true
                if (!useInstruction) continue

                when (inst) {
                    is Instruction.QueryVar -> {
                        val default = inst.default?.process(cfg, model)
                        val answer = queryUser(inst.prompt, default, validateAnswer = { value ->
                            (model[inst.validation] as? TemplateMethodModelEx)?.exec(listOf(value))?.toString()
                        })
                        model[inst.name] = answer
                    }

                    is Instruction.DefineVar -> {
                        model[inst.name] = inst.value.process(cfg, model)
                    }

                    is Instruction.ProcessFreemarker -> {
                        processing("Processing templates") {
                            val srcFile = src.toFile()
                            val filesToProcess = mutableListOf<File>()
                            srcFile.walkBottomUp().forEach { file ->
                                if (file.extension == "ftl") {
                                    filesToProcess.add(file)
                                }
                            }
                            filesToProcess.forEach { templateFile ->
                                val template = cfg.getTemplate(templateFile.toRelativeString(srcFile))
                                FileWriter(templateFile.path.removeSuffix(".ftl")).use { writer ->
                                    template.process(model, writer)
                                }
                                templateFile.delete()
                            }
                        }
                    }

                    is Instruction.Move -> {
                        val to = inst.to.process(cfg, model)
                        processing(inst.description ?: "Moving \"${inst.from}\" to \"$to\"") {
                            val matcher = inst.from.wildcardToRegex()
                            val srcFile = src.toFile()
                            val filesToMove = mutableListOf<File>()
                            srcFile.walkBottomUp().forEach { file ->
                                if (matcher.matches(file.toRelativeString(srcFile))) {
                                    filesToMove.add(file)
                                }
                            }
                            val destRoot = src.resolve(to)
                            if (destRoot.isRegularFile()) {
                                throw KobwebException("Cannot move files into target that isn't a directory")
                            }
                            filesToMove.forEach { fileToMove ->
                                val subPath = fileToMove.parentFile.toRelativeString(srcFile)
                                val destPath = destRoot.resolve(subPath)
                                if (destPath.notExists()) {
                                    destPath.createDirectories()
                                }

                                Files.move(fileToMove.toPath(), destPath.resolve(fileToMove.name))
                            }
                        }
                    }

                    is Instruction.Keep -> {
                        processing(inst.description ?: "Populating to final project") {
                            val keepMatcher = inst.files.wildcardToRegex()
                            val excludeMatcher = inst.exclude?.wildcardToRegex()

                            val srcFile = src.toFile()
                            val filesToKeep = mutableListOf<File>()
                            srcFile.walkBottomUp().forEach { file ->
                                if (file.isFile) {
                                    val relativePath = file.toRelativeString(srcFile)
                                    if (keepMatcher.matches(relativePath)) {
                                        if (excludeMatcher == null || !excludeMatcher.matches(relativePath)) {
                                            filesToKeep.add(file)
                                        }
                                    }
                                }
                            }
                            filesToKeep.forEach { fileToKeep ->
                                val subPath = fileToKeep.parentFile.toRelativeString(srcFile)
                                val destPath = dest.resolve(subPath)
                                if (destPath.notExists()) {
                                    destPath.createDirectories()
                                }

                                Files.copy(fileToKeep.toPath(), destPath.resolve(fileToKeep.name))
                            }
                        }
                    }
                }
            }
        }
    }
}