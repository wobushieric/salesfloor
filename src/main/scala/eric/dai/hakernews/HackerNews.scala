package eric.dai.hakernews

import eric.dai.model.HackerNewsModel.{Comment, Story}

import scala.concurrent.Future

trait HackerNews {

  /**
   * Get id of all top stories
   * @return Seq of story ids
   */
  def getTopStories(): Future[Seq[Int]]

  /**
   * Get Story by storyId
   * @param storyId
   * @return Story object
   */
  def getStoryById(storyId: Int): Future[Story]

  /**
   * Get comment by commentId
   * @param commentId
   * @return Comment object
   */
  def getCommentById(commentId: Int): Future[Comment]
}
