package com.example.foodicstask.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodicstask.domain.model.Message
import com.example.foodicstask.presentation.BluetoothUiState
import com.example.foodicstask.ui.theme.FoodicsTaskTheme

@Composable
fun ChatScreen(
    state: BluetoothUiState,
    onDisconnect: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val message = rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Messages",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnect"
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.messages) { message ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChatMessage(
                        message = message,
                        modifier = Modifier
                            .align(if(message.isFromCurrentUser) Alignment.End else Alignment.Start)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .imePadding()
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = message.value,
                onValueChange = { message.value = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(text = "Start typing..")
                }
            )
            IconButton(
                onClick = {
                    onSendMessage(message.value)
                    message.value = ""
                    keyboardController?.hide()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}

@Composable
fun ChatMessage(
    message: Message,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = if (message.isFromCurrentUser) 15.dp else 0.dp,
                    topEnd = 15.dp,
                    bottomStart = 15.dp,
                    bottomEnd = if (message.isFromCurrentUser) 0.dp else 15.dp
                )
            )
            .background(
                if (message.isFromCurrentUser) myMessageBgColor else otherMessageBgColor
            )
            .padding(16.dp)
    ) {
        Text(
            text = message.senderName,
            fontSize = 14.sp,
            color = if (message.isFromCurrentUser) myMessageTextColor else otherMessageTextColor
        )
        Text(
            text = message.text,
            color = if (message.isFromCurrentUser) myMessageTextColor else otherMessageTextColor,
            modifier = Modifier.widthIn(max = 250.dp)
        )
    }
}

private val myMessageBgColor = Color(0xfffcd5e2)
private val myMessageTextColor = Color(0xff4d353d)
private val otherMessageBgColor = Color(0xff3a272d)
private val otherMessageTextColor = Color(0xffa7979c)

@Preview
@Composable
fun ChatMessagePreview() {
    FoodicsTaskTheme {
        ChatMessage(
            message = Message(
                text = "Hello World!",
                senderName = "One Plus 9 5G",
                isFromCurrentUser = true
            )
        )
    }
}

@Preview
@Composable
fun ChatScreenPreview() {
    FoodicsTaskTheme {
        ChatScreen(
            state = BluetoothUiState.Default.copy(
                messages = listOf(
                    Message(
                        text = "Hello From Bluetooth Classic!",
                        senderName = "One Plus 9 5G",
                        isFromCurrentUser = true
                    ),
                    Message(
                        text = "Reply From Bluetooth Classic",
                        senderName = "Redmi 9A",
                        isFromCurrentUser = false
                    ),
                    Message(
                        text = "Second message",
                        senderName = "One Plus 9 5G",
                        isFromCurrentUser = true
                    ),
                    Message(
                        text = "Second reply",
                        senderName = "Redmi 9A",
                        isFromCurrentUser = false
                    ),
                )
            ),
            onDisconnect = {},
            onSendMessage = {},
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}