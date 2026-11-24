import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.fossify.gallery.helpers.MediaFetcher
import org.fossify.gallery.models.Favorite

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {
    private val mediaFetcher: MediaFetcher = MediaFetcher(application)
    private val favoritesDao: FavoritesDao = GalleryDatabase.getInstance(application).favoritesDao()

    fun favoriteMedium(mediumId: Int) {
        val favorite = Favorite(mediumId = mediumId, timestamp = System.currentTimeMillis())
        favoritesDao.insertFavorite(favorite)
        mediaFetcher.notifyUI()
    }
}