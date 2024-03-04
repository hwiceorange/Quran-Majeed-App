package com.raiadnan.quranreader.DataHolder


class IndexData {

    companion object {
        var loop = 0
        var primaryIndex = -1
        var secondaryIndex = -1
        var index: ArrayList<IndexModel> = ArrayList()
    }

    fun clear() {
        index.clear()
    }
}