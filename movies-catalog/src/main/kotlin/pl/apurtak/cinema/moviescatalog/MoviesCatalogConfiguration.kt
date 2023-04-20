package pl.apurtak.cinema.moviescatalog

object MoviesCatalogConfiguration {
    fun inMemoryCatalog(): MoviesCatalog = InMemoryMoviesCatalog()
}