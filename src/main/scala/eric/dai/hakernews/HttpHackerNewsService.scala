package eric.dai.hakernews

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import eric.dai.model.HackerNewsModel
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

class HttpHackerNewsService extends HackerNews with SprayJsonSupport {
  implicit val system = ActorSystem(Behaviors.empty, "HackerNews")
  implicit val executionContext = system.executionContext
  implicit val storyFormat = jsonFormat4(HackerNewsModel.Story)
  implicit val commentFormat = jsonFormat3(HackerNewsModel.Comment)
  val hackerNewsHost = "https://hacker-news.firebaseio.com"

  override def getTopStories(): Future[Seq[Int]] = {
    val responseFuture = Http().singleRequest(HttpRequest(uri = s"$hackerNewsHost/v0/topstories.json"))

    responseFuture.flatMap {
      res => Unmarshal(res.entity).to[Seq[Int]]
    }
  }

  override def getStoryById(storyId: Int): Future[HackerNewsModel.Story] = {
    val responseFuture = Http().singleRequest(HttpRequest(uri = s"$hackerNewsHost/v0/item/$storyId.json"))

    responseFuture.flatMap {
      res => Unmarshal(res.entity).to[HackerNewsModel.Story]
    }
  }

  override def getCommentById(commentId: Int): Future[HackerNewsModel.Comment] = {
    val responseFuture = Http().singleRequest(HttpRequest(uri = s"$hackerNewsHost/v0/item/$commentId.json"))

    responseFuture.flatMap {
      res => Unmarshal(res.entity).to[HackerNewsModel.Comment]
    }
  }
}
