/*
 * Copyright 2020 Precog Data
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.datasource.kafka

import slamdata.Predef._

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.TerminationMatchers
import org.specs2.mutable.Specification

import cats.effect._
import fs2.Stream
import fs2.kafka.{CommittableConsumerRecord, CommittableOffset, ConsumerRecord, ConsumerSettings}
import quasar.datasource.kafka.TestImplicits._

import scala.concurrent.duration._

class SeekConsumerSpec(implicit ec: ExecutionEnv) extends Specification with TerminationMatchers {

  "assignNonEmptyPartitionsForTopic" >> {
    "assigns all non empty partitions" >> {
      todo // requires KafkaConsumer to not be sealed (or use a mock framework)
    }

    "does not assign empty partitions" >> {
      todo // requires KafkaConsumer to not be sealed
    }

    "returns end offsets of all non empty partitions" >> {
      todo // requires KafkaConsumer to not be sealed
    }

    "does not return end offset of empty partitions" >> {
      todo // requires KafkaConsumer to not be sealed
    }
  }

  "limitStream" >> {
    val settings = ConsumerSettings[IO, Array[Byte], Array[Byte]]
    val kafkaConsumer = new SeekConsumer[IO, Array[Byte], Array[Byte]](Map.empty, settings, KafkaConsumerBuilder.RawKey)

    "terminates stream once data from the sole substream is read" >> {
      val tp = new TopicPartition("topic", 0)
      val offset = 5L
      val endOffsets = Map(tp -> (offset + 1L))
      val mkRecord = mkCommittableConsumerRecord(tp, (_: Long), "key" -> "value")
      val assignment = IO.pure(Stream.iterate[IO, Long](offset)(_ + 1).map(mkRecord))
      val stream = Stream.eval(assignment)

      kafkaConsumer.takeUntilEndOffsets(stream, endOffsets).compile.drain.unsafeRunSync() must terminate(sleep = 2.seconds)
    }

    "terminates stream even if main stream keeps producing auto assignments" >> {
      val tp = new TopicPartition("topic", 0)
      val offset = 5L
      val endOffsets = Map(tp -> (offset + 1L))
      val mkRecord = mkCommittableConsumerRecord(tp, (_: Long), "key" -> "value")
      val assignment = IO.pure(Stream.iterate[IO, Long](offset)(_ + 1).map(mkRecord))
      val stream = Stream.eval(assignment).repeat // repeat to emulate hypothetical automatic assignments

      kafkaConsumer.takeUntilEndOffsets(stream, endOffsets).compile.drain.unsafeRunSync() must terminate(sleep = 2.seconds)
    }

    "terminate stream when there previous offsets are greater or equal to current" >> {
      val settings = ConsumerSettings[IO, Array[Byte], Array[Byte]]
      val consumer = new SeekConsumer[IO, Array[Byte], Array[Byte]](
        Map(0 -> 12),
        settings,
        KafkaConsumerBuilder.RawKey)
      val tp = new TopicPartition("topic", 0)
      val endOffsetsLt = Map(tp -> 5L)
      val endOffsetsEq = Map(tp -> 12L)
      val mkRecord = mkCommittableConsumerRecord(tp, (_: Long), "key" -> "value")
      val assignment = IO.pure(Stream.iterate[IO, Long](0)(_ + 1).map(mkRecord))
      val stream = Stream.eval(assignment)

      consumer.takeUntilEndOffsets(stream, endOffsetsLt).compile.toList.unsafeRunTimed(2.seconds).must {
        beSome(List[Array[Byte]]())
      }

      consumer.takeUntilEndOffsets(stream, endOffsetsEq).compile.toList.unsafeRunTimed(2.seconds).must {
        beSome(List[Array[Byte]]())
      }
    }
    "takes messages with offset more or equal to previous" >> {
      val settings = ConsumerSettings[IO, Array[Byte], Array[Byte]]
      val consumer0 = new SeekConsumer[IO, Array[Byte], Array[Byte]](Map(0 -> 12), settings, KafkaConsumerBuilder.RawKey)
      val consumer1 = new SeekConsumer[IO, Array[Byte], Array[Byte]](Map(0 -> 4), settings, KafkaConsumerBuilder.RawKey)
      val tp = new TopicPartition("topic", 0)
      val endOffsets = Map(tp -> 42L)
      val mkRecord = (i: Long) => mkCommittableConsumerRecord(tp, i, i.toString -> s"value:$i")
      val assignment = IO.pure(Stream.iterate[IO, Long](1L)(_ + 1).map(mkRecord))
      val stream = Stream.eval(assignment)

      consumer0.takeUntilEndOffsets(stream, endOffsets).compile.toList.unsafeRunTimed(2.seconds) must beLike {
        case Some(lst) => lst.length must_=== 30
      }

      consumer1.takeUntilEndOffsets(stream, endOffsets).compile.toList.unsafeRunTimed(2.seconds) must beLike {
        case Some(lst) => lst.length must_=== 38
      }
    }
  }

  def mkCommittableConsumerRecord(tp: TopicPartition, offset: Long, entry: (String, String))
      : CommittableConsumerRecord[IO, Array[Byte], Array[Byte]] =
    CommittableConsumerRecord[IO, Array[Byte], Array[Byte]](
      ConsumerRecord(tp.topic(), tp.partition(), offset, entry._1.getBytes, entry._2.getBytes),
      CommittableOffset(tp, new OffsetAndMetadata(offset), None, _ => IO.unit))

  def mkCommittableConsumerRecord(topic: String, partition: Int, offset: Long, key: String, value: String)
      : CommittableConsumerRecord[IO, Array[Byte], Array[Byte]] =
    mkCommittableConsumerRecord(new TopicPartition(topic, partition), offset, key -> value)

}
