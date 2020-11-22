import java.util.Date
import scala.annotation.nowarn

object Decrepitude {
  @nowarn("cat=deprecation")
  val decrepit = {
    // Yo, we don't do this anymore
    new Date(2020, 11, 21)
  }
}
