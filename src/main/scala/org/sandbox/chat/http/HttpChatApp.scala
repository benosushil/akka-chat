package org.sandbox.chat.http

import org.sandbox.chat.ChatServer
import org.sandbox.chat.Settings
import org.sandbox.chat.sse.SseChatPublisher
import org.sandbox.chat.sse.SseChatService
import org.sandbox.chat.sse.SseChatServiceActions
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import org.sandbox.chat.ServiceActor

trait HttpChat {

  implicit val system = ActorSystem("chat-http")
  val settings = Settings(system)

  def onReady: Unit = {}

//  def shutdown = {
//    system.shutdown
//    system.awaitTermination
//  }

  val chatPublisher: ActorRef = system.actorOf(Props[SseChatPublisher])

  val chatServer = system.actorOf(ChatServer.props(chatPublisher), "ChuckNorris")
  waitForRunningService(chatServer)

  val sseChatService =
    system.actorOf(SseChatService.props(
        settings.sseService.interface, settings.sseService.port,
        chatPublisher))
  waitForRunningService(sseChatService)

  val chatServiceActions = new HttpChatServiceActionsImpl(chatServer, system)
//    new SseChatServiceActions(chatServer, chatPublisher, system)

  val httpChatService =
    system.actorOf(HttpChatService.props(
        settings.httpService.interface, settings.httpService.port,
        chatServer, chatServiceActions))
  waitForRunningService(httpChatService)

  system.log.info(s"HttpChatApp with ActorSystem ${system.name} started")
  system.registerOnTermination(system.log.info(s"ActorSystem ${system.name} shutting down ..."))

  onReady

  system.awaitTermination

  private def waitForRunningService(service: ActorRef) = {
    val status = ServiceActor.getStatus(service)
    require(status == ServiceActor.StatusRunning)
  }
}

object HttpChatApp extends App with HttpChat
