package com.github.shishaomeng.searcheverywhereplus.contributor

import com.github.shishaomeng.searcheverywhereplus.contributor.SearchResult
import com.github.shishaomeng.searcheverywhereplus.contributor.AiSearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.openapi.actionSystem.AnActionEvent

class AiSearchContributorFactory : SearchEverywhereContributorFactory<SearchResult> {
    override fun createContributor(initEvent: AnActionEvent): SearchEverywhereContributor<SearchResult> {
        return AiSearchEverywhereContributor(initEvent)
    }
}