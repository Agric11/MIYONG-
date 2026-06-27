package com.example.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.ClientEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.InvoiceItem
import com.example.data.entity.ProductEntity
import com.example.ui.InvoiceViewModel
import com.example.ui.Screen
import com.example.ui.components.SignaturePad
import com.example.ui.theme.CorporateBlue
import com.example.ui.theme.DarkGray
import com.example.ui.theme.LightBlueBg
import com.example.ui.theme.TealAccent
import com.example.util.InvoicePdfGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceAppUi(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen = viewModel.currentScreen

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Free Version Unobtrusive Banner Ad Mock (very clean and helpful)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE5E7EB))
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ad Banner",
                        tint = CorporateBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Iklan: Liyca Pro - Hilangkan iklan & simpan PDF di cloud secara otomatis!",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel)
                Screen.InvoiceBuilder -> InvoiceBuilderScreen(viewModel)
                Screen.Settings -> SettingsScreen(viewModel)
                Screen.ClientManager -> ClientManagerScreen(viewModel)
                Screen.ProductManager -> ProductManagerScreen(viewModel)
            }
        }
    }
}

// ----------------------------------------------------
// A. DASHBOARD & QUICK ACTION (Home Screen)
// ----------------------------------------------------
@Composable
fun DashboardScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val recentInvoices by viewModel.recentInvoices.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val unpaidCount by viewModel.unpaidCount.collectAsStateWithLifecycle()

    // Calculate Dynamic Piutang (Total amount of unpaid invoices)
    val totalPiutang = remember(allInvoices) {
        allInvoices.filter { !it.isPaid }.sumOf { inv ->
            val items = InvoicePdfGenerator.parseItemsJson(inv.itemsJson)
            items.sumOf { item ->
                val sub = item.quantity * item.price
                val disc = sub * (item.discountPercent / 100.0)
                val net = sub - disc
                val tax = if (item.hasPpn) net * 0.11 else 0.0
                net + tax
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Quick Settings Gear
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Halo!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = CorporateBlue
                    )
                    Text(
                        text = "Kelola tagihan bisnis Anda dengan mudah",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Settings) },
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = CorporateBlue
                    )
                }
            }
        }

        // Summary Widget Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CorporateBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Piutang Hari Ini",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = InvoicePdfGenerator.formatRupiah(totalPiutang),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Unpaid Count",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Invoice Belum Lunas:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "$unpaidCount Invoice",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Prominent Buat Invoice Floating / Action Widget
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.startNewInvoice() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CorporateBlue.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(LightBlueBg, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Buat Invoice",
                                tint = CorporateBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Buat Invoice Baru",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGray
                            )
                            Text(
                                text = "Isi ledger, logo, dan kirim instan",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Next",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Quick Management Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(Screen.ClientManager) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.People, "Klien", tint = TealAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kelola Klien", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Simpan data profil klien", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(Screen.ProductManager) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Inventory, "Produk", tint = CorporateBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Katalog Produk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Simpan daftar barang/jasa", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Recent Invoices Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invoice Terbaru",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGray
                )
                Text(
                    text = "Total: ${allInvoices.size}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        // Scrollable Recent Invoices (top 3)
        if (recentInvoices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Empty",
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada Invoice", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("Ketuk tombol di atas untuk membuat", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(recentInvoices) { invoice ->
                InvoiceRowItem(invoice, viewModel)
            }
        }
    }
}

@Composable
fun InvoiceRowItem(invoice: InvoiceEntity, viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val items = remember(invoice.itemsJson) { InvoicePdfGenerator.parseItemsJson(invoice.itemsJson) }
    val total = remember(items) {
        items.sumOf { item ->
            val sub = item.quantity * item.price
            val disc = sub * (item.discountPercent / 100.0)
            val net = sub - disc
            val tax = if (item.hasPpn) net * 0.11 else 0.0
            net + tax
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.editInvoice(invoice) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CorporateBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Paid / Overdue Badge
                val badgeColor = if (invoice.isPaid) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                val badgeTextColor = if (invoice.isPaid) Color(0xFF065F46) else Color(0xFF991B1B)
                val badgeText = if (invoice.isPaid) "Paid" else "Unpaid"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Klien: ${invoice.clientName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkGray
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val format = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))
                val dateStr = format.format(Date(invoice.createdAt))
                Text(
                    text = "Tanggal: $dateStr",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = InvoicePdfGenerator.formatRupiah(total),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGray
                )
            }

            HorizontalDivider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Delete Button
                IconButton(onClick = { viewModel.deleteInvoice(invoice) }) {
                    Icon(Icons.Default.Delete, "Hapus", tint = Color.Red.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Quick Share WhatsApp Button
                Button(
                    onClick = {
                        // Generate PDF dynamically and share
                        val pdfDoc = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
                        val cacheFile = File(context.cacheDir, "Liyca_Invoice_${invoice.invoiceNumber}.pdf")
                        try {
                            FileOutputStream(cacheFile).use { out ->
                                pdfDoc.writeTo(out)
                            }
                            sharePdfViaWhatsApp(context, cacheFile)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kirim WA", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

// ----------------------------------------------------
// B. MINIMALIST SINGLE-PAGE INVOICE BUILDER (Vertically Stacked Cards)
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvoiceBuilderScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showClientSelectDialog by remember { mutableStateOf(false) }

    // Pick business logo
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = viewModel.saveUriToLocalFiles(context, it, "logo")
            viewModel.businessLogoPath = localPath
        }
    }

    // Pick company stamp
    val stampPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = viewModel.saveUriToLocalFiles(context, it, "stamp")
            viewModel.companyStampPath = localPath
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        // Upper Navigation Title Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = CorporateBlue)
                    }
                    Text(
                        text = if (viewModel.editingInvoiceId == null) "Buat Invoice Baru" else "Edit Invoice",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    )
                }

                Button(
                    onClick = { viewModel.saveInvoice() },
                    colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue)
                ) {
                    Icon(Icons.Default.Save, "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Details
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Info Tagihan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.invoiceNumber,
                            onValueChange = { viewModel.invoiceNumber = it },
                            label = { Text("No. Invoice") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var isPaidVal by remember { mutableStateOf(viewModel.isPaid) }
                            // Mark Paid Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.isPaid = !viewModel.isPaid
                                    }
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Checkbox(
                                    checked = viewModel.isPaid,
                                    onCheckedChange = { viewModel.isPaid = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lunas (Paid)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Card 1 (Brand Profile)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Profil Bisnis Anda", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Logo picker display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { logoPickerLauncher.launch("image/*") }
                                .border(1.dp, CorporateBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (viewModel.businessLogoPath != null) {
                                AsyncImage(
                                    model = viewModel.businessLogoPath,
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(LightBlueBg, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, "Logo Logo", tint = CorporateBlue)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Logo Bisnis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Ketuk untuk unggah logo", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.businessName,
                            onValueChange = { viewModel.businessName = it },
                            label = { Text("Nama Bisnis") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.businessAddress,
                            onValueChange = { viewModel.businessAddress = it },
                            label = { Text("Alamat") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }

            // Card 2 (Client Profile)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Profil Klien", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                            TextButton(onClick = { showClientSelectDialog = true }) {
                                Icon(Icons.Default.Search, "Cari", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pilih Klien", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = viewModel.clientName,
                            onValueChange = { viewModel.clientName = it },
                            label = { Text("Nama Klien") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.clientContact,
                            onValueChange = { viewModel.clientContact = it },
                            label = { Text("Detail Kontak (e.g. Email / WhatsApp)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Card 3 (Itemized Ledger)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daftar Barang / Ledger", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                            Button(
                                onClick = { showAddItemDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                            ) {
                                Icon(Icons.Default.AddCircle, "Tambah", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Barang", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (viewModel.invoiceItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada barang ditambahkan.", fontSize = 13.sp, color = Color.Gray)
                            }
                        } else {
                            viewModel.invoiceItems.forEachIndexed { index, item ->
                                val sub = item.quantity * item.price
                                val disc = sub * (item.discountPercent / 100.0)
                                val net = sub - disc
                                val tax = if (item.hasPpn) net * 0.11 else 0.0
                                val total = net + tax

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkGray)
                                        Text(
                                            "${item.quantity.toString().replace(".0", "")} x ${InvoicePdfGenerator.formatRupiah(item.price)} (${item.discountPercent}% off)",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        if (item.hasPpn) {
                                            Text("Termasuk PPN 11%", fontSize = 11.sp, color = TealAccent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = InvoicePdfGenerator.formatRupiah(total),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DarkGray,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(onClick = { viewModel.removeInvoiceItem(index) }) {
                                            Icon(Icons.Default.Close, "Hapus", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 4 (Payment Instructions)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.paymentInstructions,
                            onValueChange = { viewModel.paymentInstructions = it },
                            label = { Text("Petunjuk / Rekening Penerima") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }

            // Card 5 (Authorization/Sign)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Otorisasi & Tanda Tangan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Company Stamp
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { stampPickerLauncher.launch("image/*") }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (viewModel.companyStampPath != null) {
                                    AsyncImage(
                                        model = viewModel.companyStampPath,
                                        contentDescription = "Stamp",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(Icons.Default.UploadFile, "Upload Stamp", tint = CorporateBlue, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Stempel Perusahaan", fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    Text("Format PNG/JPG", fontSize = 9.sp, color = Color.Gray)
                                }
                            }

                            // Hand Signature
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { showSignatureDialog = true }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (viewModel.signaturePath != null) {
                                    AsyncImage(
                                        model = viewModel.signaturePath,
                                        contentDescription = "Signature",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(Icons.Default.Gesture, "Draw Signature", tint = CorporateBlue, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tanda Tangan", fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    Text("Gambarkan langsung", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Live Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 13.sp, color = DarkGray)
                            Text(InvoicePdfGenerator.formatRupiah(viewModel.subtotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Diskon", fontSize = 13.sp, color = DarkGray)
                            Text("- " + InvoicePdfGenerator.formatRupiah(viewModel.totalDiscount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Pajak (PPN 11%)", fontSize = 13.sp, color = DarkGray)
                            Text(InvoicePdfGenerator.formatRupiah(viewModel.totalTax), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = CorporateBlue.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Akhir Tagihan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CorporateBlue)
                            Text(InvoicePdfGenerator.formatRupiah(viewModel.totalBill), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorporateBlue)
                        }
                    }
                }
            }
        }

        // Compliant PDF Engine & WhatsApp Sharing Floating Row Actions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PDF Download Action (Zero runtime permissions required via MediaStore)
                Button(
                    onClick = {
                        val pdfDoc = viewModel.generateInvoicePdf(context)
                        val fileName = "Liyca_Invoice_${viewModel.invoiceNumber}.pdf"
                        val uri = savePdfToDownloads(context, pdfDoc, fileName)
                        if (uri != null) {
                            Toast.makeText(context, "PDF berhasil disimpan di folder Downloads!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Gagal mengunduh PDF.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, "Download", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unduh PDF", color = Color.White, fontSize = 14.sp)
                }

                // WhatsApp Sharing Action (Bypass sharesheet directly)
                Button(
                    onClick = {
                        val pdfDoc = viewModel.generateInvoicePdf(context)
                        val cacheFile = File(context.cacheDir, "Liyca_Invoice_${viewModel.invoiceNumber}.pdf")
                        try {
                            FileOutputStream(cacheFile).use { out ->
                                pdfDoc.writeTo(out)
                            }
                            sharePdfViaWhatsApp(context, cacheFile)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, "WhatsApp", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kirim WA", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    // ----------------------------------------------------
    // POPUPS & DIALOG MODALS
    // ----------------------------------------------------

    // Signature Pad Modal
    if (showSignatureDialog) {
        Dialog(
            onDismissRequest = { showSignatureDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                SignaturePad(
                    modifier = Modifier.fillMaxWidth(),
                    onSignatureSaved = { bitmap ->
                        val localPath = viewModel.saveBitmapToLocalFiles(bitmap, "sig")
                        viewModel.signaturePath = localPath
                        showSignatureDialog = false
                    },
                    onDismiss = { showSignatureDialog = false }
                )
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddItemDialog(
            viewModel = viewModel,
            onDismiss = { showAddItemDialog = false },
            onItemAdded = { item ->
                viewModel.addInvoiceItem(item)
                showAddItemDialog = false
            }
        )
    }

    // Select Reusable Client Dialog
    if (showClientSelectDialog) {
        ClientSelectDialog(
            viewModel = viewModel,
            onDismiss = { showClientSelectDialog = false },
            onClientSelected = { client ->
                viewModel.clientName = client.name
                viewModel.clientContact = client.contact
                showClientSelectDialog = false
            }
        )
    }
}

// ----------------------------------------------------
// POPUP: Add Ledger Item Dialog
// ----------------------------------------------------
@Composable
fun AddItemDialog(
    viewModel: InvoiceViewModel,
    onDismiss: () -> Unit,
    onItemAdded: (InvoiceItem) -> Unit
) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    var discountPercent by remember { mutableStateOf("0") }
    var hasPpn by remember { mutableStateOf(false) }

    var expandedProductSelect by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tambah Item Ledger", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CorporateBlue)
                    if (products.isNotEmpty()) {
                        TextButton(onClick = { expandedProductSelect = true }) {
                            Text("Impor Katalog", fontSize = 12.sp, color = TealAccent)
                        }
                    }
                }

                // Dropdown of products if available
                if (expandedProductSelect) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Pilih Produk Catalog:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                            products.forEach { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = prod.name
                                            description = prod.description
                                            price = prod.price.toInt().toString()
                                            discountPercent = prod.discountPercent.toInt().toString()
                                            hasPpn = prod.hasPpn
                                            expandedProductSelect = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(prod.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(InvoicePdfGenerator.formatRupiah(prod.price), fontSize = 12.sp, color = CorporateBlue)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Barang/Jasa") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Singkat") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Harga (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(2f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = discountPercent,
                    onValueChange = { discountPercent = it },
                    label = { Text("Potongan Harga (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // PPN Checklist
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hasPpn = !hasPpn }
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Checkbox(checked = hasPpn, onCheckedChange = { hasPpn = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terapkan PPN 11%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && price.isNotEmpty()) {
                                onItemAdded(
                                    InvoiceItem(
                                        name = name,
                                        description = description,
                                        quantity = quantity.toDoubleOrNull() ?: 1.0,
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        discountPercent = discountPercent.toDoubleOrNull() ?: 0.0,
                                        hasPpn = hasPpn
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue),
                        enabled = name.isNotEmpty() && price.isNotEmpty()
                    ) {
                        Text("Tambahkan", color = Color.White)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// POPUP: Select Client Dialog
// ----------------------------------------------------
@Composable
fun ClientSelectDialog(
    viewModel: InvoiceViewModel,
    onDismiss: () -> Unit,
    onClientSelected: (ClientEntity) -> Unit
) {
    val clients by viewModel.allClients.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Pilih Klien Tersimpan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CorporateBlue)
                Spacer(modifier = Modifier.height(12.dp))

                if (clients.isEmpty()) {
                    Text("Belum ada data klien tersimpan.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(clients) { client ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClientSelected(client) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(client.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(client.contact, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Batal")
                }
            }
        }
    }
}

// ----------------------------------------------------
// E. SETTINGS, BACKUP & RESTORE MODULE (The Safety Net)
// ----------------------------------------------------
@Composable
fun SettingsScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val settings by viewModel.allSettings.collectAsStateWithLifecycle()

    var defaultBizName by remember { mutableStateOf("") }
    var defaultBizAddr by remember { mutableStateOf("") }
    var defaultPayment by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    // Synchronize states
    LaunchedEffect(settings) {
        defaultBizName = settings.find { it.key == "default_business_name" }?.value ?: ""
        defaultBizAddr = settings.find { it.key == "default_business_address" }?.value ?: ""
        defaultPayment = settings.find { it.key == "default_payment_method" }?.value ?: ""
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper back title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = CorporateBlue)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pengaturan default", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorporateBlue)
            }
        }

        // Card 1: Default Profiles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Profil Bisnis Default", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = defaultBizName,
                        onValueChange = { defaultBizName = it },
                        label = { Text("Nama Bisnis Default") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = defaultBizAddr,
                        onValueChange = { defaultBizAddr = it },
                        label = { Text("Alamat Default") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        }

        // Card 2: Default Payments
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rekening Pembayaran Default", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = defaultPayment,
                        onValueChange = { defaultPayment = it },
                        label = { Text("Metode Transfer / No Rekening") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        }

        // Save Defaults
        item {
            Button(
                onClick = {
                    viewModel.saveSettings(defaultBizName, defaultBizAddr, defaultPayment)
                    Toast.makeText(context, "Profil default berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    viewModel.navigateTo(Screen.Dashboard)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Konfigurasi", color = Color.White)
            }
        }

        // Card 3: Offline Backup & Restore Safety Net
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data Cadangan (Backup / Restore)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pindahkan atau amankan data lokal Anda secara penuh.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Export Action
                        Button(
                            onClick = {
                                viewModel.exportBackup { jsonString ->
                                    val backupFile = File(context.cacheDir, "liyca_invoice_backup.json")
                                    try {
                                        FileOutputStream(backupFile).use { out ->
                                            out.write(jsonString.toByteArray())
                                        }
                                        shareBackupFile(context, backupFile)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal membuat berkas cadangan.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, "Ekspor")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor", color = Color.White)
                        }

                        // Import Action
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, "Impor")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Import Backup Text Input Dialog
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Impor Data Cadangan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CorporateBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tempel teks JSON dari file cadangan Liyca Anda di bawah ini:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Backup JSON String") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showImportDialog = false }, modifier = Modifier.padding(end = 8.dp)) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (importJsonText.isNotEmpty()) {
                                    viewModel.importBackup(importJsonText) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Data berhasil dipulihkan!", Toast.LENGTH_LONG).show()
                                            showImportDialog = false
                                            viewModel.navigateTo(Screen.Dashboard)
                                        } else {
                                            Toast.makeText(context, "Format cadangan salah.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue),
                            enabled = importJsonText.isNotEmpty()
                        ) {
                            Text("Impor Sekarang", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN: Client Profile Manager
// ----------------------------------------------------
@Composable
fun ClientManagerScreen(viewModel: InvoiceViewModel) {
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = CorporateBlue)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kelola Klien", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorporateBlue)
            }
        }

        // Add client card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tambah Klien Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lengkap Klien") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Detail Kontak (Email / No Telp)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                viewModel.saveClient(ClientEntity(name = name, contact = contact))
                                name = ""
                                contact = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotEmpty()
                    ) {
                        Text("Simpan Klien", color = Color.White)
                    }
                }
            }
        }

        // Saved clients header
        item {
            Text("Klien Tersimpan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkGray)
        }

        if (clients.isEmpty()) {
            item {
                Text("Belum ada klien tersimpan.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        } else {
            items(clients) { client ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(client.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkGray)
                            Text(client.contact, fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { viewModel.deleteClient(client) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN: Product Catalog Manager
// ----------------------------------------------------
@Composable
fun ProductManagerScreen(viewModel: InvoiceViewModel) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var discountPercent by remember { mutableStateOf("0") }
    var hasPpn by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = CorporateBlue)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Katalog Produk", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorporateBlue)
            }
        }

        // Add Product Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tambah Item Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorporateBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Barang/Jasa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Harga default (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountPercent,
                            onValueChange = { discountPercent = it },
                            label = { Text("Potongan default (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasPpn = !hasPpn }
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Checkbox(checked = hasPpn, onCheckedChange = { hasPpn = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Terapkan PPN 11% secara default", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && price.isNotEmpty()) {
                                viewModel.saveProduct(
                                    ProductEntity(
                                        name = name,
                                        description = description,
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        discountPercent = discountPercent.toDoubleOrNull() ?: 0.0,
                                        hasPpn = hasPpn
                                    )
                                )
                                name = ""
                                description = ""
                                price = ""
                                discountPercent = "0"
                                hasPpn = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotEmpty() && price.isNotEmpty()
                    ) {
                        Text("Simpan ke Katalog", color = Color.White)
                    }
                }
            }
        }

        // Saved Products list header
        item {
            Text("Daftar Katalog Tersimpan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkGray)
        }

        if (products.isEmpty()) {
            item {
                Text("Belum ada produk tersimpan.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        } else {
            items(products) { prod ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkGray)
                            if (prod.description.isNotEmpty()) {
                                Text(prod.description, fontSize = 12.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Text(InvoicePdfGenerator.formatRupiah(prod.price), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CorporateBlue)
                                if (prod.discountPercent > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Potongan: ${prod.discountPercent}%", fontSize = 11.sp, color = TealAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.deleteProduct(prod) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SHARING INTENTS & FILE PERSISTENCE HELPERS
// ----------------------------------------------------

fun sharePdfViaWhatsApp(context: Context, pdfFile: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Query PackageManager to see if WhatsApp packages are available
        val pm = context.packageManager
        var targetPackage = ""
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            try {
                pm.getPackageInfo(pkg, 0)
                targetPackage = pkg
                break
            } catch (e: Exception) {
                // package not found, ignore
            }
        }

        if (targetPackage.isNotEmpty()) {
            intent.setPackage(targetPackage)
            context.startActivity(intent)
        } else {
            // Fallback to standard chooser
            val chooser = Intent.createChooser(intent, "Kirim Invoice via")
            context.startActivity(chooser)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareBackupFile(context: Context, backupFile: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, backupFile)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Simpan atau Bagikan Cadangan")
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan cadangan: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// MediaStore API helper to write to public Downloads directory without runtime permissions
fun savePdfToDownloads(context: Context, pdfDocument: PdfDocument, fileName: String): Uri? {
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
    }
    val resolver = context.contentResolver
    val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    } else {
        // Fallback for older SDKs
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            return Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    return uri
}
