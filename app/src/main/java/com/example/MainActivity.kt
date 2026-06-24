package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.AppUiContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ifn_visual_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val repository by lazy { AppRepository(database) }

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            AppViewModelFactory(application, repository)
        )[AppViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppUiContainer(viewModel = viewModel)
                }
            }
        }
    }
}

