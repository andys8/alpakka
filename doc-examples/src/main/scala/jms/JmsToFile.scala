/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package jms

// #sample
import java.nio.file.Paths

import akka.stream.{IOResult, KillSwitch}
import akka.stream.alpakka.jms.JmsSourceSettings
import akka.stream.alpakka.jms.scaladsl.JmsSource
import akka.stream.scaladsl.{FileIO, Keep, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
// #sample
import playground.ActiveMqBroker

object JmsToFile extends JmsSampleBase with App {

  ActiveMqBroker.start()

  val connectionFactory = ActiveMqBroker.createConnectionFactory
  enqueue(connectionFactory)("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")

  // format: off
  // #sample

  val jmsSource: Source[String, KillSwitch] =        // (1)
    JmsSource.textSource(
      JmsSourceSettings(connectionFactory).withBufferSize(10).withQueue("test")
    )

  val fileSink: Sink[ByteString, Future[IOResult]] = // (2)
    FileIO.toPath(Paths.get("target/out.txt"))

  val (runningSource, finished): (KillSwitch, Future[IOResult]) =
                                                     // stream element type
    jmsSource                                        //: String
      .map(ByteString(_))                            //: ByteString    (3)
      .toMat(fileSink)(Keep.both)
      .run()
  // #sample
  // format: on
  wait(1.second)
  runningSource.shutdown()
  for {
    _ <- actorSystem.terminate()
    _ <- ActiveMqBroker.stop()
  } ()

}
