package com.material3tim.data.repository

import com.material3tim.data.model.StickerPack

/**
 * 聊天表情 / 表情包的示例数据。第一阶段仅用于把表情面板 UI 填满。
 */
object EmojiRepository {

    /** 常用 emoji（面板按 8 列网格展示）。 */
    val emojis: List<String> = listOf(
        "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆",
        "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗",
        "🙂", "🤗", "🤩", "🤔", "🤨", "😐", "😑", "😶",
        "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪",
        "😫", "🥱", "😴", "😌", "😛", "😜", "😝", "🤤",
        "😒", "😓", "😔", "😕", "🙃", "🤑", "😲", "🙁",
        "😖", "😞", "😟", "😤", "😢", "😭", "😦", "😧",
        "😨", "😩", "🤯", "😬", "😰", "😱", "🥵", "🥶",
        "😳", "🤪", "😵", "🥴", "😠", "😡", "🤬", "😷",
        "🤒", "🤕", "🤢", "🤮", "🤧", "😇", "🥳", "🥺",
        "🤠", "🤡", "🤥", "🤫", "🤭", "🧐", "🤓", "😈",
        "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "👏",
    )

    /** 最近使用（示例）。 */
    val recentEmojis: List<String> = listOf("😀", "😂", "🥰", "👍", "🤔", "🎉", "❤️", "🔥")

    val stickerPacks: List<StickerPack> = listOf(
        StickerPack("sp1", "Tonton Friends", "🐻"),
        StickerPack("sp2", "Skeleton", "💀"),
        StickerPack("sp3", "Cute and lovely Fox", "🦊"),
        StickerPack("sp4", "BLUE DUCK BBAB", "🦆"),
        StickerPack("sp5", "Sosso sticker", "🐰"),
        StickerPack("sp6", "ManMandu", "🥟"),
        StickerPack("sp7", "熊猫头", "🐼"),
        StickerPack("sp8", "小黄脸", "🙂"),
        StickerPack("sp9", "Q 版三国", "⚔️"),
    )
}
