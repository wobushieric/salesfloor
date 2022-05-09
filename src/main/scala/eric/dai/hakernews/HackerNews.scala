package eric.dai.hakernews

import eric.dai.model.HackerNewsModel.{Comment, Story}

import scala.concurrent.Future

trait HackerNews {

  def getTopStories(): Future[Seq[Int]]

  def getStoryById(storyId: Int): Future[Story]

  def getCommentById(commentId: Int): Future[Comment]
}
