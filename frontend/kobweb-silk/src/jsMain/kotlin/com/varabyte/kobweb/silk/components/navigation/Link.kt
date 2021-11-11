package com.varabyte.kobweb.silk.components.navigation

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.TextDecorationLine
import com.varabyte.kobweb.compose.css.textDecorationLine
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.asAttributeBuilder
import com.varabyte.kobweb.compose.ui.clickable
import com.varabyte.kobweb.compose.ui.color
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.style.ComponentStyle
import com.varabyte.kobweb.silk.components.style.ComponentVariant
import com.varabyte.kobweb.silk.components.style.toModifier
import com.varabyte.kobweb.silk.theme.SilkTheme
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text

val LinkStyle = ComponentStyle("silk-link") { colorMode ->
    base = Modifier.styleModifier { textDecorationLine(TextDecorationLine.None) }
    link = Modifier.color(SilkTheme.palettes[colorMode].link.default)
    hover = Modifier.styleModifier { textDecorationLine(TextDecorationLine.Underline) }
    visited = Modifier.color(SilkTheme.palettes[colorMode].link.visited)
}

val UndecoratedLinkVariant = LinkStyle.addVariant("undecorated") {
    hover = Modifier.styleModifier { textDecorationLine(TextDecorationLine.None) }
}

/**
 * Linkable text which, when clicked, navigates to the target [path].
 *
 * This composable is SilkTheme-aware, and if colors are not specified, will automatically use the current theme plus
 * color mode.
 */
@Composable
fun Link(
    path: String,
    text: String? = null,
    modifier: Modifier = Modifier,
    variant: ComponentVariant? = null
) {
    val ctx = rememberPageContext()

    A(
        href = path,
        attrs = LinkStyle.toModifier(variant)
            .then(modifier)
            .clickable { evt ->
                evt.preventDefault()
                ctx.router.navigateTo(path)
            }
            .asAttributeBuilder()
    ) {
        Text(text ?: path)
    }
}