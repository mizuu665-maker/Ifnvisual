package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(private val db: AppDatabase) {

    val userDao = db.userDao()
    val productDao = db.productDao()
    val orderDao = db.orderDao()
    val paymentDao = db.paymentDao()
    val notificationDao = db.notificationDao()
    val progressTrackingDao = db.progressTrackingDao()

    // Exposing Reactive Flows
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()
    val allCustomers: Flow<List<User>> = userDao.getAllCustomers()
    val activeOrdersCount: Flow<Int> = orderDao.getActiveOrdersCount()
    val completedOrdersCount: Flow<Int> = orderDao.getCompletedOrdersCount()

    fun getOrdersByCustomer(email: String): Flow<List<Order>> {
        return orderDao.getOrdersByCustomer(email)
    }

    fun getPaymentsByOrder(orderId: Int): Flow<List<Payment>> {
        return paymentDao.getPaymentsByOrder(orderId)
    }

    fun getNotificationsByUser(userId: String): Flow<List<Notification>> {
        return notificationDao.getNotificationsByUser(userId)
    }

    fun getTrackingByOrder(orderId: Int): Flow<List<ProgressTracking>> {
        return progressTrackingDao.getTrackingByOrder(orderId)
    }

    // Suspended DB Actions
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun insertProduct(product: Product) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    suspend fun getOrderById(id: Int): Order? = orderDao.getOrderById(id)
    suspend fun insertOrder(order: Order): Long = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)

    suspend fun insertPayment(payment: Payment): Long = paymentDao.insertPayment(payment)
    suspend fun updatePayment(payment: Payment) = paymentDao.updatePayment(payment)

    suspend fun insertNotification(notification: Notification) = notificationDao.insertNotification(notification)
    suspend fun markAllNotificationsAsRead(userId: String) = notificationDao.markAllAsRead(userId)

    suspend fun insertTracking(tracking: ProgressTracking) = progressTrackingDao.insertTracking(tracking)

    // Automatic seeding of products and default accounts
    suspend fun seedDatabaseIfNeeded() {
        if (productDao.getProductCount() == 0) {
            val defaultProducts = listOf(
                // Desain Grafis
                Product(
                    name = "Poster Design",
                    category = "Desain Grafis",
                    thumbnailResName = "img_ifn_logo",
                    price = 150000,
                    description = "Desain poster promosi premium untuk media cetak maupun digital, resolusi tinggi, siap cetak.",
                    duration = "1-2 Hari"
                ),
                Product(
                    name = "Banner / Spanduk",
                    category = "Desain Grafis",
                    thumbnailResName = "img_ifn_logo",
                    price = 200000,
                    description = "Spanduk visual resolusi tinggi untuk event, toko, billboard dengan layout layout estetik modern.",
                    duration = "1-2 Hari"
                ),
                Product(
                    name = "YouTube Thumbnail",
                    category = "Desain Grafis",
                    thumbnailResName = "img_ifn_logo",
                    price = 75000,
                    description = "Thumbnail berdaya klik tinggi (high CTR) penunjang performa video Anda dengan manipulasi warna & teks dinamis.",
                    duration = "1 Hari"
                ),
                Product(
                    name = "Instagram Feed Template",
                    category = "Desain Grafis",
                    thumbnailResName = "img_ifn_logo",
                    price = 120000,
                    description = "Carousel / Single post feed Instagram estetik disesuaikan dengan niche brand Anda, rapi & catchy.",
                    duration = "2 Hari"
                ),
                // Motion Graphic
                Product(
                    name = "Bumper Opening",
                    category = "Motion Graphic",
                    thumbnailResName = "img_ifn_logo",
                    price = 450000,
                    description = "Animasi opening video berdurasi 5-10 detik yang cinematic untuk intro konten YouTube atau korporat.",
                    duration = "3-4 Hari"
                ),
                Product(
                    name = "Logo Animation",
                    category = "Motion Graphic",
                    thumbnailResName = "img_ifn_logo",
                    price = 350000,
                    description = "Mengidupkan logo statis Anda dengan gerakan transisi modern & efek visual yang dramatis dan catchy.",
                    duration = "2-3 Hari"
                ),
                Product(
                    name = "Intro Video Trailer",
                    category = "Motion Graphic",
                    thumbnailResName = "img_ifn_logo",
                    price = 600000,
                    description = "Video pembuka trailer berdurasi 15-30 detik dengan transisi animasi kelas industri, musik pengiring bebas hak cipta.",
                    duration = "4-5 Hari"
                ),
                // Video Editing
                Product(
                    name = "Instagram Reels / TikTok",
                    category = "Video Editing",
                    thumbnailResName = "img_ifn_logo",
                    price = 150000,
                    description = "Editing video vertikal berdurasi singkat up to 60 detik lengkap dengan caption dinamis, sound effects, sound trending.",
                    duration = "1-2 Hari"
                ),
                Product(
                    name = "YouTube Shorts",
                    category = "Video Editing",
                    thumbnailResName = "img_ifn_logo",
                    price = 150000,
                    description = "Editing video Shorts vertikal, penambahan visual B-roll dramatis, audio bersih, sound effect penahan retensi.",
                    duration = "1-2 Hari"
                ),
                Product(
                    name = "Video Promosi Produk",
                    category = "Video Editing",
                    thumbnailResName = "img_ifn_logo",
                    price = 800000,
                    description = "Video iklan produk profesional (30-60s) dengan color grading premium, backsound berbayar, voice over AI jernih.",
                    duration = "3-5 Hari"
                ),
                // Branding
                Product(
                    name = "Premium Logo Design",
                    category = "Branding",
                    thumbnailResName = "img_ifn_logo",
                    price = 500000,
                    description = "Pembuatan logo orisinal premium dengan filosofi mendalam, dilengkapi panduan warna dan tipografi dasar.",
                    duration = "3-4 Hari"
                ),
                Product(
                    name = "Identitas Visual Lengkap",
                    category = "Branding",
                    thumbnailResName = "img_ifn_logo",
                    price = 1200000,
                    description = "Paket komplit corporate identity: Logo, kartu nama, kop surat, mockup merchandise, dan brand guidelines (PDF).",
                    duration = "5-7 Hari"
                ),
                Product(
                    name = "Company Profile Document",
                    category = "Branding",
                    thumbnailResName = "img_ifn_logo",
                    price = 1500000,
                    description = "Penyusunan company profile interaktif berformat PDF atau cetak dengan penataan konten profesional modern.",
                    duration = "7-10 Hari"
                )
            )

            for (product in defaultProducts) {
                productDao.insertProduct(product)
            }
        }

        // Seed default accounts if they don't exist
        if (userDao.getUserByEmail("admin@ifn.com") == null) {
            userDao.insertUser(
                User(
                    email = "admin@ifn.com",
                    fullName = "Administrator IFN_VISUAL",
                    whatsappNumber = "081234567890",
                    profileRole = "ADMIN",
                    isGoogleLogin = false
                )
            )
        }

        if (userDao.getUserByEmail("customer@ifn.com") == null) {
            userDao.insertUser(
                User(
                    email = "customer@ifn.com",
                    fullName = "Budi Santoso",
                    whatsappNumber = "089876543210",
                    profileRole = "CUSTOMER",
                    isGoogleLogin = false
                )
            )
        }
    }
}
