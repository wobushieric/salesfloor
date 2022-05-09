package eric.dai.model

object HackerNewsModel {

  case class Story(
    by: String,
    kids: Option[Seq[Int]],
    id: Int,
    title: String,
  )

  case class Comment(
    by: Option[String],
    kids: Option[Seq[Int]],
    id: Int,
  )
}
