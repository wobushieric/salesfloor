package eric.dai

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.{Sink, Source}
import eric.dai.hakernews.HttpHackerNewsService
import eric.dai.model.HackerNewsModel.Story

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.Future

object App extends SprayJsonSupport {

  implicit val system = ActorSystem(Behaviors.empty, "App")
  implicit val executionContext = system.executionContext
  val hackerNews = new HttpHackerNewsService
  val parallelism = 6

  def main(args: Array[String]): Unit = {
    // TODO: Probably need to filter `type: job`
    val top30Stories = hackerNews.getTopStories().flatMap(stories => processTop30Stories(stories.take(30)))

    val storyComments = new ConcurrentHashMap[Story, ConcurrentHashMap[String, Int]]()

    top30Stories.map { stories =>
      Future.sequence(stories.filter(s => s.kids.isDefined).map { story =>
        // get all comments of a given story
        processStoryComments(story, story.kids.get, storyComments)
      })
    }.flatten
      .map(_ => printTopComments(storyComments))
  }

  private[this] def processTop30Stories
  (storyIds: Seq[Int]): Future[Seq[Story]] = {
    Source(storyIds.toStream).mapAsyncUnordered(parallelism) { storyId =>
      hackerNews.getStoryById(storyId)
    }.runWith(Sink.seq)
      .recoverWith {
        case ex: Throwable =>
          println(s"Failed fetching stories, error: $ex")
          Future.failed(ex)
      }
  }

  private[this] def processStoryComments
  (story: Story,
   commentIds: Seq[Int],
   storyCommentsMap: ConcurrentHashMap[Story, ConcurrentHashMap[String, Int]]
  ): Future[ConcurrentHashMap[Story, ConcurrentHashMap[String, Int]]] = {
    if (commentIds.nonEmpty) {
      val storyComments = Source(commentIds.toStream).mapAsyncUnordered(parallelism) { commentId =>
        hackerNews.getCommentById(commentId)
      }.runWith(Sink.seq)
        .recoverWith {
          case ex: Throwable =>
            println(s"Failed fetching comments, error: $ex")
            Future.failed(ex)
        }

      storyComments.map { comments =>
        comments.map { comment =>
          if (comment.by.isDefined) {
            // increment user comment count by 1
            val userComment = storyCommentsMap.getOrDefault(story, new ConcurrentHashMap[String, Int]())
            val commentUser = comment.by.get
            val count = userComment.getOrDefault(commentUser, 0) + 1
            userComment.put(commentUser, count)
            storyCommentsMap.put(story, userComment)

            // get nested sub comments
            if (comment.kids.isDefined) {
              processStoryComments(story, comment.kids.get, storyCommentsMap)
            }
          }
        }
        storyCommentsMap
      }
    } else {
      // return the unmodified map when the story has no comment
      Future.successful(storyCommentsMap)
    }
  }

  private[this] def printTopComments(storyComments: ConcurrentHashMap[Story, ConcurrentHashMap[String, Int]]): Unit = {
    // The passed in map contains sum of comments from each commenter of a story
    // Story -> (CommenterName -> TotalCommentsForTheStory )
    // TODO: Traverse the map and find sum of comments for each commenter of all stories
    // CommenterName -> TotalCommentsForAllTop30Stories

    // TODO: Print out (Story -> TopCommenterName -> CurrentStoryCommentCount -> TotalCommentCount)

    println(storyComments)
  }
}
