package pages

import Screen
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dialogs.PasswordPrompt
import dialogs.StickerDialog
import getTotalTrainingSessions
import gretting
import models.*
import next
import stickerUnits
import java.util.*

@Composable
fun MemberSelector(
    members: List<Member>,
    teilnahme: List<Teilnahme>,
    activeTrainer: Trainer,
    password: String,
    insertTeilnahme: (insertString: String, isExam: Boolean) -> Unit,
    changeScreen: (screen: Screen) -> Unit
) {
    val searchQuery = remember { mutableStateOf("") }
    var handleAsExam by remember { mutableStateOf(false) }

    val allMembers = remember { mutableStateListOf<Member>() }
    val newMembers = remember { mutableStateListOf<Member>() }

    remember { allMembers.addAll(members) }

    val checkedColors = remember { mutableStateListOf<DegreeColor>() }
    val checkedGroups = remember { mutableStateListOf<Group>() }

    fun <T : FilterOption> findMatch(s: String, options: List<T>, exactMach: Boolean): Boolean {
        return if (exactMach) options.any { a -> s.lowercase() == a.databaseName.lowercase() }
        else options.any { a -> s.lowercase().contains(a.databaseName.lowercase()) }
    }

    var showStickerDialog by remember { mutableStateOf(false) }
    var showCheckboxPasswordDialog by remember { mutableStateOf(false) }

    val studentsStickers = remember { mutableListOf<Member>() }

    fun submit(isExam: Boolean) {
        var teilnahmeString = ""
        for (member in newMembers) {
            teilnahmeString = teilnahmeString + member.id + ","

            if (isExam) setAddUnitsSinceLastExam(member) // set this to 0, so it won't get added in the future

            if (member.sticker_recieved != stickerUnits.keys.last()) // Wer 800 aufkelber hat, bekommt keinen weiteren (catch indexOutOfBounds)
                if (getTotalTrainingSessions(member, teilnahme) // ALLE Trainingseinheiten
                    >= stickerUnits.next(member.sticker_recieved).first
                ) studentsStickers.add(member)
        }
        insertTeilnahme(teilnahmeString, isExam)
        increaseTrainerUnitCount(activeTrainer)

        if (studentsStickers.isEmpty()) changeScreen(Screen.SUCCESS)
        else showStickerDialog = true
    }

    val leftLazyState = rememberLazyListState()
    val rightLazyState = rememberLazyListState()

    val greeting = remember { gretting() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(all = 8.dp)) {

        if (showStickerDialog) {
            StickerDialog(studentsStickers, activeTrainer) {
                showStickerDialog = false
                changeScreen(Screen.SUCCESS)
            }
        }

        if (showCheckboxPasswordDialog) {
            PasswordPrompt(
                password = password,
                result = { pwCorrect ->
                    handleAsExam = pwCorrect
                    showCheckboxPasswordDialog = !pwCorrect
                } // if password correct, set requirePasswort to false
            )
        }

        Text("$greeting ${activeTrainer.prename}, Teilnehmer auswählen", style = MaterialTheme.typography.h1)
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxSize()) {
            Row {
                LazyColumn(state = leftLazyState, modifier = Modifier.fillMaxHeight().width(250.dp)) {
                    items(allMembers.asSequence()
                        .filter { s ->
                            // filter color checkboxes
                            if (checkedColors.isEmpty()) true
                            else findMatch(s.level, checkedColors, false)
                        }.filter { s ->
                            // filter group checkboxes on top
                            if (checkedGroups.isEmpty()) true
                            else findMatch(s.group, checkedGroups, true)
                        }.filter {
                            // filter again for search ->
                            arrayListOf(
                                it.prename.lowercase(Locale.getDefault()),
                                it.surname.lowercase(Locale.getDefault())
                            ).joinToString(" ") // "prename surname"
                                .contains(searchQuery.value)
                            // <- filter again for search
                        }.sortedBy { it.prename }.sortedByDescending { it.level }.toList()
                    )
                    { /* linke spalte */ student ->
                        ListBox(student) {
                            newMembers.add(student)
                            allMembers.remove(student)
                            searchQuery.value = ""
                        }
                        Divider(modifier = Modifier.width(250.dp))
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = leftLazyState
                    )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(500.dp).fillMaxHeight()
            ) {
                Column { // search column
                    Text("Suchen:")
                    TextField(searchQuery.value, onValueChange = { newVal ->
                        searchQuery.value = newVal.lowercase(Locale.getDefault())
                    })
                }
                Column {
                    CustomFilter(DegreeColor.values(), checkedColors)
                    Divider(modifier = Modifier.padding(vertical = 30.dp))
                    CustomFilter(Group.values(), checkedGroups)
                }
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                            .clickable {
                                // just remove the tick if it was checked without password
                                if (handleAsExam) handleAsExam = false
                                else showCheckboxPasswordDialog = true
                            }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = handleAsExam,
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary),
                                onCheckedChange = {
                                    // just remove the tick if it was checked without password
                                    if (handleAsExam) handleAsExam = false
                                    else showCheckboxPasswordDialog = true

                                })
                            if (handleAsExam)
                                Text(
                                    text = "Prüfung!",
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 35.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(.9f)
                                )
                            else
                                Text(
                                    "Auswahl als Prüfung eintragen",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(.9f)
                                )
                        }
                    }

                    Button( // eingabe bestätigen
                        enabled = newMembers.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        onClick = { submit(handleAsExam) }) {
                        Text(
                            textAlign = TextAlign.Center,
                            text = if (newMembers.isEmpty()) "Teilnehmen aus der ersten Spalte auswählen" else "${newMembers.size} Teilnehmer eintragen!"
                        )
                    }
                }

            }

            Row {
                LazyColumn(state = rightLazyState, modifier = Modifier.fillMaxHeight().width(250.dp)) {
                    items(newMembers.asSequence()
                        .sortedBy { it.prename }
                        .sortedByDescending { it.level }
                        .toList()) { student ->
                        ListBox(student) {
                            allMembers.add(student)
                            newMembers.remove(student)
                        }
                        Divider(modifier = Modifier.width(250.dp))
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = rightLazyState
                    )
                )
            }
        }
    }
}

@Composable
private fun <T : FilterOption> CustomFilter(filterOptions: Array<T>, checked: MutableList<T>) {
    LazyVerticalGrid(GridCells.Fixed(2)) { // filter
        items(filterOptions) { option ->

            fun handleChecked() {
                //if (farbe.value == option) farbe.value = "" else farbe.value = option
                if (checked.contains(option)) checked.remove(option) else checked.add(option)
            }

            Box(
                modifier = Modifier.width(200.dp)
                    .clickable { handleChecked() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked.contains(option),
                        colors = when (option) {
                            is DegreeColor -> option.checkboxColors
                            else -> CheckboxDefaults.colors(MaterialTheme.colors.primary)
                        },
                        onCheckedChange = { handleChecked() },
                    )
                    Text(text = option.optionName)
                }
            }
        }
    }
}

@Composable
private fun ListBox(member: Member, onBoxClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .width(250.dp)
            .height(25.dp)
            .drawWithCache {
                val gradient = Brush.horizontalGradient(
                    colors = DegreeColor.getColorList(member.level).let { list ->
                        if (list.size < 2) list + list
                        else list
                    },
                    startX = size.width / 2 - 1,
                    endX = size.width / 2 + 1,
                )
                onDrawBehind {
                    drawRect(gradient)
                }
            }
            .clickable { onBoxClicked() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.W500,
            color = if (DegreeColor.getDegreeList(member.level).first()?.isDark == true)
                Color.White
            else
                Color.Black,
            modifier = Modifier.padding(start = 8.dp),
            text = "${member.prename} ${member.surname}"
        )
    }
}

enum class DegreeColor(
    private val color: Color,
    override val optionName: String,
    val isDark: Boolean,
    private val checkmarkColor: Color? = null
) : FilterOption {
    WHITE(color = Color.White, optionName = "Weiß", false, checkmarkColor = Color.Black) {
        override val databaseName = "Weiss"
    },
    YELLOW(color = Color(0xffffff35), optionName = "Gelb", isDark = false, checkmarkColor = Color.Black),
    RED(color = Color(0xffff0004), optionName = "Rot", isDark = false),
    ORANGE(color = Color(0xffffaa00), optionName = "Orange", isDark = false),
    GREEN(color = Color(0xff00aa00), optionName = "Grün", isDark = false),
    BLUE(color = Color(0xff0055ff), optionName = "Blau", isDark = true),
    PURPLE(color = Color(0xff5500ff), optionName = "Violett", isDark = true),
    BROWN(color = Color(0xffaa5500), optionName = "Braun", isDark = true),
    BLACK(color = Color.Black, optionName = "Schwarz", isDark = true);

    val checkboxColors
        @Composable get() = if (checkmarkColor == null) CheckboxDefaults.colors(checkedColor = color) else CheckboxDefaults.colors(
            checkedColor = color,
            checkmarkColor = checkmarkColor
        )

    companion object {
        fun getDegreeList(level: String) = level.trim().split(" ").last().split("-").map {
            // Could maybe use some better fallback or error
            values().find { color -> color.databaseName.lowercase() == it.lowercase() }
        }

        // TODO: This still isn't really nice, but a definite improvement
        fun getColorList(level: String) = getDegreeList(level).map {
            // Could maybe use some better fallback or error
            it?.color ?: Color.Transparent
        }
    }
}

interface FilterOption {
    val optionName: String
    val databaseName: String
        get() = optionName
}

private enum class Group(override val optionName: String, override val databaseName: String = optionName) :
    FilterOption {
    BENJAMINI("Karamini", databaseName = "Benjamini"),
    KIDS("Kinder Karate"),
    YOUTH("Jugend Karate"),
    NORMAL("Karate")
}
