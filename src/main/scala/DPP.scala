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

import akka.actor._

object ForkState extends Enumeration {
  type FState = Value
  val UP, DOWN = Value
}

object Fork {
  sealed trait State
  case object Up extends State
  case object Down extends State
}

class Fork extends Actor {
  import Fork._

  var state = ForkState.DOWN
  def receive = {
    case Up => state = ForkState.UP
    case Down => state = ForkState.DOWN
  }
}

object Philosopher {
  sealed trait Message
  case object PickRightForkUp extends Message
  case object PickLeftForkUp extends Message
  case object Eat extends Message
  case object PutRightForkDown extends Message
  case object PutLeftForkDown extends Message
}

class Philosopher(val rightFork: ActorRef, val leftFork: ActorRef) extends Actor with ActorLogging {
  import Fork._
  import Philosopher._

  val random = new scala.util.Random(System.currentTimeMillis)

  def receive = {
    case PickRightForkUp => {
      log.info("Picking the right fork")
      rightFork ! Up
      self ! PickLeftForkUp
    }
    case PickLeftForkUp => {
      log.info("Picking the left fork")
      leftFork ! Up
      self ! Eat
    }
    case Eat => {
      log.info("Eating ...")
      // Thread.sleep(1000 + random.nextInt(5000))
      self ! PutLeftForkDown
    }
    case PutLeftForkDown => {
      log.info("Put the left fork down")
      leftFork ! Down
      self ! PutRightForkDown
    }
    case PutRightForkDown => {
      log.info("Put the right fork down")
      rightFork ! Down
    }
  }
}

object DPP extends Logging {
  def main(args: Array[String]): Unit = {
    assert(args.length == 1, "Usage: DPP <numOfPhilosophers>")
    val numOfPhilosophers = args(0).toInt
    assert(numOfPhilosophers >= 3, "Need at least 3 philososphers")

    val table = ActorSystem("table")

    val forks = for(i <- 0 until numOfPhilosophers) yield table.actorOf(Props[Fork], "Fork-" + i)
    val philosophers = (for(i <- 0 until numOfPhilosophers-1) yield table.actorOf(Props(new Philosopher(forks(i), forks(i+1))), "Philosopher-" + i)) :+ table.actorOf(Props(new Philosopher(forks(numOfPhilosophers-1), forks(0))), "Philosopher-" + (numOfPhilosophers-1))

    philosophers.foreach(_ ! Philosopher.PickRightForkUp)

    table.awaitTermination()
  }
}
