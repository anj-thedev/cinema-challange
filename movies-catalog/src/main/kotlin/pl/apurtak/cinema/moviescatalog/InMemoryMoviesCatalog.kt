package pl.apurtak.cinema.moviescatalog

import pl.apurtak.cinema.moviescatalog.model.Movie

internal class InMemoryMoviesCatalog : MoviesCatalog {
    private var movies: List<Movie> = emptyList()

    override fun add(movie: Movie) {
        movies = movies.plus(movie)
    }

    override fun listMovies(): List<Movie> {
        return movies
    }

    override fun findById(id: Movie): Movie? {
        TODO("Not yet implemented")
    }

}