package org.example.project.cards

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import org.example.project.auth.HaEntityState

internal sealed class HaCondition {
    data class State(
        val entity: String,
        val state: List<String>,
        val stateNot: List<String>,
    ) : HaCondition()

    data class NumericState(
        val entity: String,
        val above: Double?,
        val below: Double?,
    ) : HaCondition()

    data class And(val conditions: List<HaCondition>) : HaCondition()
    data class Or(val conditions: List<HaCondition>) : HaCondition()
    data class Not(val conditions: List<HaCondition>) : HaCondition()

    // user / screen / time / location — unsupported, always passes
    data object Unknown : HaCondition()
}

internal fun parseVisibility(element: JsonElement?): List<HaCondition> {
    val arr = element as? JsonArray ?: return emptyList()
    return arr.mapNotNull { (it as? JsonObject)?.let(::parseCondition) }
}

private fun parseCondition(obj: JsonObject): HaCondition {
    return when (stringField(obj, "condition")) {
        "state" -> {
            val entity = stringField(obj, "entity") ?: return HaCondition.Unknown
            HaCondition.State(
                entity = entity,
                state = parseStringOrList(obj["state"]),
                stateNot = parseStringOrList(obj["state_not"]),
            )
        }
        "numeric_state" -> {
            val entity = stringField(obj, "entity") ?: return HaCondition.Unknown
            HaCondition.NumericState(
                entity = entity,
                above = (obj["above"] as? JsonPrimitive)?.doubleOrNull,
                below = (obj["below"] as? JsonPrimitive)?.doubleOrNull,
            )
        }
        "and" -> HaCondition.And(parseConditionList(obj["conditions"]))
        "or" -> HaCondition.Or(parseConditionList(obj["conditions"]))
        "not" -> HaCondition.Not(parseConditionList(obj["conditions"]))
        else -> HaCondition.Unknown
    }
}

private fun parseConditionList(element: JsonElement?): List<HaCondition> {
    val arr = element as? JsonArray ?: return emptyList()
    return arr.mapNotNull { (it as? JsonObject)?.let(::parseCondition) }
}

private fun parseStringOrList(element: JsonElement?): List<String> = when (element) {
    is JsonPrimitive -> element.contentOrNull?.let { listOf(it) } ?: emptyList()
    is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    else -> emptyList()
}

internal fun evaluateVisibility(
    conditions: List<HaCondition>,
    entityStates: Map<String, HaEntityState>,
): Boolean = conditions.all { evaluateCondition(it, entityStates) }

private fun evaluateCondition(
    condition: HaCondition,
    entityStates: Map<String, HaEntityState>,
): Boolean = when (condition) {
    is HaCondition.State -> {
        val state = entityStates[condition.entity]?.state ?: return false
        (condition.state.isEmpty() || state in condition.state) &&
            (condition.stateNot.isEmpty() || state !in condition.stateNot)
    }
    is HaCondition.NumericState -> {
        val value = entityStates[condition.entity]?.state?.toDoubleOrNull() ?: return false
        (condition.above == null || value > condition.above) &&
            (condition.below == null || value < condition.below)
    }
    is HaCondition.And -> condition.conditions.all { evaluateCondition(it, entityStates) }
    is HaCondition.Or -> condition.conditions.any { evaluateCondition(it, entityStates) }
    is HaCondition.Not -> condition.conditions.none { evaluateCondition(it, entityStates) }
    is HaCondition.Unknown -> true
}
