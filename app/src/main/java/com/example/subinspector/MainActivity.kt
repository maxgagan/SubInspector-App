package com.example.subinspector

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.subinspector.ui.theme.SubInspectorTheme
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubInspectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SubInspectorApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SubInspectorApp(modifier: Modifier = Modifier) {
    var premiumStatus by remember { mutableStateOf("Checking...") }
    var showDebugScreen by remember { mutableStateOf(false) }
    var showPaywallScreen by remember { mutableStateOf(false) }

    fun refreshPremiumStatus() {
        Purchases.sharedInstance.getCustomerInfo(
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val isProActive =
                        customerInfo.entitlements.active["SubInspector Pro"] != null

                    premiumStatus = if (isProActive) {
                        "Premium Active"
                    } else {
                        "Free User"
                    }
                }

                override fun onError(error: PurchasesError) {
                    premiumStatus = "Could not check status"
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        refreshPremiumStatus()
    }

    when {
        showDebugScreen -> {
            DebugScreen(
                modifier = modifier,
                premiumStatus = premiumStatus,
                onBackClick = { showDebugScreen = false }
            )
        }

        showPaywallScreen -> {
            PaywallScreen(
                modifier = modifier,
                onBackClick = { showPaywallScreen = false },
                onPurchaseOrRestoreSuccess = { refreshPremiumStatus() }
            )
        }

        else -> {
            HomeScreen(
                modifier = modifier,
                premiumStatus = premiumStatus,
                onOpenPaywallClick = { showPaywallScreen = true },
                onOpenDebugClick = { showDebugScreen = true }
            )
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    premiumStatus: String,
    onOpenPaywallClick: () -> Unit,
    onOpenDebugClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SubInspector",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "RevenueCat setup is connected",
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Premium Status: $premiumStatus",
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = onOpenPaywallClick,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Open Paywall")
        }

        Button(
            onClick = onOpenDebugClick,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Open Debug Screen")
        }
    }
}

@Composable
fun DebugScreen(
    modifier: Modifier = Modifier,
    premiumStatus: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Debug Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "RevenueCat Connected: Yes",
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Entitlement Checked: SubInspector Pro",
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Current Premium Status: $premiumStatus",
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = onBackClick,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onPurchaseOrRestoreSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var offeringStatus by remember { mutableStateOf("Loading offerings...") }
    var actionStatus by remember { mutableStateOf("No action yet") }

    var monthlyText by remember { mutableStateOf("Not found") }
    var yearlyText by remember { mutableStateOf("Not found") }
    var lifetimeText by remember { mutableStateOf("Not found") }

    var monthlyPackage by remember { mutableStateOf<Package?>(null) }
    var yearlyPackage by remember { mutableStateOf<Package?>(null) }
    var lifetimePackage by remember { mutableStateOf<Package?>(null) }

    LaunchedEffect(Unit) {
        Purchases.sharedInstance.getOfferings(
            object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    val currentOffering = offerings.current

                    if (currentOffering == null) {
                        offeringStatus = "No current offering found"
                        return
                    }

                    offeringStatus = "Offering loaded successfully"

                    monthlyPackage = currentOffering.monthly
                    yearlyPackage = currentOffering.annual
                    lifetimePackage = currentOffering.lifetime

                    monthlyText = monthlyPackage?.product?.title ?: "Monthly package not available"
                    yearlyText = yearlyPackage?.product?.title ?: "Yearly package not available"
                    lifetimeText = lifetimePackage?.product?.title ?: "Lifetime package not available"
                }

                override fun onError(error: PurchasesError) {
                    offeringStatus = "Could not load offerings"
                    monthlyText = "Error loading monthly package"
                    yearlyText = "Error loading yearly package"
                    lifetimeText = "Error loading lifetime package"
                }
            }
        )
    }

    fun purchaseRevenueCatPackage(selectedPackage: Package?) {
        if (activity == null) {
            actionStatus = "Activity context not available"
            return
        }

        if (selectedPackage == null) {
            actionStatus = "Selected package not available"
            return
        }

        actionStatus = "Starting purchase..."

        Purchases.sharedInstance.purchase(
            purchaseParams = PurchaseParams.Builder(activity, selectedPackage).build(),
            callback = object : PurchaseCallback {
                override fun onCompleted(
                    storeTransaction: StoreTransaction,
                    customerInfo: CustomerInfo
                ) {
                    val isProActive =
                        customerInfo.entitlements.active["SubInspector Pro"] != null

                    actionStatus = if (isProActive) {
                        "Purchase successful. Premium is active."
                    } else {
                        "Purchase finished, but premium not active yet."
                    }

                    onPurchaseOrRestoreSuccess()
                }

                override fun onError(
                    error: PurchasesError,
                    userCancelled: Boolean
                ) {
                    actionStatus = if (userCancelled) {
                        "Purchase cancelled by user"
                    } else {
                        "Purchase failed: ${error.message}"
                    }
                }
            }
        )
    }

    fun restorePurchases() {
        actionStatus = "Restoring purchases..."

        Purchases.sharedInstance.restorePurchases(
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val isProActive =
                        customerInfo.entitlements.active["SubInspector Pro"] != null

                    actionStatus = if (isProActive) {
                        "Restore successful. Premium is active."
                    } else {
                        "Restore completed. No active premium found."
                    }

                    onPurchaseOrRestoreSuccess()
                }

                override fun onError(error: PurchasesError) {
                    actionStatus = "Restore failed: ${error.message}"
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Paywall Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = offeringStatus,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Monthly: $monthlyText",
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(
            onClick = { purchaseRevenueCatPackage(monthlyPackage) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Buy Monthly")
        }

        Text(
            text = "Yearly: $yearlyText",
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(
            onClick = { purchaseRevenueCatPackage(yearlyPackage) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Buy Yearly")
        }

        Text(
            text = "Lifetime: $lifetimeText",
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(
            onClick = { purchaseRevenueCatPackage(lifetimePackage) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Buy Lifetime")
        }

        Text(
            text = actionStatus,
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(
            onClick = { restorePurchases() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Restore Purchases")
        }

        Button(
            onClick = onBackClick,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Back")
        }
    }
}