package com.rosvit.swimreport.fit

import cats.data.EitherT
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.*
import com.garmin.fit.{
  ActivityMesg,
  ActivityMesgListener,
  Decode,
  LapMesg,
  LapMesgListener,
  LengthMesg,
  LengthMesgListener,
  MesgBroadcaster,
  SessionMesg,
  SessionMesgListener
}
import com.rosvit.swimreport.fit.FitFileStatus.*
import fs2.Stream

import java.io.InputStream
import java.nio.file.{Files, Path}

trait FitReader[F[_]] {
  def checkIntegrity(fitPath: Path): F[FitFileStatus]
  def messages(fitPath: Path): Stream[F, FitMessage]
}

object FitReader {

  def make[F[_]: Async]: F[FitReader[F]] = Sync[F].delay {
    new FitReader[F] {

      def checkIntegrity(fitPath: Path): F[FitFileStatus] =
        (for {
          exists <- EitherT.right(Sync[F].blocking(Files.exists(fitPath)))
          _ <- EitherT.cond(exists, (), NotExist)
          isFitFile <- EitherT.right(fitInputStream(fitPath).use(is => Sync[F].blocking(Decode().isFileFit(is))))
          _ <- EitherT.cond(isFitFile, (), NotFitFile)
          valid <- EitherT.right(
            fitInputStream(fitPath).use(is => Sync[F].blocking(Decode().checkFileIntegrity(is)))
          )
          _ <- EitherT.cond(valid, (), IntegrityCheckFailed)
        } yield ())
          .fold(identity, _ => FitFileStatus.Ok)
          .recover(_ => UnexpectedError)

      def messages(fitPath: Path): Stream[F, FitMessage] = {
        def producer(queue: Queue[F, Option[FitMessage]]): Stream[F, Boolean] = Stream.eval {
          Dispatcher
            .parallel[F]
            .use { dispatcher =>
              fitInputStream(fitPath).use { is =>
                for {
                  decode <- Sync[F].delay(Decode())
                  broadcaster <- Sync[F].delay(MesgBroadcaster(decode))
                  sessionMsgListener <- Sync[F].delay {
                    new SessionMesgListener {
                      override def onMesg(mesg: SessionMesg): Unit =
                        dispatcher.unsafeRunSync(queue.offer(Some(mesg.toDomain)))
                    }
                  }
                  lapMsgListener <- Sync[F].delay {
                    new LapMesgListener {
                      override def onMesg(mesg: LapMesg): Unit =
                        dispatcher.unsafeRunSync(queue.offer(Some(mesg.toDomain)))
                    }
                  }
                  lengthMsgListener <- Sync[F].delay {
                    new LengthMesgListener {
                      override def onMesg(mesg: LengthMesg): Unit =
                        dispatcher.unsafeRunSync(queue.offer(Some(mesg.toDomain)))
                    }
                  }
                  activityMsgListener <- Sync[F].delay {
                    new ActivityMesgListener {
                      override def onMesg(mesg: ActivityMesg): Unit =
                        dispatcher.unsafeRunSync(queue.offer(Some(mesg.toDomain)))
                    }
                  }
                  _ <- Sync[F].delay(broadcaster.addListener(sessionMsgListener))
                  _ <- Sync[F].delay(broadcaster.addListener(lapMsgListener))
                  _ <- Sync[F].delay(broadcaster.addListener(lengthMsgListener))
                  _ <- Sync[F].delay(broadcaster.addListener(activityMsgListener))
                  read <- Sync[F].delay(decode.read(is, broadcaster))
                  _ <- queue.offer(None)
                } yield read
              }
            }
        }

        Stream.eval(Queue.unbounded[F, Option[FitMessage]]).flatMap { queue =>
          Stream.fromQueueNoneTerminated(queue).concurrently(producer(queue))
        }
      }

      private def fitInputStream(fitPath: Path): Resource[F, InputStream] =
        Resource.fromAutoCloseable(Sync[F].blocking(Files.newInputStream(fitPath)))
    }
  }
}
