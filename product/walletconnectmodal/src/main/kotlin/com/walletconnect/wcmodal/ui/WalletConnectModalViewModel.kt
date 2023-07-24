package com.walletconnect.wcmodal.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.android.internal.common.explorer.data.model.Wallet
import com.walletconnect.android.internal.common.explorer.domain.usecase.GetWalletsUseCaseInterface
import com.walletconnect.android.internal.common.wcKoinApp
import com.walletconnect.wcmodal.client.Modal
import com.walletconnect.wcmodal.client.WalletConnectModal
import com.walletconnect.wcmodal.domain.WalletConnectModalStorage
import com.walletconnect.wcmodal.domain.WalletConnectModalDelegate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

private const val WCM_SDK = "wcm"

internal class WalletConnectModalViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val uri = savedStateHandle.get<String>(MODAL_URI_ARG)
    private val chains = savedStateHandle.get<String?>(MODAL_CHAINS_ARG)

    private val getWalletsUseCase: GetWalletsUseCaseInterface = wcKoinApp.koin.get()
    private val wcModalStorage: WalletConnectModalStorage = wcKoinApp.koin.get()

    private val _modalState: MutableStateFlow<WalletConnectModalState?> = MutableStateFlow(null)

    val modalState: StateFlow<WalletConnectModalState?>
        get() = _modalState

    private val _modalEvents: MutableSharedFlow<WalletConnectModalEvents> = MutableSharedFlow()
    val modalEvents: SharedFlow<WalletConnectModalEvents>
        get() = _modalEvents.asSharedFlow()

    private fun subscribeToWalletEvents() {
        WalletConnectModalDelegate.wcEventModels.map { event ->
            when (event) {
                is Modal.Model.ApprovedSession -> WalletConnectModalEvents.SessionApproved
                is Modal.Model.RejectedSession -> WalletConnectModalEvents.SessionRejected
                else -> WalletConnectModalEvents.NoAction
            }
        }.onEach { event ->
            _modalEvents.emit(event)
        }
    }

    init {
        subscribeToWalletEvents()
        viewModelScope.launch {
            uri?.let {
                createModalState(it)
            } ?: _modalEvents.tryEmit(WalletConnectModalEvents.InvalidState)
        }
    }

    private suspend fun createModalState(uri: String) {
        try {
            val wallets = if (WalletConnectModal.recommendedWalletsIds.isEmpty()) {
                getWalletsUseCase(sdkType = WCM_SDK, chains = chains, excludedIds = WalletConnectModal.excludedWalletsIds)
            } else {
                getWalletsUseCase(sdkType = WCM_SDK, chains = chains, excludedIds = WalletConnectModal.excludedWalletsIds, recommendedIds = WalletConnectModal.recommendedWalletsIds).union(
                    getWalletsUseCase(sdkType = WCM_SDK, chains = chains, excludedIds = WalletConnectModal.excludedWalletsIds)
                ).toList()
            }.mapRecentWallet(wcModalStorage.getRecentWalletId())
            _modalState.value = WalletConnectModalState(uri, wallets)
        } catch (e: Exception) {
            Timber.e(e)
            _modalState.value = WalletConnectModalState(uri)
        }
    }

    fun updateRecentWalletId(id: String) = with(_modalState.value) {
        wcModalStorage.saveRecentWalletId(id)
        _modalState.value = this?.copy(wallets = this.wallets.mapRecentWallet(id))
    }
}

private fun List<Wallet>.mapRecentWallet(id: String?) = map {
    it.apply { it.recent = it.id == id }
}.sortedBy { !it.recent }