package com.github.shishaomeng.searcheverywhereplus.contributor

import javax.swing.Icon

data class SearchResult(
    val title: String,
    var searchText : String,
    val description: Any,
    val action: Action
)

data class Action(
    val type: String,
    val param: String,
    val icon: Icon?
)

