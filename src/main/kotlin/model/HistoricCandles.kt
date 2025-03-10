package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoricCandlesResult(val list: List<List<String>>)
