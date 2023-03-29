package dev.training

import dev.training.UserDb.UserDbEnv
import dev.training.UserEmailer.UserEmailerEnv
import zio._
import zio.ZIO

// domain
case class User(name: String, message: String)

// SERVICE 1
object UserEmailer {
  type UserEmailerEnv = Has[UserEmailer.Service]
  trait Service {
    def sendEmail(user: User): Task[Unit]
  }
  // impl
  val live: ZLayer[Any, Nothing, UserEmailerEnv] = ZLayer.succeed({
    new Service {
      override def sendEmail(user: User): Task[Unit] = Task {
        println(s"Sending ${user.message} to ${user.name}")
      }
    }
  })
  // api (accessor)
  def sendEmail(user: User): ZIO[UserEmailerEnv, Throwable, Unit] =
    ZIO.accessM(_.get.sendEmail(user))
}

// SERVICE 2
object UserDb {
  type UserDbEnv = Has[UserDb.Service]
  trait Service {
    def insert(user: User): Task[Unit]
  }
  // impl
  val live: ZLayer[Any, Nothing, UserDbEnv] = ZLayer.succeed({
    new Service {
      override def insert(user: User): Task[Unit] = Task {
        println(s"Inserting ${user.message} of ${user.name} into DB")
      }
    }
  })
  // api (accessor)
  def insert(user: User): ZIO[UserDbEnv, Throwable, Unit] =
    ZIO.accessM(_.get.insert(user))
}

// SERVICE 3
object UserSubscription {
  type UserSubscriptionEnv = Has[UserSubscription.Service]
  class Service(emailer: UserEmailer.Service, db: UserDb.Service) { // no need for abstraction
    def subscribe(user: User): Task[User] =
      for {
        _ <- db.insert(user)
        _ <- emailer.sendEmail(user)
      } yield user
  }
  // impl
  val live: ZLayer[UserEmailerEnv with UserDbEnv, Nothing, UserSubscriptionEnv] =
    ZLayer.fromServices[UserEmailer.Service, UserDb.Service, UserSubscription.Service]( (emailer, db) =>
      new Service(emailer, db)
    )
  // api
  def subscribe(user: User): ZIO[UserSubscriptionEnv, Throwable, User] =
    ZIO.accessM(_.get.subscribe(user))
}

object YoutubeRJVM extends zio.App {
  // COMBINING
  import UserEmailer._
  import UserDb._
  import UserSubscription._
  val horizintalLayer: ZLayer[Any, Nothing, UserEmailerEnv with UserDbEnv] =
    UserDb.live ++ UserEmailer.live // horizontal
  val subscriptionLayer: ZLayer[Any, Throwable, UserSubscriptionEnv] =
    horizintalLayer >>> UserSubscription.live

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
// for only horizontal
//    UserEmailer
//      .sendEmail(User("man", "message"))
//      .provideLayer(horizintalLayer)
//      .exitCode
    UserSubscription
      .subscribe(User("man", "message"))
      .provideLayer(subscriptionLayer)
      .map { user =>
        println(s"Regstered user: $user")
      }
      .exitCode
}
