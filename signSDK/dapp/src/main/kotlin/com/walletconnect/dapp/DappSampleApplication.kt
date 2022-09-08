package com.walletconnect.dapp

import android.app.Application
import android.util.Log
import com.walletconnect.android.api.ConnectionType
import com.walletconnect.android.api.RelayClient
import com.walletconnect.sample_common.BuildConfig
import com.walletconnect.sample_common.WALLET_CONNECT_PROD_RELAY_URL
import com.walletconnect.sample_common.tag
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient

class DappSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        //TODO: register at https://walletconnect.com/register to get a project ID
        val serverUri = "wss://$WALLET_CONNECT_PROD_RELAY_URL?projectId=${BuildConfig.PROJECT_ID}"
        RelayClient.initialize(relayServerUrl = serverUri, connectionType = ConnectionType.AUTOMATIC, application = this)

        // Sample of how to use a URI to initialize the WalletConnect SDK
        val initString = Sign.Params.Init(
            relay = RelayClient,
            metadata = Sign.Model.AppMetaData(
                name = "Kotlin Dapp",
                description = "Dapp description",
                url = "example.dapp",
                icons = listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"),
                redirect = "kotlin-dapp-wc:/request"
            )
        )

        SignClient.initialize(initString) { error ->
            Log.e(tag(this), error.throwable.stackTraceToString())
        }
    }
}