package com.example.recompositionissue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import com.example.recompositionissue.MyScreen.Event
import com.example.recompositionissue.MyScreen.State
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

val MyStateFlow = MutableStateFlow("my value")

@Composable
fun View(eventSink: (Event) -> Unit) {
  // The issue is that, sometimes, `value` stays null.
  val value by MyStateFlow.collectAsState(null)

  Scaffold { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues)) {
      val modifier = Modifier.border(2.dp, value?.let { Color.Unspecified } ?: Color.Red)
      Text(modifier = modifier, text = value ?: "null")
      Button(onClick = { eventSink(Event.Navigate) }) { Text("Refresh") }
    }
  }
}

class MainActivity : ComponentActivity() {
  init {
    // No issues if removed.
    GlobalScope.launch {
      moleculeFlow(
              RecompositionMode.Immediate,
              // No issues if snapshotNotifier set to External… But, according to its documentation,
              // we can specify `SnapshotNotifier.External` only AFTER we called `setContent`.
              // snapshotNotifier = SnapshotNotifier.External
          ) {}
          .collect {}
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val backStack = rememberSaveableBackStack(MyScreen)
      val navigator = rememberCircuitNavigator(backStack)

      CircuitCompositionLocals(circuit) {
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }
  }
}

val circuit =
    Circuit.Builder()
        .addPresenter<MyScreen, State> { screen, navigator, _ ->
          MyScreen.MyPresenter(screen, navigator)
        }
        .addUi<MyScreen, State> { state, _ -> View(state.eventSink) }
        .build()

@Parcelize
data object MyScreen : Screen {
  class State(val value: String?, val eventSink: (Event) -> Unit) : CircuitUiState

  sealed interface Event {
    data object Navigate : Event
  }

  @Suppress("unused")
  class MyPresenter(private val screen: MyScreen, private val navigator: Navigator) :
      Presenter<State> {
    @Composable
    override fun present(): State {
      val value by MyStateFlow.collectAsState(null)
      return State(value = value) {
        when (it) {
          Event.Navigate -> {
            navigator.resetRoot(MyScreen)
          }
        }
      }
    }
  }
}
