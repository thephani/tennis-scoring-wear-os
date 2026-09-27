package com.example.tennisscore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tennisscore.data.MatchRepository
import com.example.tennisscore.ui.MatchViewModel
import com.example.tennisscore.ui.TennisApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = MatchRepository(applicationContext)
        setContent {
            val model: MatchViewModel = viewModel(factory = MatchViewModel.factory(repository))
            TennisApp(model)
        }
    }
}
