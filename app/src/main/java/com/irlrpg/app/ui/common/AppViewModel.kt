package com.irlrpg.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.irlrpg.app.IrlRpgApp
import com.irlrpg.app.di.AppContainer

/**
 * Builds a ViewModel from the app's [AppContainer]. Kept deliberately simple:
 * no DI framework, so the whole object graph stays readable and startup stays
 * fast.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val context = LocalContext.current
    val container = (context.applicationContext as IrlRpgApp).container
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
