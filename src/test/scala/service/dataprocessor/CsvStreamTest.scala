package service.dataprocessor

import java.nio.file.{Files, Paths, StandardCopyOption}

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.stream.ActorMaterializer
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import service.dataprocessor.dal.{Event, EventDao}

import scala.concurrent.duration._

class CsvStreamTest extends FlatSpec with Matchers with MockFactory with ScalaFutures with CsvStream {
  implicit val system = ActorSystem("DataProcessorServiceTest")

  implicit def executor = system.dispatcher

  implicit val materializer = ActorMaterializer()

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(15.second.dilated)

  val conf = ConfigFactory.load("test-application.conf")

  it should "get only unique events from input csv file and store to db" in {
    //given
    val dataDir = conf.getString("service.dataprocessor.input-path")
    val sampleDir = "src/test/resources"
    val fileName = "test.csv"
    val eventDao = mock[EventDao]

    Paths.get(dataDir).toFile.mkdirs()
    Files.copy(Paths.get(sampleDir, fileName), Paths.get(dataDir, fileName), StandardCopyOption.REPLACE_EXISTING)

    // then on future completion
    eventDao.insertEvent _ expects where { (e: Event) => e.id == 1 } once()
    eventDao.insertEvent _ expects where { (e: Event) => e.id == 2 } once()

    //when
    val result = startCsvStream(fileName, eventDao)
    whenReady(result) { _ =>
      //then just wait for completion
    }
  }
}
