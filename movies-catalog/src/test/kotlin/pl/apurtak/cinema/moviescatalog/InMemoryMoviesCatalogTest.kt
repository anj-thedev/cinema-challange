package pl.apurtak.cinema.moviescatalog

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.jupiter.api.Test
import pl.apurtak.cinema.moviescatalog.model.Movie
import java.util.*

class InMemoryMoviesCatalogTest {

    @Test
    fun `should add movies to catalog`() {
        // given
        val shrek = Movie(
            id = UUID.randomUUID(),
            name = "Shrek",
            durationMinutes = 83,
            threeDimensionalGlassesNeeded = false
        )
        val avatar = Movie(
            id = UUID.randomUUID(),
            name = "Avatar",
            durationMinutes = 117,
            threeDimensionalGlassesNeeded = true
        )

        // when
        val catalog = InMemoryMoviesCatalog()
        catalog.add(shrek)
        catalog.add(avatar)

        // then
        assertThat(catalog.listMovies()).containsExactly(
            shrek, avatar
        )
    }
}