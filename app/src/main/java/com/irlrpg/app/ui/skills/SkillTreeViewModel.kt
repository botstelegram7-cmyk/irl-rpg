package com.irlrpg.app.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.repository.ProgressSync
import com.irlrpg.app.data.repository.SkillTreeRepository
import com.irlrpg.app.data.repository.SkillTreeState
import com.irlrpg.app.domain.game.SkillNodeView
import com.irlrpg.app.domain.game.SkillTreeEngine
import com.irlrpg.app.domain.model.SkillNodeState
import com.irlrpg.app.domain.model.SkillUnlockRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the node editor needs. */
data class NodeEditorState(
    val editing: Boolean = false,
    val nodeId: Long = 0,
    val name: String = "",
    val emoji: String = "✨",
    val description: String = "",
    val accentId: String = "lavender",
    val parentId: Long? = null,
    val attributeId: Long? = null,
    val masteryXp: Int = 240,
    val unlockRule: SkillUnlockRule = SkillUnlockRule.Root,
    val unlockParentMastery: Int = 120,
    val requiredAnyXp: Int = 500,
    val error: String? = null,
)

class SkillTreeViewModel(
    private val repository: SkillTreeRepository,
    private val progressSync: ProgressSync,
) : ViewModel() {

    val state: StateFlow<SkillTreeState> = repository.observeTree()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkillTreeState())

    private val _selected = MutableStateFlow<SkillNodeView?>(null)
    val selected: StateFlow<SkillNodeView?> = _selected.asStateFlow()

    private val _editor = MutableStateFlow<NodeEditorState?>(null)
    val editor: StateFlow<NodeEditorState?> = _editor.asStateFlow()

    private val _attributes = MutableStateFlow<List<AttributeEntity>>(emptyList())
    val attributes: StateFlow<List<AttributeEntity>> = _attributes.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            _attributes.value = repository.attributes()
        }
        // A tree change can satisfy achievements (e.g. "Grow Your Tree").
        viewModelScope.launch { runCatching { progressSync.sweep(announce = true) } }
    }

    fun select(view: SkillNodeView?) {
        _selected.value = view
    }

    /** Keeps the sheet in sync with freshly derived state after an XP change. */
    fun refreshSelection() {
        val current = _selected.value ?: return
        _selected.value = state.value.nodes.firstOrNull { it.node.id == current.node.id }
    }

    fun openCreate(parent: SkillNodeView? = null) {
        viewModelScope.launch {
            _attributes.value = repository.attributes()
            _editor.value = NodeEditorState(
                editing = false,
                parentId = parent?.node?.id,
                accentId = parent?.node?.accentId ?: "lavender",
                attributeId = parent?.node?.attributeId ?: _attributes.value.firstOrNull()?.id,
                unlockRule = if (parent == null) SkillUnlockRule.Root else SkillUnlockRule.ParentMastery,
                unlockParentMastery = if (parent == null) 0 else (parent.masteryXp / 2),
            )
        }
    }

    fun openEdit(view: SkillNodeView) {
        viewModelScope.launch {
            _attributes.value = repository.attributes()
            val node = view.node
            _editor.value = NodeEditorState(
                editing = true,
                nodeId = node.id,
                name = node.name,
                emoji = node.emoji,
                description = node.description.orEmpty(),
                accentId = node.accentId,
                parentId = node.parentId,
                attributeId = node.attributeId,
                masteryXp = node.masteryXp,
                unlockRule = SkillUnlockRule.fromName(node.unlockRule),
                unlockParentMastery = node.unlockParentMastery,
                requiredAnyXp = node.requiredAnyXp,
            )
        }
    }

    fun dismissEditor() {
        _editor.value = null
    }

    fun updateEditor(transform: (NodeEditorState) -> NodeEditorState) {
        _editor.value = _editor.value?.let(transform)
    }

    fun saveEditor() {
        val editor = _editor.value ?: return
        if (editor.name.isBlank()) {
            _editor.value = editor.copy(error = "Give the skill a name")
            return
        }
        viewModelScope.launch {
            if (editor.editing) {
                val existing = repository.node(editor.nodeId) ?: return@launch
                repository.updateNode(
                    existing.copy(
                        name = editor.name.trim(),
                        emoji = editor.emoji,
                        description = editor.description.trim().takeIf { it.isNotBlank() },
                        accentId = editor.accentId,
                        parentId = editor.parentId?.takeIf { it != existing.id },
                        attributeId = editor.attributeId,
                        masteryXp = editor.masteryXp,
                        unlockRule = editor.unlockRule.name,
                        unlockParentMastery = editor.unlockParentMastery,
                        requiredAnyXp = editor.requiredAnyXp,
                    )
                )
                _message.value = "Updated \"${editor.name.trim()}\""
            } else {
                repository.createNode(
                    name = editor.name,
                    emoji = editor.emoji,
                    description = editor.description,
                    accentId = editor.accentId,
                    parentId = editor.parentId,
                    attributeId = editor.attributeId,
                    masteryXp = editor.masteryXp,
                    unlockRule = editor.unlockRule,
                    unlockParentMastery = editor.unlockParentMastery,
                    requiredAnyXp = editor.requiredAnyXp,
                )
                _message.value = "Added \"${editor.name.trim()}\""
            }
            _editor.value = null
            progressSync.sweep(announce = true)
        }
    }

    fun deleteNode(id: Long) {
        viewModelScope.launch {
            val children = repository.deleteNode(id)
            _selected.value = null
            _message.value = if (children > 0) {
                "Node removed · $children child ${if (children == 1) "node is" else "nodes are"} now a branch root"
            } else {
                "Node removed"
            }
        }
    }

    fun childCandidates(): List<SkillNodeView> = state.value.nodes

    fun consumeMessage() {
        _message.value = null
    }

    fun remainingLabel(view: SkillNodeView): String = SkillTreeEngine.remainingLabel(view)

    fun stateLabel(state: SkillNodeState): String = state.label
}
