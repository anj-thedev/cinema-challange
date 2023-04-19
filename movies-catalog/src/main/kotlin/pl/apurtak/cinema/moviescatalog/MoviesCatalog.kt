package pl.apurtak.cinema.moviescatalog

import pl.apurtak.cinema.moviescatalog.model.Movie

interface MoviesCatalog {
    fun add(movie: Movie)
    fun listMovies(): List<Movie>
    fun findById(id: Movie): Movie?
}