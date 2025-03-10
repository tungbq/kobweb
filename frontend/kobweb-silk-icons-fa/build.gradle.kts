import com.varabyte.kobweb.gradle.publish.FILTER_OUT_MULTIPLATFORM_PUBLICATIONS
import com.varabyte.kobweb.gradle.publish.set

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    id("com.varabyte.kobweb.internal.publish")
}

group = "com.varabyte.kobweb"
version = libs.versions.kobweb.libs.get()

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    @Suppress("UNUSED_VARIABLE") // Suppress spurious warnings about sourceset variables not being used
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
                implementation(compose.runtime)

                api(project(":frontend:kobweb-compose"))
            }
        }
    }
}

kobwebPublication {
    artifactId.set("kobweb-silk-icons-fa")
    description.set("A collection of Kobweb Silk components that directly wrap Font Awesome icons")
    filter.set(FILTER_OUT_MULTIPLATFORM_PUBLICATIONS)
}

enum class IconCategory {
    SOLID,
    REGULAR,
    BRAND,
}

val regenerateIconsTask = tasks.register("regenerateIcons") {
    val srcFile = layout.projectDirectory.file("fa-icon-list.txt")
    val dstFile =
        layout.projectDirectory.file("src/jsMain/kotlin/com/varabyte/kobweb/silk/components/icons/fa/FaIcons.kt")

    inputs.files(srcFile, layout.projectDirectory.file("build.gradle.kts"))
    outputs.file(dstFile)

    // {SOLID=[ad, address-book, address-card, ...], REGULAR=[address-book, address-card, angry, ...], ... }
    val iconRawNames = srcFile.asFile
        .readLines().asSequence()
        .filter { line -> !line.startsWith("#") }
        .map { line ->
            // Convert icon name to function name, e.g.
            // align-left -> FaAlignLeft
            line.split("=", limit = 2).let { parts ->
                val category = when (parts[0]) {
                    "fas" -> IconCategory.SOLID
                    "far" -> IconCategory.REGULAR
                    "fab" -> IconCategory.BRAND
                    else -> throw GradleException("Unexpected category string: ${parts[0]}")
                }
                val names = parts[1]

                category to names.split(",")
            }
        }
        .toMap()

    // For each icon name, figure out what categories they are in. This will affect the function signature we generate.
    // {ad=[SOLID], address-book=[SOLID, REGULAR], address-card=[SOLID, REGULAR], ...
    val iconCategories = mutableMapOf<String, MutableSet<IconCategory>>()
    iconRawNames.forEach { entry ->
        val category = entry.key
        entry.value.forEach { rawName ->
            iconCategories.computeIfAbsent(rawName, { mutableSetOf() }).add(category)
        }
    }

    // Sanity check results
    iconCategories
        .filterNot { entry ->
            val categories = entry.value
            categories.size == 1 ||
                (categories.size == 2 && categories.contains(IconCategory.SOLID) && categories.contains(IconCategory.REGULAR))
        }
        .let { invalidGroupings ->
            if (invalidGroupings.isNotEmpty()) {
                throw GradleException("Found unexpected groupings. An icon should only be in its own category OR it can have solid and regular versions: $invalidGroupings")
            }
        }

    // Generate four types of functions: solid only, regular only, solid or regular, and brand
    val iconMethodEntries = iconCategories
        .map { entry ->
            val rawName = entry.key
            // Convert e.g. "align-items" to "FaAlignItems"
            @Suppress("DEPRECATION") // capitalize is way more readable than a direct replacement
            val methodName = "Fa${rawName.split("-").joinToString("") { it.capitalize() }}"
            val categories = entry.value

            when {
                categories.size == 2 -> {
                    "@Composable fun $methodName(modifier: Modifier = Modifier, style: IconStyle = IconStyle.OUTLINE, size: IconSize? = null) = FaIcon(\"$rawName\", modifier, style.category, size)"
                }

                categories.contains(IconCategory.SOLID) -> {
                    "@Composable fun $methodName(modifier: Modifier = Modifier, size: IconSize? = null) = FaIcon(\"$rawName\", modifier, IconCategory.SOLID, size)"
                }

                categories.contains(IconCategory.REGULAR) -> {
                    "@Composable fun $methodName(modifier: Modifier = Modifier, size: IconSize? = null) = FaIcon(\"$rawName\", modifier, IconCategory.REGULAR, size)"
                }

                categories.contains(IconCategory.BRAND) -> {
                    "@Composable fun $methodName(modifier: Modifier = Modifier, size: IconSize? = null) = FaIcon(\"$rawName\", modifier, IconCategory.BRAND, size)"
                }

                else -> GradleException("Unhandled icon entry: $entry")
            }
        }

    val iconsCode = """
//@formatter:off
@file:Suppress("unused", "SpellCheckingInspection")

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// THIS FILE IS AUTOGENERATED.
//
// Do not edit this file by hand. Instead, update `fa-icon-list.txt` in the module root and run the Gradle
// task "regenerateIcons"
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

package com.varabyte.kobweb.silk.components.icons.fa

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.toAttrs
import org.jetbrains.compose.web.dom.Span

enum class IconCategory(internal val className: String) {
    REGULAR("far"),
    SOLID("fas"),
    BRAND("fab");
}

enum class IconStyle(internal val category: IconCategory) {
    FILLED(IconCategory.SOLID),
    OUTLINE(IconCategory.REGULAR);
}

// See: https://fontawesome.com/docs/web/style/size
enum class IconSize(internal val className: String) {
    // Relative sizes
    XXS("fa-2xs"),
    XS("fa-xs"),
    SM("fa-sm"),
    LG("fa-lg"),
    XL("fa-xl"),
    XXL("fa-2xl"),

    // Literal sizes
    X1("fa-1x"),
    X2("fa-2x"),
    X3("fa-3x"),
    X4("fa-4x"),
    X5("fa-5x"),
    X6("fa-6x"),
    X7("fa-7x"),
    X8("fa-8x"),
    X9("fa-9x"),
    X10("fa-10x");
}

@Composable
fun FaIcon(
    name: String,
    modifier: Modifier,
    style: IconCategory = IconCategory.REGULAR,
    size: IconSize? = null,
) {
    Span(
        attrs = modifier.toAttrs {
            classes(style.className, "fa-${'$'}name")
            if (size != null) {
                classes(size.className)
            }
        }
    )
}

${iconMethodEntries.joinToString("\n")}
    """.trimIndent()

    dstFile.asFile.writeText(iconsCode)
}

tasks.named("compileKotlinJs") {
    dependsOn(regenerateIconsTask)
}

tasks.named("sourcesJar") {
    dependsOn(regenerateIconsTask)
}

tasks.named("jsSourcesJar") {
    dependsOn(regenerateIconsTask)
}
