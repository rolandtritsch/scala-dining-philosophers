/*
 __________       .__                     .___
 \______   \ ____ |  | _____    ____    __| _/
 |       _//  _ \|  | \__  \  /    \  / __ |
 |    |   (  <_> )  |__/ __ \|   |  \/ /_/ |
 |____|_  /\____/|____(____  /___|  /\____ |
 \/                 \/     \/      \/
 Copyright (c), 2013, roland@tritsch.org
 http://www.tritsch.org
*/

package org.tritsch.scala.dpp

import com.typesafe.scalalogging.slf4j.Logging
import akka.actor.ActorLogging

import scala.concurrent.duration._

import akka.actor._

object Fork {
  sealed trait Message
  case class PickUp(byPhilosopher: ActorRef) extends Message
  case class PutDown(byPhilosopher: ActorRef) extends Message
}

class Fork extends Actor with ActorLogging {
  import context._

  import Fork._
  import Philosopher._

  def available: Receive = {
    case PickUp(byPhilosopher: ActorRef) => {
      log.debug(self.path + "/available: PickUp(%s)".format(byPhilosopher.path))
      become(taken(byPhilosopher))
      byPhilosopher ! GotOne(self)
    }
  }

  def taken(byPhilosopher: ActorRef): Receive = {
    case PickUp(byOtherPhilosopher) => {
      log.debug(self.path + "/taken: PickUp(%s)".format(byOtherPhilosopher.path))
      byOtherPhilosopher ! NopeCantHaveIt(self)
    }
    case PutDown(byPhilosopher) => {
      log.debug(self.path + "/taken: PutDown(%s)".format(byPhilosopher.path))
      become(available)
    }
  }

  def receive = available
}

object Philosopher {
  sealed trait Message
  case object FeelingHungryAgain extends Message
  case object ThanksIAmStuffedNow extends Message
  case class GotOne(fork: ActorRef) extends Message
  case class NopeCantHaveIt(fork: ActorRef) extends Message
  case object Die extends Message
}

class Philosopher(name: String, rightFork: ActorRef, leftFork: ActorRef, thinkTime: Int, eatTime: Int) extends Actor with ActorLogging {
  import context._

  import Fork._
  import Philosopher._

  def thinking: Receive = {
    case FeelingHungryAgain => {
      log.debug(self.path + "/thinking: FeelingHungryAgain")
      become(hungry)
      rightFork ! PickUp(self)
      leftFork ! PickUp(self)
    }
    case Die => {
      log.debug(self.path + "/thinking: Die")
      stop(self)
    }
  }

  def hungry: Receive = {
    case GotOne(`leftFork`) => {
      log.debug(self.path + "/hungry: GotOne(left - %s)".format(leftFork.path))
      become(waitingForSecondFork(rightFork, leftFork))
    }
    case GotOne(`rightFork`) => {
      log.debug(self.path + "/hungry: GotOne(right - %s)".format(rightFork.path))
      become(waitingForSecondFork(leftFork, rightFork))
    }
    case NopeCantHaveIt(fork) => {
      log.debug(self.path + "/hungry: NopeCantHaveIt(%s)".format(fork.path))
      become(notEvenGotTheFirstFork)
    }
  }

  def waitingForSecondFork(needThisFork: ActorRef, gotThatFork: ActorRef): Receive = {
    case GotOne(`needThisFork`) => {
      log.debug(self.path + "/waitingForSecondFork: GotOne(%s)".format(needThisFork.path))
      log.info("%s has picked up %s and %s and starts to eat ...".format(name, leftFork.path.name, rightFork.path.name))
      become(eating)
      system.scheduler.scheduleOnce(eatTime seconds, self, ThanksIAmStuffedNow)
    }
    case NopeCantHaveIt(fork) => {
      log.debug(self.path + "/waitingForSecondFork: NopeCantHaveIt(%s)".format(fork.path))
      become(thinking)
      gotThatFork ! PutDown(self)
      self ! FeelingHungryAgain
    }
  }

  def notEvenGotTheFirstFork: Receive = {
    case GotOne(fork) => {
      log.debug(self.path + "/notEvenGotTheFirstFork: GotOne(%s)".format(fork.path))
      become(thinking)
      fork ! PutDown(self)
      self ! FeelingHungryAgain
    }
    case NopeCantHaveIt(fork) => {
      log.debug(self.path + "/notEvenGotTheFirstFork: NopeCantHaveIt(%s)".format(fork.path))
      become(thinking)
      self ! FeelingHungryAgain
    }
  }

  def eating: Receive = {
    case ThanksIAmStuffedNow => {
      log.debug(self.path + "/eating: ThanksIAmStuffedNow")
      become(thinking)
      leftFork ! PutDown(self)
      rightFork ! PutDown(self)
      log.info("%s puts down his forks and starts to think again ...".format(name))
      system.scheduler.scheduleOnce(thinkTime seconds, self, FeelingHungryAgain)
    }
  }

  def receive = thinking
}

object DPP extends Logging {
  def main(args: Array[String]): Unit = {
    assert(args.length == 4, "Usage: DPP <numOfPhilosophers> <thinkTime> <eatTime> <runTime>")
    val numOfPhilosophers = args(0).toInt
    val thinkTime = args(1).toInt
    val eatTime = args(2).toInt
    val runTime = args(3).toInt
    assert(numOfPhilosophers >= 3 && numOfPhilosophers <= 1000, "# of philososphers in (3, 1000)")
    assert(thinkTime >= 0 && thinkTime <= 10, "thinkTime in (0, 10)")
    assert(eatTime >= 0 && eatTime <= 10, "eatTime in (0, 10)")
    assert(runTime >= 0 && runTime <= 600, "runTime in (0, 600)")

    run(numOfPhilosophers, thinkTime, eatTime, runTime)
  }

  private def run(numOfPhilosophers: Int, thinkTime: Int, eatTime: Int, runTime: Int): Unit = {
    val table = ActorSystem("table")

    val forks = for(i <- 0 until numOfPhilosophers) yield table.actorOf(Props[Fork], "Fork-" + i)
    val philosophers = (for(i <- 0 to numOfPhilosophers-2; name = "Philosopher-" + i) yield table.actorOf(Props(new Philosopher(name, forks(i), forks(i+1), thinkTime, eatTime)), name)) :+ table.actorOf(Props(new Philosopher("Philosopher-" + (numOfPhilosophers-1), forks(numOfPhilosophers-1), forks(0), thinkTime, eatTime)), "Philosopher-" + (numOfPhilosophers-1))

    philosophers.foreach(_ ! Philosopher.FeelingHungryAgain)
    Thread.sleep((runTime seconds).toMillis)

    // @todo Figure out a clean shutdown
    philosophers.foreach(_ ! Philosopher.Die)
    table.shutdown()
  }
}
