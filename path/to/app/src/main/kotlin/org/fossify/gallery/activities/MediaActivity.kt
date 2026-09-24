import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.fossify.gallery.fragments.PhotoFragment

class MediaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoFragment = PhotoFragment()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, photoFragment).commit()
    }
}