package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Filter4
import androidx.compose.material.icons.rounded.Filter7
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Style
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class GameCategory(val titleFa: String) {
    ALL("همه ۸ بازی اصیل"),
    BOARD("تخته‌ای سه‌بعدی (منچ و مارپله)"),
    CARD("ورقی (حکم و مشابه آن)")
}

enum class GameType(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val rulesFa: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val category: GameCategory,
    val accentStart: Color,
    val accentEnd: Color,
    val icon: ImageVector,
    @DrawableRes val bannerRes: Int
) {
    MENCH_CLASSIC(
        id = "mench_classic",
        titleFa = "منچ قدیم سه‌بعدی",
        subtitleFa = "صفحه کلاسیک ۴۰ خانه‌ای با مهره‌های چوبی سه‌بعدی",
        rulesFa = "به سبک منچ‌های نوستالژیک ایرانی با ۴۰ خانه دایره‌ای! با عدد ۶ مهره وارد زمین کنید و جایزه تاس دوباره بگیرید. روی مهره حریف فرود بیایید تا به خانه اول برگردد و چهار مهره را به خانه‌های رنگی وسط برسانید.",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.BOARD,
        accentStart = Color(0xFFF5B841),
        accentEnd = Color(0xFFC2410C),
        icon = Icons.Rounded.Casino,
        bannerRes = R.drawable.img_cat_classic
    ),
    SNAKES_LADDERS(
        id = "snakes_ladders",
        titleFa = "مار و پله سه‌بعدی",
        subtitleFa = "مارهای پرجزئیات، نردبان چوبی و نمای سه‌بعدی",
        rulesFa = "از خانه ۱ تا قله ۱۰۰ مسابقه دهید! نردبان‌های چوبی شما را سریع بالا می‌برند و مارهای رنگارنگ به پایین می‌کشند. با آوردن ۶ جایزه تاس دوباره می‌گیرید.",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.BOARD,
        accentStart = Color(0xFF10B981),
        accentEnd = Color(0xFF047857),
        icon = Icons.Rounded.Route,
        bannerRes = R.drawable.img_cat_classic
    ),
    HOKM(
        id = "hokm",
        titleFa = "حکم ۴ نفره تیمی",
        subtitleFa = "بازی اصیل ایرانی با انتخاب حکم و کت کردن حریف",
        rulesFa = "۴ بازیکن در ۲ تیم روبه‌رو هم! حاکم پس از دیدن ۵ کارت اول، خال حکم را انتخاب می‌کند. رعایت خال زمین الزامی است و در صورت نداشتن می‌توان با حکم برید. اولین تیمی که ۷ دست ببرد برنده راند است (۷ به ۰ = کت با ۲ امتیاز).",
        minPlayers = 4,
        maxPlayers = 4,
        category = GameCategory.CARD,
        accentStart = Color(0xFFF43F5E),
        accentEnd = Color(0xFF9F1239),
        icon = Icons.Rounded.Style,
        bannerRes = R.drawable.img_cat_party
    ),
    SHELEM(
        id = "shelem",
        titleFa = "شلم (حکم امتیازی)",
        subtitleFa = "مزایده امتیاز ۱۰۰ تا ۱۶۵، انتخاب حکم و شکار امتیازها",
        rulesFa = "مشابه حکم با هیجان مزایده! بازیکنان امتیاز تعهدی (از ۱۰۰ تا ۱۶۵) می‌خوانند؛ برنده مزایده حاکم شده و خال حکم را برمی‌گزیند. تک و ۱۰ هرکدام ۱۰ امتیاز، ۵ برابر ۵ امتیاز و هر دست ۵ امتیاز دارد.",
        minPlayers = 4,
        maxPlayers = 4,
        category = GameCategory.CARD,
        accentStart = Color(0xFFA855F7),
        accentEnd = Color(0xFF6D28D9),
        icon = Icons.Rounded.AutoAwesome,
        bannerRes = R.drawable.img_cat_party
    ),
    CHAHAR_BARG(
        id = "chahar_barg",
        titleFa = "چهاربرگ (پاسور ۱۱)",
        subtitleFa = "جمع ۱۱، سرباز جاروکن، ۷ خاج و امتیاز سور",
        rulesFa = "کارت دست خود را با کارت‌های زمین طوری ترکیب کنید که جمعشان ۱۱ شود، یا با سرباز همه کارت‌های عددی زمین را جمع کنید! خالی کردن کل زمین «سور» (+۵ امتیاز) دارد. ۷ خاج، ۱۰ خشت و ۲ خاج امتیاز ویژه دارند.",
        minPlayers = 2,
        maxPlayers = 2,
        category = GameCategory.CARD,
        accentStart = Color(0xFF14B8A6),
        accentEnd = Color(0xFF0F766E),
        icon = Icons.Rounded.Filter4,
        bannerRes = R.drawable.img_cat_strategy
    ),
    HAFT_KHABIS(
        id = "haft_khabis",
        titleFa = "هفت خبیث ایرانی",
        subtitleFa = "هفت جریمه‌دار، هشت جایزه، ده برگشت و سرباز حکم",
        rulesFa = "کارت هم‌خال یا هم‌عدد روی زمین بگذارید تا دستتان خالی شود! ۷ دو کارت جریمه می‌دهد (قابل روی‌هم انداختن)، ۸ نوبت دوباره، ۱۰ برعکس شدن جهت، تک پرش نفر بعدی و سرباز تغییر خال است.",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.CARD,
        accentStart = Color(0xFFFB7185),
        accentEnd = Color(0xFFE11D48),
        icon = Icons.Rounded.Filter7,
        bannerRes = R.drawable.img_cat_party
    ),
    BACKGAMMON(
        id = "backgammon",
        titleFa = "تخته نرد ایرانی سه‌بعدی",
        subtitleFa = "تخته چوب گردو با دو تاس، زدن مهره و خارج کردن",
        rulesFa = "دو تاس بریزید و ۱۵ مهره خود را به خانه خودی هدایت کرده و سپس از صفحه خارج کنید! روی تک‌مهره حریف بنشینید تا زده شود. جفت آوردن ۴ حرکت به شما می‌دهد.",
        minPlayers = 2,
        maxPlayers = 2,
        category = GameCategory.BOARD,
        accentStart = Color(0xFFD97706),
        accentEnd = Color(0xFF78350F),
        icon = Icons.Rounded.Dashboard,
        bannerRes = R.drawable.img_cat_classic
    ),
    PACHISI_ROYAL(
        id = "pachisi_royal",
        titleFa = "پچسی سلطنتی (منچ ستاره‌دار)",
        subtitleFa = "منچ ۵۲ خانه‌ای با ۸ خانه امن ستاره‌دار سه‌بعدی",
        rulesFa = "نسخه استراتژیک‌تر منچ با ۵۲ خانه و ۸ خانه امن ستاره‌دار که در آن‌ها مهره شما زده نمی‌شود! با عدد ۶ مهره آزاد کنید و زودتر از همه به تاج مرکزی برسید.",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.BOARD,
        accentStart = Color(0xFF22D3EE),
        accentEnd = Color(0xFF0369A1),
        icon = Icons.Rounded.Star,
        bannerRes = R.drawable.img_cat_strategy
    );

    companion object {
        fun fromId(id: String): GameType = entries.firstOrNull { it.id == id } ?: MENCH_CLASSIC
    }
}

enum class RoomMode(val titleFa: String) {
    WIFI_HOST("میزبان اتاق وای‌فای"),
    WIFI_CLIENT("مهمان اتاق وای‌فای"),
    LOCAL_PASS_AND_PLAY("بازی گروهی روی یک گوشی"),
    SOLO_VS_BOT("بازی با هوش مصنوعی")
}

data class PlayerInfo(
    val id: String,
    val name: String,
    val avatarIndex: Int = 0,
    val colorIndex: Int = 0,
    val isHost: Boolean = false,
    val isBot: Boolean = false,
    val isReady: Boolean = true,
    val isSpeaking: Boolean = false,
    val voiceLevel: Float = 0f,
    val ipAddress: String = ""
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderColorIndex: Int,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val isReaction: Boolean = false
)

data class DiscoveredRoom(
    val roomId: String,
    val roomName: String,
    val hostName: String,
    val hostIp: String,
    val tcpPort: Int,
    val voicePort: Int,
    val gameType: GameType,
    val playerCount: Int,
    val maxPlayers: Int,
    val isPlaying: Boolean = false,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class VoiceChatState(
    val isMicActive: Boolean = false,
    val isOpenMic: Boolean = false,
    val isPushToTalkPressed: Boolean = false,
    val isSpeakerMuted: Boolean = false,
    val localMicLevel: Float = 0f,
    val activeSpeakerName: String? = null,
    val hasRecordPermission: Boolean = false,
    val errorMessage: String? = null
)

data class UserProfile(
    val id: String,
    val name: String,
    val avatarIndex: Int,
    val favoriteColorIndex: Int,
    val totalWins: Int,
    val gamesPlayed: Int
)

sealed interface AppScreen {
    data object Home : AppScreen
    data object WifiHub : AppScreen
    data object Profile : AppScreen
    data object RoomLobby : AppScreen
    data object ActiveGame : AppScreen
}

val AvatarSymbols = listOf("👑", "🦁", "🦅", "🦊", "🐉", "⚡", "🌙", "💎")

val QuickPersianReactions = listOf(
    "آفرین! 👏",
    "عجب برشی! 🔥",
    "شانس آوردی! 😅",
    "نوبت توئه! 🎲",
    "ای بابا کت شدیم! 😱",
    "بریم دست بعد! 🏆"
)
