package org.example.project.auth

data class HaHistoryPoint(val timestampMs: Long, val state: String)

data class HaHistorySeries(val entityId: String, val points: List<HaHistoryPoint>)
