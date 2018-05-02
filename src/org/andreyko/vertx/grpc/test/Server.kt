package org.andreyko.vertx.grpc.test

import com.satori.libs.vertx.kotlin.*
import io.vertx.core.*
import io.vertx.core.dns.*
import io.vertx.grpc.*
import org.slf4j.*

object Server {
  val log = LoggerFactory.getLogger(javaClass)
  
  @JvmStatic
  fun main(vararg args: String) {
    val service = object : GtfsLookupGrpc.GtfsLookupVertxImplBase() {
      override fun ping(request: PingRequest, future: VxFuture<PingReply>) {
        future.complete(PingReply.newBuilder().apply {
          message = request.message
        }.build())
      }
    }
    
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
    
    val rpcServer = VertxServerBuilder
      .forPort(vertx, 8080)
      .addService(service)
      .build()
    
    rpcServer.start()
  }
}