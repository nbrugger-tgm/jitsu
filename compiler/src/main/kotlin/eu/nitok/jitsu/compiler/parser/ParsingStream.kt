package eu.nitok.jitsu.compiler.parser

import java.util.function.Predicate
import kotlin.contracts.contract


class ParsingStream(content: String) {
    private val content: String = if (System.lineSeparator() != "\n") {
        content.replace(System.lineSeparator(), "\n");
    } else {
        content;
    };
    private var index = 0;
    private var mark = -1;
    private var markLocation = Location(-1, -1);

    private var line = 1;
    private var column = 1;

    fun interface Classifier {
        operator fun invoke(c: Char): Boolean;
    }

    fun next(classifier: Classifier): String {
        val start = index;
        skip(classifier);
        return content.substring(start, index);
    }

    fun expect(c: Char): Boolean {
        if (index >= content.length) {
            return false;
        }
        if (content[index] != c) {
            return false;
        }
        index++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return true;
    }

    fun expectEOF() = index >= content.length;

    fun range(operation: ParsingStream.() -> Unit): Range {
        val start = location();
        this.operation();
        val end = location();
        return Range(start, end);
    }

    private fun location() = Location(line, column)

    fun expect(expected: String): Boolean {
        if (content.substring(index, index + expected.length) != expected) {
            return false;
        }
        index += expected.length;
        expected.count { it == '\n' }.let {
            line += it;
            column = if (it == 0) column + expected.length else expected.length - expected.lastIndexOf('\n');
        }
        return true;
    }

    fun skip(classifier: Classifier): Int {
        var skipped = 0;
        while (index < content.length) {
            val char = content[index];
            if (!classifier(char)) break
            index++;
            skipped++;
            if (char == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return skipped;
    }

    fun skipUntil(classifier: Classifier): Int {
        var skipped = 0;
        while (index < content.length) {
            val char = content[index];
            if (classifier(char)) break
            index++;
            skipped++;
            if (char == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return skipped;
    }
    fun skipUntil(classifier: Predicate<Char>): Int {
        var skipped = 0;
        while (index < content.length) {
            val char = content[index];
            if (classifier(char)) break
            index++;
            skipped++;
            if (char == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return skipped;
    }

    fun mark() {
        if (mark != -1) {
            throw IllegalStateException("Mark already set");
        }
        mark = index;
        markLocation = location();
    }

    fun rollback() {
        if (mark == -1) {
            throw IllegalStateException("Mark not set");
        }
        index = mark;
        mark = -1;
        line = markLocation.line;
        column = markLocation.column;
    }

    fun commit() {
        if (mark == -1) {
            throw IllegalStateException("Mark not set");
        }
        mark = -1;
    }

    companion object {
        val WHITESPACE: Classifier = Classifier { c: Char -> c.isWhitespace() };
        val DIGIT: Classifier = Classifier { c: Char -> c.isDigit() };
        val LETTER: Classifier = Classifier { c: Char -> c.isLetter() };
        val LETTER_OR_DIGIT: Classifier = Classifier { c: Char -> c.isLetterOrDigit() };
    }
}
