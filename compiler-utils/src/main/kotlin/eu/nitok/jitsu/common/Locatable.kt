package eu.nitok.jitsu.common

import kotlinx.serialization.Serializable

interface Locatable {
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
