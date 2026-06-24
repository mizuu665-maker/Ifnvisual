package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ENTITIES
// ==========================================

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val fullName: String,
    val whatsappNumber: String,
    val profileRole: String = "CUSTOMER", // "ADMIN" or "CUSTOMER"
    val isGoogleLogin: Boolean = false
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Desain Grafis", "Motion Graphic", "Video Editing", "Branding"
    val thumbnailResName: String, // "img_ifn_logo_1782342207075" or fallback drawable
    val thumbnailUri: String? = null, // for custom added products
    val price: Long,
    val description: String,
    val duration: String // e.g. "1-2 Hari", "3-5 Hari"
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderNumber: String, // IFN-YYYYMMDD-XXXX
    val customerEmail: String,
    val customerName: String,
    val customerWhatsapp: String,
    val productType: String,
    val detailPesanan: String,
    val referenceUri: String? = null,
    val deadline: String,
    val catatan: String? = null,
    val status: String = "Pesanan Diterima", // "Pesanan Diterima", "Sedang Dikerjakan", "Revisi", "Finalisasi", "Selesai"
    val progressPercentage: Int = 0,
    val estimasiSelesai: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val orderNumber: String,
    val paymentMethod: String, // "BRI", "DANA", "GoPay"
    val amount: Long,
    val proofUri: String? = null, // Path to uploaded confirmation payment proof
    val status: String = "Menunggu Verifikasi", // "Menunggu Verifikasi", "Pembayaran Diterima", "Pembayaran Ditolak"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // email
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "progress_tracking")
data class ProgressTracking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val orderNumber: String,
    val status: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// DAOS
// ==========================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE profileRole = 'CUSTOMER'")
    fun getAllCustomers(): Flow<List<User>>
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE customerEmail = :email ORDER BY timestamp DESC")
    fun getOrdersByCustomer(email: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): Order?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query("SELECT COUNT(*) FROM orders WHERE status = 'Sedang Dikerjakan' OR status = 'Revisi' OR status = 'Finalisasi'")
    fun getActiveOrdersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders WHERE status = 'Selesai'")
    fun getCompletedOrdersCount(): Flow<Int>
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY timestamp DESC")
    fun getPaymentsByOrder(orderId: Int): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsByUser(userId: String): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)
}

@Dao
interface ProgressTrackingDao {
    @Query("SELECT * FROM progress_tracking WHERE orderId = :orderId ORDER BY timestamp DESC")
    fun getTrackingByOrder(orderId: Int): Flow<List<ProgressTracking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracking(tracking: ProgressTracking)
}

// ==========================================
// DATABASE
// ==========================================

@Database(
    entities = [
        User::class,
        Product::class,
        Order::class,
        Payment::class,
        Notification::class,
        ProgressTracking::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun progressTrackingDao(): ProgressTrackingDao
}
