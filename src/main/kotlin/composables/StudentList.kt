package composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import models.Member

class StudentList {
    companion object {
        val textWidth: Dp = 300.dp
    }

    @Composable
    fun studentList(id: Int, members: List<Member>, onClick: (nameString: String) -> Unit) {
        val student = members.find { it.id == id }
        return if (student != null) {
            Row(horizontalArrangement = Arrangement.Center) {
                val nameString: String = student.prename + " " + student.surname
                Text(
                    text = nameString,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onClick(nameString) }.padding(10.dp).width(textWidth)
                )
            }
        } else {
            Row { textWidth }
        }
    }
}
