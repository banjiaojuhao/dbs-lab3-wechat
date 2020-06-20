package dbs.lab3.wechat

import dbs.lab3.wechat.view.LoginView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import tornadofx.App
import tornadofx.launch
import kotlin.coroutines.CoroutineContext

class MyApp : App(LoginView::class), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx
}

fun main() {
    launch<MyApp>()
}
