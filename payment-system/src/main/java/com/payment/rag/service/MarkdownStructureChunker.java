package com.payment.rag.service;

import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownStructureChunker {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^\\d+\\.\\s+.+$");

    private final TextSplitterService textSplitterService;

    @Value("${chunking.structured.max-chars-per-chunk:1200}")
    private int maxCharsPerChunk;

    public MarkdownStructureChunker(TextSplitterService textSplitterService) {
        this.textSplitterService = textSplitterService;
    }

    public List<RagUnit> chunk(String markdown, String filename, SourceType sourceType) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }

        List<Block> blocks = extractBlocks(markdown);
        List<RagUnit> units = new ArrayList<>();
        int chunkIndex = 0;

        for (Block block : blocks) {
            List<String> chunkTexts = splitBlock(block);
            for (String chunkText : chunkTexts) {
                if (chunkText == null || chunkText.isBlank()) {
                    continue;
                }
                RagUnit unit = new RagUnit();
                unit.setSourceType(sourceType);
                unit.setTitle(block.headingPath().isBlank() ? filename : block.headingPath());
                unit.setContent(renderContent(block.headingPath(), chunkText));
                unit.setChunkIndex(chunkIndex++);
                units.add(unit);
            }
        }

        return units;
    }

    private List<Block> extractBlocks(String markdown) {
        List<Block> blocks = new ArrayList<>();
        List<String> headingStack = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        BlockType currentType = null;
        boolean inCodeBlock = false;

        for (String rawLine : markdown.replace("\r\n", "\n").split("\n", -1)) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();

            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                if (!inCodeBlock) {
                    flushBlock(blocks, current, currentType, currentHeadingPath(headingStack));
                    currentType = BlockType.CODE;
                    inCodeBlock = true;
                }
                current.append(line).append('\n');
                if (inCodeBlock && trimmed.length() >= 3 && (trimmed.startsWith("```") || trimmed.startsWith("~~~"))
                        && current.length() > trimmed.length() + 1) {
                    flushBlock(blocks, current, currentType, currentHeadingPath(headingStack));
                    currentType = null;
                    inCodeBlock = false;
                }
                continue;
            }

            if (inCodeBlock) {
                current.append(line).append('\n');
                continue;
            }

            Matcher headingMatcher = HEADING_PATTERN.matcher(trimmed);
            if (headingMatcher.matches()) {
                flushBlock(blocks, current, currentType, currentHeadingPath(headingStack));
                updateHeadingStack(headingStack, headingMatcher.group(2).trim(), headingMatcher.group(1).length());
                currentType = null;
                continue;
            }

            if (trimmed.isEmpty()) {
                flushBlock(blocks, current, currentType, currentHeadingPath(headingStack));
                currentType = null;
                continue;
            }

            BlockType lineType = detectBlockType(trimmed);
            if (currentType != null && currentType != lineType) {
                flushBlock(blocks, current, currentType, currentHeadingPath(headingStack));
            }

            if (currentType == null) {
                currentType = lineType;
            }

            current.append(line).append('\n');
        }

        flushBlock(blocks, current, currentType, currentHeadingPath(headingStack));
        return blocks;
    }

    private void flushBlock(List<Block> blocks, StringBuilder current, BlockType type, String headingPath) {
        if (current == null || current.isEmpty() || type == null) {
            current.setLength(0);
            return;
        }

        String text = current.toString().trim();
        current.setLength(0);
        if (text.isBlank()) {
            return;
        }

        blocks.add(new Block(type, headingPath, text));
    }

    private void updateHeadingStack(List<String> headingStack, String heading, int level) {
        while (headingStack.size() >= level) {
            headingStack.remove(headingStack.size() - 1);
        }
        headingStack.add(heading);
    }

    private String currentHeadingPath(List<String> headingStack) {
        return headingStack.stream()
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
    }

    private BlockType detectBlockType(String trimmedLine) {
        if (trimmedLine.startsWith("|") || trimmedLine.endsWith("|")) {
            return BlockType.TABLE;
        }
        if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("+ ")
                || ORDERED_LIST_PATTERN.matcher(trimmedLine).matches()) {
            return BlockType.LIST;
        }
        return BlockType.PARAGRAPH;
    }

    private List<String> splitBlock(Block block) {
        String normalized = normalizeWhitespace(block.text(), block.type());
        if (normalized.length() <= maxCharsPerChunk) {
            return List.of(normalized);
        }

        if (block.type() == BlockType.PARAGRAPH) {
            List<String> sentences = new ArrayList<>();
            for (String sentence : splitBySentence(normalized)) {
                if (sentence.length() > maxCharsPerChunk) {
                    sentences.addAll(textSplitterService.splitText(sentence));
                } else {
                    sentences.add(sentence);
                }
            }
            return sentences;
        }

        List<String> segments = switch (block.type()) {
            case LIST, TABLE -> splitByLines(normalized);
            case CODE -> textSplitterService.splitText(normalized);
            case PARAGRAPH -> List.of(normalized);
        };

        if (segments.isEmpty()) {
            return textSplitterService.splitText(normalized);
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String segment : segments) {
            if (segment.length() > maxCharsPerChunk) {
                if (!current.isEmpty()) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                }
                chunks.addAll(textSplitterService.splitText(segment));
                continue;
            }

            if (current.isEmpty()) {
                current.append(segment);
                continue;
            }

            if (current.length() + 1 + segment.length() > maxCharsPerChunk) {
                chunks.add(current.toString().trim());
                current.setLength(0);
                current.append(segment);
            } else {
                current.append(needsNewline(block.type()) ? '\n' : ' ').append(segment);
            }
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    private boolean needsNewline(BlockType type) {
        return type == BlockType.LIST || type == BlockType.TABLE || type == BlockType.CODE;
    }

    private List<String> splitByLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private List<String> splitBySentence(String text) {
        List<String> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.CHINA);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isBlank()) {
                sentences.add(sentence);
            }
        }
        if (sentences.isEmpty()) {
            sentences.add(text);
        }
        return sentences;
    }

    private String normalizeWhitespace(String text, BlockType type) {
        if (type == BlockType.CODE || type == BlockType.TABLE) {
            return text.trim();
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private String renderContent(String headingPath, String body) {
        if (headingPath == null || headingPath.isBlank()) {
            return body.trim();
        }
        return headingPath + "\n" + body.trim();
    }

    private enum BlockType {
        PARAGRAPH,
        LIST,
        TABLE,
        CODE
    }

    private record Block(BlockType type, String headingPath, String text) {
    }
}
