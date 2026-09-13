package com.sunodo.app.data

/**
 * The four packet types from the design doc's schema (docs/blueprint.md §3.2).
 * Every packet SunoDo produces collapses into exactly one of these — that's
 * the whole point: a rambling voice note becomes a short list of atoms,
 * each with one job, the way a ribosome reads one strand into discrete proteins.
 */
enum class PacketType {
    TASK,
    QUESTION,
    DECISION,
    INFO
}
