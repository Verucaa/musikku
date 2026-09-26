package com.zaaam.liphify.ui.nav

sealed class Tab(val route: String, val label: String) {
    data object ListenNow : Tab("listen", "Listen Now")
    data object Browse : Tab("browse", "Browse")
    data object Library : Tab("library", "Library")
}
