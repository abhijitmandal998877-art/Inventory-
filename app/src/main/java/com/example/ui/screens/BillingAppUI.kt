package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.ShopOwner
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Dynamic Color Palette
val PrimaryNavy = Color(0xFF1D1B20)
val SolidNavyLight = Color(0xFF49454F)
val AccentAzure = Color(0xFF6750A4)
val BackgroundSlate = Color(0xFFFEF7FF)
val DarkBackgroundSlate = Color(0xFF21005D)

// Safe File Share and View
fun shareInvoicePdf(context: Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.aistudio.mobilebilling.klysqp.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Invoice PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

fun viewInvoicePdf(context: Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.aistudio.mobilebilling.klysqp.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF Viewer application found on this device.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun BillingAppNavigation(viewModel: BillingViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val owner by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Display appropriate screen
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AccentAzure,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            surface = Color.White,
            onSurface = PrimaryNavy,
            background = BackgroundSlate,
            onBackground = PrimaryNavy,
            secondary = SolidNavyLight
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                Screen.Auth -> AuthScreen(viewModel = viewModel)
                else -> {
                    // Logged in scaffold shell
                    Scaffold(
                        topBar = {
                            BillingTopAppBar(owner = owner, onLogout = { viewModel.logout() })
                        },
                        bottomBar = {
                            BillingBottomNavigationBar(
                                currentScreen = currentScreen,
                                onSelectScreen = { viewModel.setScreen(it) }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                Screen.Billing -> NewBillScreen(viewModel = viewModel)
                                Screen.Scanner -> RealScannerScreen(viewModel = viewModel)
                                Screen.History -> HistoryScreen(viewModel = viewModel)
                                Screen.Profile -> ProfileScreen(viewModel = viewModel)
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingTopAppBar(owner: ShopOwner?, onLogout: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = "Shop icon",
                    tint = AccentAzure,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = owner?.shopName ?: "Mobile Billing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.0.sp,
                        color = PrimaryNavy
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Secure local session",
                            fontSize = 10.0.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onLogout,
                modifier = Modifier.testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Log Out",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = PrimaryNavy
        ),
        modifier = Modifier.shadow(1.dp)
    )
}

@Composable
fun BillingBottomNavigationBar(currentScreen: Screen, onSelectScreen: (Screen) -> Unit) {
    NavigationBar(
        containerColor = Color(0xFFF3EDF7),
        tonalElevation = 6.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.Billing,
            onClick = { onSelectScreen(Screen.Billing) },
            icon = { Icon(Icons.Default.Receipt, contentDescription = "New Bill") },
            label = { Text("Billing", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentAzure,
                unselectedIconColor = SolidNavyLight,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_billing_tab")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Scanner,
            onClick = { onSelectScreen(Screen.Scanner) },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Barcode/QR Scanner") },
            label = { Text("Scanner", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentAzure,
                unselectedIconColor = SolidNavyLight,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_scanner_tab")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.History,
            onClick = { onSelectScreen(Screen.History) },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentAzure,
                unselectedIconColor = SolidNavyLight,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_history_tab")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Profile,
            onClick = { onSelectScreen(Screen.Profile) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Update Shop Info") },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentAzure,
                unselectedIconColor = SolidNavyLight,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_profile_tab")
        )
    }
}

@Composable
fun AuthScreen(viewModel: BillingViewModel) {
    var isRegisterState by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Register shop details
    var shopName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var curSymbol by remember { mutableStateOf("$") }
    var taxRateText by remember { mutableStateOf("0.0") }
    
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isLoggingIn by viewModel.isLoggingIn.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryNavy, DarkBackgroundSlate)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Logo Title
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentAzure.copy(alpha = 0.15f))
                        .border(1.5.dp, AccentAzure, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Billing Hub",
                        tint = AccentAzure,
                        modifier = Modifier.size(34.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isRegisterState) "Create Business ID" else "Shopkeeper Secure Login",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = if (isRegisterState) "Register your store and configurations" else "Authorize terminal to invoice payments",
                    fontSize = 11.sp,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                // Error Warning Card
                AnimatedVisibility(
                    visible = authError != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = authError ?: "",
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Core Form Inputs
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearAuthError() },
                    label = { Text("Business Email Address", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentAzure,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = AccentAzure
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearAuthError() },
                    label = { Text("Account Password", color = Color.LightGray) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentAzure,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = AccentAzure
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // On-boarding Shop profile fields
                AnimatedVisibility(visible = isRegisterState) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("Shop/Brand Name", color = Color.LightGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentAzure
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_name_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Shop Location/Address", color = Color.LightGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentAzure
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { contactNumber = it },
                            label = { Text("Contact Number", color = Color.LightGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentAzure
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = curSymbol,
                                onValueChange = { curSymbol = it },
                                label = { Text("Currency Symbol", color = Color.LightGray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = AccentAzure
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp)
                            )
                            OutlinedTextField(
                                value = taxRateText,
                                onValueChange = { taxRateText = it },
                                label = { Text("Default Tax %", color = Color.LightGray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = AccentAzure
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 6.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Auth button
                Button(
                    onClick = {
                        if (isRegisterState) {
                            val taxVal = taxRateText.toDoubleOrNull() ?: 0.0
                            viewModel.registerOwner(
                                email = email,
                                passwordPlain = password,
                                shopName = shopName,
                                location = location,
                                contactNumber = contactNumber,
                                currencySymbol = curSymbol,
                                taxRate = taxVal
                            )
                        } else {
                            viewModel.login(email = email, passwordPlain = password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAzure),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isRegisterState) "Create Account & Start" else "Secure Terminal Login",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Auth state links
                TextButton(
                    onClick = {
                        isRegisterState = !isRegisterState
                        viewModel.clearAuthError()
                    },
                    modifier = Modifier.testTag("toggle_auth_link")
                ) {
                    Text(
                        text = if (isRegisterState) "Already have business ID? Sign In" else "New Shop? Register store terminal",
                        color = AccentAzure,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun NewBillScreen(viewModel: BillingViewModel) {
    val context = LocalContext.current
    var itemName by remember { mutableStateOf("") }
    var itemPriceText by remember { mutableStateOf("") }
    var itemQuantityText by remember { mutableStateOf("") }

    val customerName by viewModel.customerName.collectAsStateWithLifecycle()
    val customerContact by viewModel.customerContact.collectAsStateWithLifecycle()
    val activeCart by viewModel.activeCart.collectAsStateWithLifecycle()
    val owner by viewModel.currentUser.collectAsStateWithLifecycle()

    val subtotal by viewModel.subtotal.collectAsStateWithLifecycle()
    val taxAmount by viewModel.taxAmount.collectAsStateWithLifecycle()
    val grandTotal by viewModel.grandTotal.collectAsStateWithLifecycle()
    val currency = owner?.currencySymbol ?: "$"

    val message by viewModel.pdfGenerationMessage.collectAsStateWithLifecycle()
    
    // Auto-display Success / Alert status message as Toast
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            // Checkout drawer bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .shadow(12.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(16.dp)
            ) {
                // Checkout Calculations summary
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", color = Color.Gray, fontSize = 13.sp)
                    Text(String.format(Locale.getDefault(), "$currency %.2f", subtotal), fontWeight = FontWeight.SemiBold, color = PrimaryNavy)
                }
                
                if (taxAmount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax (${owner?.taxRate ?: 0.0}%)", color = Color.Gray, fontSize = 13.sp)
                        Text(String.format(Locale.getDefault(), "$currency %.2f", taxAmount), fontWeight = FontWeight.SemiBold, color = PrimaryNavy)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL DUE", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryNavy)
                    Text(
                        text = String.format(Locale.getDefault(), "$currency %.2f", grandTotal),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = AccentAzure
                    )
                }

                // Generates Invoice actions
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.generateInvoice(paymentStatus = "PENDING") { generatedPdf ->
                                viewInvoicePdf(context, generatedPdf)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .height(48.dp)
                            .testTag("checkout_pending_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD8D8),
                            contentColor = Color(0xFF8C0000)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = "Pending status", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invoice Unpaid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.generateInvoice(paymentStatus = "PAID") { generatedPdf ->
                                viewInvoicePdf(context, generatedPdf)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .height(48.dp)
                            .testTag("checkout_paid_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC2F0C2),
                            contentColor = Color(0xFF005300)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Paid status", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundSlate)
                .padding(16.dp)
        ) {
            
            // 1. Customer meta Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AccentAzure, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Customer Receipt Info", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { viewModel.setCustomerDetails(it, customerContact) },
                            label = { Text("Customer Name (Walk-in standard)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_name_input")
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customerContact,
                            onValueChange = { viewModel.setCustomerDetails(customerName, it) },
                            label = { Text("Customer Phone Contact", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_contact_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Manual Custom Item Addition Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = AccentAzure, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Line Items", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Item Name", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_item_name_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = itemPriceText,
                                onValueChange = { itemPriceText = it },
                                label = { Text("Unit Price", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp)
                                    .testTag("add_item_price_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                prefix = { Text(currency + " ") }
                            )

                            OutlinedTextField(
                                value = itemQuantityText,
                                onValueChange = { itemQuantityText = it },
                                label = { Text("Quantity", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .padding(start = 4.dp)
                                    .testTag("add_item_qty_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val priceVal = itemPriceText.toDoubleOrNull() ?: 0.0
                                val qtyVal = itemQuantityText.toIntOrNull() ?: 1
                                if (itemName.isNotBlank() && priceVal > 0) {
                                    viewModel.addItemToCart(itemName, priceVal, qtyVal)
                                    // Reset manual inputs
                                    itemName = ""
                                    itemPriceText = ""
                                    itemQuantityText = ""
                                    Toast.makeText(context, "Added to active bill!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter valid item name & price", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_item_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add item")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Append Item to Bill", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Basket Overview Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ITEMS LIST IN BILL (${activeCart.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    if (activeCart.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }) {
                            Text("Clear All", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Basket Items
            if (activeCart.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Empty Basket",
                                tint = Color.LightGray,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Invoice is Empty",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                            Text(
                                text = "Scan a barcode or manually add items above to build a professional invoice.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(activeCart) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PrimaryNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "$currency %.2f each", item.price),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            // Dynamic Quantity Counter Incrementor
                            Row(
                                modifier = Modifier
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val decrementorTag = "decrement_$index"
                                val incrementorTag = "increment_$index"
                                IconButton(
                                    onClick = { viewModel.updateCartItemQuantity(index, item.quantity - 1) },
                                    modifier = Modifier.size(24.dp).testTag(decrementorTag)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                                }
                                
                                Text(
                                    text = item.quantity.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.updateCartItemQuantity(index, item.quantity + 1) },
                                    modifier = Modifier.size(24.dp).testTag(incrementorTag)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Action Item Total Price
                            Text(
                                text = String.format(Locale.getDefault(), "$currency %.2f", item.totalPrice),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryNavy,
                                modifier = Modifier.widthIn(min = 60.dp),
                                textAlign = TextAlign.End
                            )

                            // Swipe delete trigger button
                            IconButton(
                                onClick = { viewModel.removeCartItem(index) },
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(32.dp)
                                    .testTag("delete_item_$index")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete from layout", tint = Color.Red.copy(0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealScannerScreen(viewModel: BillingViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold { innerP ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerP)
                .background(DarkBackgroundSlate)
        ) {
            // Viewfinder panel
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
            ) {
                if (hasCameraPermission) {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
                    
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val executor = ContextCompat.getMainExecutor(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().apply {
                                    setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview
                                    )
                                } catch (exc: Exception) {
                                    Toast.makeText(context, "Camera preview display failed", Toast.LENGTH_SHORT).show()
                                }
                            }, executor)
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Laser beam overlay animation
                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                    val laserOffset by infiniteTransition.animateFloat(
                        initialValue = 0.1f,
                        targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laser_y"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        // Draw semi transparent overlay
                        val bracketColor = Color(0xFF4CAF50)
                        
                        // Sliding green scanning laser
                        val laserSpeedY = height * laserOffset
                        drawLine(
                            color = bracketColor,
                            start = Offset(width * 0.15f, laserSpeedY),
                            end = Offset(width * 0.85f, laserSpeedY),
                            strokeWidth = 3f
                        )
                    }

                    // Scan Reticle bracket
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(240.dp)
                            .border(2.dp, Color(0xFF4CAF50).copy(0.8f), RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = "ALIGNED BARCODE / QR HERE",
                            color = Color.Green,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    // No permission fallback screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Live Viewfinder Blocked", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text(
                            text = "Camera access permission is required to scan QR codes and physical product tags.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAzure)
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Interactive simulation and manual barcode lookup
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                var barcodeInputText by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentAzure, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual Barcode / QR Decode", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                }

                // Barcode search box
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = barcodeInputText,
                        onValueChange = { barcodeInputText = it },
                        label = { Text("Enter Barcode / QR ID (e.g., 890101)", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("barcode_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (barcodeInputText.isNotBlank()) {
                                val resultMsg = viewModel.scanBarcode(barcodeInputText)
                                if (resultMsg != null) {
                                    Toast.makeText(context, resultMsg, Toast.LENGTH_SHORT).show()
                                    barcodeInputText = ""
                                } else {
                                    Toast.makeText(context, "Barcode ID not found in presets! Try 890101, 890202", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("barcode_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Decode", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                // Hot Presets simulation list
                Text(
                    text = "SIMULATE INTEGRATED BILLING BARCODES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    val presetItems = viewModel.mockProducts.entries.toList()
                    items(presetItems) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.scanBarcode(entry.key)
                                    Toast.makeText(context, "Scanned: ${entry.value.name} $${entry.value.price}", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(containerColor = BackgroundSlate),
                            border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = "Code",
                                        tint = AccentAzure,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(entry.value.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                                        Text("Scanner code: ${entry.key}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "$ %.2f", entry.value.price),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AccentAzure
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: BillingViewModel) {
    val context = LocalContext.current
    val invoices by viewModel.invoicesList.collectAsStateWithLifecycle()
    val owner by viewModel.currentUser.collectAsStateWithLifecycle()
    val currency = owner?.currencySymbol ?: "$"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Invoicing History", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PrimaryNavy)
                Text("Review and sync billing payment status offline", fontSize = 11.sp, color = Color.Gray)
            }
            Box(
                modifier = Modifier
                    .background(AccentAzure.copy(0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("${invoices.size} Bills", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentAzure)
            }
        }

        if (invoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Empty",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("No billing receipts found", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                    Text(
                        text = "Every transaction and PDF invoice generated on this terminal will be compiled chronologically here.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(invoices) { invoice ->
                    InvoiceHistoryRow(
                        invoice = invoice,
                        currency = currency,
                        onUpdateStatus = { newStat ->
                            viewModel.updateInvoicePaymentStatus(invoice.id, newStat)
                        },
                        onDelete = {
                            viewModel.deleteHistoricalInvoice(invoice.id)
                        },
                        onShare = {
                            viewModel.regeneratePreviousPdf(invoice, context) { pdf ->
                                shareInvoicePdf(context, pdf)
                            }
                        },
                        onView = {
                            viewModel.regeneratePreviousPdf(invoice, context) { pdf ->
                                viewInvoicePdf(context, pdf)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceHistoryRow(
    invoice: Invoice,
    currency: String,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onView: () -> Unit
) {
    val dateSdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val formattedTime = dateSdf.format(Date(invoice.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Invoice header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(invoice.invoiceNumber, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PrimaryNavy)
                    Text(formattedTime, fontSize = 11.sp, color = Color.Gray)
                }

                // Dynamic Action Toggle Badge
                val (badgeColor, textColor) = when (invoice.paymentStatus.uppercase(Locale.ROOT)) {
                    "PAID" -> Color(0xFFC2F0C2) to Color(0xFF005300)
                    "PENDING" -> Color(0xFFFFD8D8) to Color(0xFF8C0000)
                    else -> Color(0xFFF3EDF7) to Color(0xFF49454F)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .clickable {
                            val nextStatus = when (invoice.paymentStatus.uppercase(Locale.ROOT)) {
                                "PENDING" -> "PAID"
                                "PAID" -> "REFUNDED"
                                else -> "PENDING"
                            }
                            onUpdateStatus(nextStatus)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = invoice.paymentStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Customer details & pricing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Customer: ${invoice.customerName}", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = PrimaryNavy)
                    Text("Total items: ${invoice.items.sumOf { it.quantity }}", fontSize = 11.sp, color = Color.Gray)
                }

                Text(
                    text = String.format(Locale.getDefault(), "$currency %.2f", invoice.grandTotal),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = PrimaryNavy
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom drawer control sheet triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap status badge to change state",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onView,
                    modifier = Modifier.size(34.dp).testTag("view_pdf_${invoice.id}")
                ) {
                    Icon(Icons.Default.RemoveRedEye, contentDescription = "Open and View Invoice PDF", tint = AccentAzure, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(34.dp).testTag("share_pdf_${invoice.id}")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share PDF invoice", tint = AccentAzure, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp).testTag("delete_invoice_${invoice.id}")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Purge invoice record", tint = Color.Red.copy(0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: BillingViewModel) {
    val context = LocalContext.current
    val owner by viewModel.currentUser.collectAsStateWithLifecycle()

    var shopName by remember { mutableStateOf(owner?.shopName ?: "") }
    var location by remember { mutableStateOf(owner?.location ?: "") }
    var contactNumber by remember { mutableStateOf(owner?.contactNumber ?: "") }
    var currencySymbol by remember { mutableStateOf(owner?.currencySymbol ?: "$") }
    var taxRateText by remember { mutableStateOf(owner?.taxRate?.toString() ?: "0.0") }
    var additionalNotes by remember { mutableStateOf(owner?.additionalNotes ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .padding(16.dp)
    ) {
        
        // Settings title
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = AccentAzure, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Store Profile Configurations", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryNavy)
                    Text("These details will automatically print on generated PDF invoices", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Configuration Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Shop/Enterprise name", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_shop_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Corporate Location / Address Line", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        label = { Text("Shop Contact Number", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("Currency symbol", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )

                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text("Tax Rate (%)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = { Text("Bottom Terms / Footer Notes on PDF", fontSize = 12.sp) },
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val rate = taxRateText.toDoubleOrNull() ?: 0.0
                            if (shopName.isNotBlank()) {
                                viewModel.updateProfile(
                                    shopName = shopName,
                                    location = location,
                                    contactNumber = contactNumber,
                                    currencySymbol = currencySymbol,
                                    taxRate = rate,
                                    additionalNotes = additionalNotes
                                )
                                Toast.makeText(context, "Configurations saved securely!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Shop Name cannot be empty!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save settings")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile Settings", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
