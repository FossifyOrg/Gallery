@Database(entities = [Favorite::class], version = 1)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
}