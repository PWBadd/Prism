// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shtuchki.glassbox.player.PlayerViewModel
import com.shtuchki.glassbox.ui.AppRoot
import com.shtuchki.glassbox.ui.theme.PrismTheme

/** AppCompatActivity нужна для переключения языка внутри приложения. */
class MainActivity : AppCompatActivity() {

    private val vm: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val art by vm.artAccent.collectAsState()
            PrismTheme(artAccent = art) {
                AppRoot(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshLibrary()
    }
}
