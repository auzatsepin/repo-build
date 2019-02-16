package repo.build

import java.io.File

var File.text: String
    get() {
        return readText(Charsets.UTF_8)
    }
    set(text) {
        writeText(text, Charsets.UTF_8)
    }