package dbs.lab3.wechat.view

import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.layout.Region

fun <T> Dialog<T>.perfSize() {
    this.dialogPane.children.stream()
            .forEach { node ->
                if (node is Label) {
                    node.minHeight(Region.USE_PREF_SIZE)
                }
            }
}