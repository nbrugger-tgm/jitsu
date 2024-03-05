package eu.nitok.jitsu.compiler.parser

import kotlinx.serialization.Serializable

@Serializable
sealed interface Locatable {
    fun format(): String;

    /**
     * Includes the file name when possible
     */
    fun absoluteFormat(): String;

    /**
     * Marks the location in the text adding an optional note to it
     */
    fun mark(text: String, note: String?): String;
    fun toRange(): Range
}