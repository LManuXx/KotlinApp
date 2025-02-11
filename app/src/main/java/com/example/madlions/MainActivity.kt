package com.example.madlions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.madlions.ui.theme.AppBarColor
import com.example.madlions.ui.theme.DarkAppBar
import com.example.madlions.ui.theme.MadLionsTheme
import com.example.madlions.ui.theme.TextColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MadLionsTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MadLions App",
                        fontSize = 20.sp,
                        color = TextColor // Texto en blanco
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkAppBar, // Fondo de la barra superior
                    titleContentColor = TextColor // Color del texto
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Fondo de la app
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Greeting(name = "Android")
        }
    }
}


@Composable
fun Greeting(name: String) {
    Text(
        text = "Hello $name!",
        fontSize = 24.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        color = TextColor

    )
}

