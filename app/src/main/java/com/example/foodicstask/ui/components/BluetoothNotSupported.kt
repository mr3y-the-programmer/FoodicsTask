package com.example.foodicstask.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.foodicstask.ui.theme.FoodicsTaskTheme

@Composable
fun BluetoothNotSupportedView(
    onConfirmButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = onConfirmButtonClick
            ) {
                Text("Ok")
            }
        },
        dismissButton = null,
        icon = {
            Icon(imageVector = BluetoothIcon, contentDescription = null)
        },
        title = {
            Text(
                text = "Bluetooth is not supported!",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Bluetooth is not available on this device. if you're running this app on an emulator, try running it on a real device instead."
            )
        },
        modifier = modifier
    )
}

public val BluetoothIcon: ImageVector
    get() {
        if (_BluetoothIcon != null) {
            return _BluetoothIcon!!
        }
        _BluetoothIcon = ImageVector.Builder(
            name = "Bluetooth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(440f, 880f)
                verticalLineToRelative(-304f)
                lineTo(256f, 760f)
                lineToRelative(-56f, -56f)
                lineToRelative(224f, -224f)
                lineToRelative(-224f, -224f)
                lineToRelative(56f, -56f)
                lineToRelative(184f, 184f)
                verticalLineToRelative(-304f)
                horizontalLineToRelative(40f)
                lineToRelative(228f, 228f)
                lineToRelative(-172f, 172f)
                lineToRelative(172f, 172f)
                lineTo(480f, 880f)
                close()
                moveToRelative(80f, -496f)
                lineToRelative(76f, -76f)
                lineToRelative(-76f, -74f)
                close()
                moveToRelative(0f, 342f)
                lineToRelative(76f, -74f)
                lineToRelative(-76f, -76f)
                close()
            }
        }.build()
        return _BluetoothIcon!!
    }

private var _BluetoothIcon: ImageVector? = null


@Preview(device = "id:pixel_7")
@Composable
private fun BluetoothNotSupportedViewPreview() {
    FoodicsTaskTheme {
        BluetoothNotSupportedView(
            onConfirmButtonClick = {}
        )
    }
}
