package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.SupabaseGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Blockquote(val lines: List<String>) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class BulletList(val items: List<BulletItem>) : MarkdownBlock()
    data class NumberedList(val items: List<NumberedItem>) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

data class BulletItem(val level: Int, val text: String)
data class NumberedItem(val number: String, val text: String)

fun parseMarkdownToBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Fenced Code Block
        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            val fence = if (trimmed.startsWith("```")) "```" else "~~~"
            val language = trimmed.removePrefix(fence).trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith(fence)) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size && lines[i].trim().startsWith(fence)) {
                i++ // consume closing fence
            }
            blocks.add(MarkdownBlock.CodeBlock(language = language, code = codeLines.joinToString("\n")))
            continue
        }

        // 2. Horizontal Rule (---, ***, ___)
        if (trimmed == "---" || trimmed == "***" || trimmed == "___" ||
            trimmed.matches(Regex("""^(\-{3,}|\*{3,}|_{3,})$"""))
        ) {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        // 3. Headers (# H1, ## H2, etc.)
        val headerMatch = Regex("""^(#{1,6})\s+(.*)$""").find(trimmed)
        if (headerMatch != null) {
            val level = headerMatch.groupValues[1].length
            val text = headerMatch.groupValues[2].trim()
            blocks.add(MarkdownBlock.Header(level = level, text = text))
            i++
            continue
        }

        // 4. Blockquotes (> ...)
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                val qLine = lines[i].trim().removePrefix(">").trimStart()
                quoteLines.add(qLine)
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(lines = quoteLines))
            continue
        }

        // 5. Table (| Col 1 | Col 2 |)
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                tableLines.add(lines[i].trim())
                i++
            }
            if (tableLines.size >= 2) {
                val rawHeaders = tableLines[0].trim().removeSurrounding("|").split('|').map { it.trim() }
                val dataRows = mutableListOf<List<String>>()

                for (rowIdx in 1 until tableLines.size) {
                    val rawRow = tableLines[rowIdx].trim().removeSurrounding("|").split('|').map { it.trim() }
                    val isSeparator = rawRow.all { it.matches(Regex("""^:?-+:?$""")) }
                    if (!isSeparator && rawRow.isNotEmpty()) {
                        dataRows.add(rawRow)
                    }
                }
                blocks.add(MarkdownBlock.Table(headers = rawHeaders, rows = dataRows))
                continue
            } else {
                blocks.add(MarkdownBlock.Paragraph(text = tableLines.joinToString("\n")))
                continue
            }
        }

        // 6. Bullet Lists (* , - , + , • )
        val bulletMatch = Regex("""^(\s*)([\*\-\+•])\s+(.*)$""").find(line)
        if (bulletMatch != null) {
            val listItems = mutableListOf<BulletItem>()
            while (i < lines.size) {
                val bMatch = Regex("""^(\s*)([\*\-\+•])\s+(.*)$""").find(lines[i])
                if (bMatch != null) {
                    val indentSpaces = bMatch.groupValues[1].length
                    val level = (indentSpaces / 2).coerceIn(0, 4)
                    val itemText = bMatch.groupValues[3]
                    listItems.add(BulletItem(level = level, text = itemText))
                    i++
                } else if (lines[i].isBlank()) {
                    if (i + 1 < lines.size && Regex("""^(\s*)([\*\-\+•])\s+(.*)$""").matches(lines[i + 1])) {
                        i++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            blocks.add(MarkdownBlock.BulletList(items = listItems))
            continue
        }

        // 7. Numbered Lists (1. , 2. )
        val numberMatch = Regex("""^(\s*)(\d+)[\.\)]\s+(.*)$""").find(line)
        if (numberMatch != null) {
            val numItems = mutableListOf<NumberedItem>()
            while (i < lines.size) {
                val nMatch = Regex("""^(\s*)(\d+)[\.\)]\s+(.*)$""").find(lines[i])
                if (nMatch != null) {
                    val num = nMatch.groupValues[2]
                    val itemText = nMatch.groupValues[3]
                    numItems.add(NumberedItem(number = num, text = itemText))
                    i++
                } else if (lines[i].isBlank()) {
                    if (i + 1 < lines.size && Regex("""^(\s*)(\d+)[\.\)]\s+(.*)$""").matches(lines[i + 1])) {
                        i++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            blocks.add(MarkdownBlock.NumberedList(items = numItems))
            continue
        }

        // 8. Blank line
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 9. Regular Paragraph
        val paraLines = mutableListOf<String>()
        while (i < lines.size) {
            val currentLine = lines[i]
            val currentTrimmed = currentLine.trim()
            if (currentTrimmed.isEmpty() ||
                currentTrimmed.startsWith("```") ||
                currentTrimmed.startsWith("~~~") ||
                currentTrimmed == "---" || currentTrimmed == "***" || currentTrimmed == "___" ||
                Regex("""^(#{1,6})\s+""").matches(currentTrimmed) ||
                currentTrimmed.startsWith(">") ||
                (currentTrimmed.startsWith("|") && currentTrimmed.endsWith("|")) ||
                Regex("""^(\s*)([\*\-\+•])\s+""").matches(currentLine) ||
                Regex("""^(\s*)(\d+)[\.\)]\s+""").matches(currentLine)
            ) {
                break
            }
            paraLines.add(currentLine)
            i++
        }
        if (paraLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(text = paraLines.joinToString("\n")))
        }
    }

    return blocks
}

fun buildMarkdownAnnotatedString(
    raw: String,
    baseFontSize: TextUnit = 13.sp,
    baseColor: Color = TextPrimaryDark,
    accentColor: Color = SupabaseGreen,
    isItalicDefault: Boolean = false
): AnnotatedString {
    // Regex matching markdown inline elements:
    // 1: `code`
    // 3: [text](url)
    // 6: ~~strike~~
    // 8: ***bold-italic***, 10: ___bold-italic___
    // 12: **bold**, 14: __bold__
    // 16: *italic*, 18: _italic_
    val inlineRegex = Regex(
        """(`([^`]+)`)|(\[([^\]]+)\]\(([^)]+)\))|(~~([^~]+)~~)|(\*\*\*([^*]+)\*\*\*)|(_{3}([^_]+)_{3})|(\*\*([^*]+)\*\*)|(_{2}([^_]+)_{2})|(\*([^*]+)\*)|(_([^_]+)_)"""
    )

    return buildAnnotatedString {
        var currentIndex = 0
        val matches = inlineRegex.findAll(raw)

        for (match in matches) {
            if (match.range.first > currentIndex) {
                append(raw.substring(currentIndex, match.range.first))
            }

            val groupValues = match.groupValues
            when {
                // Inline code: group 1 & group 2
                groupValues[1].isNotEmpty() -> {
                    val codeContent = groupValues[2]
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = CharcoalSurfaceElevated,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (baseFontSize.value * 0.92f).sp
                        )
                    ) {
                        append(" $codeContent ")
                    }
                }

                // Link: group 3 (text=4, url=5)
                groupValues[3].isNotEmpty() -> {
                    val linkText = groupValues[4]
                    val linkUrl = groupValues[5]
                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF38BDF8),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(linkText)
                    }
                    pop()
                }

                // Strikethrough: group 6 & group 7
                groupValues[6].isNotEmpty() -> {
                    val strikeContent = groupValues[7]
                    withStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = TextMutedDark
                        )
                    ) {
                        append(strikeContent)
                    }
                }

                // Bold Italic: group 8 or 10
                groupValues[8].isNotEmpty() || groupValues[10].isNotEmpty() -> {
                    val biContent = if (groupValues[8].isNotEmpty()) groupValues[9] else groupValues[11]
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = baseColor
                        )
                    ) {
                        append(biContent)
                    }
                }

                // Bold: group 12 or 14
                groupValues[12].isNotEmpty() || groupValues[14].isNotEmpty() -> {
                    val boldContent = if (groupValues[12].isNotEmpty()) groupValues[13] else groupValues[15]
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = baseColor
                        )
                    ) {
                        append(boldContent)
                    }
                }

                // Italic: group 16 or 18
                groupValues[16].isNotEmpty() || groupValues[18].isNotEmpty() -> {
                    val italicContent = if (groupValues[16].isNotEmpty()) groupValues[17] else groupValues[19]
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (baseColor == TextPrimaryDark) TextSecondaryDark else baseColor
                        )
                    ) {
                        append(italicContent)
                    }
                }
            }

            currentIndex = match.range.last + 1
        }

        if (currentIndex < raw.length) {
            append(raw.substring(currentIndex))
        }

        if (isItalicDefault && this.length > 0) {
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), 0, this.length)
        }
    }
}

@Composable
fun MarkdownRichText(
    annotated: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 19.sp,
    color: Color = TextPrimaryDark
) {
    val hasLinks = remember(annotated) {
        annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty()
    }
    val uriHandler = LocalUriHandler.current

    if (hasLinks) {
        ClickableText(
            text = annotated,
            modifier = modifier,
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = color
            ),
            onClick = { offset ->
                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { link ->
                        try {
                            uriHandler.openUri(link.item)
                        } catch (_: Exception) {}
                    }
            }
        )
    } else {
        Text(
            text = annotated,
            modifier = modifier,
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = color
        )
    }
}

@Composable
fun MarkdownHeader(header: MarkdownBlock.Header) {
    val (fontSize, topPadding) = when (header.level) {
        1 -> Pair(17.sp, 8.dp)
        2 -> Pair(15.5.sp, 6.dp)
        3 -> Pair(14.sp, 5.dp)
        4 -> Pair(13.sp, 4.dp)
        else -> Pair(12.5.sp, 3.dp)
    }

    Spacer(modifier = Modifier.height(topPadding))
    Text(
        text = header.text,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = TextPrimaryDark,
        lineHeight = (fontSize.value * 1.3f).sp
    )
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
fun MarkdownParagraph(text: String, isUser: Boolean = false) {
    val annotated = buildMarkdownAnnotatedString(
        raw = text,
        baseFontSize = 13.sp,
        baseColor = TextPrimaryDark,
        accentColor = if (isUser) SupabaseGreen else Color(0xFF38BDF8)
    )
    MarkdownRichText(
        annotated = annotated,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = TextPrimaryDark
    )
}

@Composable
fun MarkdownBlockquote(lines: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(CharcoalSurfaceElevated.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(Color(0xFF38BDF8), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                lines.forEach { line ->
                    val annotated = buildMarkdownAnnotatedString(
                        raw = line,
                        baseFontSize = 12.5.sp,
                        baseColor = TextSecondaryDark,
                        isItalicDefault = true
                    )
                    MarkdownRichText(
                        annotated = annotated,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownCodeBlock(codeBlock: MarkdownBlock.CodeBlock) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1115)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16181D))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = codeBlock.language.ifBlank { "code" }.lowercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextMutedDark,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", codeBlock.code))
                            Toast.makeText(context, "Kode disalin!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Salin",
                        tint = SupabaseGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Salin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SupabaseGreen
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(
                    text = codeBlock.code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun MarkdownBulletList(items: List<BulletItem>, isUser: Boolean = false) {
    Column(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (item.level * 12).dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) SupabaseGreen else Color(0xFF38BDF8),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    val annotated = buildMarkdownAnnotatedString(
                        raw = item.text,
                        baseFontSize = 13.sp,
                        baseColor = TextPrimaryDark,
                        accentColor = if (isUser) SupabaseGreen else Color(0xFF38BDF8)
                    )
                    MarkdownRichText(
                        annotated = annotated,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextPrimaryDark
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownNumberedList(items: List<NumberedItem>, isUser: Boolean = false) {
    Column(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${item.number}.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) SupabaseGreen else Color(0xFF38BDF8),
                    modifier = Modifier.width(20.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    val annotated = buildMarkdownAnnotatedString(
                        raw = item.text,
                        baseFontSize = 13.sp,
                        baseColor = TextPrimaryDark,
                        accentColor = if (isUser) SupabaseGreen else Color(0xFF38BDF8)
                    )
                    MarkdownRichText(
                        annotated = annotated,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextPrimaryDark
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownTable(table: MarkdownBlock.Table) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurfaceElevated),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .background(CharcoalBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    table.headers.forEach { h ->
                        Text(
                            text = h,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen,
                            modifier = Modifier.widthIn(min = 65.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = CharcoalBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                table.rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (index % 2 == 1) CharcoalBg.copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { cell ->
                            val annotated = buildMarkdownAnnotatedString(
                                raw = cell,
                                baseFontSize = 12.sp,
                                baseColor = TextPrimaryDark
                            )
                            MarkdownRichText(
                                annotated = annotated,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = TextPrimaryDark,
                                modifier = Modifier.widthIn(min = 65.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownHorizontalRule() {
    HorizontalDivider(
        color = CharcoalBorder,
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    isUser: Boolean = false
) {
    val blocks = remember(markdown) {
        parseMarkdownToBlocks(markdown)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> MarkdownHeader(block)
                is MarkdownBlock.Paragraph -> MarkdownParagraph(block.text, isUser = isUser)
                is MarkdownBlock.BulletList -> MarkdownBulletList(block.items, isUser = isUser)
                is MarkdownBlock.NumberedList -> MarkdownNumberedList(block.items, isUser = isUser)
                is MarkdownBlock.Blockquote -> MarkdownBlockquote(block.lines)
                is MarkdownBlock.CodeBlock -> MarkdownCodeBlock(block)
                is MarkdownBlock.Table -> MarkdownTable(block)
                is MarkdownBlock.HorizontalRule -> MarkdownHorizontalRule()
            }
        }
    }
}
