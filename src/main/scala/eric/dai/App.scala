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
    val top30Stories = hackerNews.getTopStories().flatMap(stories => processTop30Stories(stories.take(30)))

    val storyComments = new ConcurrentHashMap[Story, ConcurrentHashMap[String, Int]]()

    top30Stories.map { stories =>
      Future.sequence(stories.filter(s => s.kids.isDefined).map { story =>
        processStoryComments(story, story.kids.get, storyComments)
      })
    }.flatten.map(_ => println(storyComments))
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
        val userComment = storyCommentsMap.getOrDefault(story, new ConcurrentHashMap[String, Int]())
        comments.map { comment =>
          if (comment.by.isDefined) {
            val commentUser = comment.by.get
            val count = userComment.getOrDefault(commentUser, 0) + 1
            userComment.put(commentUser, count)
            storyCommentsMap.put(story, userComment)
            if (comment.kids.isDefined) {
              processStoryComments(story, comment.kids.get, storyCommentsMap)
            }
          }
        }
        storyCommentsMap
      }
    } else
      Future.successful(storyCommentsMap)
  }
}
