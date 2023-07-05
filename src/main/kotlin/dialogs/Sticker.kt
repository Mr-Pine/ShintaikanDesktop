package dialogs

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import getTotalTrainingSessions
import models.Member
import models.Trainer
import models.editMemberSticker
import models.loadTeilnahme
import next
import nextStickerUnit
import stickerUnits
import java.time.LocalDate

@Composable
fun StickerDialog(
    stickerStudentsList: List<Member>,
    activeTrainer: Trainer,
    onDismiss: () -> Unit
) {
    val mutableMembers = remember { stickerStudentsList.toMutableStateList() }
    fun SnapshotStateList<Member>.filterShowSticker() = filter { it.sticker_show_again }

    val lazyState = rememberLazyListState()

    val teilnahme = loadTeilnahme()

    /**
     * This function returns true if all radio buttons have been clicked at lease once to ensure,
     * that the user has made his desicion for each student
     */
    fun buttonEnabled() = mutableMembers.filterShowSticker().all { it.radioClicked }

    Dialog(
        state = rememberDialogState(position = WindowPosition(Alignment.Center), width = 750.dp, height = 600.dp),
        title = "Aufkleber",
        onCloseRequest = {},
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(.8f).padding(bottom = 8.dp)
            ) {
                LazyColumn(
                    state = lazyState,
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(.9f)
                ) {
                    item {
                        Text("Folgende Teilnehmer bekommen Aufkleber:", style = MaterialTheme.typography.subtitle1)
                    }
                    item {
                        Divider(modifier = Modifier.padding(vertical = 10.dp))
                    }
                    items(mutableMembers.filterShowSticker()) { member ->
                        val total = getTotalTrainingSessions(member, teilnahme)
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Description(member, total)

                            RadioRecieved(member, mutableMembers)

                            Spacer(modifier = Modifier.width(8.dp))

                            RadioNotRecieved(member, mutableMembers)
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.fillMaxHeight().padding(top = 35.dp),
                    adapter = rememberScrollbarAdapter(
                        scrollState = lazyState
                    )
                )
            }
            Button(enabled = buttonEnabled(), modifier = Modifier.fillMaxWidth(.5f), onClick = {
                mutableMembers.filterShowSticker().forEach { member ->
                    if (member.stickerRecieved) {
                        val nextStickerRecieved = stickerUnits.next(member.sticker_recieved).first
                        val nextStickerRecievedBy = "$nextStickerRecieved:${activeTrainer.id}:${LocalDate.now()}"

                        mutableMembers[mutableMembers.indexOf(member)] =
                            member.copy(
                                sticker_recieved_by = nextStickerRecievedBy,
                                radioClicked = false,
                                sticker_recieved = nextStickerRecieved,
                                sticker_show_again = if (member.sticker_recieved == stickerUnits.keys.toList()[stickerUnits.keys.size - 2]) false else
                                    getTotalTrainingSessions(
                                        member,
                                        teilnahme
                                    ) >= stickerUnits.next(stickerUnits.next(member.sticker_recieved).first).first // erster Teil vor dem && ist das gegenereignis von der if oben < / >=
                            )

                        editMemberSticker(
                            member.copy(
                                sticker_recieved_by = nextStickerRecievedBy,
                                sticker_recieved = nextStickerRecieved
                            )
                        )
                    } else {
                        mutableMembers[mutableMembers.indexOf(member)] =
                            member.copy(
                                sticker_show_again = false
                            )
                    }
                }

                if (mutableMembers.none { it.sticker_show_again }) onDismiss()
            }) {
                Text("OK")
            }
        }
    }
}

@Composable
private fun RadioRecieved(
    student: Member,
    mutableMembers: SnapshotStateList<Member>
) {
    Text("Erhalten")
    RadioButton(
        student.stickerRecieved && student.radioClicked,
        onClick = {
            mutableMembers[mutableMembers.indexOf(student)] =
                student.copy(
                    stickerRecieved = true,
                    radioClicked = true
                )
        }
    )
}

@Composable
private fun RadioNotRecieved(
    student: Member,
    mutableMembers: SnapshotStateList<Member>
) {
    Text("Nicht erhalten")
    RadioButton(
        !student.stickerRecieved && student.radioClicked,
        onClick = {
            mutableMembers[mutableMembers.indexOf(student)] =
                student.copy(
                    stickerRecieved = false,
                    radioClicked = true
                )
        }
    )
}

@Composable
private fun Description(student: Member, total: Int) {
    val textModifier = Modifier.padding(8.dp).width(300.dp)
    if (student.sticker_recieved != stickerUnits.keys.last()) {
        if (
            student.sticker_recieved == stickerUnits.keys.elementAt(stickerUnits.keys.size - 2) ||
            total < student.sticker_recieved.nextStickerUnit().nextStickerUnit().first
        ) {
            Text(
                "${student.prename} ${student.surname}, hat " +
                        "$total Trainingseinheiten und bekommt einen " +
                        "${stickerUnits.next(student.sticker_recieved).second} Aufkleber",
                modifier = textModifier
            )
        } else {
            Text(
                "${student.prename} ${student.surname}, hat $total Trainingseinheiten und bekommt aber immer noch einen " +
                        "${stickerUnits.next(student.sticker_recieved).second} Aufkleber",
                modifier = textModifier
            )
        }
    }
}
