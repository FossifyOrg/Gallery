import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.fossify.gallery.helpers.MediaFetcher
import org.fossify.gallery.models.Favorite

class PhotoFragment : Fragment() {
    private lateinit var mediaFetcher: MediaFetcher
    private lateinit var favoriteViewModel: FavoriteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaFetcher = MediaFetcher(requireContext())
        favoriteViewModel = ViewModelProvider(this).get(FavoriteViewModel::class.java)
    }

    fun favoriteMedium(mediumId: Int) {
        mediaFetcher.favoriteMedium(mediumId)
        favoriteViewModel.favoriteMedium(mediumId)
    }
}