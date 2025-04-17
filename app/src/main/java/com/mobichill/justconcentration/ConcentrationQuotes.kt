package com.mobichill.justconcentration

object ConcentrationQuotes {
    val quotes = listOf(
        "Discipline is choosing between what you want now and what you want most.",
        "You don’t have to be extreme, just consistent.",
        "Focus on being productive instead of busy.",
        "The future depends on what you do today.",
        "Small progress is still progress.",
        "Great things are done by a series of small things brought together."
    )

    fun getRandomQuote(): String {
        return quotes.random()
    }
}