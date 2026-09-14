package org.fossify.gallery.asynctasks

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.fossify.commons.helpers.FAVORITES
import org.fossify.commons.helpers.SORT_BY_DATE_MODIFIED
import org.fossify.commons.helpers.SORT_BY_DATE_TAKEN
import org.fossify.commons.helpers.SORT_BY_SIZE
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.getFavoritePaths
import org.fossify.gallery.helpers.*
import org.fossify.gallery.models.Medium
import org.fossify.gallery.models.ThumbnailItem
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

class GetMediaAsynctask(
    val context: Context, val mPath: String, val isPickImage: Boolean = false, val isPickVideo: Boolean = false,
    val showAll: Boolean, val callback: (media: ArrayList<ThumbnailItem>) -> Unit
) {
    private val mediaFetcher = MediaFetcher(context)
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val hasStarted = AtomicBoolean(false)
    private val isCancellationRequested = AtomicBoolean(false)
    private val callbackLock = Any()
    private val fetchFuture = object : FutureTask<ArrayList<ThumbnailItem>>(Callable { fetchMedia() }) {
        override fun done() {
            if (isCancelled || isCancellationRequested.get()) {
                return
            }

            val media = try {
                get()
            } catch (exception: ExecutionException) {
                throw RuntimeException("An error occurred while executing media fetch", exception.cause)
            }

            if (!isCancellationRequested.get()) {
                mainThreadHandler.post {
                    synchronized(callbackLock) {
                        if (!isCancellationRequested.get()) {
                            callback(media)
                        }
                    }
                }
            }
        }
    }

    private fun fetchMedia(): ArrayList<ThumbnailItem> {
        val pathToUse = if (showAll) SHOW_ALL else mPath
        val folderGrouping = context.config.getFolderGrouping(pathToUse)
        val folderSorting = context.config.getFolderSorting(pathToUse)
        val getProperDateTaken = folderSorting and SORT_BY_DATE_TAKEN != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0

        val getProperLastModified = folderSorting and SORT_BY_DATE_MODIFIED != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0

        val getProperFileSize = folderSorting and SORT_BY_SIZE != 0
        val favoritePaths = context.getFavoritePaths()
        val getVideoDurations = context.config.showThumbnailVideoDuration
        val lastModifieds = if (getProperLastModified) mediaFetcher.getLastModifieds() else HashMap()
        val dateTakens = if (getProperDateTaken) mediaFetcher.getDateTakens() else HashMap()

        val media = if (showAll) {
            val foldersToScan = mediaFetcher.getFoldersToScan().filter { it != RECYCLE_BIN && it != FAVORITES && !context.config.isFolderProtected(it) }
            val media = ArrayList<Medium>()
            foldersToScan.forEach {
                val newMedia = mediaFetcher.getFilesFrom(
                    it, isPickImage, isPickVideo, getProperDateTaken, getProperLastModified, getProperFileSize,
                    favoritePaths, getVideoDurations, lastModifieds, dateTakens.clone() as HashMap<String, Long>, null
                )
                media.addAll(newMedia)
            }

            mediaFetcher.sortMedia(media, context.config.getFolderSorting(SHOW_ALL))
            media
        } else {
            mediaFetcher.getFilesFrom(
                mPath, isPickImage, isPickVideo, getProperDateTaken, getProperLastModified, getProperFileSize, favoritePaths,
                getVideoDurations, lastModifieds, dateTakens, null
            )
        }

        return mediaFetcher.groupMedia(media, pathToUse)
    }

    internal fun start() {
        check(hasStarted.compareAndSet(false, true)) { "This task has already been started." }
        executor.execute(fetchFuture)
    }

    fun stopFetching() {
        mediaFetcher.shouldStop = true
        synchronized(callbackLock) {
            isCancellationRequested.set(true)
            fetchFuture.cancel(true)
        }
    }

    private companion object {
        val executor = Executors.newSingleThreadExecutor()
    }
}
