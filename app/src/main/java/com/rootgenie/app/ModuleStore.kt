package com.rootgenie.app

import java.io.File

/** Shared in-memory store of generated module zips. */
object ModuleStore {
    val files = mutableListOf<File>()
    fun add(f: File) { files.add(0, f) }
}
