package org.example.project.cards

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.json.JsonObject

internal sealed class HaAction {
    data object None : HaAction()
    data class Toggle(val entity: String? = null) : HaAction()
    data class PerformAction(
        val action: String,
        val data: JsonObject? = null,
        val target: JsonObject? = null,
    ) : HaAction()
    data class Navigate(val path: String) : HaAction()
    data class OpenUrl(val url: String) : HaAction()
    data class MoreInfo(val entity: String? = null) : HaAction()
}

// (action, contextEntityId) — contextEntityId is the badge's own entity
internal val LocalHaActionHandler = staticCompositionLocalOf<(HaAction, String?) -> Unit> {
    { _: HaAction, _: String? -> }
}

internal fun parseAction(obj: JsonObject?, key: String): HaAction {
    val actionObj = obj?.get(key) as? JsonObject ?: return HaAction.None
    return when (stringField(actionObj, "action") ?: "more-info") {
        "none" -> HaAction.None
        "toggle" -> HaAction.Toggle(entity = stringField(actionObj, "entity"))
        "perform-action", "call-service" -> {
            val action = stringField(actionObj, "perform_action")
                ?: stringField(actionObj, "service") // legacy key
                ?: return HaAction.None
            HaAction.PerformAction(
                action = action,
                data = actionObj["data"] as? JsonObject
                    ?: actionObj["service_data"] as? JsonObject,
                target = actionObj["target"] as? JsonObject,
            )
        }
        "navigate" -> {
            val path = stringField(actionObj, "navigation_path") ?: return HaAction.None
            HaAction.Navigate(path)
        }
        "url" -> {
            val url = stringField(actionObj, "url_path") ?: return HaAction.None
            HaAction.OpenUrl(url)
        }
        "more-info" -> HaAction.MoreInfo(entity = stringField(actionObj, "entity"))
        else -> HaAction.None
    }
}
