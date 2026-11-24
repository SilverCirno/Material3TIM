package com.material3tim.ui.component.material

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

@Composable
fun OffsetAnchoredExpressiveMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorOffset: IntOffset = IntOffset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        // 这个 Box 只是弹出菜单的“锚点”，本身不参与排版：布局尺寸必须报 0×0。
        // 之前报的是「父级最大宽度 × 0」，会把外层 Box 撑到最大宽度——
        // 聊天页里外层 Box 包在气泡外，短文本气泡因此被顶到他自己那一侧的最左边
        // （右侧气泡与右边头像之间出现巨大空白）。
        // Popup 的锚点位置取自被 place 的子节点坐标，所以 place(anchorOffset)
        // 仍然能把菜单定位到长按点，与布局尺寸无关。
        modifier = Modifier.layout { measurable, _ ->
            val placeable = measurable.measure(Constraints())
            layout(0, 0) {
                placeable.place(anchorOffset.x, anchorOffset.y)
            }
        }
    ) {
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

fun Modifier.trackPressPosition(onPress: (Offset) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onPress(down.position)
    }
}
