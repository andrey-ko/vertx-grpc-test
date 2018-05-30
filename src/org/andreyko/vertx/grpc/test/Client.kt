package org.andreyko.vertx.grpc.test

import com.satori.libs.async.kotlin.*
import com.satori.libs.vertx.kotlin.*
import io.vertx.core.*
import io.vertx.core.dns.*
import io.vertx.grpc.*
import org.slf4j.*
import kotlin.coroutines.experimental.intrinsics.*

object Client {
  val log = LoggerFactory.getLogger(javaClass)
  
  inline suspend fun <reified T> vxSuspend(crossinline block: (VxAsyncHandler<T>) -> Unit): T {
    return suspendCoroutineOrReturn { cont ->
      block(object : VxAsyncHandler<T> {
        var asyncResult: VxAsyncResult<T>? = null
        override fun handle(event: VxAsyncResult<T>) {
          if (asyncResult != null) {
            log.error("already completed", Exception("already completed"))
            return
          }
          asyncResult = event
          if (!event.succeeded()) {
            cont.resumeWithException(event.cause())
          } else {
            try {
              cont.resume(event.result())
            } catch (ex: Throwable) {
              log.error("failed to resume coroutine", ex)
            }
          }
        }
      })
      return@suspendCoroutineOrReturn COROUTINE_SUSPENDED
    }
  }
  
  @JvmStatic
  fun main(vararg args: String) {
    
    val vertx = Vertx.vertx(VertxOptions().apply {
      eventLoopPoolSize = 1
      addressResolverOptions = AddressResolverOptions().apply {
        /*
          sometimes JDNI returns dns servers for inactive interfaces,
          those historical dns servers may be invalid and it may cause
          failures to resolve name
        */
        maxQueries = 16
      }
    })
    vertx.exceptionHandler { ex ->
      log.error("unhandled exception", ex)
      vertx.close()
    }
    
    val channel = VertxChannelBuilder
      .forAddress(vertx, "localhost", 9090)
      .usePlaintext(true)
      .build()
    
    val stub = GtfsLookupGrpc.newVertxStub(channel)
    sync {
      val reply = vxSuspend<PingReply> { ah ->
        stub.ping(PingRequest.newBuilder().apply {
          message = "hello world!"
        }.build(), ah)
      }
      println("server reply: '${reply.message}'")
    }
    println("done")
    vertx.close()
  }
}