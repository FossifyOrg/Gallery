import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.*
import org.fossify.gallery.models.Favorite

@Entity
data class Favorite(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val mediumId: Int,
    val timestamp: Long
)

@Dao
interface FavoritesDao {
    @Insert
    suspend fun insertFavorite(favorite: Favorite)

    @Query("SELECT * FROM Favorite WHERE mediumId = :mediumId")
    suspend fun getFavorite(mediumId: Int): Favorite?

    @Query("DELETE FROM Favorite WHERE mediumId = :mediumId")
    suspend fun deleteFavorite(mediumId: Int)
}

@Database(entities = [Favorite::class], version = 1)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
}

class MediaFetcher(private val context: Context) {
    private val database: GalleryDatabase = Room.databaseBuilder(
        context,
        GalleryDatabase::class.java,
        "gallery_database"
    ).build()

    private val favoritesDao: FavoritesDao = database.favoritesDao()

    fun favoriteMedium(mediumId: Int) {
        val favorite = Favorite(mediumId = mediumId, timestamp = System.currentTimeMillis())
        favoritesDao.insertFavorite(favorite)
        notifyUI()
    }

    private fun notifyUI() {
        // Notify the UI to update
        // This can be done using LiveData or a similar mechanism
        // For simplicity, we will use a simple callback
        val callback = object : Callback {
            override fun onCallback() {
                // Update the UI here
            }
        }
        callback.onCallback()
    }
}