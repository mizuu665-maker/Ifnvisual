package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    // Authentication States
    var currentUser by mutableStateOf<User?>(null)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    // Notification Trigger for UI (Simulation of Firebase Push Notification)
    private val _pushNotificationFlow = MutableSharedFlow<Pair<String, String>>()
    val pushNotificationFlow = _pushNotificationFlow.asSharedFlow()

    // Dashboard Statistics Flow
    val totalOrdersCount = repository.allOrders.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeOrdersCount = repository.activeOrdersCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedOrdersCount = repository.completedOrdersCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthlyIncome = repository.allPayments.map { payments ->
        payments.filter { it.status == "Pembayaran Diterima" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Data lists for the screens
    val allProducts = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Initialize database seeding
    init {
        viewModelScope.launch {
            repository.seedDatabaseIfNeeded()
        }
    }

    // Helper to filter orders for currently logged in customer
    fun getCustomerOrders(): Flow<List<Order>> {
        val email = currentUser?.email ?: ""
        return repository.getOrdersByCustomer(email)
    }

    // Helper to get notifications for current user
    fun getCustomerNotifications(): Flow<List<Notification>> {
        val email = currentUser?.email ?: ""
        return repository.getNotificationsByUser(email)
    }

    // Helper to get tracking logs for a specific order
    fun getTrackingByOrder(orderId: Int): Flow<List<ProgressTracking>> {
        return repository.getTrackingByOrder(orderId)
    }

    // AUTH ACTIONS
    fun login(emailInput: String, passwordInput: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val trimmedEmail = emailInput.trim().lowercase()
            if (trimmedEmail.isEmpty() || passwordInput.isEmpty()) {
                onError("Email dan Password tidak boleh kosong!")
                return@launch
            }

            // Simple user validation
            val user = repository.getUserByEmail(trimmedEmail)
            if (user != null) {
                currentUser = user
                isLoggedIn = true
                onSuccess()
            } else {
                // If it's a new email, auto-create a Customer profile for user testing convenience, or throw error
                // Let's create a Customer account automatically if not registered yet!
                if (trimmedEmail == "admin@ifn.com") {
                    onError("Email atau password admin salah!")
                } else {
                    val newUser = User(
                        email = trimmedEmail,
                        fullName = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                        whatsappNumber = "081234567890",
                        profileRole = "CUSTOMER"
                    )
                    repository.insertUser(newUser)
                    currentUser = newUser
                    isLoggedIn = true
                    onSuccess()
                }
            }
        }
    }

    fun loginWithGoogle(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // Simulate single-tap google sign-in with customer@ifn.com or Google User
            val googleEmail = "mizuu665@gmail.com" // From user metadata or custom
            var user = repository.getUserByEmail(googleEmail)
            if (user == null) {
                user = User(
                    email = googleEmail,
                    fullName = "Mizuu IFN Visual Guest",
                    whatsappNumber = "085712345678",
                    profileRole = "CUSTOMER",
                    isGoogleLogin = true
                )
                repository.insertUser(user)
            }
            currentUser = user
            isLoggedIn = true
            triggerInAppNotification("Login Berhasil", "Selamat datang kembali via Google, ${user.fullName}!")
            onSuccess()
        }
    }

    fun register(emailInput: String, fullName: String, whatsappNumber: String, role: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val trimmedEmail = emailInput.trim().lowercase()
            if (trimmedEmail.isEmpty() || fullName.isEmpty() || whatsappNumber.isEmpty()) {
                onError("Semua kolom harus diisi!")
                return@launch
            }

            val existing = repository.getUserByEmail(trimmedEmail)
            if (existing != null) {
                onError("Email sudah terdaftar!")
                return@launch
            }

            val newUser = User(
                email = trimmedEmail,
                fullName = fullName,
                whatsappNumber = whatsappNumber,
                profileRole = role
            )
            repository.insertUser(newUser)
            currentUser = newUser
            isLoggedIn = true
            onSuccess()
        }
    }

    fun forgotPassword(emailInput: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val trimmedEmail = emailInput.trim().lowercase()
            if (trimmedEmail.isEmpty()) {
                onError("Masukkan email Anda!")
                return@launch
            }
            // Simulate sending reset password
            triggerInAppNotification("Reset Password", "Link pemulihan kata sandi telah dikirim ke email: $trimmedEmail")
            onSuccess()
        }
    }

    fun logout() {
        currentUser = null
        isLoggedIn = false
    }

    // PRODUCT ACTIONS
    fun addProduct(name: String, category: String, price: Long, description: String, duration: String, thumbnailUri: String? = null) {
        viewModelScope.launch {
            val newProduct = Product(
                name = name,
                category = category,
                thumbnailResName = "img_ifn_logo",
                thumbnailUri = thumbnailUri,
                price = price,
                description = description,
                duration = duration
            )
            repository.insertProduct(newProduct)
        }
    }

    fun editProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // ORDER ACTIONS (PEMESANAN)
    fun createOrder(
        customerName: String,
        customerWhatsapp: String,
        productType: String,
        detailPesanan: String,
        referenceUri: String?,
        deadline: String,
        catatan: String?,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Generate Order Number: IFN-YYYYMMDD-XXXX
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val randomNum = (1000..9999).random() // or count-based, using a random suffix ensures uniqueness easily
            val orderNumber = "IFN-$dateStr-$randomNum"

            val order = Order(
                orderNumber = orderNumber,
                customerEmail = currentUser?.email ?: "guest@ifn.com",
                customerName = customerName,
                customerWhatsapp = customerWhatsapp,
                productType = productType,
                detailPesanan = detailPesanan,
                referenceUri = referenceUri,
                deadline = deadline,
                catatan = catatan,
                status = "Pesanan Diterima",
                progressPercentage = 0,
                estimasiSelesai = deadline
            )

            val orderId = repository.insertOrder(order).toInt()

            // 1. Insert Initial Progress Tracking
            val tracking = ProgressTracking(
                orderId = orderId,
                orderNumber = orderNumber,
                status = "Pesanan Diterima",
                description = "Pesanan jasa desain Anda telah berhasil dibuat. Silakan lakukan pembayaran pada menu Pembayaran."
            )
            repository.insertTracking(tracking)

            // 2. Insert Client Notification
            val notification = Notification(
                userId = currentUser?.email ?: "guest@ifn.com",
                title = "Pesanan Berhasil Dibuat",
                message = "Pesanan dengan nomor order $orderNumber telah masuk dalam sistem. Silakan konfirmasi pembayaran Anda."
            )
            repository.insertNotification(notification)

            // Trigger Real-Time Notification Banner simulation
            triggerInAppNotification(
                "Pesanan Masuk 🚀",
                "Pesanan baru dengan nomor $orderNumber berhasil diajukan!"
            )

            onSuccess(orderNumber)
        }
    }

    // UPDATE PROGRESS ORDER (ADMIN ACTION)
    fun updateOrderStatus(
        orderId: Int,
        newStatus: String,
        percentage: Int,
        estimasiSelesai: String,
        catatanUpdate: String
    ) {
        viewModelScope.launch {
            val order = repository.getOrderById(orderId) ?: return@launch
            val updatedOrder = order.copy(
                status = newStatus,
                progressPercentage = percentage,
                estimasiSelesai = estimasiSelesai
            )
            repository.updateOrder(updatedOrder)

            // 1. Log progress tracking history
            val tracking = ProgressTracking(
                orderId = orderId,
                orderNumber = order.orderNumber,
                status = newStatus,
                description = catatanUpdate.ifEmpty { "Status pesanan berubah menjadi '$newStatus' ($percentage%)" }
            )
            repository.insertTracking(tracking)

            // 2. Notify the customer
            val notification = Notification(
                userId = order.customerEmail,
                title = "Update Progres Pesanan",
                message = "Pesanan $newStatus: Progres order ${order.orderNumber} saat ini telah mencapai $percentage%."
            )
            repository.insertNotification(notification)

            // Simulate Firebase FCM notification trigger
            _pushNotificationFlow.emit(
                Pair("Update Pesanan ${order.orderNumber}", "Status baru: $newStatus ($percentage%)")
            )
        }
    }

    // PAYMENT ACTIONS (PEMBAYARAN)
    fun submitPayment(
        orderId: Int,
        orderNumber: String,
        paymentMethod: String,
        amount: Long,
        proofUri: String?
    ) {
        viewModelScope.launch {
            val payment = Payment(
                orderId = orderId,
                orderNumber = orderNumber,
                paymentMethod = paymentMethod,
                amount = amount,
                proofUri = proofUri,
                status = "Menunggu Verifikasi"
            )
            repository.insertPayment(payment)

            // Log Tracking
            val tracking = ProgressTracking(
                orderId = orderId,
                orderNumber = orderNumber,
                status = "Menunggu Verifikasi",
                description = "Konfirmasi pembayaran sebesar ${formatRupiah(amount)} via $paymentMethod telah diunggah. Menunggu verifikasi admin."
            )
            repository.insertTracking(tracking)

            // Notification
            val notification = Notification(
                userId = currentUser?.email ?: "guest@ifn.com",
                title = "Bukti Pembayaran Dikirim",
                message = "Bukti transfer sebesar ${formatRupiah(amount)} untuk pesanan $orderNumber sedang diproses verifikasi oleh Admin."
            )
            repository.insertNotification(notification)

            // Trigger In-App notification simulation
            triggerInAppNotification(
                "Pembayaran Berhasil Dikirim 💳",
                "Pembayaran sebesar ${formatRupiah(amount)} untuk order $orderNumber sedang diverifikasi!"
            )
        }
    }

    fun verifyPayment(paymentId: Int, isApproved: Boolean) {
        viewModelScope.launch {
            val payment = allPayments.value.find { it.id == paymentId } ?: return@launch
            val order = repository.getOrderById(payment.orderId) ?: return@launch

            val newPaymentStatus = if (isApproved) "Pembayaran Diterima" else "Pembayaran Ditolak"
            val updatedPayment = payment.copy(status = newPaymentStatus)
            repository.updatePayment(updatedPayment)

            if (isApproved) {
                // Update Order Status to "Sedang Dikerjakan" and progress percentage to 20%
                val updatedOrder = order.copy(
                    status = "Sedang Dikerjakan",
                    progressPercentage = 20
                )
                repository.updateOrder(updatedOrder)

                // Track
                val tracking = ProgressTracking(
                    orderId = payment.orderId,
                    orderNumber = payment.orderNumber,
                    status = "Sedang Dikerjakan",
                    description = "Pembayaran telah terverifikasi sukses! Tim desainer IFN_VISUAL mulai memproses pesanan Anda."
                )
                repository.insertTracking(tracking)

                // Notification
                val notification = Notification(
                    userId = order.customerEmail,
                    title = "Pembayaran Terverifikasi Selesai",
                    message = "Pembayaran untuk order ${payment.orderNumber} disetujui. Pesanan Anda mulai dikerjakan!"
                )
                repository.insertNotification(notification)

                _pushNotificationFlow.emit(
                    Pair("Pembayaran Diterima", "Pembayaran untuk order ${payment.orderNumber} telah sukses terverifikasi.")
                )
            } else {
                // Track rejection
                val tracking = ProgressTracking(
                    orderId = payment.orderId,
                    orderNumber = payment.orderNumber,
                    status = "Pembayaran Ditolak",
                    description = "Bukti transfer ditolak oleh Admin. Silakan periksa kembali bukti transfer Anda atau hubungi kami."
                )
                repository.insertTracking(tracking)

                // Notification
                val notification = Notification(
                    userId = order.customerEmail,
                    title = "Pembayaran Ditolak",
                    message = "Bukti transfer untuk order ${payment.orderNumber} tidak valid / ditolak. Silakan unggah bukti yang benar."
                )
                repository.insertNotification(notification)

                _pushNotificationFlow.emit(
                    Pair("Pembayaran Ditolak ⚠️", "Verifikasi bukti transfer order ${payment.orderNumber} gagal.")
                )
            }
        }
    }

    fun triggerInAppNotification(title: String, message: String) {
        viewModelScope.launch {
            _pushNotificationFlow.emit(Pair(title, message))
        }
    }

    // Helper functions
    fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
    }
}

// Factory for ViewModel injection
class AppViewModelFactory(private val application: Application, private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
