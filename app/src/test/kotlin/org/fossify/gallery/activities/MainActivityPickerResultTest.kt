package org.fossify.gallery.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Looper
import com.github.ajalt.reprint.core.Reprint
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import okhttp3.Response
import org.fossify.commons.FossifyApp
import org.fossify.commons.views.MySearchMenu
import org.fossify.gallery.BuildConfig
import org.fossify.gallery.R
import org.fossify.gallery.databases.GalleryDatabase
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.DIRECTORY
import org.fossify.gallery.helpers.GET_IMAGE_INTENT
import org.fossify.gallery.helpers.PICKED_PATHS
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = MainActivityPickerResultTest.PickerTestApp::class, sdk = [28])
class MainActivityPickerResultTest {
    private val controllers = mutableListOf<ActivityController<*>>()
    private val tempFiles = mutableListOf<File>()

    class PickerTestApp : FossifyApp() {
        override val isAppLockFeatureAvailable = true

        override fun onCreate() {
            super.onCreate()
            try {
                Reprint.initialize(this)
            } catch (_: Exception) {
            }

            try {
                Picasso.setSingletonInstance(
                    Picasso.Builder(this).downloader(object : Downloader {
                        override fun load(request: Request) = Response.Builder().build()
                        override fun shutdown() {}
                    }).build()
                )
            } catch (_: IllegalStateException) {
            }
        }
    }

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        seedPreferences()
    }

    @After
    fun tearDown() {
        controllers.forEach { controller ->
            try {
                if (controller.get().isDestroyed.not()) {
                    controller.pause().stop().destroy()
                }
            } catch (_: Exception) {
            }
        }
        controllers.clear()
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
        GalleryDatabase.destroyInstance()
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("Prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun cancelingAllMediaPickIntentFinishesWithCanceledResult() {
        assertCanceledAllMediaPickerFinishes(pickImageIntent())
    }

    @Test
    fun cancelingAllMediaGetContentIntentFinishesWithCanceledResult() {
        assertCanceledAllMediaPickerFinishes(getContentImageIntent())
    }

    @Test
    fun cancelingAllMediaVideoPickIntentFinishesWithCanceledResult() {
        assertCanceledAllMediaPickerFinishes(pickVideoIntent())
    }

    @Test
    fun cancelingAllMediaVideoGetContentIntentFinishesWithCanceledResult() {
        assertCanceledAllMediaPickerFinishes(getContentVideoIntent())
    }

    @Test
    fun cancelingAllMediaMultipleSelectionPickerFinishesWithCanceledResult() {
        assertCanceledAllMediaPickerFinishes(pickImageIntent(allowMultiple = true))
    }

    @Test
    fun cancelingAllMediaPickerKeepsShowAllEnabledAndAllowsANewPicker() {
        seedPreferences(showAll = true)
        val firstController = launchMainActivity(pickImageIntent())
        val firstActivity = firstController.get()
        val firstLaunch = requireLaunchedMediaPicker(firstActivity)
        Shadows.shadowOf(firstActivity).receiveResult(
            firstLaunch.intent,
            Activity.RESULT_CANCELED,
            null
        )
        idleMainLooper()

        assertTrue(firstActivity.isFinishing)
        assertTrue(firstActivity.config.showAll)

        val secondController = launchMainActivity(pickImageIntent())
        val secondLaunch = requireLaunchedMediaPicker(secondController.get())
        assertEquals(MediaActivity::class.java.name, secondLaunch.intent.component?.className)
        assertTrue(secondController.get().config.showAll)
    }

    @Test
    fun cancelingAlbumPickerLeavesFolderListAvailable() {
        seedPreferences(showAll = false)
        val album = createTempAlbum()
        val controller = launchMainActivity(pickImageIntent())
        val activity = controller.get()
        assertNull(
            "Folder-mode picker should stay on the album list",
            Shadows.shadowOf(activity).peekNextStartedActivityForResult()
        )

        val albumIntent = Intent(activity, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, album.absolutePath)
            putExtra(GET_IMAGE_INTENT, true)
        }
        activity.startActivityForResult(albumIntent, PICK_MEDIA_REQUEST_CODE)
        val launched = Shadows.shadowOf(activity).peekNextStartedActivityForResult()
        assertNotNull(launched)
        Shadows.shadowOf(activity).receiveResult(albumIntent, Activity.RESULT_CANCELED, null)
        idleMainLooper()

        assertFalse(
            "Canceling an album picker must leave MainActivity available",
            activity.isFinishing
        )
        assertFalse(activity.config.showAll)
    }

    @Test
    fun backFromFolderListRootReturnsCanceledResultToCaller() {
        seedPreferences(showAll = false)
        val controller = launchMainActivity(pickImageIntent())
        val activity = controller.get()

        activity.onBackPressedDispatcher.onBackPressed()
        idleMainLooper()

        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    @Test
    fun cancelingDefaultFolderPickerLeavesMainActivityAvailable() {
        val album = createTempAlbum()
        seedPreferences(showAll = false, defaultFolder = album.absolutePath)
        val controller = launchMainActivity(pickImageIntent())
        val activity = controller.get()
        val launched = requireLaunchedMediaPicker(activity)
        assertEquals(album.absolutePath, launched.intent.getStringExtra(DIRECTORY))

        Shadows.shadowOf(activity).receiveResult(launched.intent, Activity.RESULT_CANCELED, null)
        idleMainLooper()

        assertFalse(activity.isFinishing)
        assertFalse(activity.config.showAll)
    }

    @Test
    fun cancelingUnrelatedActivityDoesNotFinishAllMediaPickerParent() {
        seedPreferences(showAll = true)
        val controller = launchMainActivity(pickImageIntent())
        val activity = controller.get()
        requireLaunchedMediaPicker(activity)

        val unrelated = Intent(activity, SettingsActivity::class.java)
        activity.startActivityForResult(unrelated, UNRELATED_REQUEST_CODE)
        Shadows.shadowOf(activity).receiveResult(unrelated, Activity.RESULT_CANCELED, null)
        idleMainLooper()

        assertFalse(activity.isFinishing)
        assertTrue(activity.config.showAll)
    }

    @Test
    fun successfulSinglePickerResultGrantsUriAndFinishes() {
        seedPreferences(showAll = true)
        val media = createTempMediaFile("single.jpg")
        val controller = launchMainActivity(pickImageIntent())
        val activity = controller.get()
        val launched = requireLaunchedMediaPicker(activity)

        val resultData = Intent().setData(android.net.Uri.parse(media.absolutePath))
        Shadows.shadowOf(activity).receiveResult(launched.intent, Activity.RESULT_OK, resultData)
        idleMainLooper()

        val shadow = Shadows.shadowOf(activity)
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertNotNull(shadow.resultIntent?.data)
        assertTrue(
            shadow.resultIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
    }

    @Test
    fun successfulMultiplePickerResultReturnsClipDataAndFinishes() {
        seedPreferences(showAll = true)
        val first = createTempMediaFile("first.jpg")
        val second = createTempMediaFile("second.jpg")
        val controller = launchMainActivity(pickImageIntent(allowMultiple = true))
        val activity = controller.get()
        val launched = requireLaunchedMediaPicker(activity)

        val resultData = Intent().putStringArrayListExtra(
            PICKED_PATHS,
            arrayListOf(first.absolutePath, second.absolutePath)
        )
        Shadows.shadowOf(activity).receiveResult(launched.intent, Activity.RESULT_OK, resultData)
        idleMainLooper()

        val shadow = Shadows.shadowOf(activity)
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertNotNull(shadow.resultIntent?.clipData)
        assertEquals(2, shadow.resultIntent.clipData!!.itemCount)
        assertTrue(
            shadow.resultIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
    }

    @Test
    fun searchBackDismissesSearchBeforeExitingAllMediaPicker() {
        seedPreferences(showAll = true)
        val intent = Intent(RuntimeEnvironment.getApplication(), MediaActivity::class.java).apply {
            putExtra(DIRECTORY, "")
            putExtra(GET_IMAGE_INTENT, true)
        }
        val controller = launchActivity(MediaActivity::class.java, intent)
        val activity = controller.get()
        val searchMenu = activity.findViewById<MySearchMenu>(R.id.media_menu)
        openSearch(searchMenu)

        activity.onBackPressedDispatcher.onBackPressed()
        idleMainLooper()
        assertFalse(
            "The first Back should dismiss search instead of leaving the picker",
            activity.isFinishing
        )
        assertFalse(searchMenu.isSearchOpen)

        activity.onBackPressedDispatcher.onBackPressed()
        idleMainLooper()
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    @Test
    fun ordinaryAllMediaLaunchFinishesParentAndStartsMediaActivity() {
        seedPreferences(showAll = true)
        val controller = launchMainActivity(Intent())
        val activity = controller.get()
        val started = Shadows.shadowOf(activity).peekNextStartedActivity()

        assertTrue(activity.isFinishing)
        assertNotNull(started)
        assertEquals(MediaActivity::class.java.name, started.component?.className)
    }

    @Test
    fun ordinaryFolderLaunchStaysOnDirectoryList() {
        seedPreferences(showAll = false)
        val controller = launchMainActivity(Intent())
        val activity = controller.get()

        assertFalse(activity.isFinishing)
        assertNull(Shadows.shadowOf(activity).peekNextStartedActivityForResult())
    }

    private fun assertCanceledAllMediaPickerFinishes(intent: Intent) {
        seedPreferences(showAll = true)
        val controller = launchMainActivity(intent)
        val activity = controller.get()
        val launched = requireLaunchedMediaPicker(activity)

        Shadows.shadowOf(activity).receiveResult(launched.intent, Activity.RESULT_CANCELED, null)
        idleMainLooper()

        assertTrue(
            "Canceling the all-media picker must finish MainActivity instead of looping",
            activity.isFinishing
        )
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
        assertTrue(activity.config.showAll)
    }

    private fun requireLaunchedMediaPicker(activity: MainActivity): ShadowActivity.IntentForResult {
        idleMainLooper()
        val launched = Shadows.shadowOf(activity).peekNextStartedActivityForResult()
        assertNotNull("Expected MediaActivity to be started for a result", launched)
        assertEquals(MediaActivity::class.java.name, launched!!.intent.component?.className)
        assertEquals(PICK_MEDIA_REQUEST_CODE, launched.requestCode)
        return launched
    }

    private fun launchMainActivity(intent: Intent): ActivityController<MainActivity> {
        return launchActivity(MainActivity::class.java, intent)
    }

    private fun <T : Activity> launchActivity(
        clazz: Class<T>,
        intent: Intent
    ): ActivityController<T> {
        val controller = Robolectric.buildActivity(clazz, intent)
            .create()
            .start()
            .postCreate(null)
            .resume()
            .visible()
        controllers += controller
        idleMainLooper()
        return controller
    }

    private fun seedPreferences(
        showAll: Boolean = false,
        defaultFolder: String = "",
        groupDirectSubfolders: Boolean = false
    ) {
        val config = RuntimeEnvironment.getApplication().config
        config.appRunCount = 5
        config.lastVersion = BuildConfig.VERSION_CODE
        config.showAll = showAll
        config.defaultFolder = defaultFolder
        config.wereFavoritesPinned = true
        config.wasRecycleBinPinned = true
        config.wasSVGShowingHandled = true
        config.wasSortingByNumericValueAdded = true
        config.wereFavoritesMigrated = true
        config.avoidShowingAllFilesPrompt = true
        config.showPermissionRationale = false
        config.groupDirectSubfolders = groupDirectSubfolders
        config.wasNewAppShown = true
        config.temporarilyShowHidden = false
        config.temporarilyShowExcluded = false
    }

    private fun pickImageIntent(allowMultiple: Boolean = false): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            if (allowMultiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
    }

    private fun getContentImageIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
    }

    private fun pickVideoIntent(): Intent {
        return Intent(Intent.ACTION_PICK).apply { type = "video/*" }
    }

    private fun getContentVideoIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply { type = "video/*" }
    }

    private fun createTempAlbum(): File {
        val album = File(RuntimeEnvironment.getApplication().cacheDir, "picker_album").apply {
            mkdirs()
        }
        tempFiles += album
        return album
    }

    private fun createTempMediaFile(name: String): File {
        val dir = File(RuntimeEnvironment.getApplication().cacheDir, "picker_media").apply {
            mkdirs()
        }
        val file = File(dir, name)
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        tempFiles += file
        return file
    }

    private fun openSearch(searchMenu: MySearchMenu) {
        val setter = searchMenu.javaClass.methods.firstOrNull { method ->
            method.name == "setSearchOpen" && method.parameterCount == 1
        } ?: searchMenu.javaClass.methods.firstOrNull { method ->
            method.name == "openSearch" && method.parameterCount == 0
        }
        assertNotNull("MySearchMenu must expose a way to open search", setter)
        if (setter!!.parameterCount == 1) {
            setter.invoke(searchMenu, true)
        } else {
            setter.invoke(searchMenu)
        }
        if (!searchMenu.isSearchOpen) {
            var type: Class<*>? = searchMenu.javaClass
            var field: java.lang.reflect.Field? = null
            while (type != null && field == null) {
                field = try {
                    type.getDeclaredField("isSearchOpen")
                } catch (_: NoSuchFieldException) {
                    null
                }
                type = type.superclass
            }
            assertNotNull("Could not open search on MySearchMenu", field)
            field!!.isAccessible = true
            field.setBoolean(searchMenu, true)
        }
        assertTrue(searchMenu.isSearchOpen)
    }

    private fun idleMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    companion object {
        private const val PICK_MEDIA_REQUEST_CODE = 2
        private const val UNRELATED_REQUEST_CODE = 99
    }
}
